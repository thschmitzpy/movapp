CREATE TABLE IF NOT EXISTS idempotency_keys (
    chave           VARCHAR(100) PRIMARY KEY,
    endpoint        VARCHAR(100) NOT NULL,
    request_hash    VARCHAR(64)  NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    response_status INTEGER,
    response_body   TEXT,
    criado_em       TIMESTAMP    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_idempotency_keys_status_criado_em
    ON idempotency_keys(status, criado_em);
