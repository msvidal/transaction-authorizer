package com.github.msvidal.transactionauthorizer.infra.api;

import com.github.msvidal.transactionauthorizer.domain.model.Transaction;
import com.github.msvidal.transactionauthorizer.domain.model.TransactionType;
import com.github.msvidal.transactionauthorizer.application.usecase.ProcessTransactionUseCase;
import com.github.msvidal.transactionauthorizer.infra.api.dto.TransactionRequest;
import com.github.msvidal.transactionauthorizer.infra.api.dto.TransactionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javamoney.moneta.Money;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/transactions")
@Tag(name = "Transações", description = "Endpoints para autorização de transações financeiras")
@AllArgsConstructor
@Slf4j
@Profile({"api","local"})
public class TransactionController {

    private final ProcessTransactionUseCase processTransactionUseCase;

    @Operation(
            summary = "Autorizar transação",
            description = "Processa uma operação de débito ou crédito em conta de forma idempotente."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transação processada (Aprovada ou Recusada)",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos na requisição"),
            @ApiResponse(responseCode = "404", description = "Conta não encontrada (se configurado para lançar erro)"),
            @ApiResponse(responseCode = "422", description = "Erro de negócio (ex: Saldo insuficiente, se não tratado como 200)")
    })
    @PostMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> authorize(
            @PathVariable UUID transactionId,
            @RequestBody @Valid TransactionRequest request) {

        log.info("Recebida requisição de transação - ID: {}, Conta: {}, Valor: {}, Tipo: {}",
                transactionId,
                request.accountId(),
                request.amount() != null ? request.amount().value() : null,
                request.operation());

        var amount = request.amount().value();
        var currency = request.amount().currency();
        var operation = TransactionType.fromString(request.operation());

        var transaction = new Transaction(
                transactionId,
                request.accountId(),
                Money.of(amount, currency),
                operation,
                null,
                null
        );

        var transactionResult = processTransactionUseCase.execute(transaction);

        if (transactionResult == null) {
            log.warn("Use case retornou null para a transação ID: {}", transactionId);
            return ResponseEntity.badRequest().build();
        }

        TransactionResponse response = TransactionResponse.toResponse(transactionResult);

        log.info("Transação processada - ID: {}, Status: {}", transactionId, response.transaction().status());

        return ResponseEntity.ok(response);
    }
}