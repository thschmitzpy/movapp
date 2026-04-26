package com.loja.movapp.dto;

import com.loja.movapp.model.StatusVenda;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de resposta para Venda.
 * Representa os dados que a API devolve após realizar uma venda.
 * Contém o id, data, total, status e a lista de itens vendidos.
 */
public class VendaResponseDTO {

    private Long id;
    private LocalDateTime data;
    private BigDecimal total;
    private String formaPagamento;
    private String condicaoPagamento;
    private StatusVenda status;
    private List<ItemVendaResponseDTO> itens;

    public VendaResponseDTO(Long id, LocalDateTime data, BigDecimal total,
                            String formaPagamento, String condicaoPagamento,
                            StatusVenda status, List<ItemVendaResponseDTO> itens) {
        this.id                = id;
        this.data              = data;
        this.total             = total;
        this.formaPagamento    = formaPagamento;
        this.condicaoPagamento = condicaoPagamento;
        this.status            = status;
        this.itens             = itens;
    }

    public Long getId()                          { return id;                }
    public LocalDateTime getData()               { return data;              }
    public BigDecimal getTotal()                 { return total;             }
    public String getFormaPagamento()            { return formaPagamento;    }
    public String getCondicaoPagamento()         { return condicaoPagamento; }
    public StatusVenda getStatus()               { return status;            }
    public List<ItemVendaResponseDTO> getItens() { return itens;             }
}