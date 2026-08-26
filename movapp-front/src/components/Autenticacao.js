import { useState } from 'react';
  import Login from './Login';
  import EsqueciSenha from './EsqueciSenha';
  import RedefinirSenha from './RedefinirSenha';

  export default function Autenticacao({ onLogin }) {
    const [tela, setTela] = useState('login');
    const [tokenPreenchido, setTokenPreenchido] = useState('');
    const [mensagemLogin, setMensagemLogin] = useState('');

    if (tela === 'esqueci') {
          return (
            <EsqueciSenha
              onVoltar={() => setTela('login')}
              onProsseguir={(token) => {
                setTokenPreenchido(token);
                setTela('redefinir');
              }}
            />
          );
        }

        if (tela === 'redefinir') {
          return (
            <RedefinirSenha
              tokenInicial={tokenPreenchido}
              onVoltar={() => setTela('login')}
              onSucesso={() => {
                setTokenPreenchido('');
                setMensagemLogin('Senha redefinida com sucesso. Faça login com a nova senha.');
                setTela('login');
              }}
            />
          );

          return (
                <Login
                  onLogin={onLogin}
                  mensagem={mensagemLogin}
                  onLimparMensagem={() => setMensagemLogin('')}
                  onEsqueciSenha={() => setTela('esqueci')}
                />
              );
            }