package com.loja.movapp.repository;

import com.loja.movapp.model.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Repository
public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, String> {

    @Transactional
    @Modifying
    @Query("DELETE FROM BlacklistedToken t WHERE t.expiracao < :agora")
    void deleteExpired(Instant agora);
}
