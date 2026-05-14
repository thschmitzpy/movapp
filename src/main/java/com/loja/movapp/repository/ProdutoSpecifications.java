package com.loja.movapp.repository;

import com.loja.movapp.model.Produto;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

// Predicados reutilizáveis para o endpoint unificado de busca de produtos.
// Cada método retorna null quando o filtro não foi informado, deixando o
// Specification.where(...).and(...) ignorar a cláusula naturalmente.
public final class ProdutoSpecifications {

    private ProdutoSpecifications() {}

    public static Specification<Produto> nomeContem(String nome) {
        if (nome == null || nome.isBlank()) return null;
        String like = "%" + nome.toLowerCase() + "%";
        return (root, q, cb) -> cb.like(cb.lower(root.get("nome")), like);
    }

    public static Specification<Produto> precoEntre(BigDecimal min, BigDecimal max) {
        if (min == null && max == null) return null;
        return (root, q, cb) -> {
            if (min != null && max != null) return cb.between(root.get("preco"), min, max);
            if (min != null)                return cb.greaterThanOrEqualTo(root.get("preco"), min);
            return cb.lessThanOrEqualTo(root.get("preco"), max);
        };
    }

    public static Specification<Produto> ativo(Boolean ativo) {
        if (ativo == null) return null;
        return (root, q, cb) -> cb.equal(root.get("ativo"), ativo);
    }
}
