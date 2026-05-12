package com.loja.movapp.dto;

import com.loja.movapp.model.StatusVenda;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class VendaRequestDTO {

    @NotEmpty(message = "A venda precisa de pelo menos 1 item")
    private List<ItemVendaRequestDTO> itens;

    @Valid
    @NotEmpty(message = "Informe ao menos uma forma de pagamento")
    private List<PagamentoVendaRequestDTO> pagamentos;

    @NotNull(message = "Status da venda é obrigatório")
    private StatusVenda status;

    public List<ItemVendaRequestDTO> getItens()           { return itens;       }
    public List<PagamentoVendaRequestDTO> getPagamentos() { return pagamentos;  }
    public StatusVenda getStatus()                        { return status;      }

    public void setItens(List<ItemVendaRequestDTO> itens)                  { this.itens      = itens; }
    public void setPagamentos(List<PagamentoVendaRequestDTO> pagamentos)   { this.pagamentos = pagamentos; }
    public void setStatus(StatusVenda status)                              { this.status     = status; }
}
