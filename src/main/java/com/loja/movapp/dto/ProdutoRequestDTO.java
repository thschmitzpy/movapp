package com.loja.movapp.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ProdutoRequestDTO {

    @NotBlank(message = "Código não pode estar vazio")
    @Size(max = 20, message = "Código deve ter no máximo 20 caracteres")
    private String codigo;

    @NotBlank(message = "Nome não pode estar vazio")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    private String nome;

    @Size(max = 50, message = "Cor deve ter no máximo 50 caracteres")
    private String cor;

    @Size(max = 10, message = "Tamanho deve ter no máximo 10 caracteres")
    private String tamanho;

    @Min(value = 0, message = "Preço não pode ser negativo")
    private double preco;

    @Min(value = 0, message = "Estoque não pode ser negativo")
    private int estoque;

    public String getCodigo()  { return codigo;  }
    public String getNome()    { return nome;    }
    public String getCor()     { return cor;     }
    public String getTamanho() { return tamanho; }
    public double getPreco()   { return preco;   }
    public int    getEstoque() { return estoque; }

    public void setCodigo(String codigo)   { this.codigo  = codigo;  }
    public void setNome(String nome)       { this.nome    = nome;    }
    public void setCor(String cor)         { this.cor     = cor;     }
    public void setTamanho(String tamanho) { this.tamanho = tamanho; }
    public void setPreco(double preco)     { this.preco   = preco;   }
    public void setEstoque(int estoque)    { this.estoque = estoque; }
}
