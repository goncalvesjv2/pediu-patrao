package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.dto.PedidoRequestDTO;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.service.PedidoService;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PedidoController.class)
@Import(SecurityConfig.class)
class PedidoControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PedidoService pedidoService;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    private static final String JSON_PEDIDO_VALIDO = """
            {
                "clienteId": "cliente1",
                "itens": [
                    {
                        "produtoId": "produto1",
                        "quantidade": 2
                    }
                ]
            }
            """;

    private Pedido criarPedido(String id, StatusPedido status, double valorTotal) {
        Pedido pedido = new Pedido();
        pedido.setId(id);
        pedido.setClienteId("cliente1");
        pedido.setStatus(status);
        pedido.setValorTotal(valorTotal);
        return pedido;
    }

    @Test
    void deveListarPedidosComSucesso() throws Exception {
        Pedido pedido1 = criarPedido("pedido1", StatusPedido.RECEBIDO, 100.0);
        Pedido pedido2 = criarPedido("pedido2", StatusPedido.EM_PREPARO, 150.0);
        when(pedidoService.listarPedidos()).thenReturn(List.of(pedido1, pedido2));

        mockMvc.perform(get("/api/pedidos").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("pedido1"))
                .andExpect(jsonPath("$[0].status").value("RECEBIDO"))
                .andExpect(jsonPath("$[0].valorTotal").value(100.0))
                .andExpect(jsonPath("$[1].id").value("pedido2"))
                .andExpect(jsonPath("$[1].status").value("EM_PREPARO"))
                .andExpect(jsonPath("$[1].valorTotal").value(150.0));

        verify(pedidoService).listarPedidos();
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoExistiremPedidos() throws Exception {
        when(pedidoService.listarPedidos()).thenReturn(List.of());
        mockMvc.perform(get("/api/pedidos").with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(pedidoService).listarPedidos();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "GERENTE", "ATENDENTE"})
    void devePermitirListagemParaPerfisAutorizados(String perfil) throws Exception {
        when(pedidoService.listarPedidos()).thenReturn(List.of());
        mockMvc.perform(get("/api/pedidos").with(user("usuario").roles(perfil))).andExpect(status().isOk());
        verify(pedidoService).listarPedidos();
    }

    @Test
    void deveExigirAutenticacaoParaListarPedidos() throws Exception {
        mockMvc.perform(get("/api/pedidos")).andExpect(status().is3xxRedirection());
        verifyNoInteractions(pedidoService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"GERENTE", "ATENDENTE"})
    void deveCriarPedidoComPerfilAutorizado(String perfil) throws Exception {
        Pedido pedidoCriado = criarPedido("pedido1", StatusPedido.RECEBIDO, 100.0);
        when(pedidoService.criarPedido(any(PedidoRequestDTO.class), any(Authentication.class))).thenReturn(pedidoCriado);
        mockMvc.perform(post("/api/pedidos").with(user("operador").roles(perfil)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(JSON_PEDIDO_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("pedido1"))
                .andExpect(jsonPath("$.clienteId").value("cliente1"))
                .andExpect(jsonPath("$.status").value("RECEBIDO"))
                .andExpect(jsonPath("$.valorTotal").value(100.0));

        verify(pedidoService).criarPedido(
                argThat(dto ->
                        "cliente1".equals(dto.getClienteId())
                                && dto.getItens() != null
                                && dto.getItens().size() == 1
                                && "produto1".equals(
                                dto.getItens().get(0).getProdutoId()
                        )
                                && dto.getItens().get(0).getQuantidade() == 2
                ),
                argThat(authentication ->
                        "operador".equals(authentication.getName())
                )
        );
    }

    @Test
    void deveImpedirAdminDeCriarPedido() throws Exception {
        mockMvc.perform(post("/api/pedidos").with(user("admin").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(JSON_PEDIDO_VALIDO)).andExpect(status().isForbidden());
        verifyNoInteractions(pedidoService);
    }

    @Test
    void deveExigirAutenticacaoParaCriarPedido() throws Exception {
        mockMvc.perform(post("/api/pedidos").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(JSON_PEDIDO_VALIDO)).andExpect(status().is3xxRedirection());
        verifyNoInteractions(pedidoService);
    }

    @Test
    void deveImpedirCriacaoSemCsrf() throws Exception {
        mockMvc.perform(post("/api/pedidos").with(user("atendente").roles("ATENDENTE")).contentType(MediaType.APPLICATION_JSON).content(JSON_PEDIDO_VALIDO)).andExpect(status().isForbidden());
        verifyNoInteractions(pedidoService);
    }

    @Test
    void deveImpedirCriacaoComCsrfInvalido() throws Exception {
        mockMvc.perform(post("/api/pedidos").with(user("atendente").roles("ATENDENTE")).with(csrf().useInvalidToken()).contentType(MediaType.APPLICATION_JSON).content(JSON_PEDIDO_VALIDO)).andExpect(status().isForbidden());
        verifyNoInteractions(pedidoService);
    }

    @Test
    void deveRejeitarJsonInvalidoNaCriacao() throws Exception {
        mockMvc.perform(post("/api/pedidos").with(user("atendente").roles("ATENDENTE")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"clienteId\":")).andExpect(status().isBadRequest());
        verifyNoInteractions(pedidoService);
    }

    @Test
    void deveRejeitarCriacaoSemBody() throws Exception {
        mockMvc.perform(post("/api/pedidos").with(user("atendente").roles("ATENDENTE")).with(csrf()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
        verifyNoInteractions(pedidoService);
    }

    @Test
    void deveRejeitarItensComFormatoJsonInvalido() throws Exception {
        String jsonInvalido = """
                {
                    "clienteId": "cliente1",
                    "itens": "valor-invalido"
                }
                """;

        mockMvc.perform(post("/api/pedidos").with(user("atendente").roles("ATENDENTE")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(jsonInvalido)).andExpect(status().isBadRequest());
        verifyNoInteractions(pedidoService);
    }

    @ParameterizedTest
    @CsvSource({"ATENDENTE, EM_PREPARO", "GERENTE, PRONTO", "ATENDENTE, SAIU_PARA_ENTREGA", "GERENTE, FINALIZADO"})
    void deveAtualizarStatusComPerfilAutorizado(String perfil, StatusPedido novoStatus) throws Exception {
        Pedido pedidoAtualizado = criarPedido("pedido1", novoStatus, 100.0);
        when(pedidoService.atualizarStatus(eq("pedido1"), eq(novoStatus), any(Authentication.class))).thenReturn(pedidoAtualizado);

        mockMvc.perform(put("/api/pedidos/pedido1/status").with(user("operador").roles(perfil)).with(csrf()).param("status", novoStatus.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("pedido1"))
                .andExpect(jsonPath("$.status").value(novoStatus.name()));

        verify(pedidoService).atualizarStatus(
                eq("pedido1"),
                eq(novoStatus),
                argThat(authentication -> "operador".equals(authentication.getName()))
        );
    }

    @Test
    void deveImpedirAdminDeAtualizarStatus() throws Exception {
        mockMvc.perform(put("/api/pedidos/pedido1/status").with(user("admin").roles("ADMIN")).with(csrf()).param("status", "EM_PREPARO")).andExpect(status().isForbidden());
        verifyNoInteractions(pedidoService);
    }

    @Test
    void deveExigirAutenticacaoParaAtualizarStatus() throws Exception {
        mockMvc.perform(put("/api/pedidos/pedido1/status").with(csrf()).param("status", "EM_PREPARO")).andExpect(status().is3xxRedirection());
        verifyNoInteractions(pedidoService);
    }

    @Test
    void deveRejeitarAtualizacaoSemParametroStatus() throws Exception {
        mockMvc.perform(put("/api/pedidos/pedido1/status").with(user("gerente").roles("GERENTE")).with(csrf())).andExpect(status().isBadRequest());
        verifyNoInteractions(pedidoService);
    }

    @Test
    void deveRejeitarStatusInexistente() throws Exception {
        mockMvc.perform(put("/api/pedidos/pedido1/status").with(user("gerente").roles("GERENTE")).with(csrf()).param("status", "STATUS_INVALIDO")).andExpect(status().isBadRequest());
        verifyNoInteractions(pedidoService);
    }

    @Test
    void deveRejeitarStatusVazio() throws Exception {
        mockMvc.perform(put("/api/pedidos/pedido1/status").with(user("gerente").roles("GERENTE")).with(csrf()).param("status", "")).andExpect(status().isBadRequest());
        verifyNoInteractions(pedidoService);
    }

    @Test
    void deveImpedirAtualizacaoDeStatusSemCsrf() throws Exception {
        mockMvc.perform(put("/api/pedidos/pedido1/status").with(user("gerente").roles("GERENTE")).param("status", "EM_PREPARO")).andExpect(status().isForbidden());
        verifyNoInteractions(pedidoService);
    }

    @Test
    void deveImpedirAtualizacaoDeStatusComCsrfInvalido() throws Exception {
        mockMvc.perform(put("/api/pedidos/pedido1/status").with(user("gerente").roles("GERENTE")).with(csrf().useInvalidToken()).param("status", "EM_PREPARO")).andExpect(status().isForbidden());
        verifyNoInteractions(pedidoService);
    }

    @Test
    void devePermitirGerenteAplicarDesconto() throws Exception {
        Pedido pedidoComDesconto = criarPedido("pedido1", StatusPedido.RECEBIDO, 90.0);
        when(pedidoService.aplicarDesconto(eq("pedido1"), eq(10.0), any(Authentication.class))).thenReturn(pedidoComDesconto);

        mockMvc.perform(put("/api/pedidos/pedido1/desconto").with(user("gerente").roles("GERENTE")).with(csrf()).param("percentual", "10.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("pedido1"))
                .andExpect(jsonPath("$.valorTotal").value(90.0));

        verify(pedidoService).aplicarDesconto(eq("pedido1"), eq(10.0), argThat(authentication -> "gerente".equals(authentication.getName())));
    }

    @Test
    void devePermitirPercentualDecimal() throws Exception {
        Pedido pedido = criarPedido("pedido1", StatusPedido.RECEBIDO, 87.5);
        when(pedidoService.aplicarDesconto(eq("pedido1"), eq(12.5), any(Authentication.class))).thenReturn(pedido);

        mockMvc.perform(put("/api/pedidos/pedido1/desconto").with(user("gerente").roles("GERENTE")).with(csrf()).param("percentual", "12.5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorTotal").value(87.5));

        verify(pedidoService).aplicarDesconto(eq("pedido1"), eq(12.5), any(Authentication.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "ATENDENTE"})
    void deveImpedirOutrosPerfisDeAplicarDesconto(String perfil) throws Exception {
        mockMvc.perform(put("/api/pedidos/pedido1/desconto").with(user("usuario").roles(perfil)).with(csrf()).param("percentual", "10.0")).andExpect(status().isForbidden());
        verifyNoInteractions(pedidoService);
    }

    @Test
    void deveExigirAutenticacaoParaAplicarDesconto() throws Exception {
        mockMvc.perform(put("/api/pedidos/pedido1/desconto").with(csrf()).param("percentual", "10.0")).andExpect(status().is3xxRedirection());
        verifyNoInteractions(pedidoService);
    }

    @Test
    void deveRejeitarDescontoSemPercentual() throws Exception {
        mockMvc.perform(put("/api/pedidos/pedido1/desconto").with(user("gerente").roles("GERENTE")).with(csrf())).andExpect(status().isBadRequest());
        verifyNoInteractions(pedidoService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "dez"})
    void deveRejeitarPercentualNaoNumerico(String percentual) throws Exception {
        mockMvc.perform(put("/api/pedidos/pedido1/desconto").with(user("gerente").roles("GERENTE")).with(csrf()).param("percentual", percentual)).andExpect(status().isBadRequest());
        verifyNoInteractions(pedidoService);
    }

    @Test
    void deveImpedirDescontoSemCsrf() throws Exception {
        mockMvc.perform(put("/api/pedidos/pedido1/desconto").with(user("gerente").roles("GERENTE")).param("percentual", "10.0")).andExpect(status().isForbidden());
        verifyNoInteractions(pedidoService);
    }

    @Test
    void deveImpedirDescontoComCsrfInvalido() throws Exception {
        mockMvc.perform(put("/api/pedidos/pedido1/desconto").with(user("gerente").roles("GERENTE")).with(csrf().useInvalidToken()).param("percentual", "10.0")).andExpect(status().isForbidden());
        verifyNoInteractions(pedidoService);
    }

    @Test
    void devePermitirGerenteCancelarPedido() throws Exception {
        Pedido pedidoCancelado = criarPedido("pedido1", StatusPedido.CANCELADO, 100.0);
        when(pedidoService.cancelarPedido(eq("pedido1"), eq("Cliente solicitou cancelamento"), any(Authentication.class))).thenReturn(pedidoCancelado);

        mockMvc.perform(put("/api/pedidos/pedido1/cancelamento").with(user("gerente").roles("GERENTE")).with(csrf()).param("justificativa", "Cliente solicitou cancelamento"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("pedido1"))
                .andExpect(jsonPath("$.status").value("CANCELADO"));

        verify(pedidoService).cancelarPedido(eq("pedido1"), eq("Cliente solicitou cancelamento"), argThat(authentication -> "gerente".equals(authentication.getName())));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "ATENDENTE"})
    void deveImpedirOutrosPerfisDeCancelarPedido(String perfil) throws Exception {
        mockMvc.perform(put("/api/pedidos/pedido1/cancelamento").with(user("usuario").roles(perfil)).with(csrf()).param("justificativa", "Cancelamento solicitado")).andExpect(status().isForbidden());
        verifyNoInteractions(pedidoService);
    }

    @Test
    void deveExigirAutenticacaoParaCancelarPedido() throws Exception {
        mockMvc.perform(put("/api/pedidos/pedido1/cancelamento").with(csrf()).param("justificativa", "Cancelamento solicitado")).andExpect(status().is3xxRedirection());
        verifyNoInteractions(pedidoService);
    }

    @Test
    void deveRejeitarCancelamentoSemJustificativa() throws Exception {
        mockMvc.perform(put("/api/pedidos/pedido1/cancelamento").with(user("gerente").roles("GERENTE")).with(csrf())).andExpect(status().isBadRequest());
        verifyNoInteractions(pedidoService);
    }

    @Test
    void deveImpedirCancelamentoSemCsrf() throws Exception {
        mockMvc.perform(put("/api/pedidos/pedido1/cancelamento").with(user("gerente").roles("GERENTE")).param("justificativa", "Cancelamento solicitado")).andExpect(status().isForbidden());
        verifyNoInteractions(pedidoService);
    }

    @Test
    void deveImpedirCancelamentoComCsrfInvalido() throws Exception {
        mockMvc.perform(put("/api/pedidos/pedido1/cancelamento").with(user("gerente").roles("GERENTE")).with(csrf().useInvalidToken()).param("justificativa", "Cancelamento solicitado")).andExpect(status().isForbidden());
        verifyNoInteractions(pedidoService);
    }
}