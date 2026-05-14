-- Soft delete em produtos: nunca apagar fisicamente um produto que
-- já apareceu em venda. O endpoint DELETE passa a setar ativo=false.
ALTER TABLE produtos ADD COLUMN IF NOT EXISTS ativo BOOLEAN NOT NULL DEFAULT TRUE;
CREATE INDEX IF NOT EXISTS idx_produtos_ativo ON produtos(ativo);

-- Snapshot do produto no momento da venda. Sem isso, alterar o nome
-- do produto reescreve retroativamente o histórico de vendas/recibos.
ALTER TABLE itens_venda
    ADD COLUMN IF NOT EXISTS produto_nome    VARCHAR(255),
    ADD COLUMN IF NOT EXISTS produto_cor     VARCHAR(255),
    ADD COLUMN IF NOT EXISTS produto_tamanho VARCHAR(255);

-- Backfill: copia o estado atual do produto pra cada item já existente.
UPDATE itens_venda iv
   SET produto_nome    = p.produto,
       produto_cor     = p.cor,
       produto_tamanho = p.tamanho
  FROM produtos p
 WHERE iv.codigo = p.codigo
   AND iv.produto_nome IS NULL;

ALTER TABLE itens_venda ALTER COLUMN produto_nome SET NOT NULL;
