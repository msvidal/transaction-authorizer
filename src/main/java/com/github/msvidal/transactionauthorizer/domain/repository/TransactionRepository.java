package com.github.msvidal.transactionauthorizer.domain.repository;

import com.github.msvidal.transactionauthorizer.domain.model.Transaction;

import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository {
    Transaction save(Transaction transaction);
    Optional<Transaction> findById(UUID id);
}
