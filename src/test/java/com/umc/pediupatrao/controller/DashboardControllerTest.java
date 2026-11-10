package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.dto.DashboardDTO;
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
}