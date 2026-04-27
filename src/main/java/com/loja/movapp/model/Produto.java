package com.loja.movapp.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "produtos")
public class Produto {

    @Id
    @Column(name = "codigo")
    private String codigo;

    @Column(name = "produto")
    private String nome;

    @Column(name = "cor")
    private String cor;

    @Column(name = "tamanho")
    private String tamanho;

    @Column(name = "preco", precision = 10, scale = 2)
    private BigDecimal preco;

    @Column(name = "estoque")
    private int estoque;

    // Controla locking otimista: JPA inclui WHERE versao=? em cada UPDATE,
    // garantindo que vendas simultâneas não corrompam o estoque.
    @Version
    @Column(name = "versao")
    private Long versao;

    public String getCodigo()  { return codigo;  }
    public String getNome()    { return nome;    }
    public String getCor()     { return cor;     }
    public String getTamanho() { return tamanho; }
    public BigDecimal getPreco()   { return preco;   }
    public int        getEstoque() { return estoque; }
    public Long       getVersao()  { return versao;  }

    public void setCodigo(String codigo)      { this.codigo  = codigo;  }
    public void setNome(String nome)          { this.nome    = nome;    }
    public void setCor(String cor)            { this.cor     = cor;     }
    public void setTamanho(String tamanho)    { this.tamanho = tamanho; }
    public void setPreco(BigDecimal preco)    { this.preco   = preco;   }
    public void setEstoque(int estoque)       { this.estoque = estoque; }
}