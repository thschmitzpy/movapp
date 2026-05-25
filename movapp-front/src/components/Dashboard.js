import { useState, useEffect, useCallback } from 'react';
import api from '../services/api';

export default function Dashboard({ refreshAt, dataFiltro }) {
  const [metricas, setMetricas] = useState(null);

  const carregarMetricas = useCallback(async () => {
    try {
      const paramsResumo = dataFiltro ? { data: dataFiltro } : {};

      const [produtosRes, resumoRes] = await Promise.all([
        api.get('/produtos', { params: { size: 1 } }),
        api.get('/vendas/resumo', { params: paramsResumo }),
      ]);

      const totalProdutos = produtosRes.data.totalElements || 0;
      const resumo = resumoRes.data || {};

      const dataLabel = dataFiltro
        ? new Date(dataFiltro + 'T00:00:00').toLocaleDateString('pt-BR')
        : 'Geral';

      setMetricas({
        totalProdutos,
        vendasDia: Number(resumo.vendasFechadas) || 0,
        totalDia: Number(resumo.totalFechadas) || 0,
        vendasPendentes: Number(resumo.vendasPendentes) || 0,
        dataLabel,
      });
    } catch {

    }
  }, [dataFiltro]);

  useEffect(() => {
    carregarMetricas();
    const intervalo = setInterval(carregarMetricas, 60000);
    return () => clearInterval(intervalo);
  }, [carregarMetricas]);

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
          <span className="dash-label">Vendas Pendentes — {metricas.dataLabel}</span>
        </div>
      </div>
    </div>
  );
}
