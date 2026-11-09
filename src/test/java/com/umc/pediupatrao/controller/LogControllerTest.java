package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.entity.Log;
import com.umc.pediupatrao.repository.LogRepository;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LogController.class)
@Import(SecurityConfig.class)
class LogControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LogRepository logRepository;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    private Log criarLog(String id, String usuario, String perfil, String operacao) {
        Log log = new Log();
        log.setId(id);
        log.setUsuario(usuario);
        log.setPerfil(perfil);
        log.setOperacao(operacao);
        log.setEntidade("Pedido");
        log.setEntidadeId("pedido1");
        log.setDataHora(LocalDateTime.of(2026, 11, 7, 10, 30));
        return log;
    }

    @Test
    void devePermitirAdminListarLogs() throws Exception {
        Log log1 = criarLog("log1", "joao", "ADMIN", "CRIACAO_PEDIDO");
        Log log2 = criarLog("log2", "maria", "GERENTE", "ALTERACAO_STATUS");

        when(logRepository.findAll()).thenReturn(List.of(log1, log2));

        mockMvc.perform(get("/api/logs").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("log1"))
                .andExpect(jsonPath("$[0].usuario").value("joao"))
                .andExpect(jsonPath("$[0].perfil").value("ADMIN"))
                .andExpect(jsonPath("$[0].operacao").value("CRIACAO_PEDIDO"))
                .andExpect(jsonPath("$[1].id").value("log2"))
                .andExpect(jsonPath("$[1].usuario").value("maria"))
                .andExpect(jsonPath("$[1].perfil").value("GERENTE"))
                .andExpect(jsonPath("$[1].operacao").value("ALTERACAO_STATUS"));

        verify(logRepository, times(1)).findAll();
    }

    @Test
    void devePermitirGerenteListarLogs() throws Exception {
        Log log = criarLog("log1", "atendente01", "ATENDENTE", "CRIACAO_PEDIDO");
        when(logRepository.findAll()).thenReturn(List.of(log));

        mockMvc.perform(get("/api/logs").with(user("gerente").roles("GERENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("log1"))
                .andExpect(jsonPath("$[0].usuario").value("atendente01"));

        verify(logRepository).findAll();
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoExistiremLogs() throws Exception {
        when(logRepository.findAll()).thenReturn(List.of());
        mockMvc.perform(get("/api/logs").with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(logRepository).findAll();
    }

    @Test
    void deveRetornarDadosDaAuditoriaCorretamente() throws Exception {
        Log log = criarLog("log1", "joao", "GERENTE", "ALTERACAO_STATUS");
        when(logRepository.findAll()).thenReturn(List.of(log));
        mockMvc.perform(get("/api/logs").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("log1"))
                .andExpect(jsonPath("$[0].usuario").value("joao"))
                .andExpect(jsonPath("$[0].perfil").value("GERENTE"))
                .andExpect(jsonPath("$[0].operacao").value("ALTERACAO_STATUS"))
                .andExpect(jsonPath("$[0].entidade").value("Pedido"))
                .andExpect(jsonPath("$[0].entidadeId").value("pedido1"))
                .andExpect(jsonPath("$[0].dataHora").exists());

        verify(logRepository).findAll();
    }

    @Test
    void deveRetornarAlteracoesRegistradas() throws Exception {
        Log log = criarLog("log1", "gerente01", "GERENTE", "ALTERACAO_STATUS");
        Log.AlteracaoCampo alteracao = new Log.AlteracaoCampo("status", "RECEBIDO", "EM_PREPARO");
        log.setAlteracoes(List.of(alteracao));
        when(logRepository.findAll()).thenReturn(List.of(log));

        mockMvc.perform(get("/api/logs").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].alteracoes.length()").value(1))
                .andExpect(jsonPath("$[0].alteracoes[0].campo").value("status"))
                .andExpect(jsonPath("$[0].alteracoes[0].valorAnterior").value("RECEBIDO"))
                .andExpect(jsonPath("$[0].alteracoes[0].valorNovo").value("EM_PREPARO"));
        verify(logRepository).findAll();
    }

    @Test
    void deveRetornarJustificativaDoLog() throws Exception {
        Log log = criarLog("log1", "gerente01", "GERENTE", "CANCELAMENTO_PEDIDO");
        log.setJustificativa("Cliente solicitou cancelamento");
        when(logRepository.findAll()).thenReturn(List.of(log));
        mockMvc.perform(get("/api/logs").with(user("gerente").roles("GERENTE"))).andExpect(status().isOk()).andExpect(jsonPath("$[0].justificativa").value("Cliente solicitou cancelamento"));
        verify(logRepository).findAll();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ATENDENTE", "USER"})
    void deveImpedirPerfisNaoAutorizadosDeConsultarLogs(String perfil) throws Exception {
        mockMvc.perform(get("/api/logs").with(user("usuario").roles(perfil))).andExpect(status().isForbidden());
        verifyNoInteractions(logRepository);
    }

    @Test
    void deveExigirAutenticacaoParaConsultarLogs() throws Exception {
        mockMvc.perform(get("/api/logs")).andExpect(status().is3xxRedirection());
        verifyNoInteractions(logRepository);
    }

    @Test
    void devePermitirConsultaGetSemCsrf() throws Exception {
        when(logRepository.findAll()).thenReturn(List.of());
        mockMvc.perform(get("/api/logs").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        verify(logRepository).findAll();
    }

    @Test
    void deveRetornar404AoConsultarLogPorId() throws Exception {
        mockMvc.perform(get("/api/logs/log1").with(user("admin").roles("ADMIN"))).andExpect(status().isNotFound());
        verifyNoInteractions(logRepository);
    }

    @Test
    void deveRejeitarCadastroDeLogPorPost() throws Exception {
        mockMvc.perform(post("/api/logs").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(logRepository);
    }

    @Test
    void deveRejeitarAtualizacaoDeLogPorPut() throws Exception {
        mockMvc.perform(put("/api/logs").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(logRepository);
    }

    @Test
    void deveRejeitarExclusaoDeLogPorDelete() throws Exception {
        mockMvc.perform(delete("/api/logs").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(logRepository);
    }
}