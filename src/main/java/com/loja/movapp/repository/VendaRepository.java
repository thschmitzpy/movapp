package com.loja.movapp.repository;

import com.loja.movapp.model.StatusVenda;
import com.loja.movapp.model.Venda;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Repository
public interface VendaRepository extends JpaRepository<Venda, Long> {

    @Query("SELECT v FROM Venda v WHERE v.data >= :inicio AND v.data < :fim")
    Page<Venda> buscarNoIntervalo(@Param("inicio") LocalDateTime inicio,
                                  @Param("fim") LocalDateTime fim,
                                  Pageable pageable);

    @Query("SELECT COUNT(v) FROM Venda v WHERE v.status = :status")
    long contarPorStatus(@Param("status") StatusVenda status);

    @Query("""
            SELECT COUNT(v) FROM Venda v
            WHERE v.status = :status
              AND v.data >= :inicio
              AND v.data <  :fim
            """)
    long contarPorStatusNoIntervalo(@Param("status") StatusVenda status,
                                    @Param("inicio") LocalDateTime inicio,
                                    @Param("fim") LocalDateTime fim);

    @Query("SELECT COALESCE(SUM(v.total), 0) FROM Venda v WHERE v.status = :status")
    BigDecimal somarTotalPorStatus(@Param("status") StatusVenda status);

    @Query("""
            SELECT COALESCE(SUM(v.total), 0) FROM Venda v
            WHERE v.status = :status
              AND v.data >= :inicio
              AND v.data <  :fim
            """)
    BigDecimal somarTotalPorStatusNoIntervalo(@Param("status") StatusVenda status,
                                              @Param("inicio") LocalDateTime inicio,
                                              @Param("fim") LocalDateTime fim);
}
