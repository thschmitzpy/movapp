package com.loja.movapp.repository;

import com.loja.movapp.model.Venda;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface VendaRepository extends JpaRepository<Venda, Long> {

    /**
     * Busca vendas no intervalo semi-aberto [inicio, fim).
     * Importante: usar &lt; fim (não BETWEEN) para incluir vendas com
     * frações de segundo até o último instante antes de 'fim',
     * independentemente da precisão do TIMESTAMP no banco.
     */
    @Query("SELECT v FROM Venda v WHERE v.data >= :inicio AND v.data < :fim")
    Page<Venda> buscarNoIntervalo(@Param("inicio") LocalDateTime inicio,
                                  @Param("fim") LocalDateTime fim,
                                  Pageable pageable);
}
