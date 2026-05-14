package com.loja.movapp.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class ProdutoRequestDTO {

    @Size(max = 20, message = "Código deve ter no máximo 20 caracteres")
    private String codigo;

    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    private String nome;

    @Size(max = 50, message = "Cor deve ter no máximo 50 caracteres")
    private String cor;

    @Size(max = 10, message = "Tamanho deve ter no máximo 10 caracteres")
    private String tamanho;

    @DecimalMin(value = "0.01", message = "Preço deve ser maior que zero")
    @Digits(integer = 8, fraction = 2, message = "Preço deve ter até 8 dígitos inteiros e 2 decimais")
    private BigDecimal preco;

    @Min(value = 0, message = "Estoque não pode ser negativo")
    private Integer estoque;

    public String getCodigo()  { return codigo;  }
    public String getNome()    { return nome;    }
    public String getCor()     { return cor;     }
    public String getTamanho() { return tamanho; }
    public BigDecimal getPreco() { return preco;   }
    public Integer getEstoque() { return estoque; }

    public void setCodigo(String codigo)   { this.codigo  = codigo;  }
    public void setNome(String nome)       { this.nome    = nome;    }
    public void setCor(String cor)         { this.cor     = cor;     }
    public void setTamanho(String tamanho) { this.tamanho = tamanho; }
    public void setPreco(BigDecimal preco) { this.preco   = preco;   }
    public void setEstoque(Integer estoque) { this.estoque = estoque; }
}
