package com.github.msvidal.transactionauthorizer.infra.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.msvidal.transactionauthorizer.domain.model.Account;
import com.github.msvidal.transactionauthorizer.application.usecase.CreateAccountUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountSqsListenerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private CreateAccountUseCase createAccountUseCase;

    @InjectMocks
    private AccountSqsListener accountSqsListener;

    @Test
    void shouldProcessAccountMessageSuccessfully() throws Exception {
        String body = """
                {
                  "account": {
                    "id": "550e8400-e29b-41d4-a716-446655440000",
                    "owner": "550e8400-e29b-41d4-a716-446655440088",
                    "balance": 1000.00,
                    "createdAt": "2024-01-15T10:30:00Z",
                    "status": "ENABLED"
                  }
                }
                """;

        AccountDto dto = new AccountDto(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                UUID.fromString("550e8400-e29b-41d4-a716-446655440088"),
                new BigDecimal("1000.00"),
                OffsetDateTime.parse("2024-01-15T10:30:00Z"),
                "ENABLED"
        );

        when(objectMapper.readTree(body)).thenReturn(new ObjectMapper().readTree(body));
        when(objectMapper.treeToValue(any(), eq(AccountDto.class))).thenReturn(dto);

        accountSqsListener.receive(body);

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(createAccountUseCase, times(1)).execute(captor.capture());

        Account captured = captor.getValue();
        assertThat(captured.id()).isEqualTo(dto.id());
        assertThat(captured.owner()).isEqualTo(dto.owner());
        assertThat(captured.balance().value()).isEqualByComparingTo(dto.balance());
        assertThat(captured.status()).isEqualTo("ENABLED");
    }

    @Test
    void shouldProcessAccountMessageWithoutAccountWrapper() throws Exception {
        String body = """
                {
                  "id": "550e8400-e29b-41d4-a716-446655440000",
                  "owner": "550e8400-e29b-41d4-a716-446655440088",
                  "balance": 500.00,
                  "createdAt": "2024-01-15T10:30:00Z",
                  "status": "ENABLED"
                }
                """;

        AccountDto dto = new AccountDto(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                UUID.fromString("550e8400-e29b-41d4-a716-446655440088"),
                new BigDecimal("500.00"),
                OffsetDateTime.parse("2024-01-15T10:30:00Z"),
                "ENABLED"
        );

        when(objectMapper.readTree(body)).thenReturn(new ObjectMapper().readTree(body));
        when(objectMapper.treeToValue(any(), eq(AccountDto.class))).thenReturn(dto);

        accountSqsListener.receive(body);

        verify(createAccountUseCase, times(1)).execute(any(Account.class));
    }

    @Test
    void shouldProcessAccountWithZeroBalanceWhenBalanceIsNull() throws Exception {
        String body = """
                {
                  "account": {
                    "id": "550e8400-e29b-41d4-a716-446655440000",
                    "owner": "550e8400-e29b-41d4-a716-446655440088",
                    "createdAt": "2024-01-15T10:30:00Z"
                  }
                }
                """;

        AccountDto dto = new AccountDto(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                UUID.fromString("550e8400-e29b-41d4-a716-446655440088"),
                null,
                OffsetDateTime.parse("2024-01-15T10:30:00Z"),
                null
        );

        when(objectMapper.readTree(body)).thenReturn(new ObjectMapper().readTree(body));
        when(objectMapper.treeToValue(any(), eq(AccountDto.class))).thenReturn(dto);

        accountSqsListener.receive(body);

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(createAccountUseCase, times(1)).execute(captor.capture());

        Account captured = captor.getValue();
        assertThat(captured.balance().value()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(captured.status()).isEqualTo("ENABLED");
    }

    @Test
    void shouldNotProcessWhenAccountFieldIsMissing() throws Exception {
        String body = """
                {
                  "other": "data"
                }
                """;

        when(objectMapper.readTree(body)).thenReturn(new ObjectMapper().readTree(body));

        accountSqsListener.receive(body);

        verify(createAccountUseCase, never()).execute(any(Account.class));
    }

    @Test
    void shouldThrowExceptionWhenJsonParsingFails() throws Exception {
        String body = "invalid json";

        when(objectMapper.readTree(body)).thenThrow(new RuntimeException("Parse error"));

        assertThatThrownBy(() -> accountSqsListener.receive(body))
                .isInstanceOf(RuntimeException.class);

        verify(createAccountUseCase, never()).execute(any(Account.class));
    }

    @Test
    void shouldThrowExceptionWhenUseCaseFails() throws Exception {
        String body = """
                {
                  "account": {
                    "id": "550e8400-e29b-41d4-a716-446655440000",
                    "owner": "550e8400-e29b-41d4-a716-446655440088",
                    "balance": 1000.00,
                    "createdAt": "2024-01-15T10:30:00Z"
                  }
                }
                """;

        AccountDto dto = new AccountDto(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                UUID.fromString("550e8400-e29b-41d4-a716-446655440088"),
                new BigDecimal("1000.00"),
                OffsetDateTime.parse("2024-01-15T10:30:00Z"),
                null
        );

        when(objectMapper.readTree(body)).thenReturn(new ObjectMapper().readTree(body));
        when(objectMapper.treeToValue(any(), eq(AccountDto.class))).thenReturn(dto);
        doThrow(new RuntimeException("Database error")).when(createAccountUseCase).execute(any(Account.class));

        assertThatThrownBy(() -> accountSqsListener.receive(body))
                .isInstanceOf(RuntimeException.class);
    }
}
