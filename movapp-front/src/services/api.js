import axios from 'axios';
import { toast } from './toast';

const api = axios.create({
  baseURL: 'http://localhost:8080',
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`;
  }
  return config;
});

// Trata erros transversais:
//   401/403 -> sessão expirada, redireciona para login
//   5xx     -> falha do servidor, toast global
//   sem response → rede/CORS/servidor fora do ar, toast global
//   4xx     -> silencioso aqui (cada componente decide a UX contextual)
api.interceptors.response.use(
  response => response,
  error => {
    const status = error.response?.status;

    if (status === 401 || status === 403) {
      localStorage.removeItem('token');
      localStorage.removeItem('usuario');
      window.location.reload();
      return Promise.reject(error);
    }

    if (axios.isCancel?.(error) || error.code === 'ERR_CANCELED') {
      return Promise.reject(error);
    }

    if (!error.response) {
      toast.error('Não foi possível conectar ao servidor. Verifique sua conexão.');
    } else if (status >= 500) {
      toast.error('Erro no servidor. Tente novamente em instantes.');
    }

    return Promise.reject(error);
  }
);

export default api;
