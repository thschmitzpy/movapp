package com.loja.movapp.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.loja.movapp.model.BlacklistedToken;
import com.loja.movapp.repository.BlacklistedTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;


@Component
public class TokenBlacklist {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklist.class);

    @Autowired
    private BlacklistedTokenRepository repository;

    private final Cache<String, Boolean> naoBlacklistados = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10))
            .maximumSize(10_000)
            .build();

    public void add(String token, Date expiration) {
        repository.save(new BlacklistedToken(token, expiration.toInstant()));
        naoBlacklistados.invalidate(token);
        log.info("Token adicionado à blacklist (banco)");
    }

    public boolean isBlacklisted(String token) {
        if (naoBlacklistados.getIfPresent(token) != null) {
            return false;
        }
        boolean blacklisted = repository.existsById(token);
        if (!blacklisted) {
            naoBlacklistados.put(token, Boolean.TRUE);
        }
        return blacklisted;
    }

    @Scheduled(fixedRate = 3_600_000)
    public void removeExpired() {
        repository.deleteExpired(Instant.now());
        log.info("Limpeza da blacklist: tokens expirados removidos do banco");
    }
}