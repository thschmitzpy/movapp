package com.loja.movapp.security;

import com.loja.movapp.model.BlacklistedToken;
import com.loja.movapp.repository.BlacklistedTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;

/**
 * Mantém uma lista de tokens JWT invalidados (logout) persistida no banco de dados.
 * Tokens ficam na tabela até expirar; uma limpeza automática roda a cada hora.
 */
@Component
public class TokenBlacklist {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklist.class);

    @Autowired
    private BlacklistedTokenRepository repository;

    public void add(String token, Date expiration) {
        repository.save(new BlacklistedToken(token, expiration.toInstant()));
        log.info("Token adicionado à blacklist (banco)");
    }

    public boolean isBlacklisted(String token) {
        return repository.existsById(token);
    }

    @Scheduled(fixedRate = 3_600_000)
    public void removeExpired() {
        repository.deleteExpired(Instant.now());
        log.info("Limpeza da blacklist: tokens expirados removidos do banco");
    }
}
