package com.github.msvidal.transactionauthorizer.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.Objects;
import java.util.UUID;

public record Account(UUID id, UUID owner, MonetaryAmount balance, OffsetDateTime createdAt, String status) {

    public Account {
        Objects.requireNonNull(id, "id is required");
        if (balance == null) {
            balance = new MonetaryAmount(BigDecimal.ZERO, Currency.getInstance("BRL"));
        }
        if (createdAt == null) createdAt = OffsetDateTime.now(ZoneOffset.ofHours(-3));
        if (status == null) status = "ENABLED";
    }

    public Account credit(BigDecimal amount) {
        validateAmount(amount);
        return new Account(id, owner, balance.add(amount), createdAt, status);
    }

    public Account debit(BigDecimal amount) {
        validateAmount(amount);
        if (balance.compareTo(amount) < 0) {
            throw new RuntimeException("Saldo insuficiente");
        }
        return new Account(id, owner, balance.subtract(amount), createdAt, status);
    }

    private static void validateAmount(BigDecimal amount) {
        if (amount == null) throw new IllegalArgumentException("amount cannot be null");
        if (amount.signum() <= 0) throw new IllegalArgumentException("amount must be positive");
    }

    public boolean hasSufficientBalance(BigDecimal amount) {
        return balance.compareTo(amount) >= 0;
    }
}

