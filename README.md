# movapp

PDV (Ponto de Venda) construido como portfolio tecnico. Backend Spring Boot,
frontend React, banco Postgres, com observabilidade (Prometheus + Grafana)
tudo containerizado.

## Stack

- **Backend:** Java 17, Spring Boot 3.2, Spring Data JPA, Spring Security (JWT), Flyway, Caffeine
- **Frontend:** React 19, React Router 7, Axios
- **Banco:** PostgreSQL 16
- **Observabilidade:** Prometheus + Grafana (dashboards provisionados)
- **Testes:** JUnit 5, Testcontainers, k6 (carga)
- **Build:** Maven, Docker + Docker Compose

## Como rodar

Pre-requisito unico: **Docker Desktop 4.x+** (Windows/Mac) ou **Docker Engine + Compose v2** (Linux).

1. Clonar o repo:
   ```
   git clone <url> movapp
   cd movapp
   ```

2. Criar o arquivo `.env` a partir do exemplo:
   ```
   cp .env.example .env
   ```
   (No Windows: `copy .env.example .env`)

   Os valores padrao ja funcionam para dev local. Em qualquer uso serio,
   gere um novo `JWT_SECRET` (`openssl rand -base64 48`) e troque as senhas.

3. Subir tudo:
   ```
   docker compose up -d --build
   ```

4. Aguardar ~1 minuto no primeiro build. Depois acessar:

   | Servico    | URL                                       |
   |------------|-------------------------------------------|
   | Frontend   | http://localhost:3000                     |
   | Backend    | http://localhost:8080                     |
   | Swagger    | http://localhost:8080/swagger-ui.html     |
   | Health     | http://localhost:8080/actuator/health     |
   | Prometheus | http://localhost:9090                     |
   | Grafana    | http://localhost:3001                     |

## Credenciais padrao (do `.env.example`)

| Sistema | Usuario | Senha           |
|---------|---------|-----------------|
| App     | admin   | admin_dev_local |
| App     | usuario | user_dev_local  |
| Grafana | admin   | admin_dev_local |

## Comandos uteis

```
docker compose ps                # status dos containers
docker compose logs -f movapp    # logs do backend em tempo real
docker compose restart movapp    # restart so do backend
docker compose down              # para tudo (mantem volumes/dados)
docker compose down -v           # para e apaga volumes (zera dados)
docker compose build --no-cache  # rebuild ignorando cache
```

Cheatsheet completo em `docs/docker-cheatsheet.md`.

## Modo dev com hot-reload (opcional)

Para iterar sem rebuilder container a cada mudanca:

Pre-requisitos adicionais: JDK 17, Node 18+, Postgres 16 (pode subir so o
banco via `docker compose up -d postgres`).

**Windows:**
```
start.bat
```

**Manual (qualquer OS):**
```
# Terminal 1 - backend
./mvnw spring-boot:run

# Terminal 2 - frontend
cd movapp-front
npm install
npm start
```

## Estrutura

```
movapp/
├── src/                      # backend Spring Boot
│   ├── main/java/com/loja/movapp/
│   └── main/resources/
│       ├── application.properties
│       └── db/migration/      # migrations Flyway
├── movapp-front/             # frontend React
│   ├── src/
│   ├── Dockerfile
│   └── nginx.conf
├── observability/
│   ├── prometheus/
│   └── grafana/               # dashboards e datasources provisionados
├── docs/
├── tests/                    # testes de carga (k6)
├── Dockerfile                # backend
├── docker-compose.yml        # stack completa
├── docker-compose.test.yml   # postgres dedicado para testes de integracao
├── .env.example
└── pom.xml
```

## Troubleshooting

**Docker Desktop nao esta rodando**
```
open //./pipe/dockerDesktopLinuxEngine: The system cannot find the file specified.
```
Abrir o Docker Desktop e esperar o icone da baleia ficar estavel na bandeja.

**Porta ja em uso**
Se `3000`, `8080`, `5432`, `9090` ou `3001` estao ocupadas, edite a porta
HOST em `docker-compose.yml` (numero a esquerda do `:`), ex: `"3100:80"`
no servico `frontend`.

**Build do frontend falha em `npm ci`**
Divergencia entre `package.json` e `package-lock.json`. Fix:
```
cd movapp-front
rm -rf node_modules package-lock.json
npm install
cd ..
docker compose build --no-cache frontend
```

**Warning `orphan containers ([movapp-postgres-test])`**
Container do `docker-compose.test.yml` sobrando. Ignorar, ou parar com:
```
docker compose -f docker-compose.test.yml down
```

## Licenca

Projeto pessoal de portfolio, uso nao-comercial.
