import { useState } from 'react';
  import api from '../services/api';

  export default function EsqueciSenha({ onVoltar, onProsseguir }) {
    const [username, setUsername] = useState('');
    const [carregando, setCarregando] = useState(false);
    const [erro, setErro] = useState('');
    const [resposta, setResposta] = useState(null); // {token, aviso, mensagem}

    async function handleSubmit(e) {
      e.preventDefault();
      setErro('');
      setResposta(null);
      setCarregando(true);
      try {
        const res = await api.post('/auth/forgot-password', { username });
        setResposta(res.data);
      } catch (err) {
        const data = err.response?.data;
        setErro(typeof data === 'string' ? data : (data?.mensagem || 'Erro ao solicitar redefinição.'));
      } finally {
        setCarregando(false);
      }
    }

    return (
      <div className="login-wrapper">
        <div className="login-card">
          <div className="login-header">
            <h1>Redefinir Senha</h1>
            <p>Informe seu usuário para receber um token de redefinição.</p>
          </div>

          {!resposta && (
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
              <button type="submit" className="btn-primario btn-login" disabled={carregando}>
                {carregando ? 'Enviando...' : 'Solicitar token'}
              </button>
              <button type="button" className="link-esqueci" onClick={onVoltar}>
                Voltar ao login
              </button>
            </form>
          )}

          {resposta && (
            <div className="login-form">
              <div className="alerta sucesso">
                <strong>{resposta.mensagem}</strong>
                {resposta.aviso && <div style={{ marginTop: 4, fontSize: 13 }}>{resposta.aviso}</div>}
              </div>
              {resposta.token && (
                <>
                  <div className="campo">
                    <label>Token gerado (expira em {resposta.expiraEmMinutos} min)</label>
                    <input value={resposta.token} readOnly style={{ fontFamily: 'monospace', fontSize: 12 }} />
                  </div>
                  <button
                    type="button"
                    className="btn-primario btn-login"
                    onClick={() => onProsseguir(resposta.token)}
                  >
                    Redefinir agora
                  </button>
                </>
              )}
              <button type="button" className="link-esqueci" onClick={onVoltar}>
                Voltar ao login
              </button>
            </div>
          )}
        </div>
      </div>
    );
  }