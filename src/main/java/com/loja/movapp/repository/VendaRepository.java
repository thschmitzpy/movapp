package com.loja.movapp.repository;

import com.loja.movapp.model.Venda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

//Responsavel por acessar o banco de dados da tabela de vendas.
@Repository
public interface VendaRepository extends JpaRepository<Venda, Long>{
}
