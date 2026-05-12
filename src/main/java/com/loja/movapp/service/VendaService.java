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
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class,
            maxAttempts = 4,
            backoff = @Backoff(delay = 50, multiplier = 2, random = true)
    )
    @Transactional
    @CacheEvict(value = "produtos", allEntries = true)
    public VendaResponseDTO realizarVenda(VendaRequestDTO dto, String usuario) {
        log.info("Iniciando venda: {} item(ns), {} pagamento(s), usuario={}",
                dto.getItens().size(), dto.getPagamentos().size(), usuario);

        Venda venda = new Venda();
        venda.setData(LocalDateTime.now());
        venda.setStatus(dto.getStatus());

        List<ItemVenda> itens = new ArrayList<>();
        List<ItemVendaResponseDTO> itensResponse = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (ItemVendaRequestDTO itemDTO : dto.getItens()) {
            // Lock pessimista apenas quando o status é FECHADA para serializar deduções de estoque.
            // Vendas PENDENTES não alteram estoque, então um findById simples é suficiente.
            Produto produto = (dto.getStatus() == StatusVenda.FECHADA
                    ? produtoRepository.buscarParaAtualizacaoEstoque(itemDTO.getCodigoProduto())
                    : produtoRepository.findById(itemDTO.getCodigoProduto()))
                    .orElseThrow(() -> {
                        log.warn("Venda bloqueada: produto '{}' não encontrado", itemDTO.getCodigoProduto());
                        return new RecursoNaoEncontradoException(
                                "Produto \"" + itemDTO.getCodigoProduto() + "\" não encontrado!");
                    });

            if (dto.getStatus() == StatusVenda.FECHADA) {
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
            }

            ItemVenda item = new ItemVenda();
            item.setVenda(venda);
            item.setProduto(produto);
            item.setQuantidade(itemDTO.getQuantidade());
            item.setPrecoUnit(produto.getPreco());
            itens.add(item);

            total = total.add(produto.getPreco().multiply(BigDecimal.valueOf(itemDTO.getQuantidade())));
            itensResponse.add(new ItemVendaResponseDTO(
                    produto.getCodigo(), produto.getNome(),
                    itemDTO.getQuantidade(), produto.getPreco()
            ));
        }

        aplicarPagamentos(venda, dto.getPagamentos(), total);

        venda.setItens(itens);
        venda.setTotal(total);
        venda.setUsuario(usuario);
        Venda vendaSalva = vendaRepository.save(venda);

        log.info("Venda registrada: id={}, total=R${}, status={}, usuario={}",
                vendaSalva.getId(), vendaSalva.getTotal(), vendaSalva.getStatus(), vendaSalva.getUsuario());
        return toDTOComItens(vendaSalva, itensResponse);
    }

    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class,
            maxAttempts = 4,
            backoff = @Backoff(delay = 50, multiplier = 2, random = true)
    )
    @Transactional
    @CacheEvict(value = "produtos", allEntries = true)
    public VendaResponseDTO atualizarVenda(Long id, VendaRequestDTO dto, String usuario) {
        log.info("Atualizando venda: id={}, usuario={}", id, usuario);

        Venda venda = vendaRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Tentativa de atualizar venda inexistente: id={}", id);
                    return new RecursoNaoEncontradoException("Venda #" + id + " não encontrada!");
                });

        if (venda.getStatus() == StatusVenda.CANCELADA) {
            log.warn("Atualização bloqueada: venda id={} está cancelada", id);
            throw new OperacaoNaoPermitidaException("Vendas CANCELADAS não podem ser alteradas!");
        }
        if (venda.getStatus() == StatusVenda.FECHADA) {
            log.warn("Atualização bloqueada: venda id={} está fechada", id);
            throw new OperacaoNaoPermitidaException("Apenas vendas PENDENTES podem ser alteradas!");
        }

        venda.getItens().clear();

        List<ItemVendaResponseDTO> itensResponse = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (ItemVendaRequestDTO itemDTO : dto.getItens()) {
            Produto produto = (dto.getStatus() == StatusVenda.FECHADA
                    ? produtoRepository.buscarParaAtualizacaoEstoque(itemDTO.getCodigoProduto())
                    : produtoRepository.findById(itemDTO.getCodigoProduto()))
                    .orElseThrow(() -> {
                        log.warn("Atualização bloqueada: produto '{}' não encontrado", itemDTO.getCodigoProduto());
                        return new RecursoNaoEncontradoException(
                                "Produto \"" + itemDTO.getCodigoProduto() + "\" não encontrado!");
                    });

            if (dto.getStatus() == StatusVenda.FECHADA) {
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
            }

            ItemVenda novoItem = new ItemVenda();
            novoItem.setVenda(venda);
            novoItem.setProduto(produto);
            novoItem.setQuantidade(itemDTO.getQuantidade());
            novoItem.setPrecoUnit(produto.getPreco());
            venda.getItens().add(novoItem);

            total = total.add(produto.getPreco().multiply(BigDecimal.valueOf(itemDTO.getQuantidade())));
            itensResponse.add(new ItemVendaResponseDTO(
                    produto.getCodigo(), produto.getNome(),
                    itemDTO.getQuantidade(), produto.getPreco()
            ));
        }

        venda.getPagamentos().clear();
        aplicarPagamentos(venda, dto.getPagamentos(), total);

        venda.setTotal(total);
        venda.setStatus(dto.getStatus());
        venda.setUsuario(usuario);

        Venda vendaSalva = vendaRepository.save(venda);
        log.info("Venda atualizada: id={}, total=R${}, status={}, usuario={}",
                vendaSalva.getId(), vendaSalva.getTotal(), vendaSalva.getStatus(), vendaSalva.getUsuario());

        return toDTOComItens(vendaSalva, itensResponse);
    }

    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class,
            maxAttempts = 4,
            backoff = @Backoff(delay = 50, multiplier = 2, random = true)
    )
    @Transactional
    @CacheEvict(value = "produtos", allEntries = true)
    public void cancelarVenda(Long id) {
        log.info("Cancelando venda: id={}", id);

        Venda venda = vendaRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Tentativa de cancelar venda inexistente: id={}", id);
                    return new RecursoNaoEncontradoException("Venda #" + id + " não encontrada!");
                });

        if (venda.getStatus() == StatusVenda.CANCELADA) {
            throw new OperacaoNaoPermitidaException("Venda #" + id + " já está cancelada!");
        }

        if (venda.getStatus() == StatusVenda.FECHADA) {
            for (ItemVenda item : venda.getItens()) {
                // Re-busca com lock para garantir leitura do estoque atual antes de restaurar.
                Produto p = produtoRepository.buscarParaAtualizacaoEstoque(item.getProduto().getCodigo())
                        .orElseThrow(() -> new RecursoNaoEncontradoException(
                                "Produto \"" + item.getProduto().getCodigo() + "\" não encontrado ao cancelar venda"));
                p.setEstoque(p.getEstoque() + item.getQuantidade());
                produtoRepository.save(p);
                log.info("Estoque restaurado: produto='{}', novo estoque={}", p.getNome(), p.getEstoque());
            }
        }

        venda.setStatus(StatusVenda.CANCELADA);
        vendaRepository.save(venda);
        log.info("Venda cancelada: id={}", id);
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

        vendaRepository.delete(venda);
        log.info("Venda PENDENTE excluída: id={}", id);
    }


    private void aplicarPagamentos(Venda venda, List<PagamentoVendaRequestDTO> pagsReq, BigDecimal total) {
        // Valores monetários: comparar sempre com escala 2 (HALF_UP) para evitar
        // que jitter de ponto flutuante vindo do cliente (ex.: 189.89000000000001)
        // dispare falso negativo contra o total calculado em BigDecimal no backend.
        BigDecimal totalNormalizado = total.setScale(2, RoundingMode.HALF_UP);
        BigDecimal soma = pagsReq.stream()
                .map(PagamentoVendaRequestDTO::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        if (soma.compareTo(totalNormalizado) != 0) {
            throw new OperacaoNaoPermitidaException(
                    "Soma das formas de pagamento (R$ " + soma +
                            ") difere do total da venda (R$ " + totalNormalizado + ")");
        }

        for (PagamentoVendaRequestDTO p : pagsReq) {
            PagamentoVenda pv = new PagamentoVenda();
            pv.setVenda(venda);
            pv.setFormaPagamento(p.getFormaPagamento());
            pv.setCondicaoPagamento(p.getCondicaoPagamento());
            pv.setValor(p.getValor().setScale(2, RoundingMode.HALF_UP));
            venda.getPagamentos().add(pv);
        }

        if (pagsReq.size() == 1) {
            venda.setFormaPagamento(pagsReq.get(0).getFormaPagamento());
            venda.setCondicaoPagamento(pagsReq.get(0).getCondicaoPagamento());
        } else {
            venda.setFormaPagamento("MISTO");
            venda.setCondicaoPagamento(null);
        }
    }

    private VendaResponseDTO toDTOComItens(Venda v, List<ItemVendaResponseDTO> itens) {
        return new VendaResponseDTO(
                v.getId(), v.getData(), v.getTotal(),
                v.getFormaPagamento(), v.getCondicaoPagamento(),
                v.getStatus(), v.getUsuario(), itens, mapPagamentos(v));
    }

    private VendaResponseDTO toDTO(Venda v) {
        List<ItemVendaResponseDTO> itens = v.getItens().stream()
                .map(i -> new ItemVendaResponseDTO(
                        i.getProduto().getCodigo(), i.getProduto().getNome(),
                        i.getQuantidade(), i.getPrecoUnit()
                )).toList();
        return new VendaResponseDTO(
                v.getId(), v.getData(), v.getTotal(),
                v.getFormaPagamento(), v.getCondicaoPagamento(),
                v.getStatus(), v.getUsuario(), itens, mapPagamentos(v));
    }

    private List<PagamentoVendaResponseDTO> mapPagamentos(Venda v) {
        if (v.getPagamentos() == null || v.getPagamentos().isEmpty()) {
            return List.of();
        }
        return v.getPagamentos().stream()
                .map(p -> new PagamentoVendaResponseDTO(
                        p.getFormaPagamento(), p.getCondicaoPagamento(), p.getValor()))
                .toList();
    }

    public Page<VendaResponseDTO> buscarPorFiltros(Long id, LocalDate data, Pageable pageable) {
        if (id != null) {
            return vendaRepository.findById(id)
                    .map(v -> (Page<VendaResponseDTO>) new PageImpl<>(List.of(toDTO(v)), pageable, 1))
                    .orElse(Page.empty(pageable));
        }
        if (data != null) {

            LocalDateTime inicio = data.atStartOfDay();
            LocalDateTime fim    = data.plusDays(1).atStartOfDay();
            return vendaRepository.buscarNoIntervalo(inicio, fim, pageable).map(this::toDTO);
        }
        return listarVendasPaginado(pageable);
    }

    public Page<VendaResponseDTO> listarVendasPaginado(Pageable pageable) {
        return vendaRepository.findAll(pageable).map(this::toDTO);
    }

}
