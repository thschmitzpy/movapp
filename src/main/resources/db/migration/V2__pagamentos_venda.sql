CREATE TABLE IF NOT EXISTS pagamentos_venda (
    id                 BIGSERIAL      PRIMARY KEY,
    venda_id           BIGINT         NOT NULL REFERENCES vendas(id) ON DELETE CASCADE,
    forma_pagamento    VARCHAR(255)   NOT NULL,
    condicao_pagamento VARCHAR(255),
    valor              NUMERIC(10, 2) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_pagamentos_venda_venda_id ON pagamentos_venda(venda_id);
