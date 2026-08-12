package com.loja.movapp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loja.movapp.exception.OperacaoNaoPermitidaException;
import com.loja.movapp.model.IdempotencyKey;
import com.loja.movapp.model.IdempotencyStatus;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.function.Supplier;
import com.loja.movapp.exception.EstoqueInsuficienteException;
import com.loja.movapp.exception.OperacaoNaoPermitidaException;
import com.loja.movapp.exception.RecursoNaoEncontradoException;
import com.loja.movapp.exception.ErroResponse;
import com.fasterxml.jackson.databind.JsonNode;


@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

    @Autowired
    private IdempotencyKeyStore store;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MeterRegistry meterRegistry;

    public <T> T executar(String chave, String endpoint, Object requestPayload,
                          Supplier<T> acao, Class<T> tipoResposta) {
        if (chave == null || chave.isBlank()) {
            return acao.get();
        }

        String hash = sha256(toJson(requestPayload));
        return executarComChave(chave, endpoint, hash, acao, tipoResposta, true);
    }

    private <T> T executarComChave(String chave, String endpoint, String hash,
                                   Supplier<T> acao, Class<T> tipoResposta,
                                   boolean permitirReivindicarOrfa) {

        Optional<IdempotencyKey> reivindicada = store.tentarReivindicar(chave, endpoint, hash);
        if (reivindicada.isPresent()) {
            return executarAcao(chave, endpoint, acao);
        }

        return tratarChaveExistente(chave, endpoint, hash, acao, tipoResposta, permitirReivindicarOrfa);
    }

    private <T> T executarAcao(String chave, String endpoint, Supplier<T> acao) {
        try {
            T resultado = acao.get();
            store.concluir(chave, 200, toJson(resultado));
            log.info("Idempotência registrada: chave='{}', endpoint='{}'", chave, endpoint);
            return resultado;
        } catch (EstoqueInsuficienteException | OperacaoNaoPermitidaException
                 | RecursoNaoEncontradoException ex) {

            int status = statusHttpDe(ex);
            String body = toJson(new ErroResponse(status, ex.getMessage(), endpoint));
            try { store.concluir(chave, status, body); }
            catch (RuntimeException persistEx) {
                log.error("Falha ao persistir erro de negócio na chave '{}'", chave, persistEx);
            }
            throw ex;
        } catch (RuntimeException ex) {

            try { store.liberar(chave); }
            catch (RuntimeException liberarEx) {
                log.error("Falha ao liberar chave '{}' após erro na ação. " +
                        "A chave será removida pelo job de limpeza.", chave, liberarEx);
            }
            throw ex;
        }
    }

    private int statusHttpDe(RuntimeException ex) {
        if (ex instanceof RecursoNaoEncontradoException) return 404;
        return 409;
    }

    private <T> T tratarChaveExistente(String chave, String endpoint, String hashAtual,
                                       Supplier<T> acao, Class<T> tipoResposta,
                                       boolean permitirReivindicarOrfa) {
        IdempotencyKey ik = store.buscar(chave).orElse(null);
        if (ik == null) {

            if (permitirReivindicarOrfa) {
                return executarComChave(chave, endpoint, hashAtual, acao, tipoResposta, false);
            }
            throw new OperacaoNaoPermitidaException(
                    "Estado inconsistente para Idempotency-Key '" + chave + "'. Tente novamente.");
        }

        if (!ik.getRequestHash().equals(hashAtual)) {
            throw new OperacaoNaoPermitidaException(
                    "Idempotency-Key '" + chave + "' já foi usada com payload diferente. " +
                            "Use uma nova chave para uma nova requisição.");
        }

        if (ik.getStatus() == IdempotencyStatus.PROCESSANDO) {
            throw new OperacaoNaoPermitidaException(
                    "Requisição com Idempotency-Key '" + chave + "' ainda está em processamento. " +
                            "Aguarde alguns instantes e tente novamente.");
        }

        meterRegistry.counter("idempotency.replay.total", "endpoint", endpoint).increment();
        log.info("Replay idempotente: chave='{}' — retornando resposta cacheada (status={})",
        chave, ik.getResponseStatus());
        Integer status = ik.getResponseStatus();
        if (status !=null && status >=400) {
            String mensagem = extrairMensagem(ik.getResponseBody());
            if (status == 404) throw new RecursoNaoEncontradoException(mensagem);
            throw new OperacaoNaoPermitidaException(mensagem);
        }

        return fromJson(ik.getResponseBody(), tipoResposta);
    }

    private String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar payload para idempotência", e);
        }
    }

    private <T> T fromJson(String json, Class<T> tipo) {
        try {
            return objectMapper.readValue(json, tipo);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao desserializar resposta cacheada de idempotência", e);
        }
    }
    private String extrairMensagem(String responseBody) {
        try {
            JsonNode node = objectMapper.readTree(responseBody);
            JsonNode msg = node.get("mensagem");
            return msg != null && !msg.isNull() ? msg.asText() : "Erro na requisição anterior";
        } catch (Exception e) {
            return "Erro na requisição anterior";
        }
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
