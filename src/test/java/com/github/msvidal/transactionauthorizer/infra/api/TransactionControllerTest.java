package com.github.msvidal.transactionauthorizer.infra.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.msvidal.transactionauthorizer.application.usecase.ProcessTransactionUseCase;
import com.github.msvidal.transactionauthorizer.domain.model.*;
import com.github.msvidal.transactionauthorizer.infra.api.dto.TransactionAmountRequest;
import com.github.msvidal.transactionauthorizer.infra.api.dto.TransactionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Currency;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private ProcessTransactionUseCase processTransactionUseCase;

    @InjectMocks
    private TransactionController transactionController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        mockMvc = MockMvcBuilders.standaloneSetup(transactionController).build();
    }

    @Test
    void shouldProcessDebitTransactionSuccessfully() throws Exception {
        UUID transactionId = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");
        UUID accountId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

        TransactionRequest request = new TransactionRequest(
                accountId,
                new TransactionAmountRequest(new BigDecimal("50.00"), "BRL"),
                "DEBIT"
        );

        Account account = new Account(
                accountId,
                UUID.randomUUID(),
                org.javamoney.moneta.Money.of(new BigDecimal("950.00"), "BRL"),
                OffsetDateTime.now(),
                "ENABLED"
        );

        Transaction transaction = new Transaction(
                transactionId,
                accountId,
                org.javamoney.moneta.Money.of(new BigDecimal("50.00"), "BRL"),
                TransactionType.DEBIT,
                Status.SUCCEEDED,
                OffsetDateTime.now()
        );

        TransactionResult result = new TransactionResult(transaction, account);

        when(processTransactionUseCase.execute(any(Transaction.class))).thenReturn(result);

        mockMvc.perform(post("/transactions/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transaction.id").value(transactionId.toString()))
                .andExpect(jsonPath("$.transaction.type").value("DEBIT"))
                .andExpect(jsonPath("$.transaction.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.transaction.amount.amount").value(50.00))
                .andExpect(jsonPath("$.transaction.amount.currency").value("BRL"))
                .andExpect(jsonPath("$.account.id").value(accountId.toString()))
                .andExpect(jsonPath("$.account.balance.amount").value(950.00))
                .andExpect(jsonPath("$.account.balance.currency").value("BRL"));

        verify(processTransactionUseCase, times(1)).execute(any(Transaction.class));
    }

    @Test
    void shouldProcessCreditTransactionSuccessfully() throws Exception {
        UUID transactionId = UUID.randomUUID();
        UUID accountId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

        TransactionRequest request = new TransactionRequest(
                accountId,
                new TransactionAmountRequest(new BigDecimal("200.00"), "BRL"),
                "CREDIT"
        );

        Account account = new Account(
                accountId,
                UUID.randomUUID(),
                org.javamoney.moneta.Money.of(new BigDecimal("1200.00"), "BRL"),
                OffsetDateTime.now(),
                "ENABLED"
        );

        Transaction transaction = new Transaction(
                transactionId,
                accountId,
                org.javamoney.moneta.Money.of(new BigDecimal("200.00"), "BRL"),
                TransactionType.CREDIT,
                Status.SUCCEEDED,
                OffsetDateTime.now()
        );

        TransactionResult result = new TransactionResult(transaction, account);

        when(processTransactionUseCase.execute(any(Transaction.class))).thenReturn(result);

        mockMvc.perform(post("/transactions/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transaction.type").value("CREDIT"))
                .andExpect(jsonPath("$.transaction.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.account.balance.amount").value(1200.00));

        verify(processTransactionUseCase, times(1)).execute(any(Transaction.class));
    }

    @Test
    void shouldReturnFailedTransactionWhenAccountNotFound() throws Exception {
        UUID transactionId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        TransactionRequest request = new TransactionRequest(
                accountId,
                new TransactionAmountRequest(new BigDecimal("100.00"), "BRL"),
                "DEBIT"
        );

        Transaction failedTransaction = new Transaction(
                transactionId,
                accountId,
                org.javamoney.moneta.Money.of(new BigDecimal("100.00"), "BRL"),
                TransactionType.DEBIT,
                Status.FAILED,
                OffsetDateTime.now()
        );

        TransactionResult result = new TransactionResult(failedTransaction, null);

        when(processTransactionUseCase.execute(any(Transaction.class))).thenReturn(result);

        mockMvc.perform(post("/transactions/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transaction.status").value("FAILED"))
                .andExpect(jsonPath("$.account").value(nullValue()));

        verify(processTransactionUseCase, times(1)).execute(any(Transaction.class));
    }

    @Test
    void shouldReturnBadRequestWhenAccountIdIsNull() throws Exception {
        UUID transactionId = UUID.randomUUID();

        String invalidRequest = """
                {
                  "account_id": null,
                  "amount": {
                    "value": 50.00,
                    "currency": "BRL"
                  },
                  "operation": "DEBIT"
                }
                """;

        mockMvc.perform(post("/transactions/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());

        verify(processTransactionUseCase, never()).execute(any(Transaction.class));
    }

    @Test
    void shouldReturnBadRequestWhenAmountIsZero() throws Exception {
        UUID transactionId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        TransactionRequest request = new TransactionRequest(
                accountId,
                new TransactionAmountRequest(BigDecimal.ZERO, "BRL"),
                "DEBIT"
        );

        mockMvc.perform(post("/transactions/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(processTransactionUseCase, never()).execute(any(Transaction.class));
    }

    @Test
    void shouldReturnBadRequestWhenAmountIsNegative() throws Exception {
        UUID transactionId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        TransactionRequest request = new TransactionRequest(
                accountId,
                new TransactionAmountRequest(new BigDecimal("-50.00"), "BRL"),
                "DEBIT"
        );

        mockMvc.perform(post("/transactions/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(processTransactionUseCase, never()).execute(any(Transaction.class));
    }

    @Test
    void shouldReturnBadRequestWhenCurrencyIsInvalid() throws Exception {
        UUID transactionId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        String invalidRequest = String.format("""
                {
                  "account_id": "%s",
                  "amount": {
                    "value": 50.00,
                    "currency": "BR"
                  },
                  "operation": "DEBIT"
                }
                """, accountId);

        mockMvc.perform(post("/transactions/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());

        verify(processTransactionUseCase, never()).execute(any(Transaction.class));
    }

    @Test
    void shouldReturnBadRequestWhenOperationIsInvalid() throws Exception {
        UUID transactionId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        String invalidRequest = String.format("""
                {
                  "account_id": "%s",
                  "amount": {
                    "value": 50.00,
                    "currency": "BRL"
                  },
                  "operation": "INVALID"
                }
                """, accountId);

        mockMvc.perform(post("/transactions/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());

        verify(processTransactionUseCase, never()).execute(any(Transaction.class));
    }

    @Test
    void shouldAcceptOperationCaseInsensitive() throws Exception {
        UUID transactionId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        TransactionRequest request = new TransactionRequest(
                accountId,
                new TransactionAmountRequest(new BigDecimal("50.00"), "BRL"),
                "debit"
        );

        Account account = new Account(
                accountId,
                UUID.randomUUID(),
                org.javamoney.moneta.Money.of(new BigDecimal("950.00"), "BRL"),
                OffsetDateTime.now(),
                "ENABLED"
        );

        Transaction transaction = new Transaction(
                transactionId,
                accountId,
                org.javamoney.moneta.Money.of(new BigDecimal("50.00"), "BRL"),
                TransactionType.DEBIT,
                Status.SUCCEEDED,
                OffsetDateTime.now()
        );

        TransactionResult result = new TransactionResult(transaction, account);

        when(processTransactionUseCase.execute(any(Transaction.class))).thenReturn(result);

        mockMvc.perform(post("/transactions/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transaction.status").value("SUCCEEDED"));

        verify(processTransactionUseCase, times(1)).execute(any(Transaction.class));
    }

    @Test
    void shouldHandleIdempotency() throws Exception {
        UUID transactionId = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");
        UUID accountId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

        TransactionRequest request = new TransactionRequest(
                accountId,
                new TransactionAmountRequest(new BigDecimal("50.00"), "BRL"),
                "DEBIT"
        );

        Account account = new Account(
                accountId,
                UUID.randomUUID(),
                org.javamoney.moneta.Money.of(new BigDecimal("950.00"), "BRL"),
                OffsetDateTime.now(),
                "ENABLED"
        );

        Transaction existingTransaction = new Transaction(
                transactionId,
                accountId,
                org.javamoney.moneta.Money.of(new BigDecimal("50.00"), "BRL"),
                TransactionType.DEBIT,
                Status.SUCCEEDED,
                OffsetDateTime.now()
        );

        TransactionResult result = new TransactionResult(existingTransaction, account);

        when(processTransactionUseCase.execute(any(Transaction.class))).thenReturn(result);

        mockMvc.perform(post("/transactions/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transaction.id").value(transactionId.toString()))
                .andExpect(jsonPath("$.transaction.status").value("SUCCEEDED"));

        verify(processTransactionUseCase, times(1)).execute(any(Transaction.class));
    }

    @Test
    void shouldAcceptDifferentCurrencies() throws Exception {
        UUID transactionId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        TransactionRequest request = new TransactionRequest(
                accountId,
                new TransactionAmountRequest(new BigDecimal("100.00"), "USD"),
                "CREDIT"
        );

        Account account = new Account(
                accountId,
                UUID.randomUUID(),
                org.javamoney.moneta.Money.of(new BigDecimal("1100.00"), "USD"),
                OffsetDateTime.now(),
                "ENABLED"
        );

        Transaction transaction = new Transaction(
                transactionId,
                accountId,
                org.javamoney.moneta.Money.of(new BigDecimal("100.00"), "USD"),
                TransactionType.CREDIT,
                Status.SUCCEEDED,
                OffsetDateTime.now()
        );

        TransactionResult result = new TransactionResult(transaction, account);

        when(processTransactionUseCase.execute(any(Transaction.class))).thenReturn(result);

        mockMvc.perform(post("/transactions/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transaction.amount.currency").value("USD"))
                .andExpect(jsonPath("$.account.balance.currency").value("USD"));

        verify(processTransactionUseCase, times(1)).execute(any(Transaction.class));
    }
}