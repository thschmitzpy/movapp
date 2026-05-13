CREATE INDEX IF NOT EXISTS idx_vendas_data   ON vendas(data);
CREATE INDEX IF NOT EXISTS idx_vendas_status ON vendas(status);

CREATE INDEX IF NOT EXISTS idx_itens_venda_venda_id ON itens_venda(venda_id);
CREATE INDEX IF NOT EXISTS idx_itens_venda_codigo   ON itens_venda(codigo);
