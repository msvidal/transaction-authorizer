package com.github.msvidal.transactionauthorizer.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.msvidal.transactionauthorizer.domain.exception.InsufficientBalanceException;
import com.github.msvidal.transactionauthorizer.domain.model.Transaction;
import com.github.msvidal.transactionauthorizer.domain.model.TransactionResult;
import com.github.msvidal.transactionauthorizer.domain.model.TransactionType;
import com.github.msvidal.transactionauthorizer.domain.repository.AccountRepository;
import com.github.msvidal.transactionauthorizer.domain.repository.TransactionRepository;
import com.github.msvidal.transactionauthorizer.infra.messaging.dto.TransactionCaptureMessage;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authorization phase of the Auth & Capture pattern.
 * This use case performs fast validation and reserves balance without executing the transaction.
 * The actual balance update happens asynchronously in the capture phase.
 */
@Service
@AllArgsConstructor
@Slf4j
public class AuthorizeTransactionUseCase {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final SqsTemplate sqsTemplate;
    private final ObjectMapper objectMapper;

    @Value("${aws.sqs.capture-queue-name:transaction-capture-queue}")
    private String captureQueueName;

    @Transactional
    public TransactionResult execute(Transaction transaction) {
        log.info("Authorizing transaction: {}", transaction.id());

        // Check idempotency - if transaction already exists, return it
        var existingTransaction = transactionRepository.findById(transaction.id());
        if (existingTransaction.isPresent()) {
            log.info("Transaction already exists (idempotent): {}", transaction.id());
            var account = accountRepository.findById(transaction.accountId()).orElse(null);
            return new TransactionResult(existingTransaction.get(), account);
        }

        // Validate account exists
        var accountOpt = accountRepository.findById(transaction.accountId());
        if (accountOpt.isEmpty()) {
            log.warn("Account not found: {}", transaction.accountId());
            var failedTransaction = transactionRepository.save(transaction.markFailed());
            return new TransactionResult(failedTransaction, null);
        }

        var account = accountOpt.get();

        // For DEBIT transactions, verify sufficient balance (authorization check only)
        if (transaction.operation() == TransactionType.DEBIT) {
            if (!account.hasSufficientBalance(transaction.amount().value())) {
                log.warn("Insufficient balance for transaction: {}", transaction.id());
                var failedTransaction = transactionRepository.save(transaction.markFailed());
                return new TransactionResult(failedTransaction, account);
            }
        }

        // Save transaction with AUTHORIZED status
        var authorizedTransaction = transactionRepository.save(transaction.markAuthorized());
        log.info("Transaction authorized: {}", authorizedTransaction.id());

        // Enqueue for asynchronous capture
        try {
            enqueueForCapture(authorizedTransaction);
            log.info("Transaction enqueued for capture: {}", authorizedTransaction.id());
        } catch (Exception e) {
            log.error("Failed to enqueue transaction for capture: {}", authorizedTransaction.id(), e);
            // Transaction is already saved as AUTHORIZED, so we return it
            // The capture queue will have retry mechanisms
        }

        return new TransactionResult(authorizedTransaction, account);
    }

    private void enqueueForCapture(Transaction transaction) {
        try {
            TransactionCaptureMessage message = new TransactionCaptureMessage(transaction.id());
            String messageBody = objectMapper.writeValueAsString(message);
            sqsTemplate.send(captureQueueName, messageBody);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize capture message", e);
        }
    }
}
