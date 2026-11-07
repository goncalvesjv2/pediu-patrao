package com.umc.pediupatrao.config;

import com.umc.pediupatrao.service.UsuarioDetailsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityConfigTest.RotasTeste.class)
@Import({SecurityConfig.class, SecurityConfigTest.RotasTeste.class})
class SecurityConfigTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    @Test
    void devePermitirAcessoPublicoAoLogin() throws Exception {
        mockMvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test
    void deveRedirecionarUsuarioNaoAutenticadoParaLogin() throws Exception {
        mockMvc.perform(get("/api/usuarios/1")).andExpect(status().is3xxRedirection());
    }

    @Test
    void devePermitirAdminConsultarUsuarios() throws Exception {
        mockMvc.perform(get("/api/usuarios/1").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"GERENTE", "ATENDENTE"})
    void deveImpedirOutrosPerfisDeConsultarUsuarios(String perfil) throws Exception {
        mockMvc.perform(get("/api/usuarios/1").with(user("teste").roles(perfil))).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "GERENTE", "ATENDENTE"})
    void devePermitirConsultaDePedidosAosPerfisAutorizados(String perfil) throws Exception {
        mockMvc.perform(get("/api/pedidos").with(user("teste").roles(perfil))).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"GERENTE", "ATENDENTE"})
    void devePermitirCriarPedidoParaGerenteEAtendente(String perfil) throws Exception {
        mockMvc.perform(post("/api/pedidos").with(user("teste").roles(perfil)).with(csrf())).andExpect(status().isOk());
    }

    @Test
    void deveImpedirAdminDeCriarPedido() throws Exception {
        mockMvc.perform(post("/api/pedidos").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"GERENTE", "ATENDENTE"})
    void devePermitirAtualizacaoDeStatusParaPerfisAutorizados(String perfil) throws Exception {
        mockMvc.perform(put("/api/pedidos/1/status").with(user("teste").roles(perfil)).with(csrf())).andExpect(status().isOk());
    }

    @Test
    void deveImpedirAdminDeAtualizarStatus() throws Exception {
        mockMvc.perform(put("/api/pedidos/1/status").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    void devePermitirGerenteAplicarDesconto() throws Exception {
        mockMvc.perform(put("/api/pedidos/1/desconto").with(user("gerente").roles("GERENTE")).with(csrf())).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "ATENDENTE"})
    void deveImpedirOutrosPerfisDeAplicarDesconto(String perfil) throws Exception {
        mockMvc.perform(put("/api/pedidos/1/desconto").with(user("teste").roles(perfil)).with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    void devePermitirGerenteCancelarPedido() throws Exception {
        mockMvc.perform(put("/api/pedidos/1/cancelamento").with(user("gerente").roles("GERENTE")).with(csrf())).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "ATENDENTE"})
    void deveImpedirOutrosPerfisDeCancelarPedido(String perfil) throws Exception {
        mockMvc.perform(put("/api/pedidos/1/cancelamento").with(user("teste").roles(perfil)).with(csrf())).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"GERENTE", "ATENDENTE"})
    void devePermitirConsultaDeClientes(String perfil) throws Exception {
        mockMvc.perform(get("/api/clientes/1").with(user("teste").roles(perfil))).andExpect(status().isOk());
    }

    @Test
    void deveImpedirAdminDeConsultarClientes() throws Exception {
        mockMvc.perform(get("/api/clientes/1").with(user("admin").roles("ADMIN"))).andExpect(status().isForbidden());
    }

    @Test
    void devePermitirAtendenteCadastrarCliente() throws Exception {
        mockMvc.perform(post("/api/clientes").with(user("atendente").roles("ATENDENTE")).with(csrf())).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "GERENTE"})
    void deveImpedirOutrosPerfisDeCadastrarCliente(String perfil) throws Exception {
        mockMvc.perform(post("/api/clientes").with(user("teste").roles(perfil)).with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    void devePermitirAtendenteAtualizarCliente() throws Exception {
        mockMvc.perform(put("/api/clientes/1").with(user("atendente").roles("ATENDENTE")).with(csrf())).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "GERENTE"})
    void deveImpedirOutrosPerfisDeAtualizarCliente(String perfil) throws Exception {
        mockMvc.perform(put("/api/clientes/1").with(user("teste").roles(perfil)).with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    void devePermitirGerenteExcluirCliente() throws Exception {
        mockMvc.perform(delete("/api/clientes/1").with(user("gerente").roles("GERENTE")).with(csrf())).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "ATENDENTE"})
    void deveImpedirOutrosPerfisDeExcluirCliente(String perfil) throws Exception {
        mockMvc.perform(delete("/api/clientes/1").with(user("teste").roles(perfil)).with(csrf())).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "GERENTE", "ATENDENTE"})
    void devePermitirConsultaDeProdutos(String perfil) throws Exception {
        mockMvc.perform(get("/api/produtos/1").with(user("teste").roles(perfil))).andExpect(status().isOk());
    }

    @Test
    void devePermitirGerenteCadastrarProduto() throws Exception {
        mockMvc.perform(post("/api/produtos").with(user("gerente").roles("GERENTE")).with(csrf())).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "ATENDENTE"})
    void deveImpedirOutrosPerfisDeCadastrarProduto(String perfil) throws Exception {
        mockMvc.perform(post("/api/produtos").with(user("teste").roles(perfil)).with(csrf())).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "GERENTE"})
    void devePermitirConsultaDeLogs(String perfil) throws Exception {
        mockMvc.perform(get("/api/logs/1").with(user("teste").roles(perfil))).andExpect(status().isOk());
    }

    @Test
    void deveImpedirAtendenteDeConsultarLogs() throws Exception {
        mockMvc.perform(get("/api/logs/1").with(user("atendente").roles("ATENDENTE"))).andExpect(status().isForbidden());
    }

    @Test
    void deveImpedirPostSemTokenCsrf() throws Exception {
        mockMvc.perform(post("/api/clientes").with(user("atendente").roles("ATENDENTE"))).andExpect(status().isForbidden());
    }

    @Test
    void deveImpedirPutComTokenCsrfInvalido() throws Exception {
        mockMvc.perform(put("/api/pedidos/1/status").with(user("gerente").roles("GERENTE")).with(csrf().useInvalidToken())).andExpect(status().isForbidden());
    }

    @Test
    void deveImpedirDeleteSemTokenCsrf() throws Exception {
        mockMvc.perform(delete("/api/clientes/1").with(user("gerente").roles("GERENTE"))).andExpect(status().isForbidden());
    }

    @Test
    void deveRedirecionarPostSemAutenticacaoParaLogin() throws Exception {
        mockMvc.perform(post("/api/pedidos").with(csrf())).andExpect(status().is3xxRedirection());
    }

    @RestController
    public static class RotasTeste {
        @GetMapping({
                "/login",
                "/api/usuarios/{id}",
                "/api/pedidos",
                "/api/clientes/{id}",
                "/api/produtos/{id}",
                "/api/logs/{id}"
        })
        public String consultar() {
            return "OK";
        }

        @PostMapping({
                "/api/pedidos",
                "/api/clientes",
                "/api/produtos"
        })
        public String criar() {
            return "OK";
        }

        @PutMapping({
                "/api/pedidos/{id}/status",
                "/api/pedidos/{id}/desconto",
                "/api/pedidos/{id}/cancelamento",
                "/api/clientes/{id}"
        })
        public String atualizar() {
            return "OK";
        }

        @DeleteMapping("/api/clientes/{id}")
        public String excluir() {
            return "OK";
        }
    }
}