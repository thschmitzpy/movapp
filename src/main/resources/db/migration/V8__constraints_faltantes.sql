DELETE FROM itens_venda WHERE venda_id IS NULL OR codigo IS NULL;

  ALTER TABLE itens_venda
      ALTER COLUMN venda_id   SET NOT NULL,
      ALTER COLUMN codigo     SET NOT NULL,
      ALTER COLUMN quantidade SET NOT NULL,
      ALTER COLUMN preco_unit SET NOT NULL;

  ALTER TABLE itens_venda DROP CONSTRAINT IF EXISTS itens_venda_venda_id_fkey;
  ALTER TABLE itens_venda
      ADD CONSTRAINT itens_venda_venda_id_fkey
      FOREIGN KEY (venda_id) REFERENCES vendas(id) ON DELETE CASCADE;

  ALTER TABLE itens_venda
      ADD CONSTRAINT itens_venda_quantidade_positiva CHECK (quantidade > 0),
      ADD CONSTRAINT itens_venda_preco_nao_negativo  CHECK (preco_unit >= 0);

  ALTER TABLE vendas
      ALTER COLUMN data  SET NOT NULL,
      ALTER COLUMN total SET NOT NULL;

  ALTER TABLE vendas
      ADD CONSTRAINT vendas_total_nao_negativo CHECK (total >= 0);

  ALTER TABLE produtos
      ALTER COLUMN produto SET NOT NULL,
      ALTER COLUMN preco   SET NOT NULL,
      ALTER COLUMN estoque SET NOT NULL;

  ALTER TABLE produtos
      ADD CONSTRAINT produtos_preco_positivo   CHECK (preco > 0),
      ADD CONSTRAINT produtos_estoque_nao_negativo CHECK (estoque >= 0);