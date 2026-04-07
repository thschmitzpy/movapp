package com.loja.movapp.controller;

import com.loja.movapp.dto.ProdutoRequestDTO;
import com.loja.movapp.dto.ProdutoResponseDTO;
import com.loja.movapp.model.Produto;
import com.loja.movapp.service.ProdutoService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/produtos")
public class ProdutoController {

    @Autowired
    private ProdutoService service;

    @PostMapping
    public ResponseEntity<?> cadastrar(@Valid @RequestBody ProdutoRequestDTO dto) {

        if (service.existeCodigo(dto.getCodigo())) {
            return ResponseEntity.badRequest()
                    .body("Código \"" + dto.getCodigo() + "\" já cadastrado!");
        }

        ProdutoResponseDTO salvo = service.salvar(dto);

        // Retorna 201 Created
        return ResponseEntity
                .created(URI.create("/produtos/" + salvo.getCodigo()))
                .body(salvo);
    }

    @GetMapping
    public ResponseEntity<Page<ProdutoResponseDTO>> listar(Pageable pageable) {
        return ResponseEntity.ok(service.listarPaginado(pageable));
    }

    @GetMapping("/{codigo}")
    public ResponseEntity<ProdutoResponseDTO> buscar(@PathVariable String codigo) {
        return service.buscarPorCodigo(codigo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{codigo}")
    public ResponseEntity<?> editar(@PathVariable String codigo,
                                    @Valid @RequestBody ProdutoRequestDTO dto) {

        if (!service.existeCodigo(codigo)) {
            return ResponseEntity.badRequest()
                    .body("Produto com código " + codigo + " não encontrado!");
        }

        ProdutoResponseDTO atualizado = service.editar(codigo, dto);
        return ResponseEntity.ok(atualizado);
    }

    @DeleteMapping("/{codigo}")
    public ResponseEntity<?> excluir(@PathVariable String codigo) {

        if (!service.existeCodigo(codigo)) {
            return ResponseEntity.badRequest()
                    .body("Produto não encontrado!");
        }

        Produto p = service.buscarEntity(codigo)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));

        if (p.getEstoque() > 0) {
            return ResponseEntity.badRequest()
                    .body("Produto com estoque não pode ser excluído! Estoque: " + p.getEstoque());
        }

        service.excluir(codigo);

        // 204 No Content (mais profissional)
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/preco")
    public ResponseEntity<?> buscarPorFaixaDePreco(
            @RequestParam double min,
            @RequestParam double max) {

        if (min < 0 || max < 0 || min > max) {
            return ResponseEntity.badRequest()
                    .body("Valores inválidos para faixa de preço");
        }

        List<ProdutoResponseDTO> produtos = service.buscarPorFaixaDePreco(min, max);

        if (produtos.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(produtos);
    }
}