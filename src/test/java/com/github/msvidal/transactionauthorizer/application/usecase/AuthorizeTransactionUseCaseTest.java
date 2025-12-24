package com.github.msvidal.transactionauthorizer.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.msvidal.transactionauthorizer.domain.model.*;
import com.github.msvidal.transactionauthorizer.domain.repository.AccountRepository;
import com.github.msvidal.transactionauthorizer.domain.repository.TransactionRepository;
import com.github.msvidal.transactionauthorizer.infra.messaging.dto.TransactionCaptureMessage;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorizeTransactionUseCaseTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private SqsTemplate sqsTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AuthorizeTransactionUseCase authorizeTransactionUseCase;

    @Test
    void shouldAuthorizeTransactionWhenAccountExistsAndBalanceIsSufficient() throws Exception {
        // Given
        ReflectionTestUtils.setField(authorizeTransactionUseCase, "captureQueueName", "transaction-capture-queue");
        
        Transaction transaction = getTransaction(new BigDecimal("10.00"), TransactionType.DEBIT);
        Account account = getAccount(new BigDecimal("100.00"));
        Transaction authorizedTransaction = transaction.markAuthorized();

        when(transactionRepository.findById(transaction.id())).thenReturn(Optional.empty());
        when(accountRepository.findById(transaction.accountId())).thenReturn(Optional.of(account));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(authorizedTransaction);
        when(objectMapper.writeValueAsString(any(TransactionCaptureMessage.class)))
                .thenReturn("{\"transactionId\":\"" + transaction.id() + "\"}");

        // When
        TransactionResult result = authorizeTransactionUseCase.execute(transaction);

        // Then
        assertThat(result.transaction().status()).isEqualTo(Status.AUTHORIZED);
        assertThat(result.account()).isEqualTo(account);
        verify(transactionRepository).save(any(Transaction.class));
        verify(sqsTemplate).send(eq("transaction-capture-queue"), anyString());
    }

    @Test
    void shouldMarkTransactionAsFailedWhenBalanceIsInsufficient() {
        // Given
        ReflectionTestUtils.setField(authorizeTransactionUseCase, "captureQueueName", "transaction-capture-queue");
        
        Transaction transaction = getTransaction(new BigDecimal("200.00"), TransactionType.DEBIT);
        Account account = getAccount(new BigDecimal("100.00"));
        Transaction failedTransaction = transaction.markFailed();

        when(transactionRepository.findById(transaction.id())).thenReturn(Optional.empty());
        when(accountRepository.findById(transaction.accountId())).thenReturn(Optional.of(account));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(failedTransaction);

        // When
        TransactionResult result = authorizeTransactionUseCase.execute(transaction);

        // Then
        assertThat(result.transaction().status()).isEqualTo(Status.FAILED);
        assertThat(result.account()).isEqualTo(account);
        verify(transactionRepository).save(any(Transaction.class));
        verify(sqsTemplate, never()).send(anyString(), anyString());
    }

    @Test
    void shouldReturnExistingTransactionWhenAlreadyExists() {
        // Given
        Transaction existingTransaction = getTransaction(new BigDecimal("10.00"), TransactionType.DEBIT)
                .markAuthorized();
        Account account = getAccount(new BigDecimal("100.00"));

        when(transactionRepository.findById(existingTransaction.id())).thenReturn(Optional.of(existingTransaction));
        when(accountRepository.findById(existingTransaction.accountId())).thenReturn(Optional.of(account));

        // When
        TransactionResult result = authorizeTransactionUseCase.execute(existingTransaction);

        // Then
        assertThat(result.transaction()).isEqualTo(existingTransaction);
        assertThat(result.account()).isEqualTo(account);
        verify(transactionRepository, never()).save(any());
        verify(sqsTemplate, never()).send(anyString(), anyString());
    }

    @Test
    void shouldMarkTransactionAsFailedWhenAccountDoesNotExist() {
        // Given
        Transaction transaction = getTransaction(new BigDecimal("10.00"), TransactionType.DEBIT);
        Transaction failedTransaction = transaction.markFailed();

        when(transactionRepository.findById(transaction.id())).thenReturn(Optional.empty());
        when(accountRepository.findById(transaction.accountId())).thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenReturn(failedTransaction);

        // When
        TransactionResult result = authorizeTransactionUseCase.execute(transaction);

        // Then
        assertThat(result.transaction().status()).isEqualTo(Status.FAILED);
        assertThat(result.account()).isNull();
        verify(transactionRepository).save(any(Transaction.class));
        verify(sqsTemplate, never()).send(anyString(), anyString());
    }

    @Test
    void shouldAuthorizeCreditTransactionWithoutBalanceCheck() throws Exception {
        // Given
        ReflectionTestUtils.setField(authorizeTransactionUseCase, "captureQueueName", "transaction-capture-queue");
        
        Transaction transaction = getTransaction(new BigDecimal("50.00"), TransactionType.CREDIT);
        Account account = getAccount(new BigDecimal("10.00"));
        Transaction authorizedTransaction = transaction.markAuthorized();

        when(transactionRepository.findById(transaction.id())).thenReturn(Optional.empty());
        when(accountRepository.findById(transaction.accountId())).thenReturn(Optional.of(account));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(authorizedTransaction);
        when(objectMapper.writeValueAsString(any(TransactionCaptureMessage.class)))
                .thenReturn("{\"transactionId\":\"" + transaction.id() + "\"}");

        // When
        TransactionResult result = authorizeTransactionUseCase.execute(transaction);

        // Then
        assertThat(result.transaction().status()).isEqualTo(Status.AUTHORIZED);
        verify(transactionRepository).save(any(Transaction.class));
        verify(sqsTemplate).send(eq("transaction-capture-queue"), anyString());
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
