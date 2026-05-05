CREATE TABLE IF NOT EXISTS produtos (
    codigo  VARCHAR(255) PRIMARY KEY,
    produto VARCHAR(255),
    cor     VARCHAR(255),
    tamanho VARCHAR(255),
    preco   NUMERIC(10, 2),
    estoque INTEGER,
    versao  BIGINT
);

CREATE TABLE IF NOT EXISTS vendas (
    id                 BIGSERIAL    PRIMARY KEY,
    data               TIMESTAMP,
    total              NUMERIC(10, 2),
    forma_pagamento    VARCHAR(255),
    condicao_pagamento VARCHAR(255),
    status             VARCHAR(20)  NOT NULL DEFAULT 'FECHADA',
    usuario            VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS itens_venda (
    id         BIGSERIAL    PRIMARY KEY,
    venda_id   BIGINT       REFERENCES vendas(id),
    codigo     VARCHAR(255) REFERENCES produtos(codigo),
    quantidade INTEGER,
    preco_unit NUMERIC(10, 2)
);

CREATE TABLE IF NOT EXISTS blacklisted_tokens (
    token     TEXT                     PRIMARY KEY,
    expiracao TIMESTAMP WITH TIME ZONE NOT NULL
);
