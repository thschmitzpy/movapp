package com.loja.movapp.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class ItemVendaRequestDTO {

    @NotBlank(message = "Codigo no produto é obrigatório")
    private String codigoProduto;

    @Min( value = 1, message = "Quantidade minima é 1")
    private int quantidade;

    public String getCodigoProduto() {return codigoProduto; }
    public int getQuantidade()       {return quantidade; }

    public void setCodigoProduto(String codigoProduto) {this.codigoProduto = codigoProduto;  }
    public void setQuantidade(int quantidade)          {this.quantidade = quantidade;  }
}
