package com.github.msvidal.transactionauthorizer.application.usecase;

import com.github.msvidal.transactionauthorizer.domain.exception.InsufficientBalanceException;
import com.github.msvidal.transactionauthorizer.domain.model.*;
import com.github.msvidal.transactionauthorizer.domain.repository.AccountRepository;
import com.github.msvidal.transactionauthorizer.domain.repository.TransactionRepository;
import com.github.msvidal.transactionauthorizer.domain.service.TransactionOperation;
import com.github.msvidal.transactionauthorizer.domain.service.TransactionStrategyFactory;
import com.github.msvidal.transactionauthorizer.domain.service.WebhookNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class CaptureTransactionUseCaseTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionStrategyFactory strategyFactory;

    @Mock
    private WebhookNotificationService webhookNotificationService;

    @Mock
    private TransactionOperation transactionOperation;

    @InjectMocks
    private CaptureTransactionUseCase captureTransactionUseCase;

    @Test
    void shouldCaptureAuthorizedTransaction() {
        // Given
        UUID transactionId = UUID.randomUUID();
        Transaction authorizedTransaction = getTransaction(new BigDecimal("10.00"), TransactionType.DEBIT)
                .markAuthorized();
        Account account = getAccount(new BigDecimal("100.00"));
        Account updatedAccount = getAccount(new BigDecimal("90.00"));
        Transaction capturedTransaction = authorizedTransaction.markCaptured();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(authorizedTransaction));
        when(accountRepository.findById(authorizedTransaction.accountId())).thenReturn(Optional.of(account));
        when(strategyFactory.get(TransactionType.DEBIT)).thenReturn(transactionOperation);
        when(transactionOperation.execute(account, authorizedTransaction.amount())).thenReturn(updatedAccount);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(capturedTransaction);
        when(accountRepository.update(updatedAccount)).thenReturn(updatedAccount);

        // When
        captureTransactionUseCase.execute(transactionId);

        // Then
        verify(accountRepository).update(updatedAccount);
        verify(transactionRepository).save(any(Transaction.class));
        
        ArgumentCaptor<WebhookEvent> webhookCaptor = ArgumentCaptor.forClass(WebhookEvent.class);
        verify(webhookNotificationService).notify(webhookCaptor.capture());
        
        WebhookEvent capturedEvent = webhookCaptor.getValue();
        assertThat(capturedEvent.eventType()).isEqualTo("transaction.captured");
        assertThat(capturedEvent.transactionId()).isEqualTo(authorizedTransaction.id());
        assertThat(capturedEvent.status()).isEqualTo(Status.CAPTURED);
    }

    @Test
    void shouldMarkTransactionAsFailedWhenInsufficientBalance() {
        // Given
        UUID transactionId = UUID.randomUUID();
        Transaction authorizedTransaction = getTransaction(new BigDecimal("200.00"), TransactionType.DEBIT)
                .markAuthorized();
        Account account = getAccount(new BigDecimal("100.00"));
        Transaction failedTransaction = authorizedTransaction.markFailed();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(authorizedTransaction));
        when(accountRepository.findById(authorizedTransaction.accountId())).thenReturn(Optional.of(account));
        when(strategyFactory.get(TransactionType.DEBIT)).thenReturn(transactionOperation);
        when(transactionOperation.execute(account, authorizedTransaction.amount()))
                .thenThrow(new InsufficientBalanceException("Insufficient balance"));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(failedTransaction);

        // When
        captureTransactionUseCase.execute(transactionId);

        // Then
        verify(accountRepository, never()).update(any());
        verify(transactionRepository).save(any(Transaction.class));
        
        ArgumentCaptor<WebhookEvent> webhookCaptor = ArgumentCaptor.forClass(WebhookEvent.class);
        verify(webhookNotificationService).notify(webhookCaptor.capture());
        
        WebhookEvent capturedEvent = webhookCaptor.getValue();
        assertThat(capturedEvent.eventType()).isEqualTo("transaction.failed");
    }

    @Test
    void shouldSkipCaptureForAlreadyCapturedTransaction() {
        // Given
        UUID transactionId = UUID.randomUUID();
        Transaction capturedTransaction = getTransaction(new BigDecimal("10.00"), TransactionType.DEBIT)
                .markCaptured();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(capturedTransaction));

        // When
        captureTransactionUseCase.execute(transactionId);

        // Then
        verify(accountRepository, never()).findById(any());
        verify(strategyFactory, never()).get(any());
        verify(accountRepository, never()).update(any());
        verify(transactionRepository, never()).save(any());
        verify(webhookNotificationService, never()).notify(any());
    }

    @Test
    void shouldHandleTransactionNotFound() {
        // Given
        UUID transactionId = UUID.randomUUID();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

        // When
        captureTransactionUseCase.execute(transactionId);

        // Then
        verify(accountRepository, never()).findById(any());
        verify(strategyFactory, never()).get(any());
        verify(accountRepository, never()).update(any());
        verify(transactionRepository, never()).save(any());
        verify(webhookNotificationService, never()).notify(any());
    }

    @Test
    void shouldHandleAccountNotFound() {
        // Given
        UUID transactionId = UUID.randomUUID();
        Transaction authorizedTransaction = getTransaction(new BigDecimal("10.00"), TransactionType.DEBIT)
                .markAuthorized();
        Transaction failedTransaction = authorizedTransaction.markFailed();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(authorizedTransaction));
        when(accountRepository.findById(authorizedTransaction.accountId())).thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenReturn(failedTransaction);

        // When
        captureTransactionUseCase.execute(transactionId);

        // Then
        verify(strategyFactory, never()).get(any());
        verify(accountRepository, never()).update(any());
        verify(transactionRepository).save(any(Transaction.class));
        verify(webhookNotificationService).notify(any());
    }

    private static Account getAccount(BigDecimal initialBalance) {
        return new Account(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new MonetaryAmount(initialBalance, Currency.getInstance("BRL")),
                OffsetDateTime.now(),
                "ENABLED"
        );
    }

    private static Transaction getTransaction(BigDecimal amount, TransactionType transactionType) {
        return new Transaction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new MonetaryAmount(amount, Currency.getInstance("BRL")),
                transactionType,
                null,
                OffsetDateTime.now()
        );
    }
}
