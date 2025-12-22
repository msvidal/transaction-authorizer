package com.github.msvidal.transactionauthorizer.application.usecase;

import com.github.msvidal.transactionauthorizer.domain.exception.InsufficientBalanceException;
import com.github.msvidal.transactionauthorizer.domain.model.*;
import com.github.msvidal.transactionauthorizer.domain.service.TransactionOperation;
import com.github.msvidal.transactionauthorizer.domain.service.TransactionStrategyFactory;
import com.github.msvidal.transactionauthorizer.domain.repository.AccountRepository;
import com.github.msvidal.transactionauthorizer.domain.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessTransactionUseCaseTest {

    @Mock
    private TransactionStrategyFactory strategyFactory;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionOperation transactionOperation;

    @InjectMocks
    private ProcessTransactionUseCase processTransactionUseCase;

    @Test
    void shouldProcessTransactionSuccessfullyWhenAccountExistsAndBalanceIsSufficient() {
        Transaction transaction = getTransaction(new BigDecimal("10.00"), TransactionType.DEBIT);
        Account account = getAccount(new BigDecimal("100.00"));
        Account updatedAccount = getAccount(new BigDecimal("90.00"));
        Transaction succeededTransaction = transaction.markSucceeded();

        when(accountRepository.findById(transaction.accountId())).thenReturn(Optional.of(account));
        when(transactionRepository.findById(transaction.id())).thenReturn(Optional.empty());
        when(strategyFactory.get(TransactionType.DEBIT)).thenReturn(transactionOperation);
        when(transactionOperation.execute(account, transaction.amount())).thenReturn(updatedAccount);
        when(accountRepository.update(updatedAccount)).thenReturn(updatedAccount);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(succeededTransaction);

        TransactionResult result = processTransactionUseCase.execute(transaction);

        assertThat(result.transaction()).isEqualTo(succeededTransaction);
        assertThat(result.account()).isEqualTo(updatedAccount);
        verify(accountRepository).update(updatedAccount);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void shouldMarkTransactionAsFailedWhenAccountDoesNotExist() {
        Transaction transaction = getTransaction(new BigDecimal("100.00"), TransactionType.DEBIT);
        Transaction failedTransaction = transaction.markFailed();

        when(accountRepository.findById(transaction.accountId())).thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenReturn(failedTransaction);

        TransactionResult result = processTransactionUseCase.execute(transaction);

        assertThat(result.transaction()).isEqualTo(failedTransaction);
        assertThat(result.account()).isNull();
        verify(transactionRepository).save(any(Transaction.class));
        verify(strategyFactory, never()).get(any(TransactionType.class));
        verify(accountRepository, never()).update(any());
    }

    @Test
    void shouldReturnExistingTransactionWhenTransactionAlreadyExists() {
        Transaction existingTransaction = getTransaction(new BigDecimal("10.00"), TransactionType.DEBIT)
                .markSucceeded();
        Transaction transaction = getTransaction(new BigDecimal("10.00"), TransactionType.DEBIT);
        Account account = getAccount(new BigDecimal("100.00"));

        when(accountRepository.findById(transaction.accountId())).thenReturn(Optional.of(account));
        when(transactionRepository.findById(transaction.id())).thenReturn(Optional.of(existingTransaction));

        TransactionResult result = processTransactionUseCase.execute(transaction);

        assertThat(result.transaction()).isEqualTo(existingTransaction);
        assertThat(result.account()).isEqualTo(account);
        verify(strategyFactory, never()).get(any(TransactionType.class));
        verify(accountRepository, never()).update(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void shouldMarkTransactionAsFailedWhenInsufficientBalanceExceptionIsThrown() {
        Transaction transaction = getTransaction(new BigDecimal("200.00"), TransactionType.DEBIT);
        Account account = getAccount(new BigDecimal("100.00"));
        Transaction failedTransaction = transaction.markFailed();

        when(accountRepository.findById(transaction.accountId())).thenReturn(Optional.of(account));
        when(transactionRepository.findById(transaction.id())).thenReturn(Optional.empty());
        when(strategyFactory.get(TransactionType.DEBIT)).thenReturn(transactionOperation);
        when(transactionOperation.execute(account, transaction.amount()))
                .thenThrow(new InsufficientBalanceException("Saldo insuficiente"));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(failedTransaction);

        TransactionResult result = processTransactionUseCase.execute(transaction);

        assertThat(result.transaction()).isEqualTo(failedTransaction);
        assertThat(result.account()).isEqualTo(account);
        verify(accountRepository, never()).update(any());
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void shouldProcessCreditTransactionSuccessfully() {
        Transaction transaction = getTransaction(new BigDecimal("50.00"), TransactionType.CREDIT);
        Account account = getAccount(new BigDecimal("100.00"));
        Account updatedAccount = getAccount(new BigDecimal("150.00"));
        Transaction succeededTransaction = transaction.markSucceeded();

        when(accountRepository.findById(transaction.accountId())).thenReturn(Optional.of(account));
        when(transactionRepository.findById(transaction.id())).thenReturn(Optional.empty());
        when(strategyFactory.get(TransactionType.CREDIT)).thenReturn(transactionOperation);
        when(transactionOperation.execute(account, transaction.amount())).thenReturn(updatedAccount);
        when(accountRepository.update(updatedAccount)).thenReturn(updatedAccount);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(succeededTransaction);

        TransactionResult result = processTransactionUseCase.execute(transaction);

        assertThat(result.transaction()).isEqualTo(succeededTransaction);
        assertThat(result.account()).isEqualTo(updatedAccount);
        verify(accountRepository).update(updatedAccount);
        verify(transactionRepository).save(any(Transaction.class));
    }

    private static Account getAccount(BigDecimal initialBalance) {
        return new Account(
                UUID.randomUUID(),
                UUID.randomUUID(),
                org.javamoney.moneta.Money.of(initialBalance, "BRL"),
                OffsetDateTime.now(),
                "ENABLED"
        );
    }

    private static Transaction getTransaction(BigDecimal amount, TransactionType transactionType) {
        return new Transaction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                org.javamoney.moneta.Money.of(amount, "BRL"),
                transactionType,
                null,
                OffsetDateTime.now()
        );
    }
}
