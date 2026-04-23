import axios from 'axios';

// configuração base da API
const api = axios.create({
  baseURL: 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
    //autenticação Basic com admin
    'Authorization': 'Basic ' + btoa('admin:admin123')
  }
});

export default api;