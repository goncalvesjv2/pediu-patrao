package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.PedidoRequestDTO;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.service.PedidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    @Autowired
    private PedidoService pedidoService;

    @PostMapping
    public Pedido criarPedido(@RequestBody PedidoRequestDTO dto, Authentication authentication) {
        return pedidoService.criarPedido(dto, authentication);
    }

    @GetMapping
    public List<Pedido> listarPedidos() {
        return pedidoService.listarPedidos();
    }

    @PutMapping("/{id}/status")
    public Pedido atualizarStatus(@PathVariable String id, @RequestParam StatusPedido status, Authentication authentication) {
        return pedidoService.atualizarStatus(id, status, authentication);
    }

    @PutMapping("/{id}/desconto")
    public Pedido aplicarDesconto(
            @PathVariable String id,
            @RequestParam double percentual,
            Authentication authentication) {

        return pedidoService.aplicarDesconto(id, percentual, authentication);
    }

    @PutMapping("/{id}/cancelamento")
    public Pedido cancelarPedido(
            @PathVariable String id,
            @RequestParam String justificativa,
            Authentication authentication) {

        return pedidoService.cancelarPedido(id, justificativa, authentication);
    }

}