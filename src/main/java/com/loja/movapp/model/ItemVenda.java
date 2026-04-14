package com.loja.movapp.model;

import jakarta.persistence.*;

@Entity
@Table(name = "itens_venda")
public class ItemVenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "venda_id")
    private Venda venda;

    @ManyToOne
    @JoinColumn(name = "codigo")
    private Produto produto;

    @Column(name = "quantidade")
    private int quantidade;

    @Column(name = "preco_unit")
    private double precoUnit;

    public Long getId()          { return id;         }
    public Venda getVenda()      { return venda;      }
    public Produto getProduto()  { return produto;    }
    public int getQuantidade()   { return quantidade; }
    public double getPrecoUnit() { return precoUnit;  }

    public void setId(Long id)              { this.id         = id;         }
    public void setVenda(Venda venda)       { this.venda      = venda;      }
    public void setProduto(Produto produto) { this.produto    = produto;    }
    public void setQuantidade(int qtd)      { this.quantidade = qtd;        }
    public void setPrecoUnit(double preco)  { this.precoUnit  = preco;      }
}


