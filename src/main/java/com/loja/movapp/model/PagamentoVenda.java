package com.loja.movapp.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "pagamentos_venda")
public class PagamentoVenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venda_id", nullable = false)
    private Venda venda;

    @Column(name = "forma_pagamento", nullable = false)
    private String formaPagamento;

    @Column(name = "condicao_pagamento")
    private String condicaoPagamento;

    @Column(name = "valor", precision = 10, scale = 2, nullable = false)
    private BigDecimal valor;

    public Long       getId()                { return id;                }
    public Venda      getVenda()             { return venda;             }
    public String     getFormaPagamento()    { return formaPagamento;    }
    public String     getCondicaoPagamento() { return condicaoPagamento; }
    public BigDecimal getValor()             { return valor;             }

    public void setId(Long id)                              { this.id                = id; }
    public void setVenda(Venda venda)                       { this.venda             = venda; }
    public void setFormaPagamento(String formaPagamento)    { this.formaPagamento    = formaPagamento; }
    public void setCondicaoPagamento(String condicaoPagamento) { this.condicaoPagamento = condicaoPagamento; }
    public void setValor(BigDecimal valor)                  { this.valor             = valor; }
}
