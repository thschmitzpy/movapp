WITH item_errado AS (
      UPDATE itens_venda
         SET preco_unit = 19.90
       WHERE codigo     = 'MEI-001'
         AND preco_unit = 49.90
      RETURNING venda_id
  ),
  venda_recalculada AS (
      UPDATE vendas v
         SET total = (
               SELECT COALESCE(SUM(iv.preco_unit * iv.quantidade), 0)
                 FROM itens_venda iv
                WHERE iv.venda_id = v.id
             )
       WHERE v.id IN (SELECT venda_id FROM item_errado)
      RETURNING v.id, v.total
  )
  UPDATE pagamentos_venda p
     SET valor = vr.total
    FROM venda_recalculada vr
   WHERE p.venda_id = vr.id;