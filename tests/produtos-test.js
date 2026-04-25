import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import encoding from 'k6/encoding';

const errosProduto = new Rate('erros_produto');
const tempoProduto = new Trend('tempo_produto');

export const options = {
  stages: [
    { duration: '30s', target: 10  },
    { duration: '1m',  target: 50  },
    { duration: '30s', target: 100 },
    { duration: '1m',  target: 100 },
    { duration: '30s', target: 0   },
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'],
    erros_produto:     ['rate<0.05'],
  },
};

const BASE_URL = 'http://localhost:8080';
const AUTH = 'Basic ' + encoding.b64encode('admin:admin123');
const HEADERS = {
  'Content-Type': 'application/json',
  'Authorization': AUTH,
};

export default function () {

  const listar = http.get(
    `${BASE_URL}/produtos?page=0&size=10`,
    { headers: HEADERS }
  );
  check(listar, {
    'listar produtos - status 200': (r) => r.status === 200,
  });
  tempoProduto.add(listar.timings.duration);
  errosProduto.add(listar.status !== 200);
  sleep(1);

  //buscar produto por código
  const buscar = http.get(`${BASE_URL}/produtos/001`, { headers: HEADERS });
  check(buscar, {
    'buscar produto - status 200 ou 404': (r) =>
      r.status === 200 || r.status === 404,
  });
  tempoProduto.add(buscar.timings.duration);
  sleep(1);

  const faixa = http.get(
    `${BASE_URL}/produtos/preco?min=10&max=200&page=0&size=10`,
    { headers: HEADERS }
  );
  check(faixa, {
    'faixa de preço - status 200 ou 204': (r) =>
      r.status === 200 || r.status === 204,
  });
  tempoProduto.add(faixa.timings.duration);
  sleep(1);

  //cadastrar produto com código único
  const codigo = `PROD-${__VU}-${__ITER}-${Date.now()}`;
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
  tempoProduto.add(cadastrar.timings.duration);
  errosProduto.add(cadastrar.status !== 201);
  sleep(1);

  if (cadastrar.status === 201) {
    const editar = http.put(
      `${BASE_URL}/produtos/${codigo}`,
      JSON.stringify({
        codigo,
        nome:    "Produto Editado K6",
        preco:   49.90,
        estoque: 200
      }),
      { headers: HEADERS }
    );

    check(editar, {
      'editar produto - status 200': (r) => r.status === 200,
    });
    tempoProduto.add(editar.timings.duration);
    errosProduto.add(editar.status !== 200);
    sleep(1);
  }
}