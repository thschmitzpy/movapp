import {
  FORMAS_PAGAMENTO,
  CONDICOES_PAGAMENTO,
  CONDICOES_POR_FORMA,
  labelForma,
  labelCondicao,
} from './labels';

let _uidPag = 0;
export const novoUid = () => `p${++_uidPag}_${Date.now()}`;

export function pagamentoVazio(valor = '') {
  return { uid: novoUid(), formaPagamento: '', condicaoPagamento: '', valor };
}

export default function FormaPagamento({
  pagamentos,
  setPagamentos,
  total,
  vendaEditando,
  enviando,
  temItens,
  onSubmit,
}) {
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
      // Indo de 1 -> 2: o primeiro passa a ter o total como valor explícito,
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

  const pagamentoUnico = pagamentos.length === 1;
  // Para 1 forma de pagamento, o valor é implícito = total da venda.
  // Para múltiplas, soma o que o vendedor digitou em cada linha.
  const somaPagamentos = pagamentoUnico ? total : pagamentos.reduce((acc, p) => acc + (Number(p.valor) || 0), 0);
  const restante = +(total - somaPagamentos).toFixed(2);
  const pagamentoIncompleto = pagamentos.some(p => !p.formaPagamento || !p.condicaoPagamento);

  return (
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
              aria-label="Remover esta forma de pagamento"
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
          disabled={!temItens || pagamentoIncompleto}
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
          disabled={enviando || !temItens}
          onClick={() => onSubmit('PENDENTE')}
        >
          {enviando ? 'Enviando...' : vendaEditando ? 'Manter Pendente' : 'Deixar Pendente'}
        </button>
        <button
          type="button"
          className="btn-primario btn-venda"
          disabled={enviando || !temItens}
          onClick={() => onSubmit('FECHADA')}
        >
          {enviando ? 'Enviando...' : `Finalizar Venda — R$ ${total.toFixed(2)}`}
        </button>
      </div>
    </div>
  );
}
