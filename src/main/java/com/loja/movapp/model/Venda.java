package com.loja.movapp.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "vendas")
public class Venda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data")
    private LocalDateTime data;

    @Column(name = "total")
    private double total;

    @Column(name = "forma_pagamento")
    private String formaPagamento;

    @Column(name = "condicao_pagamento")
    private String condicaoPagamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "varchar(20) default 'FECHADA'")
    private StatusVenda status;

    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemVenda> itens;

    public Long getId()                    { return id;                }
    public LocalDateTime getData()         { return data;              }
    public double getTotal()               { return total;             }
    public String getFormaPagamento()      { return formaPagamento;    }
    public String getCondicaoPagamento()   { return condicaoPagamento; }
    public StatusVenda getStatus()         { return status;            }
    public List<ItemVenda> getItens()      { return itens;             }

    public void setId(Long id)                          { this.id                = id;                }
    public void setData(LocalDateTime data)             { this.data              = data;              }
    public void setTotal(double total)                  { this.total             = total;             }
    public void setFormaPagamento(String f)             { this.formaPagamento    = f;                 }
    public void setCondicaoPagamento(String c)          { this.condicaoPagamento = c;                 }
    public void setStatus(StatusVenda status)           { this.status            = status;            }
    public void setItens(List<ItemVenda> itens)         { this.itens             = itens;             }
}

