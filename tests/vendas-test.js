import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import encoding from 'k6/encoding';

const errosVenda = new Rate('erros_venda');
const tempoVenda = new Trend('tempo_venda');

export const options = {
  stages: [
    { duration: '30s', target: 10  },
    { duration: '1m',  target: 50  },
    { duration: '30s', target: 100 },
    { duration: '1m',  target: 100 },
    { duration: '30s', target: 0   },
  ],
  thresholds: {
    http_req_duration: ['p(95)<5000'],
    erros_venda:       ['rate<0.05'],
  },
};

const BASE_URL = 'http://localhost:8080';
const AUTH = 'Basic ' + encoding.b64encode('admin:admin123');
const HEADERS = {
  'Content-Type': 'application/json',
  'Authorization': AUTH,
};

export default function () {

  // cada usuário cria seu próprio produto
  const codigo = `VU-${__VU}-${__ITER}-${Date.now()}`;         //adiciona timestamp para garantir unicidade total PS: Não faço a menor idéia de como cheguei nisso

  const cadastrar = http.post(
    `${BASE_URL}/produtos`,
    JSON.stringify({
      codigo:   codigo,
      nome:     `Produto VU ${__VU}`,
      cor:      "Azul",
      tamanho:  "M",
      preco:    29.90,
      estoque:  9999
    }),
    { headers: HEADERS }
  );
  check(cadastrar, {
    'cadastrar produto - status 201': (r) => r.status === 201,
  });
  sleep(1);

  if (cadastrar.status === 201) {
    const venda = http.post(
      `${BASE_URL}/vendas`,
      JSON.stringify({
        itens: [
          {
            codigoProduto: codigo,
            quantidade:    1
          }
        ]
      }),
      { headers: HEADERS }
    );
    check(venda, {
      'realizar venda - status 200': (r) => r.status === 200,
    });
    tempoVenda.add(venda.timings.duration);
    errosVenda.add(venda.status !== 200);
    sleep(1);
  }

  const listarPaginado = http.get(
    `${BASE_URL}/vendas?page=0&size=10`,
    { headers: HEADERS }
  );
  check(listarPaginado, {
    'listar vendas paginado - status 200': (r) => r.status === 200,
  });
  tempoVenda.add(listarPaginado.timings.duration);
  sleep(1);
}