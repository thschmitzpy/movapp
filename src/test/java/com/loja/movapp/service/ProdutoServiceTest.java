package com.loja.movapp.service;

import com.loja.movapp.dto.ProdutoCreateRequestDTO;
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
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
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
    private ProdutoCreateRequestDTO createDTO;
    private ProdutoRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        produto = new Produto();
        produto.setCodigo("001");
        produto.setNome("Camiseta");
        produto.setCor("Azul");
        produto.setTamanho("M");
        produto.setPreco(new BigDecimal("29.90"));
        produto.setEstoque(50);

        createDTO = new ProdutoCreateRequestDTO();
        createDTO.setCodigo("001");
        createDTO.setNome("Camiseta");
        createDTO.setCor("Azul");
        createDTO.setTamanho("M");
        createDTO.setPreco(new BigDecimal("29.90"));
        createDTO.setEstoque(50);

        requestDTO = new ProdutoRequestDTO();
        requestDTO.setCodigo("001");
        requestDTO.setNome("Camiseta");
        requestDTO.setCor("Azul");
        requestDTO.setTamanho("M");
        requestDTO.setPreco(new BigDecimal("29.90"));
        requestDTO.setEstoque(50);
    }

    @Test
    void deveSalvarProdutoComSucesso() {
        when(repository.existsById("001")).thenReturn(false);
        when(repository.save(any(Produto.class))).thenReturn(produto);

        ProdutoResponseDTO resultado = service.salvar(createDTO);

        assertNotNull(resultado);
        assertEquals("001", resultado.getCodigo());
        assertEquals("Camiseta", resultado.getNome());
        assertTrue(resultado.isAtivo(), "produto novo nasce ativo");
        verify(repository, times(1)).save(any(Produto.class));
    }

    @Test
    void deveLancarOperacaoNaoPermitidaAoCadastrarCodigoDuplicado() {
        when(repository.existsById("001")).thenReturn(true);

        OperacaoNaoPermitidaException ex = assertThrows(OperacaoNaoPermitidaException.class,
                () -> service.salvar(createDTO));

        assertTrue(ex.getMessage().contains("já está cadastrado"));
        verify(repository, never()).save(any());
    }

    @Test
    void deveBuscarProdutoPorCodigoExistente() {
        when(repository.findById("001")).thenReturn(Optional.of(produto));

        ProdutoResponseDTO resultado = service.buscarPorCodigo("001");

        assertNotNull(resultado);
        assertEquals("Camiseta", resultado.getNome());
    }

    @Test
    void deveLancarRecursoNaoEncontradoAoBuscarCodigoInexistente() {
        when(repository.findById("999")).thenReturn(Optional.empty());

        RecursoNaoEncontradoException ex = assertThrows(RecursoNaoEncontradoException.class,
                () -> service.buscarPorCodigo("999"));

        assertTrue(ex.getMessage().contains("não encontrado"));
    }

    @Test
    void deveInativarProdutoComEstoqueZero() {
        produto.setEstoque(0);
        when(repository.findById("001")).thenReturn(Optional.of(produto));
        when(repository.save(any(Produto.class))).thenReturn(produto);

        assertDoesNotThrow(() -> service.excluir("001"));

        // Soft delete: nunca chamar deleteById; salvar com ativo=false.
        verify(repository, never()).deleteById(any());
        verify(repository, times(1)).save(produto);
        assertFalse(produto.isAtivo(), "produto deve ficar inativo após excluir");
    }

    @Test
    void deveLancarOperacaoNaoPermitidaAoExcluirProdutoComEstoque() {
        produto.setEstoque(10);
        when(repository.findById("001")).thenReturn(Optional.of(produto));

        OperacaoNaoPermitidaException ex = assertThrows(OperacaoNaoPermitidaException.class,
                () -> service.excluir("001"));

        assertTrue(ex.getMessage().contains("não pode ser excluído"));
        verify(repository, never()).deleteById(any());
        verify(repository, never()).save(any());
    }

    @Test
    void deveLancarOperacaoNaoPermitidaAoExcluirProdutoJaInativo() {
        produto.setEstoque(0);
        produto.setAtivo(false);
        when(repository.findById("001")).thenReturn(Optional.of(produto));

        OperacaoNaoPermitidaException ex = assertThrows(OperacaoNaoPermitidaException.class,
                () -> service.excluir("001"));

        assertTrue(ex.getMessage().contains("já está inativo"));
        verify(repository, never()).save(any());
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
        dtoAtualizado.setPreco(new BigDecimal("49.90"));

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
    void deveBuscarComFiltrosCombinados() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Produto> page = new PageImpl<>(List.of(produto));

        when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<ProdutoResponseDTO> resultado = service.buscar(
                "Cami", new BigDecimal("10.0"), new BigDecimal("50.0"), null, pageable);

        assertFalse(resultado.isEmpty());
        assertEquals(1, resultado.getTotalElements());
        verify(repository, times(1)).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void deveLancarExcecaoQuandoPrecoMinMaiorQueMax() {
        Pageable pageable = PageRequest.of(0, 10);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.buscar(null, new BigDecimal("100.0"), new BigDecimal("10.0"), null, pageable));

        assertTrue(ex.getMessage().contains("mínimo não pode ser maior"));
        verify(repository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void deveLancarExcecaoQuandoPrecoMinNegativo() {
        Pageable pageable = PageRequest.of(0, 10);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.buscar(null, new BigDecimal("-10.0"), new BigDecimal("50.0"), null, pageable));

        assertTrue(ex.getMessage().contains("Preço mínimo não pode ser negativo"));
    }

    @Test
    void deveLancarExcecaoQuandoPrecoMaxNegativo() {
        Pageable pageable = PageRequest.of(0, 10);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.buscar(null, null, new BigDecimal("-1.0"), null, pageable));

        assertTrue(ex.getMessage().contains("Preço máximo não pode ser negativo"));
    }
}
