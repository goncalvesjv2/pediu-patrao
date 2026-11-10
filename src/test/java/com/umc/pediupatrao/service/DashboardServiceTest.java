package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.DashboardDTO;
import com.umc.pediupatrao.dto.DashboardProdutosDTO;
import com.umc.pediupatrao.dto.DashboardStatusDTO;
import com.umc.pediupatrao.dto.DashboardVendasDTO;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.repository.PedidoRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {
    @Mock
    private PedidoRepository pedidoRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private static final LocalDate INICIO = LocalDate.of(2026, 11, 1);

    private static final LocalDate FIM = LocalDate.of(2026, 11, 30);

    private Pedido criarPedido(String id, StatusPedido status, double valorTotal, LocalDateTime horarioEntrada) {
        Pedido pedido = new Pedido();
        pedido.setId(id);
        pedido.setStatus(status);
        pedido.setValorTotal(valorTotal);
        pedido.setHorarioEntrada(horarioEntrada);
        return pedido;
    }

    private LocalDateTime data(int dia, int hora, int minuto) {
        return LocalDateTime.of(2026, 11, dia, hora, minuto);
    }

    @Test
    void deveRetornarIndicadoresZeradosQuandoNaoExistiremPedidos() {
        when(pedidoRepository.findAll()).thenReturn(List.of());
        DashboardDTO resultado = dashboardService.obterResumo();
        assertEquals(0, resultado.totalPedidos());
        assertEquals(0, resultado.pedidosFinalizados());
        assertEquals(0, resultado.pedidosCancelados());
        assertEquals(0.0, resultado.faturamento());
        assertEquals(0.0, resultado.ticketMedio());
        verify(pedidoRepository).findAll();
    }

    @Test
    void deveContarTodosOsPedidos() {
        Pedido pedido1 = criarPedido("1", StatusPedido.RECEBIDO, 50.0, data(10, 10, 0));
        Pedido pedido2 = criarPedido("2", StatusPedido.FINALIZADO, 100.0, data(11, 10, 0));
        Pedido pedido3 = criarPedido("3", StatusPedido.CANCELADO, 80.0, data(12, 10, 0));
        when(pedidoRepository.findAll()).thenReturn(List.of(pedido1, pedido2, pedido3));
        DashboardDTO resultado = dashboardService.obterResumo();
        assertEquals(3, resultado.totalPedidos());
        verify(pedidoRepository).findAll();
    }

    @Test
    void deveContarPedidosFinalizados() {
        Pedido pedido1 = criarPedido("1", StatusPedido.FINALIZADO, 100.0, data(10, 10, 0));
        Pedido pedido2 = criarPedido("2", StatusPedido.FINALIZADO, 60.0, data(11, 10, 0));
        Pedido pedido3 = criarPedido("3", StatusPedido.RECEBIDO, 40.0, data(12, 10, 0));
        when(pedidoRepository.findAll()).thenReturn(List.of(pedido1, pedido2, pedido3));
        DashboardDTO resultado = dashboardService.obterResumo();
        assertEquals(3, resultado.totalPedidos());
        assertEquals(2, resultado.pedidosFinalizados());
        assertEquals(160.0, resultado.faturamento());
        verify(pedidoRepository).findAll();
    }

    @Test
    void deveContarPedidosCancelados() {
        Pedido pedido1 = criarPedido("1", StatusPedido.CANCELADO, 100.0, data(10, 10, 0));
        Pedido pedido2 = criarPedido("2", StatusPedido.CANCELADO, 50.0, data(11, 10, 0));
        Pedido pedido3 = criarPedido("3", StatusPedido.FINALIZADO, 80.0, data(12, 10, 0));
        when(pedidoRepository.findAll()).thenReturn(List.of(pedido1, pedido2, pedido3));
        DashboardDTO resultado = dashboardService.obterResumo();
        assertEquals(2, resultado.pedidosCancelados());
        assertEquals(1, resultado.pedidosFinalizados());
        assertEquals(80.0, resultado.faturamento());
        verify(pedidoRepository).findAll();
    }

    @ParameterizedTest
    @EnumSource(
            value = StatusPedido.class,
            names = {
                    "RECEBIDO",
                    "EM_PREPARO",
                    "PRONTO",
                    "SAIU_PARA_ENTREGA"
            }
    )
    void deveIgnorarPedidosNaoFinalizadosNoFaturamento(StatusPedido status) {
        Pedido pedido = criarPedido("1", status, 100.0, data(10, 10, 0));
        when(pedidoRepository.findAll()).thenReturn(List.of(pedido));
        DashboardDTO resultado = dashboardService.obterResumo();
        assertEquals(1, resultado.totalPedidos());
        assertEquals(0, resultado.pedidosFinalizados());
        assertEquals(0.0, resultado.faturamento());
        assertEquals(0.0, resultado.ticketMedio());
        verify(pedidoRepository).findAll();
    }

    @Test
    void deveCalcularFaturamentoCorretamente() {
        Pedido pedido1 = criarPedido("1", StatusPedido.FINALIZADO, 39.90, data(10, 10, 0));
        Pedido pedido2 = criarPedido("2", StatusPedido.FINALIZADO, 60.10, data(11, 10, 0));
        Pedido pedido3 = criarPedido("3", StatusPedido.CANCELADO, 200.0, data(12, 10, 0));
        when(pedidoRepository.findAll()).thenReturn(List.of(pedido1, pedido2, pedido3));
        DashboardDTO resultado = dashboardService.obterResumo();
        assertEquals(100.0, resultado.faturamento(), 0.001);
        assertEquals(2, resultado.pedidosFinalizados());
        verify(pedidoRepository).findAll();
    }

    @Test
    void deveConsiderarValorTotalComDesconto() {
        Pedido pedido = criarPedido("1", StatusPedido.FINALIZADO, 90.0, data(10, 10, 0));
        pedido.setPercentualDesconto(10.0);
        pedido.setValorDesconto(10.0);
        when(pedidoRepository.findAll()).thenReturn(List.of(pedido));
        DashboardDTO resultado = dashboardService.obterResumo();
        assertEquals(90.0, resultado.faturamento());
        assertEquals(90.0, resultado.ticketMedio());
        verify(pedidoRepository).findAll();
    }

    @Test
    void deveCalcularTicketMedioCorretamente() {
        Pedido pedido1 = criarPedido("1", StatusPedido.FINALIZADO, 100.0, data(10, 10, 0));
        Pedido pedido2 = criarPedido("2", StatusPedido.FINALIZADO, 50.0, data(11, 10, 0));
        when(pedidoRepository.findAll()).thenReturn(List.of(pedido1, pedido2));
        DashboardDTO resultado = dashboardService.obterResumo();
        assertEquals(150.0, resultado.faturamento());
        assertEquals(75.0, resultado.ticketMedio());
        verify(pedidoRepository).findAll();
    }

    @Test
    void deveRetornarTicketMedioZeroQuandoNaoExistiremFinalizados() {
        Pedido pedido = criarPedido("1", StatusPedido.RECEBIDO, 100.0, data(10, 10, 0));
        when(pedidoRepository.findAll()).thenReturn(List.of(pedido));
        DashboardDTO resultado = dashboardService.obterResumo();
        assertEquals(0, resultado.pedidosFinalizados());
        assertEquals(0.0, resultado.ticketMedio());
        verify(pedidoRepository).findAll();
    }

    @Test
    void deveArredondarValoresParaDuasCasasDecimais() {
        Pedido pedido = criarPedido("1", StatusPedido.FINALIZADO, 10.005, data(10, 10, 0));

        when(pedidoRepository.findAll()).thenReturn(List.of(pedido));
        DashboardDTO resultado = dashboardService.obterResumo();
        assertEquals(10.01, resultado.faturamento(), 0.001);
        assertEquals(10.01, resultado.ticketMedio(), 0.001);
        verify(pedidoRepository).findAll();
    }

    @Test
    void deveConsultarTodosOsPedidosQuandoDatasForemNulas() {
        Pedido pedido1 = criarPedido("1", StatusPedido.FINALIZADO, 100.0, data(10, 10, 0));
        Pedido pedido2 = criarPedido("2", StatusPedido.FINALIZADO, 50.0, LocalDateTime.of(2025, 1, 10, 10, 0));

        when(pedidoRepository.findAll()).thenReturn(List.of(pedido1, pedido2));
        DashboardDTO resultado = dashboardService.obterResumo(null, null);
        assertEquals(2, resultado.totalPedidos());
        assertEquals(150.0, resultado.faturamento());
        verify(pedidoRepository).findAll();
    }

    @Test
    void deveConsiderarSomentePedidosDentroDoPeriodo() {
        Pedido dentroPeriodo = criarPedido("1", StatusPedido.FINALIZADO, 100.0, data(10, 10, 0));
        Pedido foraPeriodo = criarPedido("2", StatusPedido.FINALIZADO, 200.0, LocalDateTime.of(2026, 10, 10, 10, 0));

        when(pedidoRepository.findAll()).thenReturn(List.of(dentroPeriodo, foraPeriodo));
        DashboardDTO resultado = dashboardService.obterResumo(INICIO, FIM);
        assertEquals(1, resultado.totalPedidos());
        assertEquals(1, resultado.pedidosFinalizados());
        assertEquals(100.0, resultado.faturamento());
        verify(pedidoRepository).findAll();
    }

    @Test
    void deveIgnorarPedidoAnteriorAoPeriodo() {
        Pedido pedido = criarPedido("1", StatusPedido.FINALIZADO, 100.0, LocalDateTime.of(2026, 10, 31, 23, 59));
        when(pedidoRepository.findAll()).thenReturn(List.of(pedido));
        DashboardDTO resultado = dashboardService.obterResumo(INICIO, FIM);
        assertEquals(0, resultado.totalPedidos());
        assertEquals(0.0, resultado.faturamento());
        verify(pedidoRepository).findAll();
    }

    @Test
    void deveIgnorarPedidoPosteriorAoPeriodo() {
        Pedido pedido = criarPedido("1", StatusPedido.FINALIZADO, 100.0, LocalDateTime.of(2026, 12, 5, 10, 0));
        when(pedidoRepository.findAll()).thenReturn(List.of(pedido));
        DashboardDTO resultado = dashboardService.obterResumo(INICIO, FIM);
        assertEquals(0, resultado.totalPedidos());
        assertEquals(0, resultado.pedidosFinalizados());
        verify(pedidoRepository).findAll();
    }

    @Test
    void deveIncluirPedidoNoPrimeiroDiaDoPeriodo() {
        Pedido pedido = criarPedido("1", StatusPedido.FINALIZADO, 100.0, LocalDateTime.of(2026, 11, 1, 0, 0));
        when(pedidoRepository.findAll()).thenReturn(List.of(pedido));
        DashboardDTO resultado = dashboardService.obterResumo(INICIO, FIM);
        assertEquals(1, resultado.totalPedidos());
        assertEquals(100.0, resultado.faturamento());
        verify(pedidoRepository).findAll();
    }

    @Test
    void deveIncluirPedidoNoUltimoInstanteDoPeriodo() {
        Pedido pedido = criarPedido("1", StatusPedido.FINALIZADO, 100.0, LocalDateTime.of(2026, 11, 30, 23, 59, 59, 999999999));
        when(pedidoRepository.findAll()).thenReturn(List.of(pedido));
        DashboardDTO resultado = dashboardService.obterResumo(INICIO, FIM);
        assertEquals(1, resultado.totalPedidos());
        assertEquals(100.0, resultado.faturamento());
        verify(pedidoRepository).findAll();
    }

    @Test
    void deveIgnorarPedidoNoInicioDoDiaSeguinte() {
        Pedido pedido = criarPedido("1", StatusPedido.FINALIZADO, 100.0, LocalDateTime.of(2026, 12, 1, 0, 0));

        when(pedidoRepository.findAll()).thenReturn(List.of(pedido));
        DashboardDTO resultado = dashboardService.obterResumo(INICIO, FIM);

        assertEquals(0, resultado.totalPedidos());
        assertEquals(0.0, resultado.faturamento());
        verify(pedidoRepository).findAll();
    }

    @Test
    void deveIgnorarPedidoSemDataQuandoExisteFiltro() {
        Pedido pedido = criarPedido("1", StatusPedido.FINALIZADO, 100.0, null);
        when(pedidoRepository.findAll()).thenReturn(List.of(pedido));

        DashboardDTO resultado = dashboardService.obterResumo(INICIO, FIM);

        assertEquals(0, resultado.totalPedidos());
        assertEquals(0, resultado.pedidosFinalizados());
        verify(pedidoRepository).findAll();
    }

    @Test
    void deveConsiderarPedidoSemDataQuandoNaoExisteFiltro() {
        Pedido pedido = criarPedido("1", StatusPedido.FINALIZADO, 100.0, null);
        when(pedidoRepository.findAll()).thenReturn(List.of(pedido));
        DashboardDTO resultado = dashboardService.obterResumo();

        assertEquals(1, resultado.totalPedidos());
        assertEquals(1, resultado.pedidosFinalizados());
        assertEquals(100.0, resultado.faturamento());

        verify(pedidoRepository).findAll();
    }

    @Test
    void deveRejeitarDataInicialPosteriorADataFinal() {
        LocalDate inicio = LocalDate.of(2026, 11, 30);
        LocalDate fim = LocalDate.of(2026, 11, 1);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> dashboardService.obterResumo(inicio, fim));

        assertEquals("A data inicial não pode ser posterior à data final.", erro.getMessage());
        verifyNoInteractions(pedidoRepository);
    }

    @Test
    void deveRejeitarConsultaComSomenteDataInicial() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> dashboardService.obterResumo(INICIO, null));
        assertEquals("Informe a data inicial e a data final.", erro.getMessage());
        verifyNoInteractions(pedidoRepository);
    }

    @Test
    void deveRejeitarConsultaComSomenteDataFinal() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> dashboardService.obterResumo(null, FIM));
        assertEquals("Informe a data inicial e a data final.", erro.getMessage());
        verifyNoInteractions(pedidoRepository);
    }

    @Test
    void deveContarPedidoComStatusNuloSemConsiderarVenda() {
        Pedido pedido = criarPedido("1", null, 100.0, data(10, 10, 0));

        when(pedidoRepository.findAll()).thenReturn(List.of(pedido));
        DashboardDTO resultado = dashboardService.obterResumo();

        assertEquals(1, resultado.totalPedidos());
        assertEquals(0, resultado.pedidosFinalizados());
        assertEquals(0, resultado.pedidosCancelados());
        assertEquals(0.0, resultado.faturamento());
        verify(pedidoRepository).findAll();
    }

    @Test
    void deveRepassarFalhaDoRepositorio() {
        RuntimeException falha = new RuntimeException("Falha ao consultar pedidos");
        when(pedidoRepository.findAll()).thenThrow(falha);
        RuntimeException erro = assertThrows(RuntimeException.class, () -> dashboardService.obterResumo());
        assertSame(falha, erro);
        verify(pedidoRepository).findAll();
    }

    @Nested
    class AnaliseVendasPorDia {
        private final LocalDate inicio = LocalDate.of(2026, 11, 1);
        private final LocalDate fim = LocalDate.of(2026, 11, 3);

        private Pedido criarPedidoVendas(StatusPedido status, double valor, LocalDateTime horarioEntrada) {
            Pedido pedido = new Pedido();
            pedido.setStatus(status);
            pedido.setValorTotal(valor);
            pedido.setHorarioEntrada(horarioEntrada);
            return pedido;
        }

        @Test
        void deveCalcularVendasPorDia() {
            Pedido pedido1 = criarPedidoVendas(StatusPedido.FINALIZADO, 50.0, inicio.atTime(10, 0));
            Pedido pedido2 = criarPedidoVendas(StatusPedido.FINALIZADO, 80.0, inicio.atTime(15, 0));
            Pedido pedido3 = criarPedidoVendas(StatusPedido.FINALIZADO, 100.0, inicio.plusDays(1).atTime(12, 0));

            when(pedidoRepository.findAll()).thenReturn(List.of(pedido1, pedido2, pedido3));
            List<DashboardVendasDTO> resultado = dashboardService.obterVendasPorDia(inicio, fim);

            assertEquals(3, resultado.size());
            assertEquals(inicio, resultado.get(0).data());
            assertEquals(2L, resultado.get(0).pedidosFinalizados());
            assertEquals(130.0, resultado.get(0).faturamento(), 0.001);
            assertEquals(inicio.plusDays(1), resultado.get(1).data());
            assertEquals(1L, resultado.get(1).pedidosFinalizados());
            assertEquals(100.0, resultado.get(1).faturamento(), 0.001);
            assertEquals(inicio.plusDays(2), resultado.get(2).data());
            assertEquals(0L, resultado.get(2).pedidosFinalizados());
            assertEquals(0.0, resultado.get(2).faturamento(), 0.001);
            verify(pedidoRepository).findAll();
        }

        @Test
        void deveIgnorarPedidosNaoFinalizados() {
            Pedido cancelado = criarPedidoVendas(StatusPedido.CANCELADO, 100.0, inicio.atTime(10, 0));
            Pedido recebido = criarPedidoVendas(StatusPedido.RECEBIDO, 50.0, inicio.atTime(11, 0));
            Pedido emPreparo = criarPedidoVendas(StatusPedido.EM_PREPARO, 80.0, inicio.atTime(12, 0));
            when(pedidoRepository.findAll()).thenReturn(List.of(cancelado, recebido, emPreparo));
            List<DashboardVendasDTO> resultado = dashboardService.obterVendasPorDia(inicio, inicio);
            assertEquals(1, resultado.size());
            assertEquals(0L, resultado.get(0).pedidosFinalizados());
            assertEquals(0.0, resultado.get(0).faturamento(), 0.001);
        }

        @Test
        void deveRetornarZeroQuandoNaoExistiremVendas() {
            when(pedidoRepository.findAll()).thenReturn(List.of());
            List<DashboardVendasDTO> resultado = dashboardService.obterVendasPorDia(inicio, fim);
            assertEquals(3, resultado.size());

            for (DashboardVendasDTO venda : resultado) {
                assertEquals(0L, venda.pedidosFinalizados());
                assertEquals(0.0, venda.faturamento(), 0.001);
            }
        }

        @Test
        void deveIgnorarVendasForaDoPeriodo() {
            Pedido anterior = criarPedidoVendas(StatusPedido.FINALIZADO, 100.0, inicio.minusDays(1).atTime(12, 0));
            Pedido dentro = criarPedidoVendas(StatusPedido.FINALIZADO, 75.0, inicio.atTime(12, 0));
            Pedido posterior = criarPedidoVendas(StatusPedido.FINALIZADO, 200.0, fim.plusDays(1).atTime(12, 0));
            when(pedidoRepository.findAll()).thenReturn(List.of(anterior, dentro, posterior));
            List<DashboardVendasDTO> resultado = dashboardService.obterVendasPorDia(inicio, fim);
            assertEquals(3, resultado.size());
            assertEquals(1L, resultado.get(0).pedidosFinalizados());
            assertEquals(75.0, resultado.get(0).faturamento(), 0.001);
            assertEquals(0.0, resultado.get(1).faturamento(), 0.001);
            assertEquals(0.0, resultado.get(2).faturamento(), 0.001);
        }

        @Test
        void deveIgnorarPedidoSemData() {
            Pedido semData = criarPedidoVendas(StatusPedido.FINALIZADO, 100.0, null);
            Pedido comData = criarPedidoVendas(StatusPedido.FINALIZADO, 60.0, inicio.atTime(10, 0));
            when(pedidoRepository.findAll()).thenReturn(List.of(semData, comData));
            List<DashboardVendasDTO> resultado = dashboardService.obterVendasPorDia(inicio, inicio);
            assertEquals(1L, resultado.get(0).pedidosFinalizados());
            assertEquals(60.0, resultado.get(0).faturamento(), 0.001);
        }

        @Test
        void deveConsiderarValorComDesconto() {
            Pedido pedido = criarPedidoVendas(StatusPedido.FINALIZADO, 90.0, inicio.atTime(10, 0));
            pedido.setPercentualDesconto(10.0);
            pedido.setValorDesconto(10.0);
            when(pedidoRepository.findAll()).thenReturn(List.of(pedido));
            List<DashboardVendasDTO> resultado = dashboardService.obterVendasPorDia(inicio, inicio);
            assertEquals(90.0, resultado.get(0).faturamento(), 0.001);
        }

        @Test
        void deveArredondarFaturamento() {
            Pedido pedido = criarPedidoVendas(StatusPedido.FINALIZADO, 10.005, inicio.atTime(10, 0));
            when(pedidoRepository.findAll()).thenReturn(List.of(pedido));
            List<DashboardVendasDTO> resultado = dashboardService.obterVendasPorDia(inicio, inicio);
            assertEquals(10.01, resultado.get(0).faturamento(), 0.001);
        }

        @Test
        void deveRejeitarPeriodoInvertido() {
            IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> dashboardService.obterVendasPorDia(fim, inicio));
            assertEquals("A data inicial não pode ser posterior à data final.",  erro.getMessage());
            verifyNoInteractions(pedidoRepository);
        }

        @Test
        void deveExigirAsDuasDatas() {
            assertThrows(IllegalArgumentException.class, () -> dashboardService.obterVendasPorDia(inicio, null));
            assertThrows(IllegalArgumentException.class, () -> dashboardService.obterVendasPorDia(null, fim));
            verifyNoInteractions(pedidoRepository);
        }

        @Test
        void deveConsiderarSeteDiasSemFiltros() {
            when(pedidoRepository.findAll()).thenReturn(List.of());
            List<DashboardVendasDTO> resultado = dashboardService.obterVendasPorDia(null, null);
            assertEquals(7, resultado.size());
            LocalDate primeiroDia = resultado.get(0).data();
            LocalDate ultimoDia = resultado.get(6).data();
            assertEquals(primeiroDia.plusDays(6), ultimoDia);
            verify(pedidoRepository).findAll();
        }

        @Test
        void deveRetornarErroQuandoRepositorioFalhar() {
            RuntimeException falha = new RuntimeException("Erro ao consultar pedidos");
            when(pedidoRepository.findAll()).thenThrow(falha);
            RuntimeException erro = assertThrows(RuntimeException.class, () -> dashboardService.obterVendasPorDia(inicio, fim));
            assertSame(falha, erro);
        }
    }

    @Nested
    class SituacaoPedidosTest {
        private Pedido criarPedido(StatusPedido status, LocalDateTime data) {
            Pedido pedido = new Pedido();
            pedido.setStatus(status);
            pedido.setHorarioEntrada(data);
            return pedido;
        }

        private long obterQuantidade(List<DashboardStatusDTO> resultado, StatusPedido status) {
            return resultado.stream()
                    .filter(dto -> dto.status() == status)
                    .findFirst()
                    .orElseThrow()
                    .quantidade();
        }

        @Test
        void deveRetornarTodosOsStatusZeradosQuandoNaoExistemPedidos() {
            when(pedidoRepository.findAll()).thenReturn(List.of());
            List<DashboardStatusDTO> resultado = dashboardService.obterSituacaoPedidos(null, null);
            assertEquals(StatusPedido.values().length, resultado.size());

            for (DashboardStatusDTO dto : resultado) {
                assertEquals(0L, dto.quantidade());
            }

            verify(pedidoRepository).findAll();
        }

        @Test
        void deveContarPedidosPorStatusCorretamente() {
            Pedido pedido1 = criarPedido(StatusPedido.RECEBIDO, LocalDateTime.of(2026, 11, 1, 10, 0));
            Pedido pedido2 = criarPedido(StatusPedido.RECEBIDO, LocalDateTime.of(2026, 11, 2, 11, 0));
            Pedido pedido3 = criarPedido(StatusPedido.FINALIZADO, LocalDateTime.of(2026, 11, 3, 12, 0));
            Pedido pedido4 = criarPedido(StatusPedido.CANCELADO, LocalDateTime.of(2026, 11, 4, 13, 0));

            when(pedidoRepository.findAll()).thenReturn(List.of(pedido1, pedido2, pedido3, pedido4));
            List<DashboardStatusDTO> resultado = dashboardService.obterSituacaoPedidos(null, null);

            assertEquals(2L, obterQuantidade(resultado, StatusPedido.RECEBIDO));
            assertEquals(1L, obterQuantidade(resultado, StatusPedido.FINALIZADO));
            assertEquals(1L, obterQuantidade(resultado, StatusPedido.CANCELADO));
            assertEquals(0L, obterQuantidade(resultado, StatusPedido.EM_PREPARO));
        }

        @Test
        void deveFiltrarPedidosPeloPeriodoInformado() {
            LocalDate inicio = LocalDate.of(2026, 11, 1);
            LocalDate fim = LocalDate.of(2026, 11, 10);

            Pedido pedido1 = criarPedido(StatusPedido.FINALIZADO, LocalDateTime.of(2026, 11, 1, 0, 0));
            Pedido pedido2 = criarPedido(StatusPedido.FINALIZADO, LocalDateTime.of(2026, 11, 10, 23, 59));
            Pedido pedido3 = criarPedido(StatusPedido.CANCELADO, LocalDateTime.of(2026, 10, 31, 12, 0));
            Pedido pedido4 = criarPedido(StatusPedido.RECEBIDO, LocalDateTime.of(2026, 11, 11, 12, 0));

            when(pedidoRepository.findAll()).thenReturn(List.of(pedido1, pedido2, pedido3, pedido4));
            List<DashboardStatusDTO> resultado = dashboardService.obterSituacaoPedidos(inicio, fim);

            assertEquals(2L, obterQuantidade(resultado, StatusPedido.FINALIZADO));
            assertEquals(0L, obterQuantidade(resultado, StatusPedido.CANCELADO));
            assertEquals(0L, obterQuantidade(resultado, StatusPedido.RECEBIDO));
        }

        @Test
        void deveIgnorarPedidoSemDataQuandoExisteFiltro() {
            Pedido pedido = criarPedido(StatusPedido.RECEBIDO, null);
            when(pedidoRepository.findAll()).thenReturn(List.of(pedido));

            List<DashboardStatusDTO> resultado = dashboardService.obterSituacaoPedidos(
                    LocalDate.of(2026, 11, 1),
                    LocalDate.of(2026, 11, 10)
            );

            assertEquals(0L, obterQuantidade(resultado, StatusPedido.RECEBIDO));
        }

        @Test
        void deveRejeitarQuandoApenasDataInicialFoiInformada() {
            IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> dashboardService.obterSituacaoPedidos(LocalDate.of(2026, 11, 1), null));
            assertEquals("Informe a data inicial e a data final.", erro.getMessage());
            verifyNoInteractions(pedidoRepository);
        }

        @Test
        void deveRejeitarQuandoApenasDataFinalFoiInformada() {
            IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> dashboardService.obterSituacaoPedidos(null, LocalDate.of(2026, 11, 10)));
            assertEquals("Informe a data inicial e a data final.", erro.getMessage());
            verifyNoInteractions(pedidoRepository);
        }

        @Test
        void deveRejeitarPeriodoComDatasInvertidas() {
            IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> dashboardService.obterSituacaoPedidos(
                    LocalDate.of(2026, 11, 10),
                    LocalDate.of(2026, 11, 1))
            );

            assertEquals("A data inicial não pode ser posterior à data final.", erro.getMessage());
            verifyNoInteractions(pedidoRepository);
        }

        @Test
        void deveIgnorarPedidosSemStatus() {
            Pedido pedido = criarPedido(null, LocalDateTime.of(2026, 11, 5, 10, 0));
            when(pedidoRepository.findAll()).thenReturn(List.of(pedido));
            List<DashboardStatusDTO> resultado = dashboardService.obterSituacaoPedidos(null, null);

            for (DashboardStatusDTO dto : resultado) {
                assertEquals(0L, dto.quantidade());
            }
        }
    }

    @Nested
    class ProdutosMaisVendidosTest {
        private ItemPedido criarItem(String id, String nome, int quantidade) {
            return new ItemPedido(id, nome, quantidade, 10.0);
        }

        private Pedido criarPedidoComItens(String id, StatusPedido status, LocalDateTime data, ItemPedido... itens) {
            Pedido pedido = new Pedido();
            pedido.setId(id);
            pedido.setStatus(status);
            pedido.setHorarioEntrada(data);
            pedido.setItens(List.of(itens));
            return pedido;
        }

        @Test
        void deveSomarQuantidadesDoMesmoProduto() {
            Pedido pedido1 = criarPedidoComItens("1", StatusPedido.FINALIZADO, data(1, 10, 0), criarItem("P1", "Pizza Calabresa", 2));
            Pedido pedido2 = criarPedidoComItens("2", StatusPedido.FINALIZADO, data(2, 10, 0), criarItem("P1", "Pizza Calabresa", 3));

            when(pedidoRepository.findAll()).thenReturn(List.of(pedido1, pedido2));
            List<DashboardProdutosDTO> resultado = dashboardService.obterProdutosMaisVendidos(null, null);

            assertEquals(1, resultado.size());
            assertEquals("P1", resultado.get(0).produtoId());
            assertEquals("Pizza Calabresa", resultado.get(0).nomeProduto());
            assertEquals(5L, resultado.get(0).quantidadeVendida());
        }

        @Test
        void deveIgnorarPedidosNaoFinalizados() {
            Pedido finalizado = criarPedidoComItens("1", StatusPedido.FINALIZADO, data(1, 10, 0), criarItem("P1", "Pizza Calabresa", 2));
            Pedido cancelado = criarPedidoComItens("2", StatusPedido.CANCELADO, data(2, 10, 0), criarItem("P1", "Pizza Calabresa", 5));

            when(pedidoRepository.findAll()).thenReturn(List.of(finalizado, cancelado));
            List<DashboardProdutosDTO> resultado = dashboardService.obterProdutosMaisVendidos(null, null);

            assertEquals(1, resultado.size());
            assertEquals(2L, resultado.get(0).quantidadeVendida());
        }

        @Test
        void deveRetornarOsCincoProdutosMaisVendidos() {
            Pedido pedido = criarPedidoComItens("1", StatusPedido.FINALIZADO, data(1, 10, 0),
                    criarItem("P1", "Produto 1", 1),
                    criarItem("P2", "Produto 2", 2),
                    criarItem("P3", "Produto 3", 3),
                    criarItem("P4", "Produto 4", 4),
                    criarItem("P5", "Produto 5", 5),
                    criarItem("P6", "Produto 6", 6)
            );

            when(pedidoRepository.findAll()).thenReturn(List.of(pedido));

            List<DashboardProdutosDTO> resultado = dashboardService.obterProdutosMaisVendidos(null, null);

            assertEquals(5, resultado.size());
            assertEquals("P6", resultado.get(0).produtoId());
            assertEquals(6L, resultado.get(0).quantidadeVendida());
            assertEquals("P5", resultado.get(1).produtoId());
            assertEquals(5L, resultado.get(1).quantidadeVendida());
            assertEquals("P2", resultado.get(4).produtoId());
            assertEquals(2L, resultado.get(4).quantidadeVendida());
        }

        @Test
        void deveFiltrarProdutosPorPeriodo() {
            LocalDate inicio = LocalDate.of(2026, 11, 1);
            LocalDate fim = LocalDate.of(2026, 11, 10);

            Pedido pedido1 = criarPedidoComItens("1", StatusPedido.FINALIZADO, data(1, 10, 0), criarItem("P1", "Pizza Calabresa", 2));
            Pedido pedido2 = criarPedidoComItens("2", StatusPedido.FINALIZADO, data(10, 10, 0), criarItem("P1", "Pizza Calabresa", 3));
            Pedido foraPeriodo = criarPedidoComItens("3", StatusPedido.FINALIZADO, LocalDateTime.of(2026, 10, 31, 10, 0), criarItem("P1", "Pizza Calabresa", 10));

            when(pedidoRepository.findAll()).thenReturn(List.of(pedido1, pedido2, foraPeriodo));
            List<DashboardProdutosDTO> resultado = dashboardService.obterProdutosMaisVendidos(inicio, fim);

            assertEquals(1, resultado.size());
            assertEquals(5L, resultado.get(0).quantidadeVendida());
        }

        @Test
        void deveRetornarListaVaziaQuandoNaoExistiremVendas() {
            when(pedidoRepository.findAll()).thenReturn(List.of());
            List<DashboardProdutosDTO> resultado = dashboardService.obterProdutosMaisVendidos(null, null);
            assertTrue(resultado.isEmpty());
            verify(pedidoRepository).findAll();
        }

        @Test
        void deveRejeitarPeriodosInvalidos() {
            LocalDate inicio = LocalDate.of(2026, 11, 1);
            LocalDate fim = LocalDate.of(2026, 11, 10);
            assertThrows(IllegalArgumentException.class, () -> dashboardService.obterProdutosMaisVendidos(inicio, null));
            assertThrows(IllegalArgumentException.class, () -> dashboardService.obterProdutosMaisVendidos(null, fim));
            assertThrows(IllegalArgumentException.class, () -> dashboardService.obterProdutosMaisVendidos(fim, inicio));
            verifyNoInteractions(pedidoRepository);
        }
    }
}