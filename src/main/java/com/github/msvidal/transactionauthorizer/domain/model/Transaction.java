package com.github.msvidal.transactionauthorizer.domain.model;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

public record Transaction(UUID id, UUID accountId, MonetaryAmount amount, TransactionType operation,
                          Status status, OffsetDateTime createdAt) {

    public Transaction markPending() {
        OffsetDateTime ts = OffsetDateTime.now(ZoneOffset.ofHours(-3));
        return new Transaction(id, accountId, amount, operation, Status.PENDING, ts);
    }

    public Transaction markAuthorized() {
        OffsetDateTime ts = OffsetDateTime.now(ZoneOffset.ofHours(-3));
        return new Transaction(id, accountId, amount, operation, Status.AUTHORIZED, ts);
    }

    public Transaction markCaptured() {
        OffsetDateTime ts = OffsetDateTime.now(ZoneOffset.ofHours(-3));
        return new Transaction(id, accountId, amount, operation, Status.CAPTURED, ts);
    }

    public Transaction markSettled() {
        OffsetDateTime ts = OffsetDateTime.now(ZoneOffset.ofHours(-3));
        return new Transaction(id, accountId, amount, operation, Status.SETTLED, ts);
    }

    public Transaction markReversed() {
        OffsetDateTime ts = OffsetDateTime.now(ZoneOffset.ofHours(-3));
        return new Transaction(id, accountId, amount, operation, Status.REVERSED, ts);
    }

    // Legacy methods - maintained for backward compatibility
    public Transaction markSucceeded() {
        OffsetDateTime ts = OffsetDateTime.now(ZoneOffset.ofHours(-3));
        return new Transaction(id, accountId, amount, operation, Status.SUCCEEDED, ts);
    }

    public Transaction markFailed() {
        OffsetDateTime ts = OffsetDateTime.now(ZoneOffset.ofHours(-3));
        return new Transaction(id, accountId, amount, operation, Status.FAILED, ts);
    }
}