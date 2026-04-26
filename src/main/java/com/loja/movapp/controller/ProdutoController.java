package com.loja.movapp.controller;

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
    public ResponseEntity<ProdutoResponseDTO> cadastrar(@Valid @RequestBody ProdutoRequestDTO dto) {
        ProdutoResponseDTO salvo = service.salvar(dto);
        return ResponseEntity
                .created(URI.create("/produtos/" + salvo.getCodigo()))
                .body(salvo);
    }

    @GetMapping
    @Operation(summary = "Listar produtos paginado", description = "Filtra por nome quando o parâmetro 'nome' é informado")
    public ResponseEntity<Page<ProdutoResponseDTO>> listar(
            @Parameter(description = "Filtro parcial por nome (opcional)")
            @RequestParam(required = false) String nome,
            Pageable pageable) {
        if (nome != null && !nome.isBlank()) {
            return ResponseEntity.ok(service.listarPorNome(nome, pageable));
        }
        return ResponseEntity.ok(service.listarPaginado(pageable));
    }

    @GetMapping("/{codigo}")
    @Operation(summary = "Buscar por código")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Produto encontrado"),
            @ApiResponse(responseCode = "404", description = "Não encontrado")
    })
    public ResponseEntity<ProdutoResponseDTO> buscar(
            @Parameter(description = "Código do produto")
            @PathVariable String codigo) {
        return ResponseEntity.ok(service.buscarPorCodigo(codigo));
    }

    @PutMapping("/{codigo}")
    @Operation(summary = "Editar produto")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Produto atualizado"),
            @ApiResponse(responseCode = "404", description = "Não encontrado")
    })
    public ResponseEntity<ProdutoResponseDTO> editar(
            @PathVariable String codigo,
            @Valid @RequestBody ProdutoRequestDTO dto) {
        return ResponseEntity.ok(service.editar(codigo, dto));
    }

    @DeleteMapping("/{codigo}")
    @Operation(summary = "Excluir produto")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Excluído com sucesso"),
            @ApiResponse(responseCode = "404", description = "Não encontrado"),
            @ApiResponse(responseCode = "409", description = "Produto com estoque")
    })
    public ResponseEntity<Void> excluir(
            @Parameter(description = "Código do produto")
            @PathVariable String codigo) {
        service.excluir(codigo);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/preco")
    @Operation(summary = "Buscar por faixa de preço",
            description = "Retorna produtos entre o preço mínimo e máximo com paginação")
    public ResponseEntity<Page<ProdutoResponseDTO>> buscarPorFaixaDePreco(
            @Parameter(description = "Preço mínimo") @RequestParam BigDecimal min,
            @Parameter(description = "Preço máximo") @RequestParam BigDecimal max,
            Pageable pageable) {
        Page<ProdutoResponseDTO> produtos = service.buscarPorFaixaDePreco(min, max, pageable);
        if (produtos.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(produtos);
    }
}
