package com.github.msvidal.transactionauthorizer.domain.repository;

import com.github.msvidal.transactionauthorizer.domain.model.Account;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {
    Account save(Account account);
    Account update(Account account);
    Optional<Account> findById(UUID id);
}
