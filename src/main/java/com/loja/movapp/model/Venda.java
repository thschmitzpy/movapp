package com.loja.movapp.model;

import jakarta.persistence.*;
import org.springframework.boot.autoconfigure.web.WebProperties;

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

    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL)
    private List<ItemVenda> itens;

    public Long getId()            {return id;  }
    public LocalDateTime getData() {return data;  }
    public double getTotal()       {return total;  }
    public List<ItemVenda> getItens()  {return itens;  }

    public void setId(Long id)                  { this.id    = id;    }
    public void setData(LocalDateTime data)     { this.data  = data;  }
    public void setTotal(double total)          { this.total = total; }
    public void setItens(List<ItemVenda> itens) { this.itens = itens; }
}

