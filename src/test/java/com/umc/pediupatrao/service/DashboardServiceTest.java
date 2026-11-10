package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.DashboardDTO;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.repository.PedidoRepository;
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
}