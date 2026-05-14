package com.loja.movapp.controller;

import com.loja.movapp.dto.ProdutoCreateRequestDTO;
import com.loja.movapp.dto.ProdutoRequestDTO;
import com.loja.movapp.dto.ProdutoResponseDTO;
import com.loja.movapp.service.ProdutoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;

@RestController
@RequestMapping("/produtos")
@Tag(name = "Produtos", description = "Endpoints para gerenciamento de produtos")
public class ProdutoController {

    @Autowired
    private ProdutoService service;

    @PostMapping
    @Operation(summary = "Cadastrar produto")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Produto cadastrado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "409", description = "Código já cadastrado")
    })
    public ResponseEntity<ProdutoResponseDTO> cadastrar(@Valid @RequestBody ProdutoCreateRequestDTO dto) {
        ProdutoResponseDTO salvo = service.salvar(dto);
        return ResponseEntity
                .created(URI.create("/produtos/" + salvo.getCodigo()))
                .body(salvo);
    }

    @GetMapping
    @Operation(
            summary = "Buscar produtos com filtros combinados",
            description = "Filtros opcionais: nome (contém), faixa de preço (precoMin/precoMax), ativo. " +
                          "Sem ?ativo, retorna apenas o catálogo ativo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de produtos retornada"),
            @ApiResponse(responseCode = "400", description = "Faixa de preço inválida")
    })
    public ResponseEntity<Page<ProdutoResponseDTO>> buscar(
            @Parameter(description = "Filtro parcial por nome (case-insensitive)")
            @RequestParam(required = false) String nome,
            @Parameter(description = "Preço mínimo (inclusivo)")
            @RequestParam(required = false) BigDecimal precoMin,
            @Parameter(description = "Preço máximo (inclusivo)")
            @RequestParam(required = false) BigDecimal precoMax,
            @Parameter(description = "Filtrar por ativo. Sem parâmetro = só ativos.")
            @RequestParam(required = false) Boolean ativo,
            Pageable pageable) {
        return ResponseEntity.ok(service.buscar(nome, precoMin, precoMax, ativo, pageable));
    }

    @GetMapping("/{codigo}")
    @Operation(summary = "Buscar por código")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Produto encontrado"),
            @ApiResponse(responseCode = "404", description = "Não encontrado")
    })
    public ResponseEntity<ProdutoResponseDTO> buscarPorCodigo(
            @Parameter(description = "Código do produto")
            @PathVariable String codigo) {
        return ResponseEntity.ok(service.buscarPorCodigo(codigo));
    }

    @PatchMapping("/{codigo}")
    @Operation(
            summary = "Editar produto (parcial)",
            description = "Apenas os campos enviados são alterados. Campos omitidos permanecem como estão."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Produto atualizado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Não encontrado")
    })
    public ResponseEntity<ProdutoResponseDTO> editar(
            @PathVariable String codigo,
            @Valid @RequestBody ProdutoRequestDTO dto) {
        return ResponseEntity.ok(service.editar(codigo, dto));
    }

    @DeleteMapping("/{codigo}")
    @Operation(summary = "Inativar produto (soft delete)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Produto inativado"),
            @ApiResponse(responseCode = "404", description = "Não encontrado"),
            @ApiResponse(responseCode = "409", description = "Produto com estoque ou já inativo")
    })
    public ResponseEntity<Void> excluir(
            @Parameter(description = "Código do produto")
            @PathVariable String codigo) {
        service.excluir(codigo);
        return ResponseEntity.noContent().build();
    }
}
