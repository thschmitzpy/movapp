package com.loja.movapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class PagamentoVendaRequestDTO {

    @NotBlank(message = "Forma de pagamento é obrigatória")
    private String formaPagamento;

    private String condicaoPagamento;

    @NotNull(message = "Valor do pagamento é obrigatório")
    @Positive(message = "Valor do pagamento deve ser maior que zero")
    private BigDecimal valor;

    public String     getFormaPagamento()    { return formaPagamento;    }
    public String     getCondicaoPagamento() { return condicaoPagamento; }
    public BigDecimal getValor()             { return valor;             }

    public void setFormaPagamento(String formaPagamento)       { this.formaPagamento    = formaPagamento; }
    public void setCondicaoPagamento(String condicaoPagamento) { this.condicaoPagamento = condicaoPagamento; }
    public void setValor(BigDecimal valor)                     { this.valor             = valor; }
}
