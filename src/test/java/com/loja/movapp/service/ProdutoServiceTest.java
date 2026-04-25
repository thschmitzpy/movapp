package com.loja.movapp.service;

import com.loja.movapp.dto.ProdutoRequestDTO;
import com.loja.movapp.dto.ProdutoResponseDTO;
import com.loja.movapp.exception.OperacaoNaoPermitidaException;
import com.loja.movapp.exception.RecursoNaoEncontradoException;
import com.loja.movapp.model.Produto;
import com.loja.movapp.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {

    @Mock
    private ProdutoRepository repository;

    @InjectMocks
    private ProdutoService service;

    private Produto produto;
    private ProdutoRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        produto = new Produto();
        produto.setCodigo("001");
        produto.setNome("Camiseta");
        produto.setCor("Azul");
        produto.setTamanho("M");
        produto.setPreco(29.90);
        produto.setEstoque(50);

        requestDTO = new ProdutoRequestDTO();
        requestDTO.setCodigo("001");
        requestDTO.setNome("Camiseta");
        requestDTO.setCor("Azul");
        requestDTO.setTamanho("M");
        requestDTO.setPreco(29.90);
        requestDTO.setEstoque(50);
    }

    @Test
    void deveSalvarProdutoComSucesso() {
        when(repository.save(any(Produto.class))).thenReturn(produto);

        ProdutoResponseDTO resultado = service.salvar(requestDTO);

        assertNotNull(resultado);
        assertEquals("001", resultado.getCodigo());
        assertEquals("Camiseta", resultado.getNome());
        verify(repository, times(1)).save(any(Produto.class));
    }

    @Test
    void deveBuscarProdutoPorCodigoExistente() {
        when(repository.findById("001")).thenReturn(Optional.of(produto));

        Optional<ProdutoResponseDTO> resultado = service.buscarPorCodigo("001");

        assertTrue(resultado.isPresent());
        assertEquals("Camiseta", resultado.get().getNome());
    }

    @Test
    void deveRetornarVazioQuandoCodigoNaoExiste() {
        when(repository.findById("999")).thenReturn(Optional.empty());

        Optional<ProdutoResponseDTO> resultado = service.buscarPorCodigo("999");

        assertFalse(resultado.isPresent());
    }

    @Test
    void deveRetornarTrueQuandoCodigoExiste() {
        when(repository.existsById("001")).thenReturn(true);

        assertTrue(service.existeCodigo("001"));
    }

    @Test
    void deveRetornarFalseQuandoCodigoNaoExiste() {
        when(repository.existsById("999")).thenReturn(false);

        assertFalse(service.existeCodigo("999"));
    }

    @Test
    void deveExcluirProdutoComEstoqueZero() {
        produto.setEstoque(0);
        when(repository.findById("001")).thenReturn(Optional.of(produto));
        doNothing().when(repository).deleteById("001");

        assertDoesNotThrow(() -> service.excluir("001"));
        verify(repository, times(1)).deleteById("001");
    }

    @Test
    void deveLancarOperacaoNaoPermitidaAoExcluirProdutoComEstoque() {
        produto.setEstoque(10);
        when(repository.findById("001")).thenReturn(Optional.of(produto));

        OperacaoNaoPermitidaException ex = assertThrows(OperacaoNaoPermitidaException.class,
                () -> service.excluir("001"));

        assertTrue(ex.getMessage().contains("não pode ser excluído"));
        verify(repository, never()).deleteById(any());
    }

    @Test
    void deveLancarRecursoNaoEncontradoAoExcluirProdutoInexistente() {
        when(repository.findById("999")).thenReturn(Optional.empty());

        RecursoNaoEncontradoException ex = assertThrows(RecursoNaoEncontradoException.class,
                () -> service.excluir("999"));

        assertTrue(ex.getMessage().contains("não encontrado"));
    }

    @Test
    void deveEditarProdutoComSucesso() {
        when(repository.findById("001")).thenReturn(Optional.of(produto));
        when(repository.save(any(Produto.class))).thenReturn(produto);

        ProdutoRequestDTO dtoAtualizado = new ProdutoRequestDTO();
        dtoAtualizado.setNome("Camiseta Premium");
        dtoAtualizado.setPreco(49.90);

        ProdutoResponseDTO resultado = service.editar("001", dtoAtualizado);

        assertNotNull(resultado);
        verify(repository, times(1)).save(any(Produto.class));
    }

    @Test
    void deveLancarRecursoNaoEncontradoAoEditarProdutoInexistente() {
        when(repository.findById("999")).thenReturn(Optional.empty());

        RecursoNaoEncontradoException ex = assertThrows(RecursoNaoEncontradoException.class,
                () -> service.editar("999", requestDTO));

        assertTrue(ex.getMessage().contains("não encontrado"));
    }

    @Test
    void deveBuscarPorFaixaDePrecoValida() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Produto> page = new PageImpl<>(List.of(produto));

        when(repository.buscarPorFaixaDePreco(10.0, 50.0, pageable)).thenReturn(page);

        Page<ProdutoResponseDTO> resultado = service.buscarPorFaixaDePreco(10.0, 50.0, pageable);

        assertFalse(resultado.isEmpty());
        assertEquals(1, resultado.getTotalElements());
    }

    @Test
    void deveLancarExcecaoQuandoPrecoMinMaiorQueMax() {
        Pageable pageable = PageRequest.of(0, 10);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.buscarPorFaixaDePreco(100.0, 10.0, pageable));

        assertTrue(ex.getMessage().contains("mínimo não pode ser maior"));
    }

    @Test
    void deveLancarExcecaoQuandoPrecoNegativo() {
        Pageable pageable = PageRequest.of(0, 10);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.buscarPorFaixaDePreco(-10.0, 50.0, pageable));

        assertTrue(ex.getMessage().contains("não podem ser negativos"));
    }
}
