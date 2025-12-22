package com.github.msvidal.transactionauthorizer.infra.persistence.transaction;

import com.github.msvidal.transactionauthorizer.domain.model.Transaction;
import com.github.msvidal.transactionauthorizer.domain.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.javamoney.moneta.Money;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TransactionPersistenceAdapter implements TransactionRepository {

    private final TransactionJpaRepository transactionJpaRepository;

    @Override
    public Transaction save(Transaction transaction) {
        TransactionEntity saved = transactionJpaRepository.save(toEntity(transaction));
        return toDomain(saved);
    }

    @Override
    public Optional<Transaction> findById(UUID id) {
        return transactionJpaRepository.findById(id)
                .map(this::toDomain);
    }

    public TransactionEntity toEntity(Transaction transaction) {
        TransactionEntity entity = new TransactionEntity();
        entity.setId(transaction.id());
        entity.setAccountId(transaction.accountId());
        entity.setOperation(transaction.operation());
        entity.setAmount(transaction.amount().getNumber().numberValue(BigDecimal.class));
        entity.setCurrency(transaction.amount().getCurrency().getCurrencyCode());
        entity.setStatus(transaction.status());
        entity.setCreatedAt(OffsetDateTime.now());
        return entity;
    }

    public Transaction toDomain(TransactionEntity entity) {
        return new Transaction(
                entity.getId(),
                entity.getAccountId(),
                Money.of(entity.getAmount(), entity.getCurrency()),
                entity.getOperation(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
