package com.github.msvidal.transactionauthorizer.infra.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record TransactionRequest(

        @Schema(description = "ID da conta que sofrerá a transação", example = "5b19c8b6-0cc4-4c72-a989-0c2ee15fa975")
        @NotNull(message = "O ID da conta é obrigatório")
        UUID accountId,

        @Schema(description = "Dados do valor monetário")
        @NotNull(message = "O objeto de valor é obrigatório")
        @Valid
        TransactionAmountRequest amount,

        @Schema(description = "Tipo da operação", example = "CREDIT", allowableValues = {"CREDIT", "DEBIT"})
        @NotNull(message = "O tipo da operação é obrigatório")
        @Pattern(regexp = "DEBIT|CREDIT", flags = Pattern.Flag.CASE_INSENSITIVE, message = "A operação deve ser DEBIT ou CREDIT")
        String operation

) {}