package com.github.msvidal.transactionauthorizer.application.usecase;

import com.github.msvidal.transactionauthorizer.domain.exception.InsufficientBalanceException;
import com.github.msvidal.transactionauthorizer.domain.model.Transaction;
import com.github.msvidal.transactionauthorizer.domain.model.TransactionResult;
import com.github.msvidal.transactionauthorizer.domain.service.TransactionStrategyFactory;
import com.github.msvidal.transactionauthorizer.domain.repository.AccountRepository;
import com.github.msvidal.transactionauthorizer.domain.repository.TransactionRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.AllArgsConstructor;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class ProcessTransactionUseCase {

    private final TransactionStrategyFactory strategyFactory;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    @Retryable(includes = OptimisticLockException.class)
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
