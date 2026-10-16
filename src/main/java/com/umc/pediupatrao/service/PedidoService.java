package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.PedidoRequestDTO;
import com.umc.pediupatrao.dto.ItemPedidoDTO;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class PedidoService {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    public Pedido criarPedido(PedidoRequestDTO dto, Authentication authentication) {
        List<ItemPedido> itens = new ArrayList<>();
        double total = 0;

        for (ItemPedidoDTO itemDto : dto.getItens()) {
            Produto produto = produtoRepository.findById(itemDto.getProdutoId()).orElseThrow(() -> new RuntimeException("Produto não encontrado: " + itemDto.getProdutoId()));

            if (itemDto.getQuantidade() <= 0) {
                throw new IllegalArgumentException("A quantidade do produto deve ser maior que zero");
            }

            if (!Boolean.TRUE.equals(produto.getAtivo())) {
                throw new RuntimeException("Produto inativo: " + produto.getNome());
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
        pedido.setStatus("RECEBIDO");

        return pedidoRepository.save(pedido);
    }

    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll();
    }

    public Pedido atualizarStatus(String id, String status) {
        Pedido pedido = pedidoRepository.findById(id).orElseThrow();
        pedido.setStatus(status);
        return pedidoRepository.save(pedido);
    }

}
