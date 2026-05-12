package com.loja.movapp.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.math.BigDecimal;

public class PagamentoVendaResponseDTO {

    private String formaPagamento;
    private String condicaoPagamento;
    private BigDecimal valor;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public PagamentoVendaResponseDTO(String formaPagamento, String condicaoPagamento, BigDecimal valor) {
        this.formaPagamento    = formaPagamento;
        this.condicaoPagamento = condicaoPagamento;
        this.valor             = valor;
    }

    public String     getFormaPagamento()    { return formaPagamento;    }
    public String     getCondicaoPagamento() { return condicaoPagamento; }
    public BigDecimal getValor()             { return valor;             }
}
