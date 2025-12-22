package com.github.msvidal.transactionauthorizer.infra.persistence.account;

import com.github.msvidal.transactionauthorizer.domain.model.Account;
import com.github.msvidal.transactionauthorizer.domain.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.javamoney.moneta.Money;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AccountPersistenceAdapter implements AccountRepository {

    private final AccountJpaRepository accountJpaRepository;

    @Transactional
    public Account save(Account account) {
        AccountEntity saved = accountJpaRepository.save(toEntity(account));
        return toDomain(saved);
    }

    @Override
    public Account update(Account account) {
        var accountEntity = accountJpaRepository
                .findById(account.id())
                .orElseThrow(RuntimeException::new);
        accountEntity.setBalance(account.balance().getNumber().numberValue(BigDecimal.class));
        AccountEntity updated = accountJpaRepository.save(accountEntity);
        return toDomain(updated);
    }

    public Optional<Account> findById(UUID id) {
        return accountJpaRepository.findActiveById(id)
                .map(this::toDomain);
    }

    private AccountEntity toEntity(Account account) {
        AccountEntity entity = new AccountEntity();
        entity.setId(account.id());
        entity.setOwner(account.owner());
        entity.setBalance(account.balance().getNumber().numberValue(BigDecimal.class));
        entity.setCurrency(account.balance().getCurrency().getCurrencyCode());
        entity.setCreatedAt(account.createdAt());
        entity.setStatus(account.status());
        return entity;
    }

    private Account toDomain(AccountEntity accountEntity) {
        return new Account(
                accountEntity.getId(),
                accountEntity.getOwner(),
                Money.of(accountEntity.getBalance(), accountEntity.getCurrency()),
                accountEntity.getCreatedAt(),
                accountEntity.getStatus()
        );
    }
}

