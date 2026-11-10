package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.dto.DashboardDTO;
import com.umc.pediupatrao.dto.DashboardVendasDTO;
import com.umc.pediupatrao.dto.DashboardStatusDTO;
import com.umc.pediupatrao.service.DashboardService;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;
import java.util.List;
import com.umc.pediupatrao.entity.StatusPedido;
import java.util.Arrays;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
@Import(SecurityConfig.class)
class DashboardControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    private DashboardDTO criarResumo() {
        return new DashboardDTO(
                120,
                80,
                10,
                4200.00,
                52.50
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "GERENTE"})
    void devePermitirConsultaParaPerfisAutorizados(String perfil) throws Exception {
        when(dashboardService.obterResumo(null, null)).thenReturn(criarResumo());

        mockMvc.perform(get("/api/dashboard").with(user("usuario").roles(perfil)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPedidos").value(120))
                .andExpect(jsonPath("$.pedidosFinalizados").value(80))
                .andExpect(jsonPath("$.pedidosCancelados").value(10))
                .andExpect(jsonPath("$.faturamento").value(4200.0))
                .andExpect(jsonPath("$.ticketMedio").value(52.5));

        verify(dashboardService).obterResumo(null, null);
    }

    @Test
    void deveConsultarDashboardSemFiltros() throws Exception {
        when(dashboardService.obterResumo(null, null)).thenReturn(criarResumo());
        mockMvc.perform(get("/api/dashboard").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        verify(dashboardService).obterResumo(null, null);
    }

    @Test
    void deveConsultarDashboardComPeriodoValido() throws Exception {
        LocalDate inicio = LocalDate.of(2026, 11, 1);
        LocalDate fim = LocalDate.of(2026, 11, 30);
        when(dashboardService.obterResumo(inicio, fim)).thenReturn(criarResumo());

        mockMvc.perform(get("/api/dashboard").with(user("gerente").roles("GERENTE")).param("inicio", "2026-11-01").param("fim", "2026-11-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPedidos").value(120))
                .andExpect(jsonPath("$.faturamento").value(4200.0));

        verify(dashboardService).obterResumo(inicio, fim);
    }

    @Test
    void devePermitirConsultaComInicioEFimNoMesmoDia() throws Exception {
        LocalDate data = LocalDate.of(2026, 11, 10);
        when(dashboardService.obterResumo(data, data)).thenReturn(criarResumo());
        mockMvc.perform(get("/api/dashboard").with(user("admin").roles("ADMIN")).param("inicio", "2026-11-10").param("fim", "2026-11-10")).andExpect(status().isOk());
        verify(dashboardService).obterResumo(data, data);
    }

    @Test
    void deveRetornarIndicadoresZeradosQuandoNaoExistiremPedidos() throws Exception {
        DashboardDTO vazio = new DashboardDTO(0, 0, 0, 0.0, 0.0);
        when(dashboardService.obterResumo(null, null)).thenReturn(vazio);

        mockMvc.perform(get("/api/dashboard").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPedidos").value(0))
                .andExpect(jsonPath("$.pedidosFinalizados").value(0))
                .andExpect(jsonPath("$.pedidosCancelados").value(0))
                .andExpect(jsonPath("$.faturamento").value(0.0))
                .andExpect(jsonPath("$.ticketMedio").value(0.0));

        verify(dashboardService).obterResumo(null, null);
    }

    @Test
    void deveRetornarRespostaEmFormatoJson() throws Exception {
        when(dashboardService.obterResumo(null, null)).thenReturn(criarResumo());

        mockMvc.perform(get("/api/dashboard").with(user("admin").roles("ADMIN")).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalPedidos").exists())
                .andExpect(jsonPath("$.faturamento").exists())
                .andExpect(jsonPath("$.ticketMedio").exists());

        verify(dashboardService).obterResumo(null, null);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "data-invalida",
            "10/11/2026",
            "2026-13-01"
    })
    void deveRejeitarDataInicialInvalida(String dataInvalida) throws Exception {
        mockMvc.perform(get("/api/dashboard").with(user("admin").roles("ADMIN")).param("inicio", dataInvalida).param("fim", "2026-11-30")).andExpect(status().isBadRequest());
        verifyNoInteractions(dashboardService);
    }

    @Test
    void deveRejeitarDataFinalInvalida() throws Exception {
        mockMvc.perform(get("/api/dashboard").with(user("admin").roles("ADMIN")).param("inicio", "2026-11-01").param("fim", "data-invalida")).andExpect(status().isBadRequest());
        verifyNoInteractions(dashboardService);
    }

    @Test
    void deveRejeitarConsultaComSomenteDataInicial() throws Exception {
        LocalDate inicio = LocalDate.of(2026, 11, 1);
        when(dashboardService.obterResumo(inicio, null)).thenThrow(new IllegalArgumentException("Informe a data inicial e a data final."));
        mockMvc.perform(get("/api/dashboard").with(user("admin").roles("ADMIN")).param("inicio", "2026-11-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Informe a data inicial e a data final."));

        verify(dashboardService).obterResumo(inicio, null);
    }

    @Test
    void deveRejeitarConsultaComSomenteDataFinal() throws Exception {
        LocalDate fim = LocalDate.of(2026, 11, 30);
        when(dashboardService.obterResumo(null, fim)).thenThrow(new IllegalArgumentException("Informe a data inicial e a data final."));
        mockMvc.perform(get("/api/dashboard").with(user("admin").roles("ADMIN")).param("fim", "2026-11-30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Informe a data inicial e a data final."));

        verify(dashboardService).obterResumo(null, fim);
    }

    @Test
    void deveRejeitarDataInicialPosteriorADataFinal() throws Exception {
        LocalDate inicio = LocalDate.of(2026, 11, 30);
        LocalDate fim = LocalDate.of(2026, 11, 1);
        when(dashboardService.obterResumo(inicio, fim)).thenThrow(new IllegalArgumentException("A data inicial não pode ser posterior à data final."));

        mockMvc.perform(get("/api/dashboard").with(user("gerente").roles("GERENTE")).param("inicio", "2026-11-30").param("fim", "2026-11-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("A data inicial não pode ser posterior à data final."));

        verify(dashboardService).obterResumo(inicio, fim);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ATENDENTE", "USER"})
    void deveImpedirPerfisNaoAutorizadosDeConsultarDashboard(String perfil) throws Exception {
        mockMvc.perform(get("/api/dashboard").with(user("usuario").roles(perfil))).andExpect(status().isForbidden());
        verifyNoInteractions(dashboardService);
    }

    @Test
    void deveExigirAutenticacaoParaConsultarDashboard() throws Exception {
        mockMvc.perform(get("/api/dashboard")).andExpect(status().is3xxRedirection());
        verifyNoInteractions(dashboardService);
    }

    @Test
    void devePermitirConsultaGetSemCsrf() throws Exception {
        when(dashboardService.obterResumo(null, null)).thenReturn(criarResumo());
        mockMvc.perform(get("/api/dashboard").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        verify(dashboardService).obterResumo(null, null);
    }

    @Test
    void deveRejeitarPostNoDashboard() throws Exception {
        mockMvc.perform(post("/api/dashboard").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(dashboardService);
    }

    @Test
    void deveRejeitarPutNoDashboard() throws Exception {
        mockMvc.perform(put("/api/dashboard").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(dashboardService);
    }

    @Test
    void deveRejeitarDeleteNoDashboard() throws Exception {
        mockMvc.perform(delete("/api/dashboard").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(dashboardService);
    }

    @Test
    void deveRetornar404ParaEndpointResumoAntigo() throws Exception {
        mockMvc.perform(get("/api/dashboard/resumo").with(user("admin").roles("ADMIN"))).andExpect(status().isNotFound());
        verifyNoInteractions(dashboardService);
    }

    @Test
    void devePermitirConsultaDeVendasParaAdmin() throws Exception {
        DashboardVendasDTO venda = new DashboardVendasDTO(LocalDate.of(2026, 11, 1), 2L, 150.50);
        when(dashboardService.obterVendasPorDia(null, null)).thenReturn(List.of(venda));
        mockMvc.perform(get("/api/dashboard/vendas").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].data").value("2026-11-01"))
                .andExpect(jsonPath("$[0].pedidosFinalizados").value(2))
                .andExpect(jsonPath("$[0].faturamento").value(150.50));

        verify(dashboardService).obterVendasPorDia(null, null);
    }

    @Test
    void devePermitirConsultaDeVendasParaGerente() throws Exception {
        when(dashboardService.obterVendasPorDia(null, null)).thenReturn(List.of());
        mockMvc.perform(get("/api/dashboard/vendas").with(user("gerente").roles("GERENTE"))).andExpect(status().isOk());
        verify(dashboardService).obterVendasPorDia(null, null);
    }

    @Test
    void deveAplicarFiltrosNaConsultaDeVendas() throws Exception {
        LocalDate inicio = LocalDate.of(2026, 11, 1);
        LocalDate fim = LocalDate.of(2026, 11, 3);
        DashboardVendasDTO venda = new DashboardVendasDTO(inicio, 3L, 200.0);
        when(dashboardService.obterVendasPorDia(inicio, fim)).thenReturn(List.of(venda));
        mockMvc.perform(get("/api/dashboard/vendas").with(user("admin").roles("ADMIN")).param("inicio", "2026-11-01").param("fim", "2026-11-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].data").value("2026-11-01"))
                .andExpect(jsonPath("$[0].pedidosFinalizados").value(3))
                .andExpect(jsonPath("$[0].faturamento").value(200.0));

        verify(dashboardService).obterVendasPorDia(inicio, fim);
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoExistiremVendas() throws Exception {
        when(dashboardService.obterVendasPorDia(null, null)).thenReturn(List.of());
        mockMvc.perform(get("/api/dashboard/vendas").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void deveNegarConsultaDeVendasParaAtendente() throws Exception {
        mockMvc.perform(get("/api/dashboard/vendas").with(user("atendente").roles("ATENDENTE"))).andExpect(status().isForbidden());
        verifyNoInteractions(dashboardService);
    }

    @Test
    void deveExigirAutenticacaoParaConsultarVendas() throws Exception {
        mockMvc.perform(get("/api/dashboard/vendas")).andExpect(status().is3xxRedirection());
        verifyNoInteractions(dashboardService);
    }

    @Test
    void deveRejeitarConsultaDeVendasComSomenteDataInicial() throws Exception {
        LocalDate inicio = LocalDate.of(2026, 11, 1);
        when(dashboardService.obterVendasPorDia(inicio, null)).thenThrow(new IllegalArgumentException("Informe a data inicial e a data final."));
        mockMvc.perform(get("/api/dashboard/vendas").with(user("admin").roles("ADMIN")).param("inicio", "2026-11-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Informe a data inicial e a data final."));
    }

    @Test
    void deveRejeitarConsultaDeVendasComSomenteDataFinal() throws Exception {
        LocalDate fim = LocalDate.of(2026, 11, 3);
        when(dashboardService.obterVendasPorDia(null, fim)).thenThrow(new IllegalArgumentException("Informe a data inicial e a data final."));
        mockMvc.perform(get("/api/dashboard/vendas").with(user("admin").roles("ADMIN")).param("fim", "2026-11-03"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Informe a data inicial e a data final."));
    }

    @Test
    void deveRejeitarConsultaDeVendasComPeriodoInvertido() throws Exception {
        LocalDate inicio = LocalDate.of(2026, 11, 10);
        LocalDate fim = LocalDate.of(2026, 11, 1);
        when(dashboardService.obterVendasPorDia(inicio, fim)).thenThrow(new IllegalArgumentException("A data inicial não pode ser posterior à data final."));
        mockMvc.perform(get("/api/dashboard/vendas").with(user("admin").roles("ADMIN")).param("inicio", "2026-11-10").param("fim", "2026-11-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("A data inicial não pode ser posterior à data final."));
    }

    @Test
    void deveRejeitarConsultaDeVendasComDataInvalida() throws Exception {
        mockMvc.perform(get("/api/dashboard/vendas").with(user("admin").roles("ADMIN")).param("inicio", "data-invalida").param("fim", "2026-11-03")).andExpect(status().isBadRequest());
        verifyNoInteractions(dashboardService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "GERENTE"})
    void devePermitirConsultaDeStatusParaPerfisAutorizados(String perfil) throws Exception {
        List<DashboardStatusDTO> situacao = List.of(
                new DashboardStatusDTO(StatusPedido.RECEBIDO, 2L),
                new DashboardStatusDTO(StatusPedido.CANCELADO, 4L)
        );

        when(dashboardService.obterSituacaoPedidos(null, null)).thenReturn(situacao);
        mockMvc.perform(get("/api/dashboard/status").with(user("usuario").roles(perfil)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].status").value("RECEBIDO"))
                .andExpect(jsonPath("$[0].quantidade").value(2))
                .andExpect(jsonPath("$[1].status").value("CANCELADO"))
                .andExpect(jsonPath("$[1].quantidade").value(4));

        verify(dashboardService).obterSituacaoPedidos(null, null);
    }

    @Test
    void deveAplicarFiltrosNaConsultaDeStatus() throws Exception {
        LocalDate inicio = LocalDate.of(2026, 11, 1);
        LocalDate fim = LocalDate.of(2026, 11, 10);

        List<DashboardStatusDTO> situacao = List.of(new DashboardStatusDTO(StatusPedido.FINALIZADO, 3L));

        when(dashboardService.obterSituacaoPedidos(inicio, fim)).thenReturn(situacao);

        mockMvc.perform(get("/api/dashboard/status").with(user("admin").roles("ADMIN")).param("inicio", "2026-11-01").param("fim", "2026-11-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("FINALIZADO"))
                .andExpect(jsonPath("$[0].quantidade").value(3));

        verify(dashboardService).obterSituacaoPedidos(inicio, fim);
    }

    @Test
    void deveRetornarStatusZeradosQuandoNaoExistiremPedidos() throws Exception {
        List<DashboardStatusDTO> situacao = Arrays.stream(StatusPedido.values()).map(status -> new DashboardStatusDTO(status, 0L)).toList();
        when(dashboardService.obterSituacaoPedidos(null, null)).thenReturn(situacao);
        mockMvc.perform(get("/api/dashboard/status").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(StatusPedido.values().length))
                .andExpect(jsonPath("$[0].quantidade").value(0));

        verify(dashboardService).obterSituacaoPedidos(null, null);
    }

    @Test
    void deveRejeitarConsultaDeStatusComSomenteDataInicial() throws Exception {
        LocalDate inicio = LocalDate.of(2026, 11, 1);
        when(dashboardService.obterSituacaoPedidos(inicio, null)).thenThrow(new IllegalArgumentException("Informe a data inicial e a data final."));

        mockMvc.perform(get("/api/dashboard/status").with(user("admin").roles("ADMIN")).param("inicio", "2026-11-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Informe a data inicial e a data final."));

        verify(dashboardService).obterSituacaoPedidos(inicio, null);
    }

    @Test
    void deveRejeitarConsultaDeStatusComSomenteDataFinal() throws Exception {
        LocalDate fim = LocalDate.of(2026, 11, 10);
        when(dashboardService.obterSituacaoPedidos(null, fim)).thenThrow(new IllegalArgumentException("Informe a data inicial e a data final."));
        mockMvc.perform(get("/api/dashboard/status").with(user("admin").roles("ADMIN")).param("fim", "2026-11-10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Informe a data inicial e a data final."));

        verify(dashboardService).obterSituacaoPedidos(null, fim);
    }

    @Test
    void deveRejeitarConsultaDeStatusComPeriodoInvertido() throws Exception {
        LocalDate inicio = LocalDate.of(2026, 11, 10);
        LocalDate fim = LocalDate.of(2026, 11, 1);

        when(dashboardService.obterSituacaoPedidos(inicio, fim)).thenThrow(new IllegalArgumentException("A data inicial não pode ser posterior à data final."));
        mockMvc.perform(get("/api/dashboard/status").with(user("admin").roles("ADMIN")).param("inicio", "2026-11-10").param("fim", "2026-11-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("A data inicial não pode ser posterior à data final."));

        verify(dashboardService).obterSituacaoPedidos(inicio, fim);
    }

    @Test
    void deveRejeitarConsultaDeStatusComDataInvalida() throws Exception {
        mockMvc.perform(get("/api/dashboard/status").with(user("admin").roles("ADMIN")).param("inicio", "data-invalida").param("fim", "2026-11-10")).andExpect(status().isBadRequest());
        verifyNoInteractions(dashboardService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ATENDENTE", "USER"})
    void deveNegarConsultaDeStatusParaPerfisNaoAutorizados(String perfil) throws Exception {
        mockMvc.perform(get("/api/dashboard/status").with(user("usuario").roles(perfil))).andExpect(status().isForbidden());
        verifyNoInteractions(dashboardService);
    }

    @Test
    void deveExigirAutenticacaoParaConsultarStatus() throws Exception {
        mockMvc.perform(get("/api/dashboard/status")).andExpect(status().is3xxRedirection());
        verifyNoInteractions(dashboardService);
    }
}