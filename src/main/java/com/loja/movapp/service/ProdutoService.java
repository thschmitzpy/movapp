package com.loja.movapp.service;

import com.loja.movapp.dto.ProdutoCreateRequestDTO;
import com.loja.movapp.dto.ProdutoRequestDTO;
import com.loja.movapp.dto.ProdutoResponseDTO;
import com.loja.movapp.exception.OperacaoNaoPermitidaException;
import com.loja.movapp.exception.RecursoNaoEncontradoException;
import com.loja.movapp.model.Produto;
import com.loja.movapp.repository.ProdutoRepository;
import com.loja.movapp.repository.ProdutoSpecifications;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProdutoService {

    private static final Logger log = LoggerFactory.getLogger(ProdutoService.class);

    @Autowired
    private ProdutoRepository repository;

    private Produto toEntity(ProdutoCreateRequestDTO dto) {
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
                p.getTamanho(), p.getPreco(), p.getEstoque(), p.isAtivo()
        );
    }

    @Transactional
    @CachePut(value = "produtos", key = "#result.codigo")
    public ProdutoResponseDTO salvar(ProdutoCreateRequestDTO dto) {
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
    public Page<ProdutoResponseDTO> buscar(String nome,
                                           BigDecimal precoMin,
                                           BigDecimal precoMax,
                                           Boolean ativo,
                                           Pageable pageable) {
        validarFaixaDePreco(precoMin, precoMax);

        // Sem filtro de ativo no querystring, exibimos só catálogo vendável.
        // ?ativo=false expõe inativos para telas administrativas.
        Boolean filtroAtivo = ativo == null ? Boolean.TRUE : ativo;

        Specification<Produto> spec = Specification
                .where(ProdutoSpecifications.ativo(filtroAtivo))
                .and(ProdutoSpecifications.nomeContem(nome))
                .and(ProdutoSpecifications.precoEntre(precoMin, precoMax));

        return repository.findAll(spec, pageable).map(this::toDTO);
    }

    private void validarFaixaDePreco(BigDecimal min, BigDecimal max) {
        if (min != null && min.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Preço mínimo não pode ser negativo!");
        }
        if (max != null && max.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Preço máximo não pode ser negativo!");
        }
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new IllegalArgumentException("O preço mínimo não pode ser maior que o máximo!");
        }
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

        if (!p.isAtivo()) {
            log.warn("Exclusão ignorada: produto codigo={} já está inativo", codigo);
            throw new OperacaoNaoPermitidaException(
                    "Produto \"" + p.getNome() + "\" já está inativo.");
        }

        if (p.getEstoque() > 0) {
            log.warn("Exclusão bloqueada: produto='{}' possui {} unidade(s) em estoque", p.getNome(), p.getEstoque());
            throw new OperacaoNaoPermitidaException(
                    "Produto \"" + p.getNome() + "\" não pode ser excluído pois possui " +
                            p.getEstoque() + " unidade(s) em estoque.");
        }

        p.setAtivo(false);
        repository.save(p);
        log.info("Produto inativado: codigo={}", codigo);
    }

    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class,
            maxAttempts = 4,
            backoff = @Backoff(delay = 50, multiplier = 2, random = true)
    )
    @Transactional
    @CachePut(value = "produtos", key = "#codigo")
    public ProdutoResponseDTO editar(String codigo, ProdutoRequestDTO dto) {
        Produto p = repository.findById(codigo)
                .orElseThrow(() -> {
                    log.warn("Tentativa de editar produto inexistente: codigo={}", codigo);
                    return new RecursoNaoEncontradoException(
                            "Produto com código \"" + codigo + "\" não encontrado!");
                });

        // Bean validation no DTO já rejeita preço <= 0 e estoque < 0 com 400.
        if (dto.getNome()    != null) p.setNome(dto.getNome());
        if (dto.getCor()     != null) p.setCor(dto.getCor());
        if (dto.getTamanho() != null) p.setTamanho(dto.getTamanho());
        if (dto.getPreco()   != null) p.setPreco(dto.getPreco());
        if (dto.getEstoque() != null) p.setEstoque(dto.getEstoque());

        ProdutoResponseDTO atualizado = toDTO(repository.save(p));
        log.info("Produto atualizado: codigo={}", codigo);
        return atualizado;
    }
}
