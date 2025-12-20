package com.github.msvidal.transactionauthorizer.domain.service;

import com.github.msvidal.transactionauthorizer.domain.exception.InsufficientBalanceException;
import com.github.msvidal.transactionauthorizer.domain.model.Account;
import com.github.msvidal.transactionauthorizer.domain.model.MonetaryAmount;
import com.github.msvidal.transactionauthorizer.domain.model.TransactionType;
import org.springframework.stereotype.Component;

@Component
public class DebitOperation implements TransactionOperation {

    @Override
    public String getTransactionType() {
        return TransactionType.DEBIT.name();
    }

    public Account execute(Account account, MonetaryAmount amount) {
        if (!account.hasSufficientBalance(amount.value())) {
            throw new InsufficientBalanceException("Saldo insuficiente para realizar a operação");
        }
        return account.debit(amount.value());
    }
}