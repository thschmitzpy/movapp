import { useState, useEffect, useCallback, Fragment } from 'react';
import api from '../../services/api';
import { useToast } from '../../context/ToastContext';
import { useFocusTrap } from '../../hooks/useFocusTrap';
import { labelForma, labelCondicao, toInputDate } from './labels';

const PAGE_SIZE = 20;
const STATUS_FILTROS = ['TODAS', 'FECHADA', 'PENDENTE', 'CANCELADA'];

export default function ListaVendas({
  dataFiltro,
  onDataFiltroChange,
  refreshKey,
  vendaEditandoId,
  onIniciarEdicao,
  onVendaCancelada,
}) {
  const toast = useToast();

  const [vendas, setVendas] = useState([]);
  const [vendaExpandida, setVendaExpandida] = useState(null);
  const [filtroStatus, setFiltroStatus] = useState('TODAS');
  const [carregando, setCarregando] = useState(false);
  const [pagina, setPagina] = useState(1);
  const [confirmarCancelamento, setConfirmarCancelamento] = useState(null);

  const fecharModalCancelar = useCallback(() => setConfirmarCancelamento(null), []);
  const modalCancelarRef = useFocusTrap(!!confirmarCancelamento, fecharModalCancelar);

  const carregarVendas = useCallback(async () => {
    setCarregando(true);
    try {
      const params = dataFiltro
        ? `data=${dataFiltro}&size=500&sort=id,desc`
        : 'size=200&sort=id,desc';
      const res = await api.get(`/vendas?${params}`);
      setVendas(res.data.content);
      setPagina(1);
    } catch (err) {
      const status = err?.response?.status;
      // 401/403 já redireciona via interceptor; 5xx/rede já mostra toast global.
      // 4xx é improvável aqui (listagem) — silencia.
      if (status && status !== 401 && status !== 403 && status < 500) {
        toast.error('Não foi possível carregar as vendas.');
      }
    } finally {
      setCarregando(false);
    }
  }, [dataFiltro, toast]);

  useEffect(() => { carregarVendas(); }, [carregarVendas, refreshKey]);

  async function executarCancelamento() {
    const venda = confirmarCancelamento;
    setConfirmarCancelamento(null);
    try {
      await api.put(`/vendas/${venda.id}/cancelar`);
      toast.success(`Venda #${venda.id} cancelada. Estoque restaurado se aplicável.`);
      onVendaCancelada?.(venda.id);
      carregarVendas();
    } catch (err) {
      const data = err.response?.data;
      const msg = typeof data === 'string' ? data : data?.mensagem || data?.message || 'Erro ao cancelar venda.';
      toast.error(msg);
    }
  }

  const vendasPorData = dataFiltro
    ? vendas.filter(v => toInputDate(v.data) === dataFiltro)
    : vendas;

  const vendasFiltradas = filtroStatus === 'TODAS'
    ? vendasPorData
    : vendasPorData.filter(v => v.status === filtroStatus);

  const totalPaginas = Math.max(1, Math.ceil(vendasFiltradas.length / PAGE_SIZE));
  const paginaAtual = Math.min(pagina, totalPaginas);
  const vendasPagina = vendasFiltradas.slice((paginaAtual - 1) * PAGE_SIZE, paginaAtual * PAGE_SIZE);

  return (
    <div className="card">
      {confirmarCancelamento && (
        <div className="modal-overlay" onClick={fecharModalCancelar}>
          <div
            ref={modalCancelarRef}
            className="modal-box"
            role="dialog"
            aria-modal="true"
            aria-labelledby="modal-cancelar-titulo"
            onClick={e => e.stopPropagation()}
          >
            <h3 id="modal-cancelar-titulo">Cancelar Venda #{confirmarCancelamento.id}</h3>
            <p>Total: <strong>R$ {Number(confirmarCancelamento.total).toFixed(2)}</strong> — {labelForma(confirmarCancelamento.formaPagamento)}</p>
            {confirmarCancelamento.status === 'FECHADA' && (
              <p>O estoque dos itens será restaurado automaticamente.</p>
            )}
            <p className="modal-aviso">Esta ação não pode ser desfeita.</p>
            <div className="modal-botoes">
              <button className="btn-secundario" onClick={fecharModalCancelar}>Voltar</button>
              <button className="btn-cancelar btn-modal-confirmar" onClick={executarCancelamento}>Confirmar Cancelamento</button>
            </div>
          </div>
        </div>
      )}

      <div className="cabecalho-lista">
        <h2>Vendas {dataFiltro && <span className="data-filtro-label">{new Date(dataFiltro + 'T00:00:00').toLocaleDateString('pt-BR')}</span>}</h2>
        <div className="lista-controles">
          <div className="filtro-data-wrapper">
            <input
              type="date"
              className="input-data-filtro"
              value={dataFiltro}
              onChange={e => { onDataFiltroChange(e.target.value); setPagina(1); setVendaExpandida(null); }}
            />
            {dataFiltro && (
              <button
                className="btn-hoje"
                onClick={() => { onDataFiltroChange(''); setPagina(1); setVendaExpandida(null); }}
                title="Limpar filtro de data"
              >
                ✕ Limpar
              </button>
            )}
          </div>
          <button className="btn-secundario" onClick={carregarVendas} disabled={carregando}>
            {carregando ? 'Atualizando...' : 'Atualizar'}
          </button>
        </div>
      </div>

      <div className="filtros-status">
        {STATUS_FILTROS.map(f => {
          const count = f === 'TODAS' ? vendasPorData.length : vendasPorData.filter(v => v.status === f).length;
          return (
            <button
              key={f}
              className={`filtro-btn ${filtroStatus === f ? 'filtro-ativo' : ''} filtro-${f.toLowerCase()}`}
              onClick={() => { setFiltroStatus(f); setVendaExpandida(null); setPagina(1); }}
            >
              {f} <span className="filtro-count">{count}</span>
            </button>
          );
        })}
      </div>

      {carregando ? (
        <p className="vazio">Carregando vendas...</p>
      ) : vendasFiltradas.length === 0 ? (
        <p className="vazio">Nenhuma venda com status {filtroStatus}.</p>
      ) : (
        <>
          <div className="tabela-wrapper">
            <table className="tabela">
              <thead>
                <tr>
                  <th></th>
                  <th>#ID</th>
                  <th>Data / Hora</th>
                  <th>Itens</th>
                  <th>Pagamento</th>
                  <th>Condição</th>
                  <th>Total</th>
                  <th>Status</th>
                  <th>Usuário</th>
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                {vendasPagina.map(v => {
                  const expandida = vendaExpandida === v.id;
                  return (
                    <Fragment key={v.id}>
                      <tr
                        className={`linha-venda ${vendaEditandoId === v.id ? 'linha-editando' : ''} ${expandida ? 'linha-expandida' : ''}`}
                        onClick={() => setVendaExpandida(expandida ? null : v.id)}
                      >
                        <td className="col-expand">{expandida ? '▾' : '▸'}</td>
                        <td>{v.id}</td>
                        <td className="col-datahora">
                          <span>{new Date(v.data).toLocaleDateString('pt-BR')}</span>
                          <span>{new Date(v.data).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })}</span>
                        </td>
                        <td>{v.itens?.length || 0} item(s)</td>
                        <td>{labelForma(v.formaPagamento)}</td>
                        <td>{labelCondicao(v.condicaoPagamento)}</td>
                        <td>R$ {Number(v.total).toFixed(2)}</td>
                        <td onClick={e => e.stopPropagation()}>
                          {v.status === 'PENDENTE' ? (
                            <span
                              className="badge badge-pendente badge-clicavel"
                              role="button"
                              tabIndex={0}
                              onClick={() => onIniciarEdicao?.(v)}
                              onKeyDown={e => {
                                if (e.key === 'Enter' || e.key === ' ') {
                                  e.preventDefault();
                                  onIniciarEdicao?.(v);
                                }
                              }}
                              title="Clique para editar esta venda pendente"
                              aria-label={`Editar venda pendente #${v.id}`}
                            >
                              PENDENTE ✎
                            </span>
                          ) : (
                            <span className={`badge ${v.status === 'CANCELADA' ? 'badge-cancelada' : 'badge-ok'}`}>
                              {v.status}
                            </span>
                          )}
                        </td>
                        <td className="col-usuario" title={v.usuario || ''}>
                          {v.usuario || <span className="sem-usuario">—</span>}
                        </td>
                        <td onClick={e => e.stopPropagation()}>
                          {v.status !== 'CANCELADA' && (
                            <button className="btn-cancelar" onClick={() => setConfirmarCancelamento(v)}>Cancelar</button>
                          )}
                        </td>
                      </tr>

                      {expandida && (
                        <tr className="linha-detalhes">
                          <td colSpan={10}>
                            <div className="detalhes-itens">
                              <table className="tabela-itens">
                                <thead>
                                  <tr>
                                    <th>Código</th>
                                    <th>Produto</th>
                                    <th>Qtd</th>
                                    <th>Preço Unit.</th>
                                    <th>Subtotal</th>
                                  </tr>
                                </thead>
                                <tbody>
                                  {v.itens?.map(i => (
                                    <tr key={i.codigoProduto}>
                                      <td>{i.codigoProduto}</td>
                                      <td>{i.nomeProduto}</td>
                                      <td>{i.quantidade}</td>
                                      <td>R$ {Number(i.precoUnit).toFixed(2)}</td>
                                      <td>R$ {Number(i.subtotal).toFixed(2)}</td>
                                    </tr>
                                  ))}
                                </tbody>
                                <tfoot>
                                  <tr>
                                    <td colSpan={4} style={{ textAlign: 'right', fontWeight: 'bold' }}>Total da Venda:</td>
                                    <td style={{ fontWeight: 'bold' }}>R$ {Number(v.total).toFixed(2)}</td>
                                  </tr>
                                </tfoot>
                              </table>

                              {v.pagamentos?.length > 1 && (
                                <table className="tabela-itens tabela-pagamentos">
                                  <thead>
                                    <tr>
                                      <th colSpan={3}>Formas de pagamento</th>
                                    </tr>
                                    <tr>
                                      <th>Forma</th>
                                      <th>Condição</th>
                                      <th>Valor</th>
                                    </tr>
                                  </thead>
                                  <tbody>
                                    {v.pagamentos.map((p, idx) => (
                                      <tr key={idx}>
                                        <td>{labelForma(p.formaPagamento)}</td>
                                        <td>{p.condicaoPagamento ? labelCondicao(p.condicaoPagamento) : '—'}</td>
                                        <td>R$ {Number(p.valor).toFixed(2)}</td>
                                      </tr>
                                    ))}
                                  </tbody>
                                </table>
                              )}
                            </div>
                          </td>
                        </tr>
                      )}
                    </Fragment>
                  );
                })}
              </tbody>
            </table>
          </div>
          {totalPaginas > 1 && (
            <div className="paginacao">
              <button
                className="btn-secundario btn-pag"
                onClick={() => setPagina(p => Math.max(1, p - 1))}
                disabled={paginaAtual === 1}
              >
                ← Anterior
              </button>
              <span className="pag-info">
                Página {paginaAtual} de {totalPaginas}
                <span className="pag-total"> ({vendasFiltradas.length} vendas)</span>
              </span>
              <button
                className="btn-secundario btn-pag"
                onClick={() => setPagina(p => Math.min(totalPaginas, p + 1))}
                disabled={paginaAtual === totalPaginas}
              >
                Próxima →
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
