import { useState, useEffect, useCallback } from 'react';
import api from '../services/api';

export default function Dashboard({ refreshAt, dataFiltro }) {
  const [metricas, setMetricas] = useState(null);

  const carregarMetricas = useCallback(async () => {
    try {
      const hoje = dataFiltro || new Date().toISOString().slice(0, 10);

      const [produtosRes, vendasDiaRes, vendasRecentesRes] = await Promise.all([
        api.get('/produtos', { params: { size: 1 } }),
        api.get('/vendas', { params: { data: hoje, size: 500, sort: 'id,desc' } }),
        api.get('/vendas', { params: { size: 200, sort: 'id,desc' } }),
      ]);

      const totalProdutos = produtosRes.data.totalElements || 0;
      const vendasDoDia = vendasDiaRes.data.content || [];
      const vendasRecentes = vendasRecentesRes.data.content || [];

      const vendasDia = vendasDoDia.filter(v => v.status === 'FECHADA');
      const vendasPendentes = vendasRecentes.filter(v => v.status === 'PENDENTE');
      const totalDia = vendasDia.reduce((acc, v) => acc + Number(v.total), 0);

      const dataLabel = dataFiltro
        ? new Date(dataFiltro + 'T00:00:00').toLocaleDateString('pt-BR')
        : 'Hoje';

      setMetricas({
        totalProdutos,
        vendasDia: vendasDia.length,
        totalDia,
        vendasPendentes: vendasPendentes.length,
        dataLabel,
      });
    } catch {
      // falha silenciosa — dashboard é complementar
    }
  }, [dataFiltro]);

  // Carga inicial + atualização automática a cada 1 min
  useEffect(() => {
    carregarMetricas();
    const intervalo = setInterval(carregarMetricas, 60000);
    return () => clearInterval(intervalo);
  }, [carregarMetricas]);

  // Atualiza imediatamente ao realizar/cancelar venda ou mudar data
  useEffect(() => {
    if (refreshAt) carregarMetricas();
  }, [refreshAt, carregarMetricas]);

  if (!metricas) return null;

  return (
    <div className="dashboard">
      <div className="dash-card dash-produtos">
        <div className="dash-icone">📦</div>
        <div className="dash-info">
          <span className="dash-valor">{metricas.totalProdutos}</span>
          <span className="dash-label">Produtos Cadastrados</span>
        </div>
      </div>

      <div className="dash-card dash-vendas">
        <div className="dash-icone">🛒</div>
        <div className="dash-info">
          <span className="dash-valor">{metricas.vendasDia}</span>
          <span className="dash-label">Vendas Finalizadas — {metricas.dataLabel}</span>
        </div>
      </div>

      <div className="dash-card dash-total">
        <div className="dash-icone">💰</div>
        <div className="dash-info">
          <span className="dash-valor">
            R$ {metricas.totalDia.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}
          </span>
          <span className="dash-label">Faturamento — {metricas.dataLabel}</span>
        </div>
      </div>

      <div className="dash-card dash-pendentes">
        <div className="dash-icone">⏳</div>
        <div className="dash-info">
          <span className="dash-valor">{metricas.vendasPendentes}</span>
          <span className="dash-label">Vendas Pendentes</span>
        </div>
      </div>
    </div>
  );
}
