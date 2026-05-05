import { useState } from 'react';
import Login from './components/Login';
import Dashboard from './components/Dashboard';
import CadastroProduto from './components/CadastroProduto';
import RealizarVenda from './components/RealizarVenda';
import api from './services/api';
import './App.css';

const ABAS = [
  { id: 'cadastro', label: 'Cadastro de Produtos' },
  { id: 'venda', label: 'Realizar Venda' },
];

function App() {
  const tokenSalvo = localStorage.getItem('token');
  const usuarioSalvo = localStorage.getItem('usuario');

  const [logado, setLogado] = useState(!!tokenSalvo);
  const [usuario, setUsuario] = useState(usuarioSalvo ? JSON.parse(usuarioSalvo) : null);
  const [abaAtiva, setAbaAtiva] = useState('cadastro');
  const [dashRefresh, setDashRefresh] = useState(0);
  const [dataFiltro, setDataFiltro] = useState('');

  function atualizarDash() { setDashRefresh(n => n + 1); }

  function handleLogin(dados) {
    setUsuario(dados);
    setLogado(true);
  }

  async function handleLogout() {
    try { await api.post('/auth/logout'); } catch {}
    localStorage.removeItem('token');
    localStorage.removeItem('usuario');
    setLogado(false);
    setUsuario(null);
  }

  if (!logado) {
    return <Login onLogin={handleLogin} />;
  }

  const isAdmin = usuario?.role === 'ROLE_ADMIN';

  return (
    <div className="app">
      <header className="header">
        <div>
          <h1>MovApp</h1>
          <p>Sistema de Gestão de Loja</p>
        </div>
        <div className="header-usuario">
          <span>{usuario?.username} {isAdmin ? '(Admin)' : '(Usuário)'}</span>
          <button className="btn-logout" onClick={handleLogout}>Sair</button>
        </div>
      </header>

      <nav className="abas">
        {ABAS.map(aba => (
          <button
            key={aba.id}
            className={`aba ${abaAtiva === aba.id ? 'aba-ativa' : ''}`}
            onClick={() => setAbaAtiva(aba.id)}
          >
            {aba.label}
          </button>
        ))}
      </nav>

      {abaAtiva === 'venda' && <Dashboard refreshAt={dashRefresh} dataFiltro={dataFiltro} />}

      <main className="conteudo">
        {abaAtiva === 'cadastro' && <CadastroProduto isAdmin={isAdmin} />}
        {abaAtiva === 'venda' && (
          <RealizarVenda
            onVendaAtualizada={atualizarDash}
            dataFiltro={dataFiltro}
            onDataFiltroChange={setDataFiltro}
          />
        )}
      </main>
    </div>
  );
}

export default App;
