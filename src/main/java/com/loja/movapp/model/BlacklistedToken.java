package com.loja.movapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "blacklisted_tokens")
public class BlacklistedToken {

    @Id
    @Column(columnDefinition = "TEXT")
    private String token;

    @Column(nullable = false)
    private Instant expiracao;

    public BlacklistedToken() {}

    public BlacklistedToken(String token, Instant expiracao) {
        this.token     = token;
        this.expiracao = expiracao;
    }

    public String  getToken()     { return token;     }
    public Instant getExpiracao() { return expiracao; }
}
