package com.github.msvidal.transactionauthorizer.application.usecase;

import com.github.msvidal.transactionauthorizer.domain.model.Account;
import com.github.msvidal.transactionauthorizer.domain.repository.AccountRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@AllArgsConstructor
public class CreateAccountUseCase {

    private final AccountRepository accountRepository;

    @Transactional
    public void execute(Account account) {
        accountRepository.save(account);
    }
}
