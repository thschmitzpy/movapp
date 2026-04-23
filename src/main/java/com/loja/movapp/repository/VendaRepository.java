package com.loja.movapp.repository;

import com.loja.movapp.model.Venda;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface VendaRepository extends JpaRepository<Venda, Long> {

    Page<Venda> findByDataBetween(LocalDateTime inicio, LocalDateTime fim, Pageable pageable);
}
