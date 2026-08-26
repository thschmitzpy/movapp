package com.loja.movapp.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

@Component
public class PasswordResetTokenStore {

    public static final Duration TTL = Duration.ofMinutes(15);

    private final SecureRandom random = new SecureRandom();

    private final Cache<String, String> tokens = Caffeine.newBuilder()
            .expireAfterWrite(TTL)
            .maximumSize(1_000)
            .build();

    public String gerar(String username) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.put(token, username);
        return token;
    }

    public Optional<String> consumir(String token) {
        String username = tokens.getIfPresent(token);
        if (username != null) tokens.invalidate(token);
        return Optional.ofNullable(username);
    }
}
