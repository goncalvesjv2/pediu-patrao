package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.PedidoRequestDTO;
import com.umc.pediupatrao.dto.ItemPedidoDTO;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PedidoService {
    @Autowired
    private PedidoRepository pedidoRepository;
    @Autowired
    private ProdutoRepository produtoRepository;
    @Autowired
    private ClienteRepository clienteRepository;

    private static final Map<StatusPedido, Set<StatusPedido>> TRANSICOES_VALIDAS = Map.of(
            StatusPedido.RECEBIDO, Set.of(StatusPedido.EM_PREPARO),
            StatusPedido.EM_PREPARO, Set.of(StatusPedido.PRONTO),
            StatusPedido.PRONTO, Set.of(StatusPedido.SAIU_PARA_ENTREGA),
            StatusPedido.SAIU_PARA_ENTREGA, Set.of(StatusPedido.FINALIZADO),
            StatusPedido.FINALIZADO, Set.of(),
            StatusPedido.CANCELADO, Set.of()
    );


    public Pedido criarPedido(PedidoRequestDTO dto, Authentication authentication) {
        if (dto == null) {
            throw new IllegalArgumentException("Os dados do pedido são obrigatórios");
        }

        if (dto.getClienteId() == null || dto.getClienteId().isBlank()) {
            throw new IllegalArgumentException("O cliente é obrigatório");
        }

        clienteRepository.findById(dto.getClienteId()).orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado"));

        if (dto.getItens() == null || dto.getItens().isEmpty()) {
            throw new IllegalArgumentException("O pedido deve possuir pelo menos um item");
        }

        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("Não foi possível identificar o responsável pelo pedido");
        }

        List<ItemPedido> itens = new ArrayList<>();
        double total = 0;

        for (ItemPedidoDTO itemDto : dto.getItens()) {
            if (itemDto == null) {
                throw new IllegalArgumentException("O item do pedido é obrigatório");
            }

            if (itemDto.getProdutoId() == null || itemDto.getProdutoId().isBlank()) {
                throw new IllegalArgumentException("O produto é obrigatório");
            }

            if (itemDto.getQuantidade() <= 0) {
                throw new IllegalArgumentException("A quantidade do produto deve ser maior que zero");
            }

            Produto produto = produtoRepository.findById(itemDto.getProdutoId())
                    .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado: " + itemDto.getProdutoId()));

            if (!Boolean.TRUE.equals(produto.getAtivo())) {
                throw new IllegalArgumentException("Produto inativo: " + produto.getNome());
            }

            ItemPedido item = new ItemPedido(
                    produto.getId(),
                    produto.getNome(),
                    itemDto.getQuantidade(),
                    produto.getPreco()
            );

            itens.add(item);
            total += item.getSubtotal();
        }

        Pedido pedido = new Pedido();
        pedido.setClienteId(dto.getClienteId());
        pedido.setItens(itens);
        pedido.setValorTotal(total);
        pedido.setResponsavelId(authentication.getName());
        pedido.setHorarioEntrada(LocalDateTime.now());
        pedido.setStatus(StatusPedido.RECEBIDO);

        return pedidoRepository.save(pedido);
    }

    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll();
    }

    public Pedido atualizarStatus(String id, StatusPedido novoStatus, Authentication authentication) {
        if (novoStatus == null) {
            throw new IllegalArgumentException("O novo status é obrigatório");
        }

        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado: " + id));

        StatusPedido statusAtual = pedido.getStatus();
        Set<StatusPedido> destinosPermitidos = TRANSICOES_VALIDAS.get(statusAtual);

        if (destinosPermitidos == null || !destinosPermitidos.contains(novoStatus)) {
            throw new IllegalArgumentException("Transição inválida: não é possível ir de " + statusAtual + " para " + novoStatus);
        }

        pedido.setStatus(novoStatus);

        if (novoStatus == StatusPedido.SAIU_PARA_ENTREGA) {
            if (authentication == null || authentication.getName() == null) {
                throw new IllegalArgumentException("Não foi possível identificar o responsável pela saída");
            }
            pedido.setResponsavelSaidaId(authentication.getName());
            pedido.setHorarioSaida(LocalDateTime.now());
        }

        return pedidoRepository.save(pedido);
    }
}