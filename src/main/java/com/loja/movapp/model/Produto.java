package com.loja.movapp.model;

import jakarta.persistence.*;

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

    @Column(name = "preco")
    private double preco;

    @Column(name = "estoque")
    private int estoque;

    @Column(name = "tecido")
    private String tecido;

    @Column(name = "tiporoupa")
    private String tipoRoupa;

    @Column(name = "modelagem")
    private String modelagem;

    @Column(name = "estilo")
    private String estilo;

    @Column(name = "genero")
    private String genero;


    public String getCodigo()    { return codigo;    }
    public String getNome()      { return nome;      }
    public String getCor()       { return cor;       }
    public String getTamanho()   { return tamanho;   }
    public double getPreco()     { return preco;     }
    public int    getEstoque()   { return estoque;   }
    public String getTecido()    { return tecido;    }
    public String getTipoRoupa() { return tipoRoupa; }
    public String getModelagem() { return modelagem; }
    public String getEstilo()    { return estilo;    }
    public String getGenero()    { return genero;    }


    public void setCodigo(String codigo)       { this.codigo    = codigo;    }
    public void setNome(String nome)           { this.nome      = nome;      }
    public void setCor(String cor)             { this.cor       = cor;       }
    public void setTamanho(String tamanho)     { this.tamanho   = tamanho;   }
    public void setPreco(double preco)         { this.preco     = preco;     }
    public void setEstoque(int estoque)        { this.estoque   = estoque;   }
    public void setTecido(String tecido)       { this.tecido    = tecido;    }
    public void setTipoRoupa(String tipoRoupa) { this.tipoRoupa = tipoRoupa; }
    public void setModelagem(String modelagem) { this.modelagem = modelagem; }
    public void setEstilo(String estilo)       { this.estilo    = estilo;    }
    public void setGenero(String genero)       { this.genero    = genero;    }
}