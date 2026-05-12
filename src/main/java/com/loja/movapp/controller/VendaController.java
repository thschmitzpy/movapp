package com.loja.movapp.controller;

import com.loja.movapp.dto.VendaRequestDTO;
import com.loja.movapp.dto.VendaResponseDTO;
import com.loja.movapp.service.IdempotencyService;
import com.loja.movapp.service.VendaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/vendas")
@Tag(name = "Vendas", description = "Endpoints para gerenciamento de vendas")
public class VendaController {

    @Autowired
    private VendaService service;

    @Autowired
    private IdempotencyService idempotencyService;

    @PostMapping
    @Operation(summary = "Realizar venda",
            description = "Registra uma nova venda e atualiza o estoque. " +
                    "Aceita o header opcional 'Idempotency-Key' (UUID) para garantir que retries " +
                    "ou duplo-clique no cliente não criem vendas duplicadas — a primeira execução " +
                    "é registrada e chamadas subsequentes com a mesma chave retornam a resposta cacheada.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Venda registrada com sucesso (ou replay idempotente)"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado"),
            @ApiResponse(responseCode = "409",
                    description = "Estoque insuficiente, conflito de concorrência, " +
                            "ou Idempotency-Key reutilizada com payload diferente / em processamento")
    })
    public ResponseEntity<VendaResponseDTO> realizarVenda(
            @Valid @RequestBody VendaRequestDTO dto,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal UserDetails userDetails) {
        VendaResponseDTO resposta = idempotencyService.executar(
                idempotencyKey,
                "POST /vendas",
                dto,
                () -> service.realizarVenda(dto, userDetails.getUsername()),
                VendaResponseDTO.class);
        return ResponseEntity.ok(resposta);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar venda pendente",
            description = "Permite alterar itens, pagamento e status de uma venda PENDENTE")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Venda atualizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Venda ou produto não encontrado"),
            @ApiResponse(responseCode = "409", description = "Venda não está PENDENTE ou estoque insuficiente")
    })
    public ResponseEntity<VendaResponseDTO> atualizar(
            @Parameter(description = "ID da venda") @PathVariable Long id,
            @Valid @RequestBody VendaRequestDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(service.atualizarVenda(id, dto, userDetails.getUsername()));
    }

    @GetMapping
    @Operation(summary = "Listar / filtrar vendas paginado",
            description = "Filtra por ID ou por data (yyyy-MM-dd). Sem filtros retorna todas paginado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de vendas retornada com sucesso")
    })
    public ResponseEntity<Page<VendaResponseDTO>> listarVendas(
            @Parameter(description = "ID exato da venda") @RequestParam(required = false) Long id,
            @Parameter(description = "Data da venda (yyyy-MM-dd)") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            Pageable pageable) {
        return ResponseEntity.ok(service.buscarPorFiltros(id, data, pageable));
    }

    @PutMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar venda",
            description = "Cancela uma venda FECHADA (restaura estoque) ou PENDENTE. Não pode cancelar venda já cancelada.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Venda cancelada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Venda não encontrada"),
            @ApiResponse(responseCode = "409", description = "Venda já está cancelada")
    })
    public ResponseEntity<Void> cancelar(
            @Parameter(description = "ID da venda") @PathVariable Long id) {
        service.cancelarVenda(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir venda pendente",
            description = "Remove uma venda PENDENTE. Vendas FECHADAS ou CANCELADAS não podem ser excluídas.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Venda excluída com sucesso"),
            @ApiResponse(responseCode = "404", description = "Venda não encontrada"),
            @ApiResponse(responseCode = "409", description = "Venda não está PENDENTE")
    })
    public ResponseEntity<Void> excluir(
            @Parameter(description = "ID da venda") @PathVariable Long id) {
        service.excluirVenda(id);
        return ResponseEntity.noContent().build();
    }

}
