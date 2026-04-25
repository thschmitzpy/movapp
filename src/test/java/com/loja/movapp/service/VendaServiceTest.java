package com.loja.movapp.service;

import com.loja.movapp.dto.ItemVendaRequestDTO;
import com.loja.movapp.dto.VendaRequestDTO;
import com.loja.movapp.dto.VendaResponseDTO;
import com.loja.movapp.exception.EstoqueInsuficienteException;
import com.loja.movapp.exception.OperacaoNaoPermitidaException;
import com.loja.movapp.exception.RecursoNaoEncontradoException;
import com.loja.movapp.model.ItemVenda;
import com.loja.movapp.model.Produto;
import com.loja.movapp.model.StatusVenda;
import com.loja.movapp.model.Venda;
import com.loja.movapp.repository.ProdutoRepository;
import com.loja.movapp.repository.VendaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VendaServiceTest {

    @Mock
    private VendaRepository vendaRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    @InjectMocks
    private VendaService service;

    private Produto produto;
    private VendaRequestDTO vendaRequest;
    private ItemVendaRequestDTO itemRequest;

    @BeforeEach
    void setUp() {
        produto = new Produto();
        produto.setCodigo("001");
        produto.setNome("Camiseta");
        produto.setCor("Azul");
        produto.setPreco(29.90);
        produto.setEstoque(50);

        itemRequest = new ItemVendaRequestDTO();
        itemRequest.setCodigoProduto("001");
        itemRequest.setQuantidade(2);

        vendaRequest = new VendaRequestDTO();
        vendaRequest.setItens(List.of(itemRequest));
        vendaRequest.setFormaPagamento("Pix");
        vendaRequest.setCondicaoPagamento("À vista");
        vendaRequest.setStatus(StatusVenda.PENDENTE);
    }

    private Venda vendaSalvaMock(Long id, double total) {
        Venda v = new Venda();
        v.setId(id);
        v.setData(LocalDateTime.now());
        v.setTotal(total);
        v.setFormaPagamento("Pix");
        v.setCondicaoPagamento("À vista");
        v.setStatus(StatusVenda.PENDENTE);
        v.setItens(new ArrayList<>());
        return v;
    }

    @Test
    void deveRealizarVendaComSucesso() {
        when(produtoRepository.findById("001")).thenReturn(Optional.of(produto));
        when(produtoRepository.save(any(Produto.class))).thenReturn(produto);
        when(vendaRepository.save(any(Venda.class))).thenReturn(vendaSalvaMock(1L, 59.80));

        VendaResponseDTO resultado = service.realizarVenda(vendaRequest);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        verify(produtoRepository, times(1)).save(any(Produto.class));
        verify(vendaRepository, times(1)).save(any(Venda.class));
    }

    @Test
    void deveDescontarEstoqueAoRealizarVenda() {
        int estoqueAntes = produto.getEstoque();

        when(produtoRepository.findById("001")).thenReturn(Optional.of(produto));
        when(produtoRepository.save(any(Produto.class))).thenReturn(produto);
        when(vendaRepository.save(any(Venda.class))).thenReturn(vendaSalvaMock(1L, 59.80));

        service.realizarVenda(vendaRequest);

        assertEquals(estoqueAntes - 2, produto.getEstoque());
    }

    @Test
    void deveCalcularTotalCorretamente() {
        when(produtoRepository.findById("001")).thenReturn(Optional.of(produto));
        when(produtoRepository.save(any(Produto.class))).thenReturn(produto);
        when(vendaRepository.save(any(Venda.class))).thenReturn(vendaSalvaMock(1L, 59.80));

        VendaResponseDTO resultado = service.realizarVenda(vendaRequest);

        assertEquals(59.80, resultado.getTotal());
    }

    @Test
    void deveRealizarVendaComMultiplosProdutos() {
        Produto produto2 = new Produto();
        produto2.setCodigo("002");
        produto2.setNome("Calça");
        produto2.setPreco(89.90);
        produto2.setEstoque(30);

        ItemVendaRequestDTO item2 = new ItemVendaRequestDTO();
        item2.setCodigoProduto("002");
        item2.setQuantidade(1);

        VendaRequestDTO vendaDois = new VendaRequestDTO();
        vendaDois.setItens(List.of(itemRequest, item2));
        vendaDois.setFormaPagamento("Cartão de Crédito");
        vendaDois.setCondicaoPagamento("2x sem juros");
        vendaDois.setStatus(StatusVenda.PENDENTE);

        when(produtoRepository.findById("001")).thenReturn(Optional.of(produto));
        when(produtoRepository.findById("002")).thenReturn(Optional.of(produto2));
        when(produtoRepository.save(any(Produto.class))).thenReturn(produto);
        when(vendaRepository.save(any(Venda.class))).thenReturn(vendaSalvaMock(1L, 149.70));

        VendaResponseDTO resultado = service.realizarVenda(vendaDois);

        assertNotNull(resultado);
        verify(produtoRepository, times(2)).save(any(Produto.class));
    }

    @Test
    void deveLancarRecursoNaoEncontradoQuandoProdutoInexistente() {
        when(produtoRepository.findById("001")).thenReturn(Optional.empty());

        RecursoNaoEncontradoException ex = assertThrows(RecursoNaoEncontradoException.class,
                () -> service.realizarVenda(vendaRequest));

        assertTrue(ex.getMessage().contains("não encontrado"));
        verify(vendaRepository, never()).save(any());
    }

    @Test
    void deveLancarEstoqueInsuficienteQuandoQuantidadeMaiorQueEstoque() {
        produto.setEstoque(1);
        when(produtoRepository.findById("001")).thenReturn(Optional.of(produto));

        EstoqueInsuficienteException ex = assertThrows(EstoqueInsuficienteException.class,
                () -> service.realizarVenda(vendaRequest));

        assertTrue(ex.getMessage().contains("Estoque insuficiente"));
        verify(vendaRepository, never()).save(any());
        verify(produtoRepository, never()).save(any());
    }

    @Test
    void deveLancarEstoqueInsuficienteQuandoEstoqueZero() {
        produto.setEstoque(0);
        when(produtoRepository.findById("001")).thenReturn(Optional.of(produto));

        EstoqueInsuficienteException ex = assertThrows(EstoqueInsuficienteException.class,
                () -> service.realizarVenda(vendaRequest));

        assertTrue(ex.getMessage().contains("Estoque insuficiente"));
    }

    @Test
    void deveLancarRecursoNaoEncontradoAoAtualizarVendaInexistente() {
        when(vendaRepository.findById(99L)).thenReturn(Optional.empty());

        RecursoNaoEncontradoException ex = assertThrows(RecursoNaoEncontradoException.class,
                () -> service.atualizarVenda(99L, vendaRequest));

        assertTrue(ex.getMessage().contains("não encontrada"));
    }

    @Test
    void deveLancarOperacaoNaoPermitidaAoAtualizarVendaFechada() {
        Venda vendaFechada = vendaSalvaMock(1L, 59.80);
        vendaFechada.setStatus(StatusVenda.FECHADA);

        when(vendaRepository.findById(1L)).thenReturn(Optional.of(vendaFechada));

        OperacaoNaoPermitidaException ex = assertThrows(OperacaoNaoPermitidaException.class,
                () -> service.atualizarVenda(1L, vendaRequest));

        assertTrue(ex.getMessage().contains("PENDENTES"));
    }

    @Test
    void deveLancarOperacaoNaoPermitidaAoExcluirVendaFechada() {
        Venda vendaFechada = vendaSalvaMock(1L, 59.80);
        vendaFechada.setStatus(StatusVenda.FECHADA);

        when(vendaRepository.findById(1L)).thenReturn(Optional.of(vendaFechada));

        OperacaoNaoPermitidaException ex = assertThrows(OperacaoNaoPermitidaException.class,
                () -> service.excluirVenda(1L));

        assertTrue(ex.getMessage().contains("PENDENTES"));
    }

    @Test
    void deveListarVendasVazia() {
        when(vendaRepository.findAll()).thenReturn(new ArrayList<>());

        var resultado = service.listarVendas();

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }
}
