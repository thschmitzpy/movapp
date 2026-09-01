import { useState, useEffect, useCallback, Fragment } from 'react';
import api from '../../services/api';
import { toast } from '../../services/toast';
import { useFocusTrap } from '../../hooks/useFocusTrap';
import { labelForma, labelCondicao } from './labels';

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
  const [vendas, setVendas] = useState([]);
  const [vendaExpandida, setVendaExpandida] = useState(null);
  const [filtroStatus, setFiltroStatus] = useState('TODAS');
  const [carregando, setCarregando] = useState(false);
  const [pagina, setPagina] = useState(1);
  const [totalPaginas, setTotalPaginas] = useState(1);
  const [totalElementos, setTotalElementos] = useState(0);
  const [confirmarCancelamento, setConfirmarCancelamento] = useState(null);

  const fecharModalCancelar = useCallback(() => setConfirmarCancelamento(null), []);
  const modalCancelarRef = useFocusTrap(!!confirmarCancelamento, fecharModalCancelar);

  const carregarVendas = useCallback(async (pg = 1) => {
    setCarregando(true);
    try {
      const params = { page: pg - 1, size: PAGE_SIZE, sort: 'id,desc' };
      if (dataFiltro) params.data = dataFiltro;
      if (filtroStatus !== 'TODAS') params.status = filtroStatus;
      const res = await api.get('/vendas', { params });
      setVendas(res.data.content || []);
      setTotalPaginas(res.data.totalPages || 1);
      setTotalElementos(res.data.totalElements || 0);
      setPagina(pg);
    } catch (err) {
      const status = err?.response?.status;

      if (status && status !== 401 && status !== 403 && status < 500) {
        toast.error('Não foi possível carregar as vendas.');
      }
    } finally {
      setCarregando(false);
    }
  }, [dataFiltro, filtroStatus]);

  useEffect(() => { carregarVendas(1); }, [carregarVendas, refreshKey]);

  async function executarCancelamento() {
    const venda = confirmarCancelamento;
    setConfirmarCancelamento(null);
    try {
      await api.put(`/vendas/${venda.id}/cancelar`);
      toast.success(`Venda #${venda.id} cancelada. Estoque restaurado se aplicável.`);
      onVendaCancelada?.(venda.id);
      carregarVendas(pagina);
    } catch (err) {
      const data = err.response?.data;
      const msg = typeof data === 'string' ? data : data?.mensagem || data?.message || 'Erro ao cancelar venda.';
      toast.error(msg);
    }
  }

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
              onChange={e => { onDataFiltroChange(e.target.value); setVendaExpandida(null); }}
            />
            {dataFiltro && (
              <button
                className="btn-hoje"
                onClick={() => { onDataFiltroChange(''); setVendaExpandida(null); }}
                title="Limpar filtro de data"
              >
                ✕ Limpar
              </button>
            )}
          </div>
          <button className="btn-secundario" onClick={() => carregarVendas(pagina)} disabled={carregando}>
            {carregando ? 'Atualizando...' : 'Atualizar'}
          </button>
        </div>
      </div>

      <div className="filtros-status">
        {STATUS_FILTROS.map(f => {
          const count = filtroStatus === f ? totalElementos : null;
          return (
            <button
              key={f}
              className={`filtro-btn ${filtroStatus === f ? 'filtro-ativo' : ''} filtro-${f.toLowerCase()}`}
              onClick={() => { setFiltroStatus(f); setVendaExpandida(null); }}
            >
              {f} {count != null && <span className="filtro-count">{count}</span>}
            </button>
          );
        })}
      </div>

      {carregando ? (
        <p className="vazio">Carregando vendas...</p>
      ) : vendas.length === 0 ? (
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
                {vendas.map(v => {
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
                onClick={() => carregarVendas(pagina - 1)}
                disabled={pagina === 1 || carregando}
              >
                ← Anterior
              </button>
              <span className="pag-info">
                Página {pagina} de {totalPaginas}
                <span className="pag-total"> ({totalElementos} vendas)</span>
              </span>
              <button
                className="btn-secundario btn-pag"
                onClick={() => carregarVendas(pagina + 1)}
                disabled={pagina === totalPaginas || carregando}
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
