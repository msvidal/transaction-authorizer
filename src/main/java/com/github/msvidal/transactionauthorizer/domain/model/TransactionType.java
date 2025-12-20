package com.github.msvidal.transactionauthorizer.domain.model;

public enum TransactionType {
    DEBIT,
    CREDIT;

    public static TransactionType fromString(String value) {
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid transaction type: " + value);
        }
    }
}
