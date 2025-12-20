package com.github.msvidal.transactionauthorizer.infra.persistence.transaction;

import com.github.msvidal.transactionauthorizer.domain.model.Status;
import com.github.msvidal.transactionauthorizer.domain.model.TransactionType;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_transactions_id", columnList = "id"),
        @Index(name = "idx_transactions_account_id", columnList = "account_id")
})
public class TransactionEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType operation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Version
    private Long version;
}