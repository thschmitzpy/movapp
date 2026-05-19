# Docker — Cheatsheet para o movapp

Comandos práticos para validar e diagnosticar containers da stack
(movapp + prometheus + grafana + postgres-test).

---

## 1. Listar containers

```cmd
docker ps                  :: só os que estão rodando
docker ps -a               :: inclui parados
docker compose ps          :: só os do compose atual
```

## 2. Logs (o mais usado em troubleshooting)

```cmd
docker logs movapp                       :: últimos logs
docker logs movapp --tail 50             :: últimas 50 linhas
docker logs movapp -f                    :: follow em tempo real (Ctrl+C para sair)
docker logs movapp --since 5m            :: últimos 5 minutos

docker compose logs                      :: logs de todos os serviços
docker compose logs movapp -f            :: só um serviço, follow
docker compose logs movapp prometheus    :: múltiplos
```

## 3. Entrar dentro do container (shell)

```cmd
docker exec -it movapp sh                :: shell interativo (alpine usa sh)
docker exec -it movapp-prometheus sh
docker exec -it movapp-grafana bash      :: grafana tem bash
```

Dentro: `ls`, `cat /etc/...`, `wget -O- http://movapp:8080/actuator/health`, etc.

## 4. Comando avulso dentro do container (sem shell)

```cmd
docker exec movapp ls /app
docker exec movapp env                                 :: variáveis de ambiente
docker exec movapp-prometheus cat /etc/prometheus/prometheus.yml
docker exec movapp-prometheus wget -qO- http://movapp:8080/actuator/health
```

Útil para validar que **um container enxerga o outro** na rede docker.

## 5. Recursos consumidos (CPU/RAM)

```cmd
docker stats                  :: live, todos os containers
docker stats movapp           :: só um
docker stats --no-stream      :: snapshot único
```

## 6. Detalhes do container (config completa)

```cmd
docker inspect movapp                                          :: JSON completo
docker inspect movapp --format "{{.State.Status}}"             :: só status
docker inspect movapp --format "{{.NetworkSettings.IPAddress}}"
docker inspect movapp --format "{{.Config.Env}}"               :: env vars
docker inspect movapp --format "{{json .State.Health}}"        :: healthcheck
```

## 7. Portas mapeadas

```cmd
docker port movapp            :: lista host_port -> container_port
```

## 8. Rede

```cmd
docker network ls                          :: todas as redes
docker network inspect movapp_movapp-net   :: quem está na rede
```

A última mostra todos os containers conectados e seus IPs internos —
útil para confirmar que prometheus, grafana e movapp estão na mesma rede.

## 9. Imagens

```cmd
docker images                              :: imagens locais
docker images movapp                       :: versões da imagem movapp
docker image inspect movapp                :: detalhes
docker history movapp                      :: layers que compõem a imagem
```

## 10. Volumes (estado persistente)

```cmd
docker volume ls
docker volume inspect movapp_grafana-data
```

Útil para saber onde o Grafana guarda configuração entre restarts.

## 11. Eventos em tempo real

```cmd
docker events                          :: mostra create/start/stop/die de tudo
docker events --filter container=movapp
```

Bom para diagnosticar "por que o container está reiniciando?".

## 12. Ciclo de vida (compose)

```cmd
docker compose up -d                  :: sobe tudo em background
docker compose down                   :: derruba tudo (mantém volumes)
docker compose down -v                :: derruba + apaga volumes (CUIDADO: perde dados Grafana)
docker compose restart movapp         :: restart só de um serviço
docker compose stop movapp            :: para sem remover
docker compose start movapp           :: liga de novo
docker compose build movapp           :: rebuilda só o serviço (se tiver build: no compose)
docker compose pull                   :: baixa imagens atualizadas
```

---

## Diagnósticos comuns para a stack movapp

**"O movapp está saudável?"**
```cmd
docker inspect movapp --format "{{.State.Status}}: {{.State.Health.Status}}"
```

**"O Prometheus consegue alcançar o movapp?"**
```cmd
docker exec movapp-prometheus wget -qO- http://movapp:8080/actuator/health
```

**"Quais variáveis o movapp está usando?"**
```cmd
docker exec movapp env | findstr SPRING
```

**"Container reiniciou quantas vezes?"**
```cmd
docker inspect movapp --format "{{.RestartCount}}"
```

**"Quando o container foi iniciado?"**
```cmd
docker inspect movapp --format "{{.State.StartedAt}}"
```

**Snapshot rápido — status + consumo de todos os serviços:**
```cmd
docker compose ps && docker stats --no-stream
```

---

## Fluxo completo: do zero ao dashboard

```cmd
:: 1. Build da imagem (depois de alterar código)
mvnw.cmd clean package -DskipTests
docker build -t movapp .

:: 2. Subir tudo
docker compose up -d

:: 3. Verificar saúde
docker compose ps
curl http://localhost:8080/actuator/health

:: 4. Abrir UIs
::    - App:        http://localhost:8080/swagger-ui.html
::    - Prometheus: http://localhost:9090/targets
::    - Grafana:    http://localhost:3001  (admin / admin)
```

---

## Limpeza (quando o disco encher)

```cmd
docker system df                  :: quanto espaço Docker está usando
docker system prune               :: remove containers parados, redes não usadas, build cache
docker system prune -a            :: + remove imagens não usadas (mais agressivo)
docker volume prune               :: remove volumes não referenciados (CUIDADO)
```
