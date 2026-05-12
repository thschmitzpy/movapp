package com.loja.movapp.repository;

import com.loja.movapp.model.IdempotencyKey;
import com.loja.movapp.model.IdempotencyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> {

    @Transactional
    @Modifying
    @Query("DELETE FROM IdempotencyKey k WHERE k.status = :status AND k.criadoEm < :limite")
    int deleteByStatusEAntesDe(@Param("status") IdempotencyStatus status,
                               @Param("limite") LocalDateTime limite);
}
