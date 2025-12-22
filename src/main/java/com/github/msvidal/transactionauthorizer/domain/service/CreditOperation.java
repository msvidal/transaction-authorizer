package com.github.msvidal.transactionauthorizer.domain.service;

import com.github.msvidal.transactionauthorizer.domain.model.Account;
import com.github.msvidal.transactionauthorizer.domain.model.TransactionType;
import org.springframework.stereotype.Component;

import javax.money.MonetaryAmount;
import java.math.BigDecimal;

@Component
public class CreditOperation implements TransactionOperation {

    @Override
    public String getTransactionType() {
        return TransactionType.CREDIT.name();
    }

    @Override
    public Account execute(Account account, MonetaryAmount amount) {
        BigDecimal value = amount.getNumber().numberValue(BigDecimal.class);
        return account.credit(value);
    }
}