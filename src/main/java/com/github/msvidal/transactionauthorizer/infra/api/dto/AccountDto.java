package com.github.msvidal.transactionauthorizer.infra.api.dto;

import java.util.UUID;

public record AccountDto(
        UUID id,
        AmountDto balance
) {}