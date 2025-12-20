package com.github.msvidal.transactionauthorizer.domain.service;

import com.github.msvidal.transactionauthorizer.domain.model.TransactionType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class TransactionStrategyFactory {

    private final Map<String, TransactionOperation> strategies;

    public TransactionStrategyFactory(List<TransactionOperation> operations) {
        this.strategies = operations.stream()
                .collect(Collectors.toMap(TransactionOperation::getTransactionType, Function.identity()));
    }

    public TransactionOperation get(TransactionType type) {
        TransactionOperation operation = strategies.get(type.name().toUpperCase());
        if (operation == null) {
            throw new IllegalArgumentException("Tipo de transação inválido: " + type);
        }
        return operation;
    }
}