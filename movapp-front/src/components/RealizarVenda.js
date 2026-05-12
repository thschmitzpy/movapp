import { useState, useEffect, useCallback, useRef, Fragment } from 'react';
import api from '../services/api';

const FORMAS_PAGAMENTO = ['DINHEIRO', 'CARTAO_CREDITO', 'CARTAO_DEBITO', 'PIX'];
const CONDICOES_PAGAMENTO = ['A_VISTA', 'PARCELADO_2X', 'PARCELADO_3X', 'PARCELADO_6X', 'PARCELADO_12X'];

const CONDICOES_POR_FORMA = {
  DINHEIRO:       ['A_VISTA'],
  PIX:            ['A_VISTA'],
  CARTAO_DEBITO:  ['A_VISTA'],
  CARTAO_CREDITO: CONDICOES_PAGAMENTO,
};

const LABEL_FORMA = {
  DINHEIRO:       'Dinheiro',
  CARTAO_CREDITO: 'Cartão de Crédito',
  CARTAO_DEBITO:  'Cartão de Débito',
  PIX:            'PIX',
  MISTO:          'Múltiplas formas',
};

let _uidPag = 0;
const novoUid = () => `p${++_uidPag}_${Date.now()}`;

// UUID v4 para a chave de idempotência. Usa crypto.randomUUID quando disponível
// (browsers modernos / contextos seguros) e cai em fallback caso contrário.
const novaIdempotencyKey = () =>
  (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function')
    ? crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(36).slice(2)}-${Math.random().toString(36).slice(2)}`;

function pagamentoVazio(valor = '') {
  return { uid: novoUid(), formaPagamento: '', condicaoPagamento: '', valor };
}

const LABEL_CONDICAO = {
  A_VISTA:       'À Vista',
  PARCELADO_2X:  'Parcelado 2x',
  PARCELADO_3X:  'Parcelado 3x',
  PARCELADO_6X:  'Parcelado 6x',
  PARCELADO_12X: 'Parcelado 12x',
};

const labelForma   = v => LABEL_FORMA[v]   ?? v;
const labelCondicao = v => LABEL_CONDICAO[v] ?? v;

function toInputDate(dt) {
  const d = new Date(dt);
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${mm}-${dd}`;
}

export default function RealizarVenda({ onVendaAtualizada, dataFiltro, onDataFiltroChange }) {
  const [itens, setItens] = useState([]);
  const [codigoBusca, setCodigoBusca] = useState('');
  const [quantidade, setQuantidade] = useState('');
  const [produtoBuscado, setProdutoBuscado] = useState(null);
  const [pagamentos, setPagamentos] = useState([pagamentoVazio()]);
  const [mensagem, setMensagem] = useState(null);
  const [vendas, setVendas] = useState([]);
  const [buscandoProduto, setBuscandoProduto] = useState(false);
  const [enviando, setEnviando] = useState(false);
  // Chave de idempotência da finalização em curso. Gerada na 1ª tentativa e
  // mantida durante retries (erro de rede / 5xx) para que o backend reconheça
  // a mesma intenção e não duplique a venda. Resetada em sucesso e em erros
  // 4xx (que indicam que o usuário precisa corrigir e enviar uma nova venda).
  const idempotencyKeyRef = useRef(null);
  const [vendaEditando, setVendaEditando] = useState(null); // venda PENDENTE em edição
  const [vendaExpandida, setVendaExpandida] = useState(null); // id da venda com itens visíveis
  const [filtroStatus, setFiltroStatus] = useState('TODAS');
  const [carregandoVendas, setCarregandoVendas] = useState(false);
  const [pagina, setPagina] = useState(1);
  const [confirmarCancelamento, setConfirmarCancelamento] = useState(null);
  const topoRef = useRef(null);

  const PAGE_SIZE = 20;

  const carregarVendas = useCallback(async () => {
    setCarregandoVendas(true);
    try {
      const params = dataFiltro
        ? `data=${dataFiltro}&size=500&sort=id,desc`
        : 'size=200&sort=id,desc';
      const res = await api.get(`/vendas?${params}`);
      setVendas(res.data.content);
      setPagina(1);
    } catch (err) {
      const status = err?.response?.status;
      if (status !== 401 && status !== 403) {
        exibirMensagem('Erro ao carregar vendas. Verifique se o servidor está disponível.', 'erro');
      }
    } finally {
      setCarregandoVendas(false);
    }
  }, [dataFiltro]);

  useEffect(() => { carregarVendas(); }, [carregarVendas]);

  function exibirMensagem(texto, tipo = 'sucesso') {
    setMensagem({ texto, tipo });
    setTimeout(() => setMensagem(null), 4000);
  }

  function iniciarEdicao(venda) {
    setVendaEditando(venda);
    setItens(venda.itens.map(i => ({
      codigoProduto: i.codigoProduto,
      nomeProduto: i.nomeProduto,
      preco: i.precoUnit,
      quantidade: i.quantidade,
    })));
    const pagsBackend = venda.pagamentos?.length
      ? venda.pagamentos.map(p => ({
          uid: novoUid(),
          formaPagamento: p.formaPagamento || '',
          condicaoPagamento: p.condicaoPagamento || '',
          valor: p.valor != null ? String(p.valor) : '',
        }))
      : [{
          uid: novoUid(),
          formaPagamento: venda.formaPagamento || '',
          condicaoPagamento: venda.condicaoPagamento || '',
          valor: venda.total != null ? String(venda.total) : '',
        }];
    setPagamentos(pagsBackend);
    setProdutoBuscado(null);
    setCodigoBusca('');
    setQuantidade('');
    setTimeout(() => topoRef.current?.scrollIntoView({ behavior: 'smooth' }), 50);
  }

  function cancelarEdicao() {
    setVendaEditando(null);
    setItens([]);
    setPagamentos([pagamentoVazio()]);
    setProdutoBuscado(null);
    setCodigoBusca('');
    setQuantidade('');
  }

  async function buscarProduto() {
    if (!codigoBusca.trim()) return;
    setBuscandoProduto(true);
    try {
      const res = await api.get(`/produtos/${codigoBusca.trim()}`);
      setProdutoBuscado(res.data);
    } catch {
      setProdutoBuscado(null);
      exibirMensagem(`Produto "${codigoBusca}" não encontrado.`, 'erro');
    } finally {
      setBuscandoProduto(false);
    }
  }

  function adicionarItem() {
    if (!produtoBuscado) return;
    const qtd = parseInt(quantidade);
    if (!qtd || qtd < 1) { exibirMensagem('Informe uma quantidade válida.', 'erro'); return; }
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
    const existe = itens.find(i => i.codigoProduto === produtoBuscado.codigo);
    if (existe) {
      setItens(itens.map(i =>
        i.codigoProduto === produtoBuscado.codigo
          ? { ...i, quantidade: i.quantidade + qtd }
          : i
      ));
    } else {
      setItens([...itens, {
        codigoProduto: produtoBuscado.codigo,
        nomeProduto: produtoBuscado.nome,
        preco: produtoBuscado.preco,
        quantidade: qtd,
        estoque: produtoBuscado.estoque,
      }]);
    }
    setProdutoBuscado(null);
    setCodigoBusca('');
    setQuantidade('');
  }

  function removerItem(codigo) {
    setItens(itens.filter(i => i.codigoProduto !== codigo));
  }

  function alterarQuantidadeItem(codigo, novaQtd) {
    const qtd = parseInt(novaQtd);
    if (!qtd || qtd < 1) return;
    const item = itens.find(i => i.codigoProduto === codigo);
    if (item?.estoque != null && qtd > item.estoque) {
      exibirMensagem(`Estoque insuficiente. Máximo disponível: ${item.estoque}`, 'erro');
      return;
    }
    setItens(itens.map(i => i.codigoProduto === codigo ? { ...i, quantidade: qtd } : i));
  }

  function atualizarPagamento(idx, campo, valor) {
    setPagamentos(prev => prev.map((p, i) => {
      if (i !== idx) return p;
      const next = { ...p, [campo]: valor };
      if (campo === 'formaPagamento') {
        const condicoesPermitidas = CONDICOES_POR_FORMA[valor] ?? [];
        if (!condicoesPermitidas.includes(next.condicaoPagamento)) {
          next.condicaoPagamento = condicoesPermitidas.length === 1 ? condicoesPermitidas[0] : '';
        }
      }
      return next;
    }));
  }

  function adicionarPagamento() {
    setPagamentos(prev => {
      // Indo de 1 → 2: o primeiro passa a ter o total como valor explícito,
      // e o segundo é criado em branco para o vendedor preencher.
      if (prev.length === 1) {
        const valorPrimeiro = total > 0 ? total.toFixed(2) : '';
        return [{ ...prev[0], valor: valorPrimeiro }, pagamentoVazio('')];
      }
      const jaAlocado = prev.reduce((acc, p) => acc + (Number(p.valor) || 0), 0);
      const restanteAtual = Math.max(0, total - jaAlocado);
      return [...prev, pagamentoVazio(restanteAtual ? restanteAtual.toFixed(2) : '')];
    });
  }

  function removerPagamento(idx) {
    setPagamentos(prev => {
      if (prev.length === 1) return prev;
      const restantes = prev.filter((_, i) => i !== idx);
      if (restantes.length === 1) return [{ ...restantes[0], valor: '' }];
      return restantes;
    });
  }

  function preencherRestante(idx) {
    setPagamentos(prev => {
      const outras = prev.reduce((acc, p, i) => i === idx ? acc : acc + (Number(p.valor) || 0), 0);
      const falta = Math.max(0, total - outras);
      return prev.map((p, i) => i === idx ? { ...p, valor: falta.toFixed(2) } : p);
    });
  }

  const total = itens.reduce((acc, i) => acc + i.preco * i.quantidade, 0);
  const pagamentoUnico = pagamentos.length === 1;
  // Para 1 forma de pagamento, o valor é implícito = total da venda.
  // Para múltiplas, soma o que o vendedor digitou em cada linha.
  const somaPagamentos = pagamentoUnico ? total : pagamentos.reduce((acc, p) => acc + (Number(p.valor) || 0), 0);
  const restante = +(total - somaPagamentos).toFixed(2);
  const pagamentoIncompleto = pagamentos.some(p => !p.formaPagamento || !p.condicaoPagamento);

  async function handleSubmit(statusVenda) {
    if (itens.length === 0) { exibirMensagem('Adicione pelo menos 1 item.', 'erro'); return; }

    const pagsValidos = pagamentos.filter(p =>
      p.formaPagamento || p.condicaoPagamento || (p.valor && Number(p.valor) > 0)
    );

    if (pagsValidos.length === 0) { exibirMensagem('Informe ao menos uma forma de pagamento.', 'erro'); return; }

    if (pagsValidos.length !== pagamentos.length) {
      setPagamentos(pagsValidos.length === 1 ? [{ ...pagsValidos[0], valor: '' }] : pagsValidos);
    }

    const ehUnico = pagsValidos.length === 1;

    for (const p of pagsValidos) {
      if (!p.formaPagamento) { exibirMensagem('Selecione a forma em todos os pagamentos.', 'erro'); return; }
      if (!p.condicaoPagamento) { exibirMensagem('Selecione a condição em todos os pagamentos.', 'erro'); return; }
    }

    // Valor só precisa ser validado quando há mais de uma forma de pagamento
    // (com forma única, o valor é implícito = total da venda).
    if (!ehUnico) {
      for (const p of pagsValidos) {
        if (!p.valor || Number(p.valor) <= 0) { exibirMensagem('Informe um valor maior que zero em cada pagamento.', 'erro'); return; }
      }
      const soma = pagsValidos.reduce((acc, p) => acc + Number(p.valor), 0);
      const restanteCalc = +(total - soma).toFixed(2);
      if (Math.abs(restanteCalc) > 0.001) {
        exibirMensagem(
          restanteCalc > 0
            ? `Faltam R$ ${restanteCalc.toFixed(2)} para fechar o total da venda.`
            : `Pagamentos excedem o total em R$ ${Math.abs(restanteCalc).toFixed(2)}.`,
          'erro'
        );
        return;
      }
    }

    setEnviando(true);
    const body = {
      itens: itens.map(i => ({ codigoProduto: i.codigoProduto, quantidade: i.quantidade })),
      pagamentos: pagsValidos.map(p => ({
        formaPagamento: p.formaPagamento,
        condicaoPagamento: p.condicaoPagamento,
        valor: ehUnico ? total : Number(p.valor),
      })),
      status: statusVenda,
    };

    try {
      if (vendaEditando) {
        await api.put(`/vendas/${vendaEditando.id}`, body);
        exibirMensagem(statusVenda === 'FECHADA'
          ? `Venda #${vendaEditando.id} finalizada com sucesso!`
          : `Venda #${vendaEditando.id} atualizada e mantida como pendente.`
        );
      } else {
        if (!idempotencyKeyRef.current) {
          idempotencyKeyRef.current = novaIdempotencyKey();
        }
        await api.post('/vendas', body, {
          headers: { 'Idempotency-Key': idempotencyKeyRef.current },
        });
        exibirMensagem('Venda realizada com sucesso!');
      }
      idempotencyKeyRef.current = null;
      cancelarEdicao();
      carregarVendas();
      onVendaAtualizada?.();
    } catch (err) {
      const status = err.response?.status;
      // 4xx → cliente precisa corrigir o payload, próxima tentativa é uma nova intenção.
      // 5xx / sem resposta (rede) → mantém a chave para retry seguro.
      if (status && status >= 400 && status < 500) {
        idempotencyKeyRef.current = null;
      }
      const data = err.response?.data;
      const msg = typeof data === 'string' ? data : data?.mensagem || data?.message || data?.erro || 'Erro ao salvar venda.';
      exibirMensagem(msg, 'erro');
    } finally {
      setEnviando(false);
    }
  }

  async function executarCancelamento() {
    const venda = confirmarCancelamento;
    setConfirmarCancelamento(null);
    try {
      await api.put(`/vendas/${venda.id}/cancelar`);
      exibirMensagem(`Venda #${venda.id} cancelada. Estoque restaurado se aplicável.`);
      if (vendaEditando?.id === venda.id) cancelarEdicao();
      carregarVendas();
      onVendaAtualizada?.();
    } catch (err) {
      const data = err.response?.data;
      const msg = typeof data === 'string' ? data : data?.mensagem || data?.message || 'Erro ao cancelar venda.';
      exibirMensagem(msg, 'erro');
    }
  }

  return (
    <div className="secao">
      <div ref={topoRef} />

      {mensagem && (
        <div className={`alerta ${mensagem.tipo}`}>{mensagem.texto}</div>
      )}

      {confirmarCancelamento && (
        <div className="modal-overlay">
          <div className="modal-box">
            <h3>Cancelar Venda #{confirmarCancelamento.id}</h3>
            <p>Total: <strong>R$ {Number(confirmarCancelamento.total).toFixed(2)}</strong> — {labelForma(confirmarCancelamento.formaPagamento)}</p>
            {confirmarCancelamento.status === 'FECHADA' && (
              <p>O estoque dos itens será restaurado automaticamente.</p>
            )}
            <p className="modal-aviso">Esta ação não pode ser desfeita.</p>
            <div className="modal-botoes">
              <button className="btn-secundario" onClick={() => setConfirmarCancelamento(null)}>Voltar</button>
              <button className="btn-cancelar btn-modal-confirmar" onClick={executarCancelamento}>Confirmar Cancelamento</button>
            </div>
          </div>
        </div>
      )}

      <div className="card">
        <div className="cabecalho-venda">
          <h2>
            {vendaEditando
              ? `Editando Venda #${vendaEditando.id} — PENDENTE`
              : 'Nova Venda'}
          </h2>
          {vendaEditando && (
            <button className="btn-secundario" onClick={cancelarEdicao}>
              Descartar edição
            </button>
          )}
        </div>

        <div className="busca-produto">
          <h3>Adicionar Produto</h3>
          <div className="linha-busca">
            <input
              placeholder="Código do produto"
              value={codigoBusca}
              onChange={e => setCodigoBusca(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && buscarProduto()}
            />
            <input
              type="number" min="1" value={quantidade}
              onChange={e => setQuantidade(e.target.value)}
              style={{ width: 80 }}
              placeholder="Qtd"
            />
            <button className="btn-secundario" onClick={buscarProduto} disabled={buscandoProduto}>
              {buscandoProduto ? 'Buscando...' : 'Buscar'}
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

        {itens.length > 0 && (
          <div className="carrinho">
            <h3>Itens da Venda</h3>
            <table className="tabela">
              <thead>
                <tr>
                  <th>Código</th>
                  <th>Produto</th>
                  <th>Qtd</th>
                  <th>Preço Unit.</th>
                  <th>Subtotal</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {itens.map(i => (
                  <tr key={i.codigoProduto}>
                    <td>{i.codigoProduto}</td>
                    <td>{i.nomeProduto}</td>
                    <td>
                      <input
                        type="number"
                        min="1"
                        max={i.estoque ?? undefined}
                        value={i.quantidade}
                        onChange={e => alterarQuantidadeItem(i.codigoProduto, e.target.value)}
                        style={{ width: 64, textAlign: 'center' }}
                      />
                    </td>
                    <td>R$ {Number(i.preco).toFixed(2)}</td>
                    <td>R$ {(i.preco * i.quantidade).toFixed(2)}</td>
                    <td>
                      <button className="btn-excluir" onClick={() => removerItem(i.codigoProduto)}>Remover</button>
                    </td>
                  </tr>
                ))}
              </tbody>
              <tfoot>
                <tr>
                  <td colSpan={4} style={{ textAlign: 'right', fontWeight: 'bold' }}>Total:</td>
                  <td colSpan={2} style={{ fontWeight: 'bold', color: '#2e7d32' }}>
                    R$ {total.toFixed(2)}
                  </td>
                </tr>
              </tfoot>
            </table>
          </div>
        )}

        <div className="form-pagamento">
          <h3>Pagamento</h3>

          {pagamentos.map((p, idx) => (
            <div key={p.uid} className="pagamento-linha">
              <div className="campo">
                <label>Forma *</label>
                <select
                  value={p.formaPagamento}
                  onChange={e => atualizarPagamento(idx, 'formaPagamento', e.target.value)}
                >
                  <option value="">Selecione...</option>
                  {FORMAS_PAGAMENTO.map(f => <option key={f} value={f}>{labelForma(f)}</option>)}
                </select>
              </div>
              <div className="campo">
                <label>Condição *</label>
                <select
                  value={p.condicaoPagamento}
                  onChange={e => atualizarPagamento(idx, 'condicaoPagamento', e.target.value)}
                  disabled={!p.formaPagamento}
                >
                  <option value="">Selecione...</option>
                  {(CONDICOES_POR_FORMA[p.formaPagamento] ?? CONDICOES_PAGAMENTO).map(c => (
                    <option key={c} value={c}>{labelCondicao(c)}</option>
                  ))}
                </select>
              </div>
              <div className="campo">
                <label>Valor R$ {!pagamentoUnico && '*'}</label>
                <div className="campo-valor-linha">
                  <input
                    type="number"
                    step="0.01"
                    min="0.01"
                    value={pagamentoUnico ? (total > 0 ? total.toFixed(2) : '') : p.valor}
                    onChange={e => atualizarPagamento(idx, 'valor', e.target.value)}
                    disabled={pagamentoUnico}
                    title={pagamentoUnico ? 'Pagamento único — valor é o total da venda' : ''}
                  />
                  {!pagamentoUnico && (
                    <button
                      type="button"
                      className="btn-secundario btn-preencher"
                      onClick={() => preencherRestante(idx)}
                      title="Preencher com o restante"
                      disabled={total <= 0}
                    >
                      =
                    </button>
                  )}
                </div>
              </div>
              {pagamentos.length > 1 && (
                <button
                  type="button"
                  className="btn-excluir btn-remover-pag"
                  onClick={() => removerPagamento(idx)}
                  title="Remover esta forma de pagamento"
                >
                  ×
                </button>
              )}
            </div>
          ))}

          <div className="pagamento-acoes">
            <button
              type="button"
              className="btn-secundario"
              onClick={adicionarPagamento}
              disabled={itens.length === 0 || pagamentoIncompleto}
              title={pagamentoIncompleto ? 'Preencha forma e condição dos pagamentos atuais antes de adicionar outro' : ''}
            >
              + Adicionar forma de pagamento
            </button>
          </div>

          {!pagamentoUnico && (
            <div className="resumo-pagamento">
              <div>Total da venda: <strong>R$ {total.toFixed(2)}</strong></div>
              <div>Total informado: <strong>R$ {somaPagamentos.toFixed(2)}</strong></div>
              <div className={Math.abs(restante) > 0.001 ? 'restante-erro' : 'restante-ok'}>
                {restante > 0 && <>Restante: <strong>R$ {restante.toFixed(2)}</strong></>}
                {restante < 0 && <>Excedente: <strong>R$ {Math.abs(restante).toFixed(2)}</strong></>}
                {Math.abs(restante) <= 0.001 && total > 0 && <>✓ Pagamento completo</>}
              </div>
            </div>
          )}

          <div className="botoes-venda">
            <button
              type="button"
              className="btn-pendente"
              disabled={enviando || itens.length === 0}
              onClick={() => handleSubmit('PENDENTE')}
            >
              {enviando ? 'Enviando...' : vendaEditando ? 'Manter Pendente' : 'Deixar Pendente'}
            </button>
            <button
              type="button"
              className="btn-primario btn-venda"
              disabled={enviando || itens.length === 0}
              onClick={() => handleSubmit('FECHADA')}
            >
              {enviando ? 'Enviando...' : `Finalizar Venda — R$ ${total.toFixed(2)}`}
            </button>
          </div>
        </div>
      </div>

      <div className="card">
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
            <button className="btn-secundario" onClick={carregarVendas} disabled={carregandoVendas}>
              {carregandoVendas ? 'Atualizando...' : 'Atualizar'}
            </button>
          </div>
        </div>

        <div className="filtros-status">
          {['TODAS', 'FECHADA', 'PENDENTE', 'CANCELADA'].map(f => {
            const base = dataFiltro
              ? vendas.filter(v => toInputDate(v.data) === dataFiltro)
              : vendas;
            const count = f === 'TODAS' ? base.length : base.filter(v => v.status === f).length;
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

        {carregandoVendas ? (
          <p className="vazio">Carregando vendas...</p>
        ) : (() => {
          const vendasPorData = dataFiltro
            ? vendas.filter(v => toInputDate(v.data) === dataFiltro)
            : vendas;

          const vendasFiltradas = filtroStatus === 'TODAS'
            ? vendasPorData
            : vendasPorData.filter(v => v.status === filtroStatus);

          const totalPaginas = Math.max(1, Math.ceil(vendasFiltradas.length / PAGE_SIZE));
          const paginaAtual = Math.min(pagina, totalPaginas);
          const vendasPagina = vendasFiltradas.slice((paginaAtual - 1) * PAGE_SIZE, paginaAtual * PAGE_SIZE);

          return vendasFiltradas.length === 0 ? (
            <p className="vazio">Nenhuma venda com status {filtroStatus}.</p>
          ) : (
          <Fragment>
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
                        className={`linha-venda ${vendaEditando?.id === v.id ? 'linha-editando' : ''} ${expandida ? 'linha-expandida' : ''}`}
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
                              onClick={() => iniciarEdicao(v)}
                              title="Clique para editar esta venda pendente"
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
          </Fragment>
          );
        })()}
      </div>
    </div>
  );
}
