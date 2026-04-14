package com.loja.movapp.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class VendaRequestDTO {

    @NotEmpty(message = "A venda precisa de pelo menos 1 item")
    private List<ItemVendaRequestDTO> itens;

    public List<ItemVendaRequestDTO> getItens()           { return itens; }
    public void setItens(List<ItemVendaRequestDTO> itens) { this.itens = itens; }
}

