package com.loja.movapp.service;

import com.loja.movapp.dto.ProdutoRequestDTO;
import com.loja.movapp.dto.ProdutoResponseDTO;
import com.loja.movapp.model.Produto;
import com.loja.movapp.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProdutoService {

    @Autowired
    private ProdutoRepository repository;

    private Produto toEntity(ProdutoRequestDTO dto) {
        Produto p = new Produto();
        p.setCodigo(dto.getCodigo());
        p.setNome(dto.getNome());
        p.setCor(dto.getCor());
        p.setTamanho(dto.getTamanho());;
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

    public ProdutoResponseDTO salvar(ProdutoRequestDTO dto) {
        Produto salvo = repository.save(toEntity(dto));
        return toDTO(salvo);
    }

    public List<ProdutoResponseDTO> listarTodos() {
        return repository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<ProdutoResponseDTO> buscarPorCodigo(String codigo) {
        return repository.findById(codigo)
                .map(this::toDTO);
    }

    public void excluir(String codigo) {
        repository.deleteById(codigo);
    }


    public boolean existeCodigo(String codigo) {
        return repository.existsById(codigo);
    }

    public Optional<Produto> buscarEntity(String codigo) {
        return repository.findById(codigo);
    }

    public ProdutoResponseDTO editar(String codigo, ProdutoRequestDTO dto) {
        Produto p = repository.findById(codigo).orElse(null);
        if (p == null) return null;

        if (dto.getNome()      != null) p.setNome(dto.getNome());
        if (dto.getCor()       != null) p.setCor(dto.getCor());
        if (dto.getTamanho()   != null) p.setTamanho(dto.getTamanho());
        if (dto.getPreco()     >  0)    p.setPreco(dto.getPreco());
        if (dto.getEstoque()   >= 0)    p.setEstoque(dto.getEstoque());

        return toDTO(repository.save(p));
    }

    public List<ProdutoResponseDTO> buscarPorFaixaDePreco(double min, double max) {
        if (min < 0 || max < 0){
            throw new IllegalArgumentException("Os valores não podem ficar negativos");
        }
        if (min > max){
            throw new IllegalArgumentException("O preço minimo não pode ser maior que o máximo");
        }

        return repository.buscarPorFaixaDePreco(min, max)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public Page<ProdutoResponseDTO> listarPaginado(Pageable pageable) {
        return repository.findAll(pageable)
                .map(this::toDTO);
    }

}