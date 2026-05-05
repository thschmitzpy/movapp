import { useState } from 'react';
import api from '../services/api';

export default function Login({ onLogin }) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [erro, setErro] = useState('');
  const [carregando, setCarregando] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setErro('');
    setCarregando(true);
    try {
      const res = await api.post('/auth/login', { username, password });
      localStorage.setItem('token', res.data.token);
      localStorage.setItem('usuario', JSON.stringify({ username: res.data.username, role: res.data.role }));
      onLogin(res.data);
    } catch (err) {
      setErro(err.response?.data || 'Erro ao conectar com o servidor.');
    } finally {
      setCarregando(false);
    }
  }

  return (
    <div className="login-wrapper">
      <div className="login-card">
        <div className="login-header">
          <h1>MovApp</h1>
          <p>Sistema de Gestão de Loja</p>
        </div>
        <form onSubmit={handleSubmit} className="login-form">
          {erro && <div className="alerta erro">{erro}</div>}
          <div className="campo">
            <label>Usuário</label>
            <input
              value={username}
              onChange={e => setUsername(e.target.value)}
              required
              autoFocus
              placeholder="Digite seu usuário"
            />
          </div>
          <div className="campo">
            <label>Senha</label>
            <input
              type="password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              required
              placeholder="Digite sua senha"
            />
          </div>
          <button type="submit" className="btn-primario btn-login" disabled={carregando}>
            {carregando ? 'Entrando...' : 'Entrar'}
          </button>
        </form>
      </div>
    </div>
  );
}
