package com.github.msvidal.transactionauthorizer.application.usecase;

import com.github.msvidal.transactionauthorizer.domain.exception.InsufficientBalanceException;
import com.github.msvidal.transactionauthorizer.domain.model.Transaction;
import com.github.msvidal.transactionauthorizer.domain.model.TransactionResult;
import com.github.msvidal.transactionauthorizer.domain.service.TransactionStrategyFactory;
import com.github.msvidal.transactionauthorizer.domain.repository.AccountRepository;
import com.github.msvidal.transactionauthorizer.domain.repository.TransactionRepository;
import lombok.AllArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @deprecated Use {@link AuthorizeTransactionUseCase} and {@link CaptureTransactionUseCase} 
 * for the new Auth & Capture asynchronous pattern. This class is maintained for backward 
 * compatibility with existing synchronous flows.
 */
@Deprecated
@Service
@AllArgsConstructor
public class ProcessTransactionUseCase {

    private final TransactionStrategyFactory strategyFactory;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    @Retryable(
            value = {ObjectOptimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 50, multiplier = 2)
    )
    public TransactionResult execute(Transaction transaction) {
        var accountOpt = accountRepository.findById(transaction.accountId());

        if (accountOpt.isEmpty()) {
            var failedTransaction = transactionRepository.save(transaction.markFailed());
            return new TransactionResult(failedTransaction, null);
        }

        var account = accountOpt.get();

        var transactionOpt = transactionRepository.findById(transaction.id());

        if (!transactionOpt.isEmpty()) {
            return new TransactionResult(transactionOpt.get(), account);
        }

        try {
            var accountUpdated = strategyFactory
                    .get(transaction.operation())
                    .execute(account, transaction.amount());

            var accountSaved = accountRepository.update(accountUpdated);
            var transactionSaved = transactionRepository.save(transaction.markSucceeded());

            return new TransactionResult(transactionSaved, accountSaved);
        } catch (InsufficientBalanceException e) {
            var failedTransaction = transactionRepository.save(transaction.markFailed());
            return new TransactionResult(failedTransaction, account);
        }
    }
}
