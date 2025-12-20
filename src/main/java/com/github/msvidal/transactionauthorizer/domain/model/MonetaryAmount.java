package com.github.msvidal.transactionauthorizer.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import static java.util.Objects.requireNonNull;

public record MonetaryAmount(BigDecimal value, Currency currency) {

    public MonetaryAmount(BigDecimal value, Currency currency) {
        requireNonNull(value, "value is required");
        this.currency = requireNonNull(currency, "currency is required");
        this.value = value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public MonetaryAmount add(BigDecimal increment) {
        requireNonNull(increment, "increment is required");
        return new MonetaryAmount(value.add(increment), currency);
    }

    public MonetaryAmount subtract(BigDecimal decrement) {
        requireNonNull(decrement, "decrement is required");
        return new MonetaryAmount(value.subtract(decrement), currency);
    }

    public int compareTo(BigDecimal other) {
        requireNonNull(other, "other is required");
        return value.compareTo(other);
    }
}
