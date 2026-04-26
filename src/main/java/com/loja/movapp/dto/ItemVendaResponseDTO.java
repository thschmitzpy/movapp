package com.loja.movapp.dto;

import java.math.BigDecimal;

public class ItemVendaResponseDTO {

    private String codigoProduto;
    private String nomeProduto;
    private int quantidade;
    private BigDecimal precoUnit;
    private BigDecimal subtotal;

    public ItemVendaResponseDTO(String codigoProduto, String nomeProduto, int quantidade,
                                BigDecimal precoUnit) {

        this.codigoProduto = codigoProduto;
        this.nomeProduto   = nomeProduto;
        this.quantidade    = quantidade;
        this.precoUnit     = precoUnit;
        this.subtotal      = precoUnit.multiply(BigDecimal.valueOf(quantidade));
    }

    public String    getCodigoProduto() {return codigoProduto; }
    public String    getNomeProduto()   {return nomeProduto; }
    public int       getQuantidade()    {return quantidade; }
    public BigDecimal getPrecoUnit()    {return precoUnit; }
    public BigDecimal getSubtotal()     {return subtotal; }
}

