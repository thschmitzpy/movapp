package com.loja.movapp.controller;

import com.loja.movapp.dto.VendaRequestDTO;
import com.loja.movapp.dto.VendaResponseDTO;
import com.loja.movapp.service.VendaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller de Venda.
 * Responsável por receber as requisições de venda
 * e devolver as respostas para o cliente.
 */
@RestController
@RequestMapping("/vendas")
@Tag(name = "Vendas", description = "Endpoints para gerenciamento de vendas")
public class VendaController {

    @Autowired
    private VendaService service;


    @PostMapping
    @Operation(summary = "Realizar venda",
            description = "Registra uma nova venda e atualiza o estoque")
    public ResponseEntity<?> realizarVenda(@Valid @RequestBody VendaRequestDTO dto) {
        try {
            VendaResponseDTO venda = service.realizarVenda(dto);
            return ResponseEntity.ok(venda);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar venda pendente",
            description = "Permite alterar itens, pagamento e status de uma venda PENDENTE")
    public ResponseEntity<?> atualizar(
            @Parameter(description = "ID da venda") @PathVariable Long id,
            @Valid @RequestBody VendaRequestDTO dto) {
        try {
            return ResponseEntity.ok(service.atualizarVenda(id, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    @Operation(summary = "Listar / filtrar vendas paginado",
            description = "Filtra por ID ou por data (yyyy-MM-dd). Sem filtros retorna todas paginado.")
    public ResponseEntity<Page<VendaResponseDTO>> listarVendas(
            @Parameter(description = "ID exato da venda") @RequestParam(required = false) Long id,
            @Parameter(description = "Data da venda (yyyy-MM-dd)") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            Pageable pageable) {
        return ResponseEntity.ok(service.buscarPorFiltros(id, data, pageable));
    }

    @GetMapping("/todas")
    @Operation(summary = "Listar todas as vendas",
            description = "Retorna todas as vendas sem paginação")
    public ResponseEntity<List<VendaResponseDTO>> listarTodas() {
        return ResponseEntity.ok(service.listarVendas());
    }
}