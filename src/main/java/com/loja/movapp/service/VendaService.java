package com.loja.movapp.service;

import com.loja.movapp.dto.*;
import com.loja.movapp.model.*;
import com.loja.movapp.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service de Venda.
 * Responsável pelas regras de negócio das vendas.
 * Registra a venda e atualiza o estoque automaticamente.
 * Limpa o cache de produtos após cada venda pois o estoque muda!
 */
@Service
public class VendaService {

    @Autowired
    private VendaRepository vendaRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    /**
     * Realiza uma venda com vários produtos.
     * Verifica o estoque de cada produto antes de vender.
     * Diminui o estoque automaticamente após a venda.
     * Limpa o cache de produtos pois o estoque foi alterado!
     */
    @Transactional
    @CacheEvict(value = "produtos", allEntries = true)
    public VendaResponseDTO realizarVenda(VendaRequestDTO dto) {

        Venda venda = new Venda();
        venda.setData(LocalDateTime.now());
        venda.setFormaPagamento(dto.getFormaPagamento());
        venda.setCondicaoPagamento(dto.getCondicaoPagamento());
        venda.setStatus(dto.getStatus());

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
                vendaSalva.getFormaPagamento(),
                vendaSalva.getCondicaoPagamento(),
                vendaSalva.getStatus(),
                itensResponse
        );
    }

    /**
     * Atualiza uma venda PENDENTE.
     * Devolve o estoque dos itens antigos e deduz o dos novos.
     * Lança exceção se a venda for FECHADA.
     */
    @Transactional
    @CacheEvict(value = "produtos", allEntries = true)
    public VendaResponseDTO atualizarVenda(Long id, VendaRequestDTO dto) {

        Venda venda = vendaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Venda #" + id + " não encontrada!"));

        if (venda.getStatus() != StatusVenda.PENDENTE) {
            throw new RuntimeException("Apenas vendas PENDENTES podem ser alteradas!");
        }

        // Devolve estoque de todos os itens atuais
        for (ItemVenda item : venda.getItens()) {
            Produto p = item.getProduto();
            p.setEstoque(p.getEstoque() + item.getQuantidade());
            produtoRepository.save(p);
        }

        // Remove os itens antigos (orphanRemoval cuida do DELETE)
        venda.getItens().clear();

        // Processa os novos itens
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

            ItemVenda novoItem = new ItemVenda();
            novoItem.setVenda(venda);
            novoItem.setProduto(produto);
            novoItem.setQuantidade(itemDTO.getQuantidade());
            novoItem.setPrecoUnit(produto.getPreco());
            venda.getItens().add(novoItem);

            total += produto.getPreco() * itemDTO.getQuantidade();

            itensResponse.add(new ItemVendaResponseDTO(
                    produto.getCodigo(), produto.getNome(),
                    itemDTO.getQuantidade(), produto.getPreco()
            ));
        }

        venda.setTotal(total);
        venda.setFormaPagamento(dto.getFormaPagamento());
        venda.setCondicaoPagamento(dto.getCondicaoPagamento());
        venda.setStatus(dto.getStatus());

        Venda vendaSalva = vendaRepository.save(venda);

        return new VendaResponseDTO(
                vendaSalva.getId(), vendaSalva.getData(), vendaSalva.getTotal(),
                vendaSalva.getFormaPagamento(), vendaSalva.getCondicaoPagamento(),
                vendaSalva.getStatus(), itensResponse
        );
    }

    /**
     * Exclui uma venda PENDENTE e devolve o estoque de todos os itens.
     */
    @Transactional
    @CacheEvict(value = "produtos", allEntries = true)
    public void excluirVenda(Long id) {

        Venda venda = vendaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Venda #" + id + " não encontrada!"));

        if (venda.getStatus() != StatusVenda.PENDENTE) {
            throw new RuntimeException("Apenas vendas PENDENTES podem ser excluídas!");
        }

        for (ItemVenda item : venda.getItens()) {
            Produto p = item.getProduto();
            p.setEstoque(p.getEstoque() + item.getQuantidade());
            produtoRepository.save(p);
        }

        vendaRepository.delete(venda);
    }

    // ── helper: Venda → VendaResponseDTO ────────────────────────
    private VendaResponseDTO toDTO(Venda v) {
        List<ItemVendaResponseDTO> itens = v.getItens().stream()
                .map(i -> new ItemVendaResponseDTO(
                        i.getProduto().getCodigo(),
                        i.getProduto().getNome(),
                        i.getQuantidade(),
                        i.getPrecoUnit()
                )).toList();
        return new VendaResponseDTO(
                v.getId(), v.getData(), v.getTotal(),
                v.getFormaPagamento(), v.getCondicaoPagamento(), v.getStatus(), itens);
    }

    // ── busca com filtros opcionais ──────────────────────────────
    public Page<VendaResponseDTO> buscarPorFiltros(Long id, LocalDate data, Pageable pageable) {

        if (id != null) {
            return vendaRepository.findById(id)
                    .map(v -> (Page<VendaResponseDTO>) new PageImpl<>(List.of(toDTO(v)), pageable, 1))
                    .orElse(Page.empty(pageable));
        }

        if (data != null) {
            LocalDateTime inicio = data.atStartOfDay();
            LocalDateTime fim    = data.atTime(23, 59, 59);
            return vendaRepository.findByDataBetween(inicio, fim, pageable).map(this::toDTO);
        }

        return listarVendasPaginado(pageable);
    }

    public Page<VendaResponseDTO> listarVendasPaginado(Pageable pageable) {
        return vendaRepository.findAll(pageable).map(this::toDTO);
    }

    public List<VendaResponseDTO> listarVendas() {
        return vendaRepository.findAll().stream().map(this::toDTO).toList();
    }
}