package com.github.msvidal.transactionauthorizer.infra.api.dto;

import java.math.BigDecimal;

public record AmountDto(
        BigDecimal amount,
        String currency
) {}