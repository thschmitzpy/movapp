package com.loja.movapp.integration;

import com.loja.movapp.dto.ItemVendaRequestDTO;
import com.loja.movapp.dto.PagamentoVendaRequestDTO;
import com.loja.movapp.dto.VendaRequestDTO;
import com.loja.movapp.exception.EstoqueInsuficienteException;
import com.loja.movapp.model.Produto;
import com.loja.movapp.model.StatusVenda;
import com.loja.movapp.repository.ProdutoRepository;
import com.loja.movapp.repository.VendaRepository;
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
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Concorrência de estoque — vendas paralelas")
class VendaConcorrenciaIT {

    @Autowired
    private VendaService vendaService;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private VendaRepository vendaRepository;

    private static final String CODIGO_PRODUTO = "CONC-001";
    private static final int ESTOQUE_INICIAL = 5;
    private static final int THREADS = 10;
    private static final BigDecimal PRECO_UNITARIO = new BigDecimal("10.00");

    @BeforeEach
    void setUp() {
        vendaRepository.deleteAll();
        produtoRepository.deleteAll();

        Produto p = new Produto();
        p.setCodigo(CODIGO_PRODUTO);
        p.setNome("Produto Concorrência");
        p.setCor("Azul");
        p.setTamanho("M");
        p.setPreco(PRECO_UNITARIO);
        p.setEstoque(ESTOQUE_INICIAL);
        produtoRepository.save(p);
    }

    @Test
    @DisplayName("10 vendas paralelas de 1 unidade contra estoque 5: 5 sucessos, 5 falhas, estoque final 0")
    void naoDeveVenderMaisQueOEstoque() throws Exception {
        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        List<CompletableFuture<Throwable>> futures = new ArrayList<>(THREADS);

        for (int i = 0; i < THREADS; i++) {
            String usuario = "thread-" + i;
            CompletableFuture<Throwable> f = CompletableFuture.supplyAsync(() -> {
                try {
                    // Sincroniza a largada para garantir concorrência real:
                    // sem isso, a primeira thread pode terminar antes da última começar.
                    largada.await();
                    vendaService.realizarVenda(montarVendaUnitaria(), usuario);
                    return null;
                } catch (Throwable t) {
                    return desembrulharCauseEsperada(t);
                }
            }, pool);
            futures.add(f);
        }

        largada.countDown();

        List<Throwable> resultados = new ArrayList<>(THREADS);
        for (CompletableFuture<Throwable> f : futures) {
            resultados.add(f.get(30, TimeUnit.SECONDS));
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS), "Pool não encerrou no tempo esperado");

        long sucessos           = resultados.stream().filter(Objects::isNull).count();
        long estoqueInsuficiente = resultados.stream()
                .filter(t -> t instanceof EstoqueInsuficienteException).count();
        List<Throwable> inesperadas = resultados.stream()
                .filter(t -> t != null && !(t instanceof EstoqueInsuficienteException))
                .toList();

        Produto produtoFinal = produtoRepository.findById(CODIGO_PRODUTO).orElseThrow();
        long vendasNoBanco = vendaRepository.count();

        assertEquals(0, inesperadas.size(),
                "Não deveriam ocorrer falhas inesperadas. Ocorreram: " + inesperadas);
        assertEquals(ESTOQUE_INICIAL, sucessos,
                "Devem ocorrer exatamente " + ESTOQUE_INICIAL + " vendas bem-sucedidas");
        assertEquals(THREADS - ESTOQUE_INICIAL, estoqueInsuficiente,
                "Vendas restantes devem falhar com EstoqueInsuficienteException");
        assertEquals(0, produtoFinal.getEstoque(),
                "Estoque final deve ser zero — nunca negativo");
        assertEquals(ESTOQUE_INICIAL, vendasNoBanco,
                "Apenas as vendas bem-sucedidas devem estar persistidas");
    }

    /**
     * Spring Retry encapsula a exceção da última tentativa em uma {@code RuntimeException}
     * só quando esgota retries — para {@code EstoqueInsuficienteException} (não retentada)
     * a exceção vem direta. Mas para robustez, descemos até a causa raiz quando aplicável.
     */
    private Throwable desembrulharCauseEsperada(Throwable t) {
        Throwable atual = t;
        while (atual.getCause() != null && atual.getCause() != atual
                && !(atual instanceof EstoqueInsuficienteException)) {
            atual = atual.getCause();
        }
        return atual;
    }

    private VendaRequestDTO montarVendaUnitaria() {
        ItemVendaRequestDTO item = new ItemVendaRequestDTO();
        item.setCodigoProduto(CODIGO_PRODUTO);
        item.setQuantidade(1);

        PagamentoVendaRequestDTO pag = new PagamentoVendaRequestDTO();
        pag.setFormaPagamento("Pix");
        pag.setCondicaoPagamento("À vista");
        pag.setValor(PRECO_UNITARIO);

        VendaRequestDTO dto = new VendaRequestDTO();
        dto.setItens(List.of(item));
        dto.setPagamentos(List.of(pag));
        dto.setStatus(StatusVenda.FECHADA);
        return dto;
    }
}
