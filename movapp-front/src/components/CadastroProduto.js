import { useState, useEffect, useCallback } from 'react';
import api from '../services/api';

const camposVazios = { codigo: '', nome: '', cor: '', tamanho: '', preco: '', estoque: '' };
const PAGE_SIZE = 15;

export default function CadastroProduto({ isAdmin = false }) {
  const [produtos, setProdutos] = useState([]);
  const [form, setForm] = useState(camposVazios);
  const [editando, setEditando] = useState(null);
  const [mensagem, setMensagem] = useState(null);
  const [busca, setBusca] = useState('');
  const [buscando, setBuscando] = useState(false);
  const [pagina, setPagina] = useState(1);
  const [totalPaginas, setTotalPaginas] = useState(1);
  const [totalElementos, setTotalElementos] = useState(0);
  const [enviando, setEnviando] = useState(false);
  const [confirmarExclusao, setConfirmarExclusao] = useState(null);

  const carregarProdutos = useCallback(async (termo, pg = 1) => {
    setBuscando(true);
    try {
      const params = { page: pg - 1, size: PAGE_SIZE };
      if (termo) params.nome = termo;
      const res = await api.get('/produtos', { params });
      setProdutos(res.data.content || []);
      setTotalPaginas(res.data.totalPages || 1);
      setTotalElementos(res.data.totalElements || 0);
    } catch {
      exibirMensagem('Erro ao carregar produtos.', 'erro');
    } finally {
      setBuscando(false);
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // Carrega ao montar
  useEffect(() => { carregarProdutos('', 1); }, [carregarProdutos]);

  // Debounce: aguarda 400ms após o usuário parar de digitar e reseta para página 1
  useEffect(() => {
    setPagina(1);
    const timer = setTimeout(() => carregarProdutos(busca, 1), 400);
    return () => clearTimeout(timer);
  }, [busca, carregarProdutos]);

  function exibirMensagem(texto, tipo = 'sucesso') {
    setMensagem({ texto, tipo });
    setTimeout(() => setMensagem(null), 3500);
  }

  function handleChange(e) {
    setForm({ ...form, [e.target.name]: e.target.value });
  }

  function irParaPagina(pg) {
    setPagina(pg);
    carregarProdutos(busca, pg);
  }

  function iniciarEdicao(produto) {
    setEditando(produto.codigo);
    setForm({
      codigo: produto.codigo,
      nome: produto.nome,
      cor: produto.cor || '',
      tamanho: produto.tamanho || '',
      preco: produto.preco,
      estoque: produto.estoque,
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  function cancelarEdicao() {
    setEditando(null);
    setForm(camposVazios);
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setEnviando(true);
    const body = {
      codigo: form.codigo,
      nome: form.nome,
      cor: form.cor,
      tamanho: form.tamanho,
      preco: parseFloat(form.preco),
      estoque: parseInt(form.estoque),
    };
    try {
      if (editando) {
        await api.put(`/produtos/${editando}`, body);
        exibirMensagem('Produto atualizado com sucesso!');
      } else {
        await api.post('/produtos', body);
        exibirMensagem('Produto cadastrado com sucesso!');
      }
      cancelarEdicao();
      carregarProdutos(busca, pagina);
    } catch (err) {
      const data = err.response?.data;
      const msg = typeof data === 'string' ? data : data?.mensagem || data?.message || 'Erro ao salvar produto.';
      exibirMensagem(msg, 'erro');
    } finally {
      setEnviando(false);
    }
  }

  async function confirmarExcluir() {
    const produto = confirmarExclusao;
    setConfirmarExclusao(null);
    try {
      await api.delete(`/produtos/${produto.codigo}`);
      exibirMensagem(`Produto "${produto.nome}" excluído.`);
      carregarProdutos(busca, pagina);
    } catch (err) {
      const data = err.response?.data;
      const msg = typeof data === 'string' ? data : data?.mensagem || data?.message || 'Erro ao excluir produto.';
      exibirMensagem(msg, 'erro');
    }
  }

  return (
    <div className="secao">
      {mensagem && (
        <div className={`alerta ${mensagem.tipo}`}>{mensagem.texto}</div>
      )}

      {confirmarExclusao && (
        <div className="modal-overlay">
          <div className="modal-box">
            <h3>Confirmar exclusão</h3>
            <p>Deseja excluir o produto <strong>{confirmarExclusao.nome}</strong> ({confirmarExclusao.codigo})?</p>
            <p className="modal-aviso">Esta ação não pode ser desfeita.</p>
            <div className="modal-botoes">
              <button className="btn-secundario" onClick={() => setConfirmarExclusao(null)}>Cancelar</button>
              <button className="btn-excluir btn-modal-confirmar" onClick={confirmarExcluir}>Excluir</button>
            </div>
          </div>
        </div>
      )}

      {isAdmin && <div className="card">
        <h2>{editando ? 'Editar Produto' : 'Cadastrar Produto'}</h2>
        <form onSubmit={handleSubmit} className="form-grid">
          <div className="campo">
            <label>Código *</label>
            <input name="codigo" value={form.codigo} onChange={handleChange}
              required maxLength={20} disabled={!!editando} placeholder="Ex: PROD001" />
          </div>
          <div className="campo">
            <label>Nome *</label>
            <input name="nome" value={form.nome} onChange={handleChange}
              required maxLength={100} placeholder="Nome do produto" />
          </div>
          <div className="campo">
            <label>Cor</label>
            <input name="cor" value={form.cor} onChange={handleChange}
              maxLength={50} placeholder="Ex: Azul" />
          </div>
          <div className="campo">
            <label>Tamanho</label>
            <input name="tamanho" value={form.tamanho} onChange={handleChange}
              maxLength={10} placeholder="Ex: M, GG, 42" />
          </div>
          <div className="campo">
            <label>Preço (R$) *</label>
            <input name="preco" type="number" step="0.01" min="0" value={form.preco}
              onChange={handleChange} required placeholder="0,00" />
          </div>
          <div className="campo">
            <label>Estoque *</label>
            <input name="estoque" type="number" min="0" value={form.estoque}
              onChange={handleChange} required placeholder="0" />
          </div>
          <div className="botoes-form">
            <button type="submit" className="btn-primario" disabled={enviando}>
              {enviando ? 'Salvando...' : editando ? 'Salvar Alterações' : 'Cadastrar'}
            </button>
            {editando && (
              <button type="button" className="btn-secundario" onClick={cancelarEdicao} disabled={enviando}>
                Cancelar
              </button>
            )}
          </div>
        </form>
      </div>}

      <div className="card">
        <div className="cabecalho-lista">
          <h2>Produtos Cadastrados {totalElementos > 0 && <span className="pag-total"> ({totalElementos})</span>}</h2>
          <div className="busca-wrapper">
            <input
              className="input-busca"
              placeholder="Buscar por nome..."
              value={busca}
              onChange={e => setBusca(e.target.value)}
            />
            {buscando && <span className="busca-spinner">⟳</span>}
            {busca && !buscando && (
              <button className="busca-limpar" onClick={() => setBusca('')} title="Limpar busca">✕</button>
            )}
          </div>
        </div>
        {produtos.length === 0 ? (
          <p className="vazio">Nenhum produto encontrado.</p>
        ) : (
          <div className="tabela-wrapper">
            <table className="tabela">
              <thead>
                <tr>
                  <th>Código</th>
                  <th>Nome</th>
                  <th>Cor</th>
                  <th>Tamanho</th>
                  <th>Preço</th>
                  <th>Estoque</th>
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                {produtos.map(p => (
                  <tr key={p.codigo}>
                    <td>{p.codigo}</td>
                    <td>{p.nome}</td>
                    <td>{p.cor || '-'}</td>
                    <td>{p.tamanho || '-'}</td>
                    <td>R$ {Number(p.preco).toFixed(2)}</td>
                    <td>
                      <span className={`badge ${p.estoque === 0 ? 'badge-vazio' : p.estoque <= 5 ? 'badge-baixo' : 'badge-ok'}`}>
                        {p.estoque === 0 ? 'Sem estoque' : p.estoque <= 5 ? `${p.estoque} — baixo` : p.estoque}
                      </span>
                    </td>
                    <td className="acoes">
                      {isAdmin && <>
                        <button className="btn-editar" onClick={() => iniciarEdicao(p)}>Editar</button>
                        <button className="btn-excluir" onClick={() => setConfirmarExclusao(p)}>Excluir</button>
                      </>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {totalPaginas > 1 && (
          <div className="paginacao">
            <button
              className="btn-secundario btn-pag"
              onClick={() => irParaPagina(pagina - 1)}
              disabled={pagina === 1 || buscando}
            >
              ← Anterior
            </button>
            <span className="pag-info">
              Página {pagina} de {totalPaginas}
              <span className="pag-total"> ({totalElementos} produtos)</span>
            </span>
            <button
              className="btn-secundario btn-pag"
              onClick={() => irParaPagina(pagina + 1)}
              disabled={pagina === totalPaginas || buscando}
            >
              Próxima →
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
