package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.DashboardDTO;
import com.umc.pediupatrao.dto.DashboardStatusDTO;
import com.umc.pediupatrao.dto.DashboardVendasDTO;
import com.umc.pediupatrao.dto.DashboardProdutosDTO;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import com.umc.pediupatrao.entity.ItemPedido;
import java.util.Comparator;

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
        // Dashboard de vendas
        public List<DashboardVendasDTO> obterVendasPorDia (LocalDate inicio, LocalDate fim){
            if ((inicio == null) != (fim == null)) {
                throw new IllegalArgumentException("Informe a data inicial e a data final.");
            }

            if (inicio == null) {
                fim = LocalDate.now();
                inicio = fim.minusDays(6);
            }

            if (inicio.isAfter(fim)) {
                throw new IllegalArgumentException("A data inicial não pode ser posterior à data final.");
            }

            Map<LocalDate, Long> pedidosPorDia = new HashMap<>();
            Map<LocalDate, BigDecimal> faturamentoPorDia = new HashMap<>();
            List<Pedido> pedidos = pedidoRepository.findAll();

            for (Pedido pedido : pedidos) {
                if (pedido.getStatus() != StatusPedido.FINALIZADO) {
                    continue;
                }

                if (pedido.getHorarioEntrada() == null) {
                    continue;
                }

                LocalDate dataPedido = pedido.getHorarioEntrada().toLocalDate();

                if (dataPedido.isBefore(inicio) ||
                        dataPedido.isAfter(fim)) {
                    continue;
                }

                pedidosPorDia.merge(
                        dataPedido,
                        1L,
                        Long::sum
                );

                faturamentoPorDia.merge(
                        dataPedido,
                        BigDecimal.valueOf(pedido.getValorTotal()),
                        BigDecimal::add
                );
            }

            List<DashboardVendasDTO> resultado = new ArrayList<>();

            for (LocalDate data = inicio;
                 !data.isAfter(fim);
                 data = data.plusDays(1)) {

                long quantidade = pedidosPorDia.getOrDefault(data, 0L);

                BigDecimal faturamento = faturamentoPorDia.getOrDefault(data, BigDecimal.ZERO);

                double valorArredondado = faturamento.setScale(2, RoundingMode.HALF_UP).doubleValue();

                resultado.add(new DashboardVendasDTO(data, quantidade, valorArredondado));
            }

            return resultado;
        }

        // Situação dos pedidos
        public List<DashboardStatusDTO> obterSituacaoPedidos(LocalDate inicio, LocalDate fim) {
            if ((inicio == null) != (fim == null)) {
                throw new IllegalArgumentException(
                        "Informe a data inicial e a data final."
                );
            }

            if (inicio != null && inicio.isAfter(fim)) {
                throw new IllegalArgumentException(
                        "A data inicial não pode ser posterior à data final."
                );
            }

            List<Pedido> pedidos = pedidoRepository.findAll();

            Map<StatusPedido, Long> quantidadePorStatus = new EnumMap<>(StatusPedido.class);

            for (StatusPedido status : StatusPedido.values()) {
                quantidadePorStatus.put(status, 0L);
            }

            for (Pedido pedido : pedidos) {
                if (inicio != null) {
                    LocalDateTime dataPedido = pedido.getHorarioEntrada();

                    if (dataPedido == null || dataPedido.toLocalDate().isBefore(inicio) || dataPedido.toLocalDate().isAfter(fim)) {
                        continue;
                    }
                }

                StatusPedido status = pedido.getStatus();

                if (status == null) {
                    continue;
                }

                quantidadePorStatus.merge(status, 1L, Long::sum);
            }

            List<DashboardStatusDTO> resultado = new ArrayList<>();

            for (StatusPedido status : StatusPedido.values()) {
                long quantidade = quantidadePorStatus.get(status);
                resultado.add(new DashboardStatusDTO(status, quantidade));
            }

            return resultado;
        }

        // Produtos mais vendidos
        public List<DashboardProdutosDTO> obterProdutosMaisVendidos(LocalDate inicio, LocalDate fim) {
            if ((inicio == null) != (fim == null)) {
                throw new IllegalArgumentException(
                        "Informe a data inicial e a data final."
                );
            }

            if (inicio != null && inicio.isAfter(fim)) {
                throw new IllegalArgumentException(
                        "A data inicial não pode ser posterior à data final."
                );
            }

            List<Pedido> pedidos = pedidoRepository.findAll();
            Map<String, Long> quantidadePorProduto = new HashMap<>();
            Map<String, String> nomePorProduto = new HashMap<>();

            for (Pedido pedido : pedidos) {
                if (pedido.getStatus() != StatusPedido.FINALIZADO) {
                    continue;
                }

                if (inicio != null) {
                    LocalDateTime dataPedido = pedido.getHorarioEntrada();

                    if (dataPedido == null ||
                            dataPedido.toLocalDate().isBefore(inicio) ||
                            dataPedido.toLocalDate().isAfter(fim)) {
                        continue;
                    }
                }

                if (pedido.getItens() == null) {
                    continue;
                }

                for (ItemPedido item : pedido.getItens()) {

                    if (item == null || item.getProdutoId() == null || item.getProdutoId().isBlank() || item.getQuantidade() <= 0) {
                        continue;
                    }

                    String produtoId = item.getProdutoId();
                    String nomeProduto = item.getNomeProduto();

                    if (nomeProduto == null || nomeProduto.isBlank()) {
                        nomeProduto = "Produto sem nome";
                    }

                    quantidadePorProduto.merge(
                            produtoId,
                            (long) item.getQuantidade(),
                            Long::sum
                    );

                    nomePorProduto.putIfAbsent(
                            produtoId,
                            nomeProduto
                    );
                }
            }

            return quantidadePorProduto.entrySet()
                    .stream()
                    .map(entrada -> new DashboardProdutosDTO(entrada.getKey(), nomePorProduto.get(entrada.getKey()), entrada.getValue()))
                    .sorted(Comparator.comparingLong(DashboardProdutosDTO::quantidadeVendida).reversed().thenComparing(DashboardProdutosDTO::produtoId))
                    .limit(5)
                    .toList();
        }

    private double arredondar(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}