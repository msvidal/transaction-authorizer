package com.github.msvidal.transactionauthorizer.application.usecase;

import com.github.msvidal.transactionauthorizer.domain.exception.InsufficientBalanceException;
import com.github.msvidal.transactionauthorizer.domain.model.*;
import com.github.msvidal.transactionauthorizer.domain.repository.AccountRepository;
import com.github.msvidal.transactionauthorizer.domain.repository.TransactionRepository;
import com.github.msvidal.transactionauthorizer.domain.service.TransactionStrategyFactory;
import com.github.msvidal.transactionauthorizer.domain.service.WebhookNotificationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Capture phase of the Auth & Capture pattern.
 * This use case executes the actual balance update asynchronously after authorization.
 */
@Service
@AllArgsConstructor
@Slf4j
public class CaptureTransactionUseCase {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final TransactionStrategyFactory strategyFactory;
    private final WebhookNotificationService webhookNotificationService;

    @Transactional
    @Retryable(
            value = {ObjectOptimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 50, multiplier = 2)
    )
    public void execute(UUID transactionId) {
        log.info("Capturing transaction: {}", transactionId);

        // Fetch transaction
        var transactionOpt = transactionRepository.findById(transactionId);
        if (transactionOpt.isEmpty()) {
            log.error("Transaction not found for capture: {}", transactionId);
            return;
        }

        var transaction = transactionOpt.get();

        // Skip if already captured or in final state
        if (transaction.status() == Status.CAPTURED || 
            transaction.status() == Status.SETTLED ||
            transaction.status() == Status.FAILED) {
            log.info("Transaction already in final state: {} - {}", transactionId, transaction.status());
            return;
        }

        // Fetch account
        var accountOpt = accountRepository.findById(transaction.accountId());
        if (accountOpt.isEmpty()) {
            log.error("Account not found for capture: {}", transaction.accountId());
            var failedTransaction = transactionRepository.save(transaction.markFailed());
            sendWebhook(failedTransaction, null, "transaction.failed");
            return;
        }

        var account = accountOpt.get();

        try {
            // Execute the operation (debit or credit)
            var updatedAccount = strategyFactory
                    .get(transaction.operation())
                    .execute(account, transaction.amount());

            // Save updated account and mark transaction as captured
            accountRepository.update(updatedAccount);
            var capturedTransaction = transactionRepository.save(transaction.markCaptured());

            log.info("Transaction captured successfully: {}", transactionId);

            // Send webhook notification
            sendWebhook(capturedTransaction, updatedAccount, "transaction.captured");

        } catch (InsufficientBalanceException e) {
            log.error("Insufficient balance during capture for transaction: {}", transactionId, e);
            var failedTransaction = transactionRepository.save(transaction.markFailed());
            sendWebhook(failedTransaction, account, "transaction.failed");
        } catch (Exception e) {
            log.error("Unexpected error during capture for transaction: {}", transactionId, e);
            var failedTransaction = transactionRepository.save(transaction.markFailed());
            sendWebhook(failedTransaction, account, "transaction.failed");
        }
    }

    private void sendWebhook(Transaction transaction, Account account, String eventType) {
        try {
            WebhookEvent event = new WebhookEvent(
                    UUID.randomUUID(),
                    eventType,
                    transaction.id(),
                    transaction.accountId(),
                    transaction.status(),
                    OffsetDateTime.now(),
                    Map.of(
                            "amount", transaction.amount().value(),
                            "currency", transaction.amount().currency().getCurrencyCode(),
                            "operation", transaction.operation().name()
                    )
            );
            webhookNotificationService.notify(event);
        } catch (Exception e) {
            // Webhook failures should not fail the transaction
            log.error("Failed to send webhook for transaction: {}", transaction.id(), e);
        }
    }
}
