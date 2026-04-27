package com.loja.movapp.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mantém uma lista de tokens JWT invalidados (logout).
 * Tokens ficam na lista até expirar, uma limpeza automática roda a cada hora.
 */
@Component
public class TokenBlacklist {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklist.class);

    private final ConcurrentHashMap<String, Date> blacklist = new ConcurrentHashMap<>();

    public void add(String token, Date expiration) {
        blacklist.put(token, expiration);
        log.info("Token adicionado à blacklist; total={}", blacklist.size());
    }

    public boolean isBlacklisted(String token) {
        return blacklist.containsKey(token);
    }

    @Scheduled(fixedRate = 3_600_000)
    public void removeExpired() {
        Date now = new Date();
        int before = blacklist.size();
        blacklist.entrySet().removeIf(e -> e.getValue().before(now));
        int removed = before - blacklist.size();
        if (removed > 0) {
            log.info("Limpeza da blacklist: {} token(s) expirado(s) removido(s)", removed);
        }
    }
}
