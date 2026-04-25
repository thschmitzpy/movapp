package com.loja.movapp.service;

import com.loja.movapp.dto.*;
import com.loja.movapp.exception.EstoqueInsuficienteException;
import com.loja.movapp.exception.OperacaoNaoPermitidaException;
import com.loja.movapp.exception.RecursoNaoEncontradoException;
import com.loja.movapp.model.*;
import com.loja.movapp.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Service
public class VendaService {

    private static final Logger log = LoggerFactory.getLogger(VendaService.class);

    @Autowired
    private VendaRepository vendaRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Transactional
    @CacheEvict(value = "produtos", allEntries = true)
    public VendaResponseDTO realizarVenda(VendaRequestDTO dto) {
        log.info("Iniciando venda: {} item(ns), pagamento={}", dto.getItens().size(), dto.getFormaPagamento());

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
                    .orElseThrow(() -> {
                        log.warn("Venda bloqueada: produto '{}' não encontrado", itemDTO.getCodigoProduto());
                        return new RecursoNaoEncontradoException(
                                "Produto \"" + itemDTO.getCodigoProduto() + "\" não encontrado!");
                    });

            if (produto.getEstoque() < itemDTO.getQuantidade()) {
                log.warn("Venda bloqueada: estoque insuficiente para '{}'. Disponível: {}, solicitado: {}",
                        produto.getNome(), produto.getEstoque(), itemDTO.getQuantidade());
                throw new EstoqueInsuficienteException(
                        "Estoque insuficiente para \"" + produto.getNome() +
                                "\". Disponível: " + produto.getEstoque() +
                                ", solicitado: " + itemDTO.getQuantidade());
            }

            produto.setEstoque(produto.getEstoque() - itemDTO.getQuantidade());
            produtoRepository.save(produto);
            log.info("Estoque atualizado: produto='{}', novo estoque={}", produto.getNome(), produto.getEstoque());

            ItemVenda item = new ItemVenda();
            item.setVenda(venda);
            item.setProduto(produto);
            item.setQuantidade(itemDTO.getQuantidade());
            item.setPrecoUnit(produto.getPreco());
            itens.add(item);

            total += produto.getPreco() * itemDTO.getQuantidade();
            itensResponse.add(new ItemVendaResponseDTO(
                    produto.getCodigo(), produto.getNome(),
                    itemDTO.getQuantidade(), produto.getPreco()
            ));
        }

        venda.setItens(itens);
        venda.setTotal(total);
        Venda vendaSalva = vendaRepository.save(venda);

        log.info("Venda registrada: id={}, total=R${}, status={}", vendaSalva.getId(), vendaSalva.getTotal(), vendaSalva.getStatus());
        return new VendaResponseDTO(
                vendaSalva.getId(), vendaSalva.getData(), vendaSalva.getTotal(),
                vendaSalva.getFormaPagamento(), vendaSalva.getCondicaoPagamento(),
                vendaSalva.getStatus(), itensResponse
        );
    }

    @Transactional
    @CacheEvict(value = "produtos", allEntries = true)
    public VendaResponseDTO atualizarVenda(Long id, VendaRequestDTO dto) {
        log.info("Atualizando venda: id={}", id);

        Venda venda = vendaRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Tentativa de atualizar venda inexistente: id={}", id);
                    return new RecursoNaoEncontradoException("Venda #" + id + " não encontrada!");
                });

        if (venda.getStatus() != StatusVenda.PENDENTE) {
            log.warn("Atualização bloqueada: venda id={} está com status={}", id, venda.getStatus());
            throw new OperacaoNaoPermitidaException("Apenas vendas PENDENTES podem ser alteradas!");
        }

        for (ItemVenda item : venda.getItens()) {
            Produto p = item.getProduto();
            p.setEstoque(p.getEstoque() + item.getQuantidade());
            produtoRepository.save(p);
        }
        venda.getItens().clear();

        List<ItemVendaResponseDTO> itensResponse = new ArrayList<>();
        double total = 0;

        for (ItemVendaRequestDTO itemDTO : dto.getItens()) {
            Produto produto = produtoRepository.findById(itemDTO.getCodigoProduto())
                    .orElseThrow(() -> {
                        log.warn("Atualização bloqueada: produto '{}' não encontrado", itemDTO.getCodigoProduto());
                        return new RecursoNaoEncontradoException(
                                "Produto \"" + itemDTO.getCodigoProduto() + "\" não encontrado!");
                    });

            if (produto.getEstoque() < itemDTO.getQuantidade()) {
                log.warn("Atualização bloqueada: estoque insuficiente para '{}'. Disponível: {}, solicitado: {}",
                        produto.getNome(), produto.getEstoque(), itemDTO.getQuantidade());
                throw new EstoqueInsuficienteException(
                        "Estoque insuficiente para \"" + produto.getNome() +
                                "\". Disponível: " + produto.getEstoque() +
                                ", solicitado: " + itemDTO.getQuantidade());
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
        log.info("Venda atualizada: id={}, total=R${}, status={}", vendaSalva.getId(), vendaSalva.getTotal(), vendaSalva.getStatus());

        return new VendaResponseDTO(
                vendaSalva.getId(), vendaSalva.getData(), vendaSalva.getTotal(),
                vendaSalva.getFormaPagamento(), vendaSalva.getCondicaoPagamento(),
                vendaSalva.getStatus(), itensResponse
        );
    }

    @Transactional
    @CacheEvict(value = "produtos", allEntries = true)
    public void excluirVenda(Long id) {
        log.info("Excluindo venda: id={}", id);

        Venda venda = vendaRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Tentativa de excluir venda inexistente: id={}", id);
                    return new RecursoNaoEncontradoException("Venda #" + id + " não encontrada!");
                });

        if (venda.getStatus() != StatusVenda.PENDENTE) {
            log.warn("Exclusão bloqueada: venda id={} está com status={}", id, venda.getStatus());
            throw new OperacaoNaoPermitidaException("Apenas vendas PENDENTES podem ser excluídas!");
        }

        for (ItemVenda item : venda.getItens()) {
            Produto p = item.getProduto();
            p.setEstoque(p.getEstoque() + item.getQuantidade());
            produtoRepository.save(p);
        }

        vendaRepository.delete(venda);
        log.info("Venda excluída e estoque restaurado: id={}", id);
    }

    private VendaResponseDTO toDTO(Venda v) {
        List<ItemVendaResponseDTO> itens = v.getItens().stream()
                .map(i -> new ItemVendaResponseDTO(
                        i.getProduto().getCodigo(), i.getProduto().getNome(),
                        i.getQuantidade(), i.getPrecoUnit()
                )).toList();
        return new VendaResponseDTO(
                v.getId(), v.getData(), v.getTotal(),
                v.getFormaPagamento(), v.getCondicaoPagamento(), v.getStatus(), itens);
    }

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
