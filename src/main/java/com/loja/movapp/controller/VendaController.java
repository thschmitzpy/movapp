package com.loja.movapp.controller;

import com.loja.movapp.dto.VendaRequestDTO;
import com.loja.movapp.dto.VendaResponseDTO;
import com.loja.movapp.service.VendaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping
    @Operation(summary = "Listar vendas",
            description = "Retorna todas as vendas realizadas")
    public ResponseEntity<List<VendaResponseDTO>> listarVendas() {
        return ResponseEntity.ok(service.listarVendas());
    }
}
