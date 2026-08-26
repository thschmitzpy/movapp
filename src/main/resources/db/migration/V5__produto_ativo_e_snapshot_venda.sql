ALTER TABLE produtos ADD COLUMN IF NOT EXISTS ativo BOOLEAN NOT NULL DEFAULT TRUE;
CREATE INDEX IF NOT EXISTS idx_produtos_ativo ON produtos(ativo);

ALTER TABLE itens_venda
    ADD COLUMN IF NOT EXISTS produto_nome    VARCHAR(255),
    ADD COLUMN IF NOT EXISTS produto_cor     VARCHAR(255),
    ADD COLUMN IF NOT EXISTS produto_tamanho VARCHAR(255);

UPDATE itens_venda iv
   SET produto_nome    = p.produto,
       produto_cor     = p.cor,
       produto_tamanho = p.tamanho
  FROM produtos p
 WHERE iv.codigo = p.codigo
   AND iv.produto_nome IS NULL;

ALTER TABLE itens_venda ALTER COLUMN produto_nome SET NOT NULL;
