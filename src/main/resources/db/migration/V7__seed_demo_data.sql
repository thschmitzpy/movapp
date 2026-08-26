DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM produtos) THEN

        INSERT INTO produtos (codigo, produto, cor, tamanho, preco, estoque, ativo, versao) VALUES
            ('CAM-001', 'Camiseta Básica',   'Branco', 'M',     49.90,  40, TRUE, 0),
            ('CAM-002', 'Camiseta Básica',   'Preto',  'M',     49.90,  35, TRUE, 0),
            ('CAM-003', 'Camiseta Básica',   'Preto',  'G',     49.90,  25, TRUE, 0),
            ('CAL-001', 'Calça Jeans Slim',  'Azul',   '40',   179.90,  15, TRUE, 0),
            ('CAL-002', 'Calça Jeans Slim',  'Preto',  '42',   179.90,  12, TRUE, 0),
            ('VES-001', 'Vestido Longo',     'Verde',  'P',    219.00,   8, TRUE, 0),
            ('TEN-001', 'Tênis Casual',      'Branco', '40',   259.90,  10, TRUE, 0),
            ('MEI-001', 'Meia Esportiva',    'Cinza',  'Único', 19.90,  60, TRUE, 0);

        -- Venda 1: hoje, fechada, dinheiro, 1 item
        WITH nova_venda AS (
            INSERT INTO vendas (data, total, forma_pagamento, condicao_pagamento, status, usuario, versao)
            VALUES (CURRENT_TIMESTAMP - INTERVAL '1 hour', 49.90, 'DINHEIRO', 'A_VISTA', 'FECHADA', 'admin', 0)
            RETURNING id
        ), ins_item AS (
            INSERT INTO itens_venda (venda_id, codigo, quantidade, preco_unit, produto_nome, produto_cor, produto_tamanho)
            SELECT id, 'CAM-001', 1, 49.90, 'Camiseta Básica', 'Branco', 'M' FROM nova_venda
        )
        INSERT INTO pagamentos_venda (venda_id, forma_pagamento, condicao_pagamento, valor)
        SELECT id, 'DINHEIRO', 'A_VISTA', 49.90 FROM nova_venda;

        -- Venda 2: hoje, fechada, cartão, 2 itens
        WITH nova_venda AS (
            INSERT INTO vendas (data, total, forma_pagamento, condicao_pagamento, status, usuario, versao)
            VALUES (CURRENT_TIMESTAMP - INTERVAL '3 hours', 229.80, 'CARTAO_CREDITO', '3X', 'FECHADA', 'admin', 0)
            RETURNING id
        ), ins_item1 AS (
            INSERT INTO itens_venda (venda_id, codigo, quantidade, preco_unit, produto_nome, produto_cor, produto_tamanho)
            SELECT id, 'CAL-001', 1, 179.90, 'Calça Jeans Slim', 'Azul', '40' FROM nova_venda
        ), ins_item2 AS (
            INSERT INTO itens_venda (venda_id, codigo, quantidade, preco_unit, produto_nome, produto_cor, produto_tamanho)
            SELECT id, 'MEI-001', 1, 49.90, 'Meia Esportiva', 'Cinza', 'Único' FROM nova_venda
        )
        INSERT INTO pagamentos_venda (venda_id, forma_pagamento, condicao_pagamento, valor)
        SELECT id, 'CARTAO_CREDITO', '3X', 229.80 FROM nova_venda;


        WITH nova_venda AS (
            INSERT INTO vendas (data, total, forma_pagamento, condicao_pagamento, status, usuario, versao)
            VALUES (CURRENT_TIMESTAMP - INTERVAL '1 day', 259.90, 'PIX', 'A_VISTA', 'FECHADA', 'usuario', 0)
            RETURNING id
        ), ins_item AS (
            INSERT INTO itens_venda (venda_id, codigo, quantidade, preco_unit, produto_nome, produto_cor, produto_tamanho)
            SELECT id, 'TEN-001', 1, 259.90, 'Tênis Casual', 'Branco', '40' FROM nova_venda
        )
        INSERT INTO pagamentos_venda (venda_id, forma_pagamento, condicao_pagamento, valor)
        SELECT id, 'PIX', 'A_VISTA', 259.90 FROM nova_venda;


        WITH nova_venda AS (
            INSERT INTO vendas (data, total, forma_pagamento, condicao_pagamento, status, usuario, versao)
            VALUES (CURRENT_TIMESTAMP - INTERVAL '3 days', 398.90, 'CARTAO_CREDITO', 'A_VISTA', 'FECHADA', 'admin', 0)
            RETURNING id
        ), ins_item1 AS (
            INSERT INTO itens_venda (venda_id, codigo, quantidade, preco_unit, produto_nome, produto_cor, produto_tamanho)
            SELECT id, 'VES-001', 1, 219.00, 'Vestido Longo', 'Verde', 'P' FROM nova_venda
        ), ins_item2 AS (
            INSERT INTO itens_venda (venda_id, codigo, quantidade, preco_unit, produto_nome, produto_cor, produto_tamanho)
            SELECT id, 'CAL-002', 1, 179.90, 'Calça Jeans Slim', 'Preto', '42' FROM nova_venda
        )
        INSERT INTO pagamentos_venda (venda_id, forma_pagamento, condicao_pagamento, valor)
        SELECT id, 'CARTAO_CREDITO', 'A_VISTA', 398.90 FROM nova_venda;


        WITH nova_venda AS (
            INSERT INTO vendas (data, total, forma_pagamento, condicao_pagamento, status, usuario, versao)
            VALUES (CURRENT_TIMESTAMP - INTERVAL '30 minutes', 99.80, 'DINHEIRO', 'A_VISTA', 'PENDENTE', 'usuario', 0)
            RETURNING id
        )
        INSERT INTO itens_venda (venda_id, codigo, quantidade, preco_unit, produto_nome, produto_cor, produto_tamanho)
        SELECT id, 'CAM-002', 2, 49.90, 'Camiseta Básica', 'Preto', 'M' FROM nova_venda;

    END IF;
END $$;
