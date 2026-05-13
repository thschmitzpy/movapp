package com.loja.movapp.integration;

import com.loja.movapp.dto.ItemVendaRequestDTO;
import com.loja.movapp.dto.PagamentoVendaRequestDTO;
import com.loja.movapp.dto.VendaRequestDTO;
import com.loja.movapp.dto.VendaResponseDTO;
import com.loja.movapp.exception.OperacaoNaoPermitidaException;
import com.loja.movapp.model.Produto;
import com.loja.movapp.model.StatusVenda;
import com.loja.movapp.repository.IdempotencyKeyRepository;
import com.loja.movapp.repository.ProdutoRepository;
import com.loja.movapp.repository.VendaRepository;
import com.loja.movapp.service.IdempotencyService;
import com.loja.movapp.service.VendaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Idempotência de venda — replay e concorrência por Idempotency-Key")
class IdempotencyConcorrenciaIT {

    @Autowired private IdempotencyService idempotencyService;
    @Autowired private VendaService vendaService;
    @Autowired private ProdutoRepository produtoRepository;
    @Autowired private VendaRepository vendaRepository;
    @Autowired private IdempotencyKeyRepository idempotencyKeyRepository;

    private static final String CODIGO_PRODUTO = "IDEMP-001";
    private static final BigDecimal PRECO = new BigDecimal("25.00");

    @BeforeEach
    void setUp() {
        idempotencyKeyRepository.deleteAll();
        vendaRepository.deleteAll();
        produtoRepository.deleteAll();

        Produto p = new Produto();
        p.setCodigo(CODIGO_PRODUTO);
        p.setNome("Produto Idempotência");
        p.setCor("Verde");
        p.setTamanho("M");
        p.setPreco(PRECO);
        p.setEstoque(100);
        produtoRepository.save(p);
    }

    @Test
    @DisplayName("Mesma chave + mesmo payload em sequência: 2ª chamada devolve resposta cacheada e não cria venda nova")
    void replaySequencial_devolveRespostaCacheadaENaoCriaVendaNova() {
        String chave = UUID.randomUUID().toString();
        VendaRequestDTO dto = montarVenda(1);

        VendaResponseDTO primeira = executar(chave, dto);
        VendaResponseDTO segunda  = executar(chave, dto);

        assertEquals(primeira.getId(), segunda.getId(),
                "Replay deve devolver a mesma venda da primeira execução");
        assertEquals(1L, vendaRepository.count(),
                "Apenas 1 venda deve estar persistida após o replay");
        assertEquals(1L, idempotencyKeyRepository.count(),
                "Apenas 1 registro de idempotência deve existir");
    }

    @Test
    @DisplayName("10 chamadas paralelas com mesma chave: exatamente 1 venda criada, todas as respostas ok apontam pra ela")
    void replayConcorrente_apenasUmaVendaCriadaParaNRequisicoes() throws Exception {
        String chave = UUID.randomUUID().toString();
        VendaRequestDTO dto = montarVenda(1);
        int threads = 10;

        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<CompletableFuture<Resultado>> futures = new ArrayList<>(threads);

        for (int i = 0; i < threads; i++) {
            CompletableFuture<Resultado> f = CompletableFuture.supplyAsync(() -> {
                try {
                    largada.await();
                    return new Resultado(executar(chave, dto), null);
                } catch (Throwable t) {
                    return new Resultado(null, t);
                }
            }, pool);
            futures.add(f);
        }

        largada.countDown();

        List<Resultado> resultados = new ArrayList<>(threads);
        for (CompletableFuture<Resultado> f : futures) {
            resultados.add(f.get(30, TimeUnit.SECONDS));
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS), "Pool não encerrou no tempo esperado");

        List<VendaResponseDTO> sucessos = resultados.stream()
                .filter(Resultado::foiSucesso)
                .map(Resultado::resposta)
                .toList();
        long errosInesperados = resultados.stream().filter(Resultado::foiErroInesperado).count();
        Set<Long> idsRetornados = sucessos.stream()
                .map(VendaResponseDTO::getId)
                .collect(Collectors.toSet());

        assertEquals(0, errosInesperados,
                "Não deveriam ocorrer falhas além de OperacaoNaoPermitida ('ainda processando')");
        assertTrue(sucessos.size() >= 1,
                "Pelo menos uma das chamadas deve ter completado com sucesso");
        assertEquals(1, idsRetornados.size(),
                "Todos os sucessos devem apontar pra mesma venda. IDs distintos: " + idsRetornados);
        assertEquals(1L, vendaRepository.count(),
                "Apenas 1 venda deve ter sido persistida apesar das " + threads + " requisições paralelas");
    }

    @Test
    @DisplayName("Mesma chave com payload diferente: 2ª chamada lança OperacaoNaoPermitida")
    void mesmaChaveComPayloadDiferente_lancaOperacaoNaoPermitida() {
        String chave = UUID.randomUUID().toString();
        VendaRequestDTO dto1 = montarVenda(1);
        VendaRequestDTO dto2 = montarVenda(2); // quantidade diferente => hash diferente

        executar(chave, dto1);

        OperacaoNaoPermitidaException ex = assertThrows(OperacaoNaoPermitidaException.class,
                () -> executar(chave, dto2));
        assertTrue(ex.getMessage().contains("payload diferente"),
                "Mensagem deve indicar payload diferente. Veio: " + ex.getMessage());
        assertEquals(1L, vendaRepository.count(),
                "Apenas a 1ª venda deve ter sido persistida; a 2ª foi rejeitada antes de executar");
    }

    @Test
    @DisplayName("Sem Idempotency-Key: chamada passa direto e nada é registrado na tabela de chaves")
    void semChave_passaDiretoSemRegistrar() {
        VendaRequestDTO dto = montarVenda(1);

        VendaResponseDTO r = executar(null, dto);

        assertNotNull(r.getId());
        assertEquals(1L, vendaRepository.count(),
                "A venda foi criada normalmente sem precisar de chave");
        assertEquals(0L, idempotencyKeyRepository.count(),
                "Sem chave, nenhum registro de idempotência deve ser criado");
    }

    private VendaResponseDTO executar(String chave, VendaRequestDTO dto) {
        return idempotencyService.executar(
                chave,
                "POST /vendas",
                dto,
                () -> vendaService.realizarVenda(dto, "teste"),
                VendaResponseDTO.class);
    }

    private VendaRequestDTO montarVenda(int qtd) {
        ItemVendaRequestDTO item = new ItemVendaRequestDTO();
        item.setCodigoProduto(CODIGO_PRODUTO);
        item.setQuantidade(qtd);

        PagamentoVendaRequestDTO pag = new PagamentoVendaRequestDTO();
        pag.setFormaPagamento("Pix");
        pag.setCondicaoPagamento("À vista");
        pag.setValor(PRECO.multiply(BigDecimal.valueOf(qtd)));

        VendaRequestDTO dto = new VendaRequestDTO();
        dto.setItens(List.of(item));
        dto.setPagamentos(List.of(pag));
        dto.setStatus(StatusVenda.FECHADA);
        return dto;
    }

    /**
     * Resultado de uma chamada concorrente. Exceção {@link OperacaoNaoPermitidaException}
     * é esperada (caminho "ainda processando" do {@link IdempotencyService}); qualquer outra
     * é falha real do teste.
     */
    private record Resultado(VendaResponseDTO resposta, Throwable erro) {
        boolean foiSucesso()        { return resposta != null; }
        boolean foiErroInesperado() {
            return resposta == null && erro != null
                    && !(erro instanceof OperacaoNaoPermitidaException);
        }
    }
}
