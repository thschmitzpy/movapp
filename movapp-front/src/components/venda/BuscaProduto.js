import { useState } from 'react';
import api from '../../services/api';

export default function BuscaProduto({ itens, onAdicionarItem, exibirMensagem }) {
  const [codigoBusca, setCodigoBusca] = useState('');
  const [quantidade, setQuantidade] = useState('');
  const [produtoBuscado, setProdutoBuscado] = useState(null);
  const [buscando, setBuscando] = useState(false);

  async function buscarProduto() {
    if (!codigoBusca.trim()) return;
    setBuscando(true);
    try {
      const res = await api.get(`/produtos/${codigoBusca.trim()}`);
      setProdutoBuscado(res.data);
    } catch (err) {
      setProdutoBuscado(null);
      const status = err.response?.status;

      if (status === 404) {
        exibirMensagem(`Produto "${codigoBusca}" não encontrado.`, 'erro');
      }
    } finally {
      setBuscando(false);
    }
  }

  function adicionarItem() {
    if (!produtoBuscado) return;
    const qtd = parseInt(quantidade);
    if (!qtd || qtd < 1) {
      exibirMensagem('Informe uma quantidade válida.', 'erro');
      return;
    }
    const jaNoCarrinho = itens.find(i => i.codigoProduto === produtoBuscado.codigo)?.quantidade ?? 0;
    if (jaNoCarrinho + qtd > produtoBuscado.estoque) {
      const disponivel = produtoBuscado.estoque - jaNoCarrinho;
      exibirMensagem(
        disponivel > 0
          ? `Estoque insuficiente. Você já tem ${jaNoCarrinho} no carrinho. Disponível para adicionar: ${disponivel}`
          : `Estoque insuficiente. Todo o estoque disponível (${produtoBuscado.estoque}) já está no carrinho.`,
        'erro'
      );
      return;
    }

    onAdicionarItem(produtoBuscado, qtd);

    setProdutoBuscado(null);
    setCodigoBusca('');
    setQuantidade('');
  }

  return (
    <div className="busca-produto">
      <h3>Adicionar Produto</h3>
      <div className="linha-busca">
        <input
          placeholder="Código do produto"
          value={codigoBusca}
          onChange={e => setCodigoBusca(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && buscarProduto()}
          aria-label="Código do produto"
        />
        <input
          type="number"
          min="1"
          value={quantidade}
          onChange={e => setQuantidade(e.target.value)}
          style={{ width: 80 }}
          placeholder="Qtd"
          aria-label="Quantidade"
        />
        <button className="btn-secundario" onClick={buscarProduto} disabled={buscando}>
          {buscando ? 'Buscando...' : 'Buscar'}
        </button>
      </div>

      {produtoBuscado && (
        <div className="produto-encontrado">
          <span>
            <strong>{produtoBuscado.nome}</strong> — R$ {Number(produtoBuscado.preco).toFixed(2)} — Estoque: {produtoBuscado.estoque}
          </span>
          <button
            className="btn-primario"
            onClick={adicionarItem}
            disabled={!quantidade || parseInt(quantidade) < 1}
            title={!quantidade ? 'Informe a quantidade antes de adicionar' : ''}
          >
            + Adicionar
          </button>
        </div>
      )}
    </div>
  );
}
