package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.service.ClienteService;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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

@WebMvcTest(ClienteController.class)
@Import(SecurityConfig.class)
class ClienteControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClienteService clienteService;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    private static final String JSON_CLIENTE_VALIDO = """
            {
                "nome": "Maria",
                "telefone": "11999999999",
                "cep": "12345000",
                "logradouro": "Rua das Flores",
                "numero": "100",
                "bairro": "Centro",
                "cidade": "Jacareí",
                "estado": "SP"
            }
            """;

    private Cliente criarCliente(String id, String nome, String telefone) {
        Cliente cliente = new Cliente();
        cliente.setId(id);
        cliente.setNome(nome);
        cliente.setTelefone(telefone);
        return cliente;
    }

    @ParameterizedTest
    @ValueSource(strings = {"GERENTE", "ATENDENTE"})
    void deveListarClientesParaPerfisAutorizados(String perfil) throws Exception {
        Cliente cliente1 = criarCliente("1", "Maria", "11999999999");
        Cliente cliente2 = criarCliente("2", "João", "11988888888");

        when(clienteService.listarClientes()).thenReturn(List.of(cliente1, cliente2));
        mockMvc.perform(get("/api/clientes").with(user("teste").roles(perfil))).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value("1")).andExpect(jsonPath("$[0].nome").value("Maria")).andExpect(jsonPath("$[1].id").value("2")).andExpect(jsonPath("$[1].nome").value("João"));
        verify(clienteService).listarClientes();
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoExistemClientes() throws Exception {
        when(clienteService.listarClientes()).thenReturn(List.of());
        mockMvc.perform(get("/api/clientes").with(user("atendente").roles("ATENDENTE"))).andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(clienteService).listarClientes();
    }

    @Test
    void deveImpedirAdminDeConsultarClientes() throws Exception {
        mockMvc.perform(get("/api/clientes").with(user("admin").roles("ADMIN"))).andExpect(status().isForbidden());
        verifyNoInteractions(clienteService);
    }

    @Test
    void devePermitirAtendenteCadastrarCliente() throws Exception {
        Cliente clienteSalvo = criarCliente("1", "Maria", "11999999999");
        when(clienteService.salvar(any(Cliente.class), any(Authentication.class))).thenReturn(clienteSalvo);
        mockMvc.perform(post("/api/clientes").with(user("atendente").roles("ATENDENTE")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(JSON_CLIENTE_VALIDO)).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value("1")).andExpect(jsonPath("$.nome").value("Maria")).andExpect(jsonPath("$.telefone").value("11999999999"));
        verify(clienteService).salvar(
                argThat(cliente -> cliente.getId() == null && "Maria".equals(cliente.getNome()) && "11999999999".equals(cliente.getTelefone())),
                argThat(authentication -> "atendente".equals(authentication.getName()))
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "GERENTE"})
    void deveImpedirOutrosPerfisDeCadastrarCliente(String perfil) throws Exception {
        mockMvc.perform(post("/api/clientes").with(user("teste").roles(perfil)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(JSON_CLIENTE_VALIDO)).andExpect(status().isForbidden());
        verifyNoInteractions(clienteService);
    }

    @Test
    void deveRetornar400QuandoJsonDoCadastroForInvalido() throws Exception {
        mockMvc.perform(post("/api/clientes").with(user("atendente").roles("ATENDENTE")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"nome\":")).andExpect(status().isBadRequest());
        verifyNoInteractions(clienteService);
    }

    @Test
    void deveRetornar400QuandoCadastroNaoPossuiBody() throws Exception {
        mockMvc.perform(post("/api/clientes").with(user("atendente").roles("ATENDENTE")).with(csrf()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
        verifyNoInteractions(clienteService);
    }

    @Test
    void deveRejeitarCadastroComTipoDeConteudoInvalido() throws Exception {
        mockMvc.perform(post("/api/clientes").with(user("atendente").roles("ATENDENTE")).with(csrf()).contentType(MediaType.TEXT_PLAIN).content("nome=Maria")).andExpect(status().isUnsupportedMediaType());
        verifyNoInteractions(clienteService);
    }

    @Test
    void devePermitirAtendenteAtualizarCliente() throws Exception {
        when(clienteService.salvar(any(Cliente.class), any(Authentication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/clientes/1").with(user("atendente").roles("ATENDENTE")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "nome": "Maria Atualizada",
                                    "telefone": "11977777777"
                                }
                                """)
                ).andExpect(status().isOk()).andExpect(jsonPath("$.id").value("1")).andExpect(jsonPath("$.nome").value("Maria Atualizada")).andExpect(jsonPath("$.telefone").value("11977777777"));
        verify(clienteService).salvar(
                argThat(cliente -> "1".equals(cliente.getId()) && "Maria Atualizada".equals(cliente.getNome())),
                argThat(authentication -> "atendente".equals(authentication.getName()))
        );
    }

    @Test
    void deveUtilizarIdDaUrlAoAtualizarCliente() throws Exception {
        when(clienteService.salvar(any(Cliente.class), any(Authentication.class))).thenAnswer(invocation -> invocation.getArgument(0));
        mockMvc.perform(
                put("/api/clientes/1").with(user("atendente").roles("ATENDENTE")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "id": "999",
                                    "nome": "Maria",
                                    "telefone": "11999999999"
                                }
                                """)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value("1"));
        verify(clienteService).salvar(argThat(cliente -> "1".equals(cliente.getId())), any(Authentication.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "GERENTE"})
    void deveImpedirOutrosPerfisDeAtualizarCliente(String perfil) throws Exception {
        mockMvc.perform(put("/api/clientes/1").with(user("teste").roles(perfil)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(JSON_CLIENTE_VALIDO)).andExpect(status().isForbidden());
        verifyNoInteractions(clienteService);
    }

    @Test
    void deveRetornar400QuandoAtualizacaoNaoPossuiBody() throws Exception {
        mockMvc.perform(put("/api/clientes/1").with(user("atendente").roles("ATENDENTE")).with(csrf()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
        verifyNoInteractions(clienteService);
    }

    @Test
    void deveRetornar400QuandoJsonDaAtualizacaoForInvalido() throws Exception {
        mockMvc.perform(put("/api/clientes/1").with(user("atendente").roles("ATENDENTE")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"nome\":")).andExpect(status().isBadRequest());
        verifyNoInteractions(clienteService);
    }

    @Test
    void devePermitirGerenteExcluirCliente() throws Exception {
        mockMvc.perform(delete("/api/clientes/1").with(user("gerente").roles("GERENTE")).with(csrf())).andExpect(status().isNoContent());
        verify(clienteService).excluir("1");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "ATENDENTE"})
    void deveImpedirOutrosPerfisDeExcluirCliente(String perfil) throws Exception {
        mockMvc.perform(delete("/api/clientes/1").with(user("teste").roles(perfil)).with(csrf())).andExpect(status().isForbidden());
        verifyNoInteractions(clienteService);
    }

    @Test
    void deveRedirecionarUsuarioNaoAutenticado() throws Exception {
        mockMvc.perform(get("/api/clientes")).andExpect(status().is3xxRedirection());
        verifyNoInteractions(clienteService);
    }


    @Test
    void deveImpedirCadastroSemCsrf() throws Exception {
        mockMvc.perform(post("/api/clientes").with(user("atendente").roles("ATENDENTE")).contentType(MediaType.APPLICATION_JSON).content(JSON_CLIENTE_VALIDO)).andExpect(status().isForbidden());
        verifyNoInteractions(clienteService);
    }

    @Test
    void deveImpedirAtualizacaoSemCsrf() throws Exception {
        mockMvc.perform(put("/api/clientes/1").with(user("atendente").roles("ATENDENTE")).contentType(MediaType.APPLICATION_JSON).content(JSON_CLIENTE_VALIDO)).andExpect(status().isForbidden());
        verifyNoInteractions(clienteService);
    }

    @Test
    void deveImpedirExclusaoSemCsrf() throws Exception {
        mockMvc.perform(delete("/api/clientes/1").with(user("gerente").roles("GERENTE"))).andExpect(status().isForbidden());
        verifyNoInteractions(clienteService);
    }

    @Test
    void deveImpedirCadastroComCsrfInvalido() throws Exception {
        mockMvc.perform(post("/api/clientes").with(user("atendente").roles("ATENDENTE")).with(csrf().useInvalidToken()).contentType(MediaType.APPLICATION_JSON).content(JSON_CLIENTE_VALIDO)).andExpect(status().isForbidden());
        verifyNoInteractions(clienteService);
    }
}
