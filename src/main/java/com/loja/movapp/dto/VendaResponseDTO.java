package com.loja.movapp.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.loja.movapp.model.StatusVenda;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de resposta para Venda.
 * Contém id, data, total, status, itens e a lista de pagamentos.
 * Os campos formaPagamento/condicaoPagamento são mantidos para compatibilidade
 * (preenchidos com o único pagamento ou "MISTO" quando há múltiplas formas).
 */
public class VendaResponseDTO {

    private Long id;
    private LocalDateTime data;
    private BigDecimal total;
    private String formaPagamento;
    private String condicaoPagamento;
    private StatusVenda status;
    private String usuario;
    private List<ItemVendaResponseDTO> itens;
    private List<PagamentoVendaResponseDTO> pagamentos;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public VendaResponseDTO(Long id, LocalDateTime data, BigDecimal total,
                            String formaPagamento, String condicaoPagamento,
                            StatusVenda status, String usuario,
                            List<ItemVendaResponseDTO> itens,
                            List<PagamentoVendaResponseDTO> pagamentos) {
        this.id                = id;
        this.data              = data;
        this.total             = total;
        this.formaPagamento    = formaPagamento;
        this.condicaoPagamento = condicaoPagamento;
        this.status            = status;
        this.usuario           = usuario;
        this.itens             = itens;
        this.pagamentos        = pagamentos;
    }

    public Long getId()                                  { return id;                }
    public LocalDateTime getData()                       { return data;              }
    public BigDecimal getTotal()                         { return total;             }
    public String getFormaPagamento()                    { return formaPagamento;    }
    public String getCondicaoPagamento()                 { return condicaoPagamento; }
    public StatusVenda getStatus()                       { return status;            }
    public String getUsuario()                           { return usuario;           }
    public List<ItemVendaResponseDTO> getItens()         { return itens;             }
    public List<PagamentoVendaResponseDTO> getPagamentos() { return pagamentos;      }
}
