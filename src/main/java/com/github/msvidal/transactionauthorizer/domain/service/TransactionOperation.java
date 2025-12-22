package com.github.msvidal.transactionauthorizer.domain.service;

import com.github.msvidal.transactionauthorizer.domain.model.Account;

import javax.money.MonetaryAmount;

public interface TransactionOperation {
    String getTransactionType();
    Account execute(Account account, MonetaryAmount amount);
}