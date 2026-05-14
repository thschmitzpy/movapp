package com.loja.movapp.repository;

import com.loja.movapp.model.Produto;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProdutoRepository
        extends JpaRepository<Produto, String>, JpaSpecificationExecutor<Produto> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Produto p WHERE p.codigo = :codigo")
    Optional<Produto> buscarParaAtualizacaoEstoque(@Param("codigo") String codigo);
}
