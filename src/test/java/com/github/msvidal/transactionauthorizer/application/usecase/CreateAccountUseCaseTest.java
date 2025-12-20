package com.github.msvidal.transactionauthorizer.application.usecase;

import com.github.msvidal.transactionauthorizer.domain.model.Account;
import com.github.msvidal.transactionauthorizer.domain.model.MonetaryAmount;
import com.github.msvidal.transactionauthorizer.domain.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Currency;
import java.util.UUID;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateAccountUseCaseTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private CreateAccountUseCase createAccountUseCase;

    @Test
    void shouldSaveAccountSuccessfully() {
        Account account = getAccount(BigDecimal.ZERO);

        createAccountUseCase.execute(account);

        verify(accountRepository, times(1)).save(account);
    }

    @Test
    void shouldSaveAccountWithInitialBalance() {
        Account account = getAccount(new BigDecimal("100.00"));

        createAccountUseCase.execute(account);

        verify(accountRepository, times(1)).save(account);
    }

    @Test
    void shouldSaveAccountWithNullId() {
        Account account = getAccount(BigDecimal.ZERO);

        createAccountUseCase.execute(account);

        verify(accountRepository, times(1)).save(account);
    }

    private static Account getAccount(BigDecimal initialBalance) {
        return new Account(UUID.randomUUID(), UUID.randomUUID(), new MonetaryAmount(initialBalance,
                Currency.getInstance("BRL")), OffsetDateTime.now(), "ENABLED");
    }
}

