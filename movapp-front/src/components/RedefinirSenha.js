import { useState } from 'react';
  import api from '../services/api';

  export default function RedefinirSenha({ tokenInicial, onVoltar, onSucesso }) {
    const [token, setToken] = useState(tokenInicial || '');
    const [novaSenha, setNovaSenha] = useState('');
    const [confirmar, setConfirmar] = useState('');
    const [erro, setErro] = useState('');
    const [carregando, setCarregando] = useState(false);

    async function handleSubmit(e) {
          e.preventDefault();
          setErro('');
          if (novaSenha.length < 6) {
            setErro('A nova senha deve ter no mínimo 6 caracteres.');
            return;
          }
          if (novaSenha !== confirmar) {
            setErro('As senhas não coincidem.');
            return;
          }
          setCarregando(true);
          try {
            await api.post('/auth/reset-password', { token, novaSenha });
            onSucesso();
          } catch (err) {
            const data = err.response?.data;
            setErro(typeof data === 'string' ? data : (data?.mensagem || 'Erro ao redefinir a senha.'));
          } finally {
            setCarregando(false);
          }
        }

        return (
              <div className="login-wrapper">
                <div className="login-card">
                  <div className="login-header">
                    <h1>Nova Senha</h1>
                    <p>Informe o token e defina a nova senha.</p>
                  </div>
                  <form onSubmit={handleSubmit} className="login-form">
                    {erro && <div className="alerta erro">{erro}</div>}
                    <div className="campo">
                      <label>Token</label>
                      <input
                        value={token}
                        onChange={e => setToken(e.target.value)}
                        required
                        placeholder="Cole o token recebido"
                        style={{ fontFamily: 'monospace', fontSize: 12 }}
                      />
                    </div>
                    <div className="campo">
                      <label>Nova senha</label>
                      <input
                        type="password"
                        value={novaSenha}
                        onChange={e => setNovaSenha(e.target.value)}
                        required
                        minLength={6}
                        placeholder="Mínimo 6 caracteres"
                      />
                    </div>
                    <div className="campo">
                      <label>Confirmar nova senha</label>
                      <input
                        type="password"
                        value={confirmar}
                        onChange={e => setConfirmar(e.target.value)}
                        required
                        minLength={6}
                        placeholder="Repita a nova senha"
                      />
                    </div>
                    <button type="submit" className="btn-primario btn-login" disabled={carregando}>
                      {carregando ? 'Salvando...' : 'Redefinir senha'}
                    </button>
                    <button type="button" className="link-esqueci" onClick={onVoltar}>
                      Voltar ao login
                    </button>
                  </form>
                </div>
              </div>
            );
          }