package com.github.msvidal.transactionauthorizer.infra.persistence.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountJpaRepository extends JpaRepository<AccountEntity, UUID> {

    @Query("SELECT a FROM AccountEntity a WHERE a.id = :id AND a.status = 'ENABLED'")
    Optional<AccountEntity> findActiveById(@Param("id") UUID id);

}
