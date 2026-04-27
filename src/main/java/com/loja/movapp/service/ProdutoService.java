package com.loja.movapp.service;

import com.loja.movapp.dto.ProdutoRequestDTO;
import com.loja.movapp.dto.ProdutoResponseDTO;
import com.loja.movapp.exception.OperacaoNaoPermitidaException;
import com.loja.movapp.exception.RecursoNaoEncontradoException;
import com.loja.movapp.model.Produto;
import com.loja.movapp.repository.ProdutoRepository;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProdutoService {

    private static final Logger log = LoggerFactory.getLogger(ProdutoService.class);

    @Autowired
    private ProdutoRepository repository;

    private Produto toEntity(ProdutoRequestDTO dto) {
        Produto p = new Produto();
        p.setCodigo(dto.getCodigo());
        p.setNome(dto.getNome());
        p.setCor(dto.getCor());
        p.setTamanho(dto.getTamanho());
        p.setPreco(dto.getPreco());
        p.setEstoque(dto.getEstoque());
        return p;
    }

    private ProdutoResponseDTO toDTO(Produto p) {
        return new ProdutoResponseDTO(
                p.getCodigo(), p.getNome(), p.getCor(),
                p.getTamanho(), p.getPreco(), p.getEstoque()
        );
    }

    @Transactional
    @CachePut(value = "produtos", key = "#result.codigo")
    public ProdutoResponseDTO salvar(ProdutoRequestDTO dto) {
        if (repository.existsById(dto.getCodigo())) {
            log.warn("Cadastro bloqueado: código '{}' já existe", dto.getCodigo());
            throw new OperacaoNaoPermitidaException(
                    "Código \"" + dto.getCodigo() + "\" já está cadastrado!");
        }
        log.info("Cadastrando produto: codigo={}, nome={}", dto.getCodigo(), dto.getNome());
        ProdutoResponseDTO salvo = toDTO(repository.save(toEntity(dto)));
        log.info("Produto cadastrado com sucesso: codigo={}", salvo.getCodigo());
        return salvo;
    }

    @Transactional(readOnly = true)
    public Page<ProdutoResponseDTO> listarPaginado(Pageable pageable) {
        return repository.findAll(pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<ProdutoResponseDTO> listarPorNome(String nome, Pageable pageable) {
        log.info("Buscando produtos por nome: '{}'", nome);
        return repository.findByNomeContainingIgnoreCase(nome, pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "produtos", key = "#codigo")
    public ProdutoResponseDTO buscarPorCodigo(String codigo) {
        return repository.findById(codigo)
                .map(this::toDTO)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Produto com código \"" + codigo + "\" não encontrado!"));
    }

    @Transactional
    @CacheEvict(value = "produtos", key = "#codigo")
    public void excluir(String codigo) {
        Produto p = repository.findById(codigo)
                .orElseThrow(() -> {
                    log.warn("Tentativa de excluir produto inexistente: codigo={}", codigo);
                    return new RecursoNaoEncontradoException(
                            "Produto com código \"" + codigo + "\" não encontrado!");
                });

        if (p.getEstoque() > 0) {
            log.warn("Exclusão bloqueada: produto='{}' possui {} unidade(s) em estoque", p.getNome(), p.getEstoque());
            throw new OperacaoNaoPermitidaException(
                    "Produto \"" + p.getNome() + "\" não pode ser excluído pois possui " +
                            p.getEstoque() + " unidade(s) em estoque.");
        }

        repository.deleteById(codigo);
        log.info("Produto excluído: codigo={}", codigo);
    }

    @Transactional(readOnly = true)
    public Page<ProdutoResponseDTO> buscarPorFaixaDePreco(BigDecimal min, BigDecimal max, Pageable pageable) {
        if (min.compareTo(BigDecimal.ZERO) < 0 || max.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Os valores de preço não podem ser negativos!");
        }
        if (min.compareTo(max) > 0) {
            throw new IllegalArgumentException("O preço mínimo não pode ser maior que o máximo!");
        }
        log.info("Buscando produtos por faixa de preço: min={}, max={}", min, max);
        return repository.buscarPorFaixaDePreco(min, max, pageable).map(this::toDTO);
    }

    @Transactional
    @CachePut(value = "produtos", key = "#codigo")
    public ProdutoResponseDTO editar(String codigo, ProdutoRequestDTO dto) {
        Produto p = repository.findById(codigo)
                .orElseThrow(() -> {
                    log.warn("Tentativa de editar produto inexistente: codigo={}", codigo);
                    return new RecursoNaoEncontradoException(
                            "Produto com código \"" + codigo + "\" não encontrado!");
                });

        if (dto.getNome()    != null) p.setNome(dto.getNome());
        if (dto.getCor()     != null) p.setCor(dto.getCor());
        if (dto.getTamanho() != null) p.setTamanho(dto.getTamanho());
        if (dto.getPreco()   != null && dto.getPreco().compareTo(BigDecimal.ZERO) > 0) p.setPreco(dto.getPreco());
        if (dto.getEstoque() >= 0)    p.setEstoque(dto.getEstoque());

        ProdutoResponseDTO atualizado = toDTO(repository.save(p));
        log.info("Produto atualizado: codigo={}", codigo);
        return atualizado;
    }
}
