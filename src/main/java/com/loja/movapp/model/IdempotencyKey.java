package com.loja.movapp.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Registro de uma chave de idempotência (header Idempotency-Key).
 * Garante que requisições repetidas com a mesma chave (ex.: duplo clique
 * no botão Finalizar, retry de rede) não produzam efeitos duplicados:
 * a primeira execução é registrada e as repetições recebem a resposta cacheada.
 */
@Entity
@Table(name = "idempotency_keys")
public class IdempotencyKey {

    @Id
    @Column(name = "chave", length = 100)
    private String chave;

    @Column(name = "endpoint", nullable = false, length = 100)
    private String endpoint;

    /** SHA-256 do payload da requisição. Permite detectar reuso da mesma chave com payload diferente. */
    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private IdempotencyStatus status;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    public String             getChave()          { return chave;          }
    public String             getEndpoint()       { return endpoint;       }
    public String             getRequestHash()    { return requestHash;    }
    public IdempotencyStatus  getStatus()         { return status;         }
    public Integer            getResponseStatus() { return responseStatus; }
    public String             getResponseBody()   { return responseBody;   }
    public LocalDateTime      getCriadoEm()       { return criadoEm;       }

    public void setChave(String chave)                   { this.chave          = chave;          }
    public void setEndpoint(String endpoint)             { this.endpoint       = endpoint;       }
    public void setRequestHash(String requestHash)       { this.requestHash    = requestHash;    }
    public void setStatus(IdempotencyStatus status)      { this.status         = status;         }
    public void setResponseStatus(Integer responseStatus){ this.responseStatus = responseStatus; }
    public void setResponseBody(String responseBody)     { this.responseBody   = responseBody;   }
    public void setCriadoEm(LocalDateTime criadoEm)      { this.criadoEm       = criadoEm;       }
}
