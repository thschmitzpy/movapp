package com.loja.movapp.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class ProdutoRequestDTO {

    @NotBlank(message = "Código não pode estar vazio")
    private String codigo;

    @NotBlank(message = "Nome não pode estar vazio")
    private String nome;

    private String  cor;
    private String tamanho;

    @Min(value = 0, message = "Preço não pode ser negativo")
    private double preco;

    @Min(value = 0, message = "Estoque não pode estar negativo !")
    private int estoque;

    public String getCodigo()  { return codigo;  }
    public String getNome()    { return nome;  }
    public String getCor()     { return cor;  }
    public String getTamanho() { return tamanho;  }
    public double getPreco()   { return preco; }
    public int getEstoque()    { return estoque; }

    public void setCodigo(String codigo)   { this.codigo = codigo;  }
    public void setNome(String nome)       { this.nome = nome;  }
    public void setCor(String cor)         { this.cor = cor;  }
    public void setTamanho(String tamanho) { this.tamanho = tamanho;  }
    public void setPreco(double preco)     { this.preco = preco;  }
    public void setEstoque(int estoque)    { this.estoque = estoque;  }

}

