package com.loja.movapp.service;

import com.loja.movapp.dto.ProdutoRequestDTO;
import com.loja.movapp.dto.ProdutoResponseDTO;
import com.loja.movapp.model.Produto;
import com.loja.movapp.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service de Produto.
 * Responsável por todas as regras de negócio dos produtos.
 * Nenhuma validação deve estar fora daqui!
 */
@Service
public class ProdutoService {

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


    @CachePut(value = "produtos", key = "#result.codigo")
    public ProdutoResponseDTO salvar(ProdutoRequestDTO dto) {
        return toDTO(repository.save(toEntity(dto)));
    }


    public Page<ProdutoResponseDTO> listarPaginado(Pageable pageable) {
        return repository.findAll(pageable).map(this::toDTO);
    }

    public Page<ProdutoResponseDTO> listarPorNome(String nome, Pageable pageable) {
        return repository.findByNomeContainingIgnoreCase(nome, pageable).map(this::toDTO);
    }


    @Cacheable(value = "produtos", key = "#codigo")
    public Optional<ProdutoResponseDTO> buscarPorCodigo(String codigo) {
        return repository.findById(codigo).map(this::toDTO);
    }


    public boolean existeCodigo(String codigo) {
        return repository.existsById(codigo);
    }


    @CacheEvict(value = "produtos", key = "#codigo")
    public void excluir(String codigo) {

        Produto p = repository.findById(codigo)
                .orElseThrow(() -> new RuntimeException(
                        "Produto com código " + codigo + " não encontrado!"));

        if (p.getEstoque() > 0) {
            throw new RuntimeException(
                    "Produto \"" + p.getNome() + "\" não pode ser excluído! " +
                            "Motivo: ainda possui " + p.getEstoque() + " unidades em estoque.");
        }

        repository.deleteById(codigo);
    }



    // ── buscar por faixa de preço ────────────────────
    /**
     * Busca produtos entre o preço mínimo e máximo COM paginação.
     * Regra: min não pode ser maior que max e ambos não podem ser negativos.
     */
    public Page<ProdutoResponseDTO> buscarPorFaixaDePreco(
                                                             double min, double max, Pageable pageable) {

        if (min < 0 || max < 0) {
            throw new IllegalArgumentException(
                    "Os valores de preço não podem ser negativos!");
        }

        if (min > max) {
            throw new IllegalArgumentException(
                    "O preço mínimo não pode ser maior que o máximo!");
        }

        return repository.buscarPorFaixaDePreco(min, max, pageable)
                .map(this::toDTO);
    }


    /**
     * Edita o produto e atualiza o cache.
     * Só atualiza os campos que vieram preenchidos.
     */
    @CachePut(value = "produtos", key = "#codigo")
    public ProdutoResponseDTO editar(String codigo, ProdutoRequestDTO dto) {

        Produto p = repository.findById(codigo)
                .orElseThrow(() -> new RuntimeException(
                        "Produto com código " + codigo + " não encontrado!"));

        if (dto.getNome()    != null) p.setNome(dto.getNome());
        if (dto.getCor()     != null) p.setCor(dto.getCor());
        if (dto.getTamanho() != null) p.setTamanho(dto.getTamanho());
        if (dto.getPreco()   >  0)    p.setPreco(dto.getPreco());
        if (dto.getEstoque() >= 0)    p.setEstoque(dto.getEstoque());

        return toDTO(repository.save(p));
    }
}