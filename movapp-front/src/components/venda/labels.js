export const FORMAS_PAGAMENTO = ['DINHEIRO', 'CARTAO_CREDITO', 'CARTAO_DEBITO', 'PIX'];
export const CONDICOES_PAGAMENTO = ['A_VISTA', 'PARCELADO_2X', 'PARCELADO_3X', 'PARCELADO_6X', 'PARCELADO_12X'];

export const CONDICOES_POR_FORMA = {
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

const LABEL_CONDICAO = {
  A_VISTA:       'À Vista',
  PARCELADO_2X:  'Parcelado 2x',
  PARCELADO_3X:  'Parcelado 3x',
  PARCELADO_6X:  'Parcelado 6x',
  PARCELADO_12X: 'Parcelado 12x',
};

export const labelForma    = v => LABEL_FORMA[v]    ?? v;
export const labelCondicao = v => LABEL_CONDICAO[v] ?? v;

export function toInputDate(dt) {
  const d = new Date(dt);
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${mm}-${dd}`;
}
