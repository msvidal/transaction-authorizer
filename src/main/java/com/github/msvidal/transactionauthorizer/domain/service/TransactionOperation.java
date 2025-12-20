package com.github.msvidal.transactionauthorizer.domain.service;

import com.github.msvidal.transactionauthorizer.domain.model.Account;
import com.github.msvidal.transactionauthorizer.domain.model.MonetaryAmount;

public interface TransactionOperation {
    String getTransactionType();
    Account execute(Account account, MonetaryAmount amount);
}