package com.github.msvidal.transactionauthorizer.infra.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record TransactionAmountRequest(

        @Schema(description = "Valor numérico da transação", example = "97.07")
        @NotNull(message = "O valor da transação é obrigatório")
        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero")
        BigDecimal value,

        @Schema(description = "Código da moeda (ISO 4217)", example = "BRL")
        @NotNull(message = "A moeda é obrigatória")
        @Pattern(regexp = "[A-Z]{3}", message = "A moeda deve seguir o padrão ISO 4217 (ex: BRL, USD)")
        String currency
) {}