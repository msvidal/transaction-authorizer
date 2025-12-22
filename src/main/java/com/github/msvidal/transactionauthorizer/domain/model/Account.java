package com.github.msvidal.transactionauthorizer.domain.model;

import org.javamoney.moneta.Money;

import javax.money.MonetaryAmount;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

public record Account(UUID id, UUID owner, MonetaryAmount balance, OffsetDateTime createdAt, String status) {

    public Account {
        Objects.requireNonNull(id, "id is required");
        if (balance == null) {
            balance = Money.of(BigDecimal.ZERO, "BRL");
        }
        if (createdAt == null) createdAt = OffsetDateTime.now(ZoneOffset.ofHours(-3));
        if (status == null) status = "ENABLED";
    }

    public Account credit(BigDecimal amount) {
        validateAmount(amount);
        MonetaryAmount newBalance = balance.add(Money.of(amount, balance.getCurrency()));
        return new Account(id, owner, newBalance, createdAt, status);
    }

    public Account debit(BigDecimal amount) {
        validateAmount(amount);
        BigDecimal balanceValue = getBalanceValue();
        if (balanceValue.compareTo(amount) < 0) {
            throw new RuntimeException("Saldo insuficiente");
        }
        MonetaryAmount newBalance = balance.subtract(Money.of(amount, balance.getCurrency()));
        return new Account(id, owner, newBalance, createdAt, status);
    }

    private static void validateAmount(BigDecimal amount) {
        if (amount == null) throw new IllegalArgumentException("amount cannot be null");
        if (amount.signum() <= 0) throw new IllegalArgumentException("amount must be positive");
    }

    public boolean hasSufficientBalance(BigDecimal amount) {
        return getBalanceValue().compareTo(amount) >= 0;
    }

    private BigDecimal getBalanceValue() {
        return balance.getNumber().numberValue(BigDecimal.class);
    }
}

