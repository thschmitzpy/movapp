package com.loja.movapp.dto;

public class ItemVendaResponseDTO {

    private String codigoProduto;
    private String nomeProduto;
    private int quantidade;
    private double precoUnit;
    private double subtotal;

    public ItemVendaResponseDTO(String codigoProduto, String nomeProduto, int quantidade,
                                double precoUnit) {

        this.codigoProduto = codigoProduto;
        this.nomeProduto = nomeProduto;
        this.quantidade = quantidade;
        this.precoUnit = precoUnit;
        this.subtotal = quantidade * precoUnit;
    }

    public String getCodigoProduto() {return codigoProduto; }
    public String getNomeProduto()   {return nomeProduto; }
    public int getQuantidade()       {return quantidade; }
    public double getPrecoUnit()     {return precoUnit; }
    public double getSubtotal()      {return subtotal; }
}

