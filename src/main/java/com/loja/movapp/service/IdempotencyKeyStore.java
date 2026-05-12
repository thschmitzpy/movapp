package com.loja.movapp.service;

import com.loja.movapp.model.IdempotencyKey;
import com.loja.movapp.model.IdempotencyStatus;
import com.loja.movapp.repository.IdempotencyKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Operações transacionais isoladas para chaves de idempotência.
 *
 * Está em uma classe separada (não dentro do {@link IdempotencyService}) porque
 * @Transactional(REQUIRES_NEW) só é aplicado em chamadas que passam pelo proxy
 * Spring — chamadas internas (this.metodo()) são ignoradas. Mantendo essas
 * operações em outro bean, garante que cada uma rode em sua própria transação,
 * tornando a chave reivindicada visível para requisições concorrentes imediatamente.
 */
@Component
public class IdempotencyKeyStore {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyKeyStore.class);

    @Autowired
    private IdempotencyKeyRepository repo;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<IdempotencyKey> tentarReivindicar(String chave, String endpoint, String requestHash) {
        try {
            IdempotencyKey ik = new IdempotencyKey();
            ik.setChave(chave);
            ik.setEndpoint(endpoint);
            ik.setRequestHash(requestHash);
            ik.setStatus(IdempotencyStatus.PROCESSANDO);
            ik.setCriadoEm(LocalDateTime.now());
            return Optional.of(repo.saveAndFlush(ik));
        } catch (DataIntegrityViolationException race) {
            log.info("Chave de idempotência '{}' já reivindicada por outra requisição", chave);
            return Optional.empty();
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void concluir(String chave, int responseStatus, String responseBody) {
        IdempotencyKey ik = repo.findById(chave).orElseThrow(() ->
                new IllegalStateException("Chave '" + chave + "' não encontrada ao concluir"));
        ik.setStatus(IdempotencyStatus.CONCLUIDA);
        ik.setResponseStatus(responseStatus);
        ik.setResponseBody(responseBody);
        repo.save(ik);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void liberar(String chave) {
        repo.deleteById(chave);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<IdempotencyKey> buscar(String chave) {
        return repo.findById(chave);
    }

    static final Duration RETENCAO_CONCLUIDAS = Duration.ofHours(24);

    static final Duration RETENCAO_PROCESSANDO_ORFAS = Duration.ofMinutes(15);

    @Scheduled(fixedRate = 3_600_000)
    public void limparChavesAntigas() {
        LocalDateTime limiteConcluidas = LocalDateTime.now().minus(RETENCAO_CONCLUIDAS);
        int concluidas = repo.deleteByStatusEAntesDe(IdempotencyStatus.CONCLUIDA, limiteConcluidas);

        LocalDateTime limiteProcessando = LocalDateTime.now().minus(RETENCAO_PROCESSANDO_ORFAS);
        int orfas = repo.deleteByStatusEAntesDe(IdempotencyStatus.PROCESSANDO, limiteProcessando);

        if (concluidas > 0 || orfas > 0) {
            log.info("Limpeza de idempotência: {} concluídas antigas, {} órfãs PROCESSANDO removidas",
                    concluidas, orfas);
        }
    }
}
