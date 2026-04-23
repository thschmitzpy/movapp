import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import encoding from 'k6/encoding';

const erros = new Rate('erros');
const tempoResposta = new Trend('tempo_resposta');

export const options = {


  stages: [
    { duration: '30s', target: 10  }, // sobe para 10 usuários em 30s
    { duration: '1m',  target: 50  }, // sobe para 50 usuários em 1min
    { duration: '30s', target: 100 }, // sobe para 100 usuários em 30s
    { duration: '1m',  target: 100 }, // mantém 100 usuários por 1min
    { duration: '30s', target: 0   }, // desce para 0 usuários em 30s
  ],


  thresholds: {
    http_req_duration: ['p(95)<2000'], // 95% das requisições abaixo de 2s
    erros:             ['rate<0.05'],  // menos de 5% de erros
  },
};


const BASE_URL = 'http://localhost:8080';
const AUTH = 'Basic ' + encoding.b64encode('admin:admin123');

const HEADERS = {
  'Content-Type': 'application/json',
  'Authorization': AUTH,
};


export default function () {


  const listar = http.get(`${BASE_URL}/produtos`, { headers: HEADERS });
  check(listar, {
    'listar produtos - status 200': (r) => r.status === 200,
  });
  tempoResposta.add(listar.timings.duration);
  erros.add(listar.status !== 200);
  sleep(1);


  const buscar = http.get(`${BASE_URL}/produtos/001`, { headers: HEADERS });
  check(buscar, {
    'buscar produto - status 200 ou 404': (r) =>
      r.status === 200 || r.status === 404,
  });
  sleep(1);


  const faixa = http.get(
    `${BASE_URL}/produtos/preco?min=10&max=200`,
    { headers: HEADERS }
  );
  check(faixa, {
    'faixa de preço - status 200 ou 204': (r) =>
      r.status === 200 || r.status === 204,
  });
  sleep(1);

  //  cadastra produto com código único garantido
  // __VU   = número do usuário virtual (1, 2, 3... 100)
  // __ITER = número da iteração (0, 1, 2, 3...)
  // combinando os dois garante que nunca vai repetir!
  const codigo = `K6-${__VU}-${__ITER}`;

  const cadastrar = http.post(
    `${BASE_URL}/produtos`,
    JSON.stringify({
      codigo:   codigo,
      nome:     "Produto Teste K6",
      cor:      "Azul",
      tamanho:  "M",
      preco:    29.90,
      estoque:  100
    }),
    { headers: HEADERS }
  );
  check(cadastrar, {
    'cadastrar produto - status 201': (r) => r.status === 201,
  });
  erros.add(cadastrar.status !== 201);
  sleep(1);


  const vendas = http.get(`${BASE_URL}/vendas`, { headers: HEADERS });
  check(vendas, {
    'listar vendas - status 200': (r) => r.status === 200,
  });
  sleep(1);
}

