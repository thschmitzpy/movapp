package com.loja.movapp.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de resposta para Venda.
 * Representa os dados que a API devolve após realizar uma venda.
 * Contém o id, data, total e a lista de itens vendidos.
 */
public class VendaResponseDTO {

    private Long id;

    private LocalDateTime data;

    private double total;

    private List<ItemVendaResponseDTO> itens;

    public VendaResponseDTO(Long id, LocalDateTime data,
                            double total, List<ItemVendaResponseDTO> itens) {
        this.id    = id;
        this.data  = data;
        this.total = total;
        this.itens = itens;
    }

    public Long getId()                          { return id;    }
    public LocalDateTime getData()               { return data;  }
    public double getTotal()                     { return total; }
    public List<ItemVendaResponseDTO> getItens() { return itens; }
}