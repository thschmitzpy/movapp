package com.loja.movapp.repository;

import com.loja.movapp.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public interface ProdutoRepository  extends JpaRepository <Produto, String>{

    @Query("SELECT p FROM Produto p WHERE p.preco >= :min AND p.preco <= :max")
    List<Produto> buscarPorFaixaDePreco(@Param("min") double min,
                                        @Param("max") double max);

    Page<Produto> findAll(Pageable pageable);

    Page<Produto> findByNomeContainingIgnoreCase(String nome, Pageable pageable);

    @Query("SELECT p FROM Produto p WHERE p.preco >= :min AND p.preco <= :max")
    Page<Produto> buscarPorFaixaDePreco(
            @Param("min") double min,
            @Param("max") double max,
            Pageable pageable
    );
}







