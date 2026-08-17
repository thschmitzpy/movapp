import { useState, useCallback, useRef } from 'react';
import api from '../services/api';
import ListaVendas from './venda/ListaVendas';
import CarrinhoPDV from './venda/CarrinhoPDV';
import BuscaProduto from './venda/BuscaProduto';
import FormaPagamento, { pagamentoVazio, novoUid } from './venda/FormaPagamento';

const novaIdempotencyKey = () =>
  (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function')
    ? crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(36).slice(2)}-${Math.random().toString(36).slice(2)}`;

export default function RealizarVenda({ onVendaAtualizada, dataFiltro, onDataFiltroChange }) {
  const [itens, setItens] = useState([]);
  const [pagamentos, setPagamentos] = useState([pagamentoVazio()]);
  const [mensagem, setMensagem] = useState(null);
  const [enviando, setEnviando] = useState(false);
  const idempotencyKeyRef = useRef(null);
  const ultimaAssinaturaRef = useRef(null);
  const [vendaEditando, setVendaEditando] = useState(null);
  const [refreshLista, setRefreshLista] = useState(0);
  const topoRef = useRef(null);

  const dispararRefresh = useCallback(() => setRefreshLista(n => n + 1), []);

  function exibirMensagem(texto, tipo = 'sucesso') {
    setMensagem({ texto, tipo });
    setTimeout(() => setMensagem(null), 4000);
  }

  const iniciarEdicao = useCallback((venda) => {
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

    setTimeout(() => topoRef.current?.scrollIntoView({ behavior: 'smooth' }), 50);
  }, []);

  const cancelarEdicao = useCallback(() => {
    setVendaEditando(null);
    setItens([]);
    setPagamentos([pagamentoVazio()]);
  }, []);


  const handleVendaCancelada = useCallback((idCancelada) => {
    setVendaEditando(prev => {
      if (prev?.id === idCancelada) {
        setItens([]);
        setPagamentos([pagamentoVazio()]);
        return null;
      }
      return prev;
    });
    onVendaAtualizada?.();
  }, [onVendaAtualizada]);

  const handleAdicionarItem = useCallback((produto, qtd) => {
    setItens(prev => {
      const existe = prev.find(i => i.codigoProduto === produto.codigo);
      if (existe) {
        return prev.map(i =>
          i.codigoProduto === produto.codigo
            ? { ...i, quantidade: i.quantidade + qtd, estoque: produto.estoque }
            : i
        );
      }
      return [...prev, {
        codigoProduto: produto.codigo,
        nomeProduto: produto.nome,
        preco: produto.preco,
        quantidade: qtd,
        estoque: produto.estoque,
      }];
    });
  }, []);

  const removerItem = useCallback((codigo) => {
    setItens(prev => prev.filter(i => i.codigoProduto !== codigo));
  }, []);

  const alterarQuantidadeItem = useCallback((codigo, novaQtd) => {
    const qtd = parseInt(novaQtd);
    if (!qtd || qtd < 1) return;
    const item = itens.find(i => i.codigoProduto === codigo);
    if (item?.estoque != null && qtd > item.estoque) {
      exibirMensagem(`Estoque insuficiente. Máximo disponível: ${item.estoque}`, 'erro');
      return;
    }
    setItens(itens.map(i => i.codigoProduto === codigo ? { ...i, quantidade: qtd } : i));
  }, [itens]);

  const total = itens.reduce((acc, i) => acc + i.preco * i.quantidade, 0);

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
    if (!vendaEditando) {
        const assinatura = JSON.stringify(body);
        if (!idempotencyKeyRef.current || ultimaAssinaturaRef.current !== assinatura) {
          idempotencyKeyRef.current = novaIdempotencyKey();
        }
        ultimaAssinaturaRef.current = assinatura;
      }

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
      ultimaAssinaturaRef.current = null
      cancelarEdicao();
      dispararRefresh();
      onVendaAtualizada?.();
    } catch (err) {
      const status = err.response?.status;

      if (status && status >= 400 && status < 500) {
        idempotencyKeyRef.current = null;
        ultimaAssinaturaRef.current = null;
      }
      const data = err.response?.data;
      const msg = typeof data === 'string' ? data : data?.mensagem || data?.message || data?.erro || 'Erro ao salvar venda.';
      exibirMensagem(msg, 'erro');
    } finally {
      setEnviando(false);
    }
  }

  return (
    <div className="secao">
      <div ref={topoRef} />

      {mensagem && (
        <div className={`alerta ${mensagem.tipo}`}>{mensagem.texto}</div>
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

        <BuscaProduto
          key={vendaEditando?.id ?? 'nova'}
          itens={itens}
          onAdicionarItem={handleAdicionarItem}
          exibirMensagem={exibirMensagem}
        />

        <CarrinhoPDV
          itens={itens}
          total={total}
          onAlterarQuantidade={alterarQuantidadeItem}
          onRemoverItem={removerItem}
        />

        <FormaPagamento
          pagamentos={pagamentos}
          setPagamentos={setPagamentos}
          total={total}
          vendaEditando={vendaEditando}
          enviando={enviando}
          temItens={itens.length > 0}
          onSubmit={handleSubmit}
        />
      </div>

      <ListaVendas
        dataFiltro={dataFiltro}
        onDataFiltroChange={onDataFiltroChange}
        refreshKey={refreshLista}
        vendaEditandoId={vendaEditando?.id}
        onIniciarEdicao={iniciarEdicao}
        onVendaCancelada={handleVendaCancelada}
      />
    </div>
  );
}
