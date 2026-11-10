package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.DashboardDTO;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DashboardService {
    private final PedidoRepository pedidoRepository;

    public DashboardService(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    public DashboardDTO obterResumo() {
        return obterResumo(null, null);
    }

    public DashboardDTO obterResumo(LocalDate inicio, LocalDate fim) {
        if ((inicio == null) != (fim == null)) {
            throw new IllegalArgumentException(
                    "Informe a data inicial e a data final."
            );
        }

        if (inicio != null && inicio.isAfter(fim)) {
            throw new IllegalArgumentException("A data inicial não pode ser posterior à data final.");
        }

        LocalDateTime inicioPeriodo = null;
        LocalDateTime fimExclusivo = null;

        if (inicio != null) {
            inicioPeriodo = inicio.atStartOfDay();
            fimExclusivo = fim.plusDays(1).atStartOfDay();
        }

        List<Pedido> pedidos = pedidoRepository.findAll();

        long totalPedidos = 0;
        long pedidosFinalizados = 0;
        long pedidosCancelados = 0;
        double faturamento = 0.0;

        for (Pedido pedido : pedidos) {
            if (inicioPeriodo != null) {
                LocalDateTime dataPedido = pedido.getHorarioEntrada();

                if (dataPedido == null || dataPedido.isBefore(inicioPeriodo) || !dataPedido.isBefore(fimExclusivo)) {
                    continue;
                }
            }

            totalPedidos++;

            if (pedido.getStatus() == StatusPedido.FINALIZADO) {
                pedidosFinalizados++;
                faturamento += pedido.getValorTotal();
            }

            if (pedido.getStatus() == StatusPedido.CANCELADO) {
                pedidosCancelados++;
            }
        }

        double ticketMedio = pedidosFinalizados == 0 ? 0.0 : faturamento / pedidosFinalizados;

        return new DashboardDTO(
                totalPedidos,
                pedidosFinalizados,
                pedidosCancelados,
                arredondar(faturamento),
                arredondar(ticketMedio)
        );
    }

    private double arredondar(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}