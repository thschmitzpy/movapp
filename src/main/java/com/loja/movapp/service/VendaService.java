package com.loja.movapp.service;

import com.loja.movapp.dto.*;
import com.loja.movapp.model.*;
import com.loja.movapp.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service de Venda.
 * Responsável pelas regras de negócio das vendas.
 * Registra a venda e atualiza o estoque automaticamente.
 */
@Service
public class VendaService {

    @Autowired
    private VendaRepository vendaRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Transactional
    public VendaResponseDTO realizarVenda(VendaRequestDTO dto) {

        Venda venda = new Venda();
        venda.setData(LocalDateTime.now());

        List<ItemVenda> itens = new ArrayList<>();
        List<ItemVendaResponseDTO> itensResponse = new ArrayList<>();
        double total = 0;

        for (ItemVendaRequestDTO itemDTO : dto.getItens()) {

            Produto produto = produtoRepository.findById(itemDTO.getCodigoProduto())
                    .orElseThrow(() -> new RuntimeException(
                            "Produto " + itemDTO.getCodigoProduto() + " não encontrado!"));


            if (produto.getEstoque() < itemDTO.getQuantidade()) {
                throw new RuntimeException(
                        "Estoque insuficiente para " + produto.getNome() +
                                "! Disponível: " + produto.getEstoque());
            }

            produto.setEstoque(produto.getEstoque() - itemDTO.getQuantidade());
            produtoRepository.save(produto);

            // cria o item da venda
            ItemVenda item = new ItemVenda();
            item.setVenda(venda);
            item.setProduto(produto);
            item.setQuantidade(itemDTO.getQuantidade());
            item.setPrecoUnit(produto.getPreco());
            itens.add(item);

            total += produto.getPreco() * itemDTO.getQuantidade();

            itensResponse.add(new ItemVendaResponseDTO(
                    produto.getCodigo(),
                    produto.getNome(),
                    itemDTO.getQuantidade(),
                    produto.getPreco()
            ));
        }

        venda.setItens(itens);
        venda.setTotal(total);
        Venda vendaSalva = vendaRepository.save(venda);

        return new VendaResponseDTO(
                vendaSalva.getId(),
                vendaSalva.getData(),
                vendaSalva.getTotal(),
                itensResponse
        );
    }

    /**
     * Lista todas as vendas realizadas.
     */
    public List<VendaResponseDTO> listarVendas() {
        return vendaRepository.findAll()
                .stream()
                .map(v -> new VendaResponseDTO(
                        v.getId(),
                        v.getData(),
                        v.getTotal(),
                        v.getItens().stream()
                                .map(i -> new ItemVendaResponseDTO(
                                        i.getProduto().getCodigo(),
                                        i.getProduto().getNome(),
                                        i.getQuantidade(),
                                        i.getPrecoUnit()
                                )).toList()
                )).toList();
    }
}