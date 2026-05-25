package com.loja.movapp.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.math.BigDecimal;

public class ResumoVendasResponseDTO {

    private Long vendasFechadas;
    private Long vendasPendentes;
    private BigDecimal totalFechadas;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ResumoVendasResponseDTO(Long vendasFechadas, Long vendasPendentes, BigDecimal totalFechadas) {
        this.vendasFechadas  = vendasFechadas  != null ? vendasFechadas  : 0L;
        this.vendasPendentes = vendasPendentes != null ? vendasPendentes : 0L;
        this.totalFechadas   = totalFechadas   != null ? totalFechadas   : BigDecimal.ZERO;
    }

    public Long       getVendasFechadas()  { return vendasFechadas;  }
    public Long       getVendasPendentes() { return vendasPendentes; }
    public BigDecimal getTotalFechadas()   { return totalFechadas;   }
}
