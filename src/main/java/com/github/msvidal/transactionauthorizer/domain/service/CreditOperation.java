package com.github.msvidal.transactionauthorizer.domain.service;

import com.github.msvidal.transactionauthorizer.domain.model.Account;
import com.github.msvidal.transactionauthorizer.domain.model.MonetaryAmount;
import com.github.msvidal.transactionauthorizer.domain.model.TransactionType;
import org.springframework.stereotype.Component;

@Component
public class CreditOperation implements TransactionOperation {

    @Override
    public String getTransactionType() {
        return TransactionType.CREDIT.name();
    }

    @Override
    public Account execute(Account account, MonetaryAmount amount) {
        return account.credit(amount.value());
    }
}