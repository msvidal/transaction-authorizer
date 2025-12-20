package com.github.msvidal.transactionauthorizer.domain.model;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

public record Transaction(UUID id, UUID accountId, MonetaryAmount amount, TransactionType operation,
                          Status status, OffsetDateTime createdAt) {

    public Transaction markSucceeded() {
        OffsetDateTime ts = OffsetDateTime.now(ZoneOffset.ofHours(-3));
        return new Transaction(id, accountId, amount, operation, Status.SUCCEEDED, ts);
    }

    public Transaction markFailed() {
        OffsetDateTime ts = OffsetDateTime.now(ZoneOffset.ofHours(-3));
        return new Transaction(id, accountId, amount, operation, Status.FAILED, ts);
    }
}