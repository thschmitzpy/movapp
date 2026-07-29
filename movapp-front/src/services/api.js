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

api.interceptors.response.use(
  response => response,
  error => {
    const status = error.response?.status;

    if (status === 401 || status === 403) {

        const url = error.config?.url || '';                                                                            // Exceção: 401 do próprio /auth/login é senha errada
        if (url.includes('/auth/login') || url.includes('/auth/logout')) {
          return Promise.reject(error);
        }

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
