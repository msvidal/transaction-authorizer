package com.github.msvidal.transactionauthorizer.infra.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TransactionDto(
        UUID id,
        String type,
        AmountDto amount,
        String status,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        OffsetDateTime timestamp
) {}