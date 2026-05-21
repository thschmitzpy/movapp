import { memo } from 'react';

function CarrinhoPDV({ itens, total, onAlterarQuantidade, onRemoverItem }) {
  if (itens.length === 0) return null;

  return (
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
                  onChange={e => onAlterarQuantidade(i.codigoProduto, e.target.value)}
                  style={{ width: 64, textAlign: 'center' }}
                  aria-label={`Quantidade de ${i.nomeProduto}`}
                />
              </td>
              <td>R$ {Number(i.preco).toFixed(2)}</td>
              <td>R$ {(i.preco * i.quantidade).toFixed(2)}</td>
              <td>
                <button
                  className="btn-excluir"
                  onClick={() => onRemoverItem(i.codigoProduto)}
                  aria-label={`Remover ${i.nomeProduto} do carrinho`}
                >
                  Remover
                </button>
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
  );
}

export default memo(CarrinhoPDV);
