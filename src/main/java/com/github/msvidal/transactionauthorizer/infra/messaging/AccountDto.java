package com.github.msvidal.transactionauthorizer.infra.messaging;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AccountDto(
        UUID id,
        UUID owner,
        BigDecimal balance,
        OffsetDateTime createdAt,
        String status
) {}
