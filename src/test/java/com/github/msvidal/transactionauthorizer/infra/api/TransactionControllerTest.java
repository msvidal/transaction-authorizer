package com.github.msvidal.transactionauthorizer.infra.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.msvidal.transactionauthorizer.application.usecase.AuthorizeTransactionUseCase;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Currency;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private AuthorizeTransactionUseCase authorizeTransactionUseCase;

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
                new MonetaryAmount(new BigDecimal("950.00"), Currency.getInstance("BRL")),
                OffsetDateTime.now(),
                "ENABLED"
        );

        Transaction transaction = new Transaction(
                transactionId,
                accountId,
                new MonetaryAmount(new BigDecimal("50.00"), Currency.getInstance("BRL")),
                TransactionType.DEBIT,
                Status.AUTHORIZED,
                OffsetDateTime.now()
        );

        TransactionResult result = new TransactionResult(transaction, account);

        when(authorizeTransactionUseCase.execute(any(Transaction.class))).thenReturn(result);

        mockMvc.perform(post("/transactions/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transaction.id").value(transactionId.toString()))
                .andExpect(jsonPath("$.transaction.type").value("DEBIT"))
                .andExpect(jsonPath("$.transaction.status").value("AUTHORIZED"))
                .andExpect(jsonPath("$.transaction.amount.amount").value(50.00))
                .andExpect(jsonPath("$.transaction.amount.currency").value("BRL"))
                .andExpect(jsonPath("$.account.id").value(accountId.toString()))
                .andExpect(jsonPath("$.account.balance.amount").value(950.00))
                .andExpect(jsonPath("$.account.balance.currency").value("BRL"));

        verify(authorizeTransactionUseCase, times(1)).execute(any(Transaction.class));
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
                new MonetaryAmount(new BigDecimal("1200.00"), Currency.getInstance("BRL")),
                OffsetDateTime.now(),
                "ENABLED"
        );

        Transaction transaction = new Transaction(
                transactionId,
                accountId,
                new MonetaryAmount(new BigDecimal("200.00"), Currency.getInstance("BRL")),
                TransactionType.CREDIT,
                Status.AUTHORIZED,
                OffsetDateTime.now()
        );

        TransactionResult result = new TransactionResult(transaction, account);

        when(authorizeTransactionUseCase.execute(any(Transaction.class))).thenReturn(result);

        mockMvc.perform(post("/transactions/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transaction.type").value("CREDIT"))
                .andExpect(jsonPath("$.transaction.status").value("AUTHORIZED"))
                .andExpect(jsonPath("$.account.balance.amount").value(1200.00));

        verify(authorizeTransactionUseCase, times(1)).execute(any(Transaction.class));
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
                new MonetaryAmount(new BigDecimal("100.00"), Currency.getInstance("BRL")),
                TransactionType.DEBIT,
                Status.FAILED,
                OffsetDateTime.now()
        );

        TransactionResult result = new TransactionResult(failedTransaction, null);

        when(authorizeTransactionUseCase.execute(any(Transaction.class))).thenReturn(result);

        mockMvc.perform(post("/transactions/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transaction.status").value("FAILED"))
                .andExpect(jsonPath("$.account").value(nullValue()));

        verify(authorizeTransactionUseCase, times(1)).execute(any(Transaction.class));
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

        verify(authorizeTransactionUseCase, never()).execute(any(Transaction.class));
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

        verify(authorizeTransactionUseCase, never()).execute(any(Transaction.class));
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

        verify(authorizeTransactionUseCase, never()).execute(any(Transaction.class));
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

        verify(authorizeTransactionUseCase, never()).execute(any(Transaction.class));
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

        verify(authorizeTransactionUseCase, never()).execute(any(Transaction.class));
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
                new MonetaryAmount(new BigDecimal("950.00"), Currency.getInstance("BRL")),
                OffsetDateTime.now(),
                "ENABLED"
        );

        Transaction transaction = new Transaction(
                transactionId,
                accountId,
                new MonetaryAmount(new BigDecimal("50.00"), Currency.getInstance("BRL")),
                TransactionType.DEBIT,
                Status.AUTHORIZED,
                OffsetDateTime.now()
        );

        TransactionResult result = new TransactionResult(transaction, account);

        when(authorizeTransactionUseCase.execute(any(Transaction.class))).thenReturn(result);

        mockMvc.perform(post("/transactions/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transaction.status").value("AUTHORIZED"));

        verify(authorizeTransactionUseCase, times(1)).execute(any(Transaction.class));
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
                new MonetaryAmount(new BigDecimal("950.00"), Currency.getInstance("BRL")),
                OffsetDateTime.now(),
                "ENABLED"
        );

        Transaction existingTransaction = new Transaction(
                transactionId,
                accountId,
                new MonetaryAmount(new BigDecimal("50.00"), Currency.getInstance("BRL")),
                TransactionType.DEBIT,
                Status.AUTHORIZED,
                OffsetDateTime.now()
        );

        TransactionResult result = new TransactionResult(existingTransaction, account);

        when(authorizeTransactionUseCase.execute(any(Transaction.class))).thenReturn(result);

        mockMvc.perform(post("/transactions/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transaction.id").value(transactionId.toString()))
                .andExpect(jsonPath("$.transaction.status").value("AUTHORIZED"));

        verify(authorizeTransactionUseCase, times(1)).execute(any(Transaction.class));
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
                new MonetaryAmount(new BigDecimal("1100.00"), Currency.getInstance("USD")),
                OffsetDateTime.now(),
                "ENABLED"
        );

        Transaction transaction = new Transaction(
                transactionId,
                accountId,
                new MonetaryAmount(new BigDecimal("100.00"), Currency.getInstance("USD")),
                TransactionType.CREDIT,
                Status.AUTHORIZED,
                OffsetDateTime.now()
        );

        TransactionResult result = new TransactionResult(transaction, account);

        when(authorizeTransactionUseCase.execute(any(Transaction.class))).thenReturn(result);

        mockMvc.perform(post("/transactions/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transaction.amount.currency").value("USD"))
                .andExpect(jsonPath("$.account.balance.currency").value("USD"));

        verify(authorizeTransactionUseCase, times(1)).execute(any(Transaction.class));
    }

    @Test
    void shouldReturnDatesInIso8601() throws Exception {
        UUID transactionId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        TransactionRequest request = new TransactionRequest(
                accountId,
                new TransactionAmountRequest(new BigDecimal("50.00"), "BRL"),
                "DEBIT"
        );

        Account account = new Account(
                accountId,
                UUID.randomUUID(),
                new MonetaryAmount(new BigDecimal("950.00"), Currency.getInstance("BRL")),
                OffsetDateTime.now(),
                "ENABLED"
        );

        Transaction transaction = new Transaction(
                transactionId,
                accountId,
                new MonetaryAmount(new BigDecimal("50.00"), Currency.getInstance("BRL")),
                TransactionType.DEBIT,
                Status.AUTHORIZED,
                OffsetDateTime.now()
        );

        TransactionResult result = new TransactionResult(transaction, account);

        when(authorizeTransactionUseCase.execute(any(Transaction.class))).thenReturn(result);

        MvcResult mvcResult = mockMvc.perform(post("/transactions/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        assertIso8601DatesInResponse(content);
    }

    @Test
    void shouldReturnValidIso4217AndPositiveAmounts() throws Exception {
        UUID transactionId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        TransactionRequest request = new TransactionRequest(
                accountId,
                new TransactionAmountRequest(new BigDecimal("50.00"), "BRL"),
                "DEBIT"
        );

        Account account = new Account(
                accountId,
                UUID.randomUUID(),
                new MonetaryAmount(new BigDecimal("950.00"), Currency.getInstance("BRL")),
                OffsetDateTime.now(),
                "ENABLED"
        );

        Transaction transaction = new Transaction(
                transactionId,
                accountId,
                new MonetaryAmount(new BigDecimal("50.00"), Currency.getInstance("BRL")),
                TransactionType.DEBIT,
                Status.AUTHORIZED,
                OffsetDateTime.now()
        );

        TransactionResult result = new TransactionResult(transaction, account);

        when(authorizeTransactionUseCase.execute(any(Transaction.class))).thenReturn(result);

        MvcResult mvcResult = mockMvc.perform(post("/transactions/{transactionId}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String content = mvcResult.getResponse().getContentAsString();

        assertIso4217CurrenciesInResponse(content);
        assertPositiveAmountsInResponse(content);
    }

    private void assertIso4217CurrenciesInResponse(String json) {
        Pattern pattern = Pattern.compile("\"currency\"\\s*:\\s*\"([A-Z]{3})\"");
        Matcher matcher = pattern.matcher(json);
        boolean found = false;
        while (matcher.find()) {
            found = true;
            String code = matcher.group(1);
            try {
                Currency.getInstance(code);
            } catch (IllegalArgumentException e) {
                fail("Código de moeda inválido: " + code);
            }
        }
        if (!found) {
            fail("Nenhum código de moeda ISO‑4217 encontrado na resposta");
        }
    }

    private void assertPositiveAmountsInResponse(String json) {
        Pattern pattern = Pattern.compile("\"amount\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)");
        Matcher matcher = pattern.matcher(json);
        boolean found = false;
        while (matcher.find()) {
            found = true;
            BigDecimal value = new BigDecimal(matcher.group(1));
            if (value.compareTo(BigDecimal.ZERO) <= 0) {
                fail("Valor não positivo encontrado: " + value);
            }
        }
        if (!found) {
            fail("Nenhum valor numérico de `amount` encontrado na resposta");
        }
    }

    private void assertIso8601DatesInResponse(String json) {
        Pattern pattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?(?:Z|[+-]\\d{2}:\\d{2})");
        Matcher matcher = pattern.matcher(json);
        boolean found = false;
        while (matcher.find()) {
            found = true;
            String dateStr = matcher.group();
            try {
                OffsetDateTime.parse(dateStr);
            } catch (DateTimeParseException e) {
                fail("Data não está em ISO8601: " + dateStr);
            }
        }
        if (!found) {
            fail("Nenhuma data no formato ISO8601 encontrada na resposta");
        }
    }

}