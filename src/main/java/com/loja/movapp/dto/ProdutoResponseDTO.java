package com.loja.movapp.dto;

import java.math.BigDecimal;

public class ProdutoResponseDTO {

    private String codigo;
    private String nome;
    private String cor;
    private String tamanho;
    private BigDecimal preco;
    private int estoque;

    public ProdutoResponseDTO(String codigo, String nome, String cor, String tamanho,
                              BigDecimal preco, int estoque) {

        this.codigo  = codigo;
        this.nome    = nome;
        this.cor     = cor;
        this.tamanho = tamanho;
        this.preco   = preco;
        this.estoque = estoque;
    }

    public String    getCodigo()  {return codigo; }
    public String    getNome()    {return nome; }
    public String    getCor()     {return cor; }
    public String    getTamanho() {return tamanho; }
    public BigDecimal getPreco()  {return preco; }
    public int       getEstoque() {return estoque; }
}
