package com.github.msvidal.transactionauthorizer.infra.persistence.account;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Data
@Table(name = "accounts", indexes = {
        @Index(name = "idx_accounts_id", columnList = "id"),
        @Index(name = "idx_accounts_id_status", columnList = "id, status")
})
public class AccountEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID owner;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private String status;

    @Version
    private Long version;
}