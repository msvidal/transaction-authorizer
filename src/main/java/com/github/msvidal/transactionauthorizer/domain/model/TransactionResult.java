package com.github.msvidal.transactionauthorizer.domain.model;

public record TransactionResult(
        Transaction transaction,
        Account account
) {}
