package com.loja.movapp.dto;

import com.loja.movapp.model.StatusVenda;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class VendaRequestDTO {

    @NotEmpty(message = "A venda precisa de pelo menos 1 item")
    private List<ItemVendaRequestDTO> itens;

    @NotBlank(message = "Forma de pagamento é obrigatória")
    private String formaPagamento;

    @NotBlank(message = "Condição de pagamento é obrigatória")
    private String condicaoPagamento;

    @NotNull(message = "Status da venda é obrigatório")
    private StatusVenda status;

    public List<ItemVendaRequestDTO> getItens()              { return itens;             }
    public String getFormaPagamento()                        { return formaPagamento;    }
    public String getCondicaoPagamento()                     { return condicaoPagamento; }
    public StatusVenda getStatus()                           { return status;            }

    public void setItens(List<ItemVendaRequestDTO> itens)        { this.itens             = itens;             }
    public void setFormaPagamento(String formaPagamento)         { this.formaPagamento    = formaPagamento;    }
    public void setCondicaoPagamento(String condicaoPagamento)   { this.condicaoPagamento = condicaoPagamento; }
    public void setStatus(StatusVenda status)                    { this.status            = status;            }
}

