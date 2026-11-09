package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.service.UsuarioDetailsService;
import com.umc.pediupatrao.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UsuarioController.class)
@Import(SecurityConfig.class)
class UsuarioControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    private static final String JSON_USUARIO_VALIDO = """
            {
                "username": "maria",
                "password": "senha12345",
                "role": "ATENDENTE"
            }
            """;

    private Usuario criarUsuario(String id, String username, String password, Perfil perfil) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setUsername(username);
        usuario.setPassword(password);
        usuario.setRole(perfil);
        return usuario;
    }

    @Test
    void deveListarUsuariosComoAdmin() throws Exception {
        Usuario usuario1 = criarUsuario("1", "joao", "hash1", Perfil.ADMIN);
        Usuario usuario2 = criarUsuario("2", "maria", "hash2", Perfil.ATENDENTE);
        when(usuarioService.listarTodos()).thenReturn(List.of(usuario1, usuario2));
        mockMvc.perform(get("/api/usuarios").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("joao"))
                .andExpect(jsonPath("$[1].username").value("maria"))
                .andExpect(jsonPath("$[0].password").doesNotExist());
        verify(usuarioService).listarTodos();
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoExistemUsuarios() throws Exception {
        when(usuarioService.listarTodos()).thenReturn(List.of());
        mockMvc.perform(get("/api/usuarios").with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(usuarioService).listarTodos();
    }

    @Test
    void deveBuscarUsuarioExistentePorId() throws Exception {
        Usuario usuario = criarUsuario("1", "joao", "hash123", Perfil.ADMIN);
        when(usuarioService.buscarPorId("1")).thenReturn(Optional.of(usuario));
        mockMvc.perform(get("/api/usuarios/1").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.username").value("joao"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.password").doesNotExist());
        verify(usuarioService).buscarPorId("1");
    }

    @Test
    void deveRetornar404QuandoUsuarioNaoExiste() throws Exception {
        when(usuarioService.buscarPorId("99")).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/usuarios/99").with(user("admin").roles("ADMIN"))).andExpect(status().isNotFound());
        verify(usuarioService).buscarPorId("99");
    }

    @Test
    void deveCadastrarUsuarioComoAdmin() throws Exception {
        Usuario criado = criarUsuario("2", "maria", "hash123", Perfil.ATENDENTE);
        when(usuarioService.salvarUsuario(any(Usuario.class))).thenReturn(criado);
        mockMvc.perform(post("/api/usuarios").with(user("admin").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(JSON_USUARIO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("2"))
                .andExpect(jsonPath("$.username").value("maria"))
                .andExpect(jsonPath("$.role").value("ATENDENTE"))
                .andExpect(jsonPath("$.password").doesNotExist());

        verify(usuarioService).salvarUsuario(argThat(usuario -> "maria".equals(usuario.getUsername()) && "senha12345".equals(usuario.getPassword()) && usuario.getRole() == Perfil.ATENDENTE));
    }

    @Test
    void deveRetornar400QuandoCadastroForRejeitado() throws Exception {
        when(usuarioService.salvarUsuario(any(Usuario.class))).thenThrow(new IllegalArgumentException("O username é obrigatório."));
        mockMvc.perform(post("/api/usuarios").with(user("admin").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(JSON_USUARIO_VALIDO)).andExpect(status().isBadRequest()).andExpect(content().string("O username é obrigatório."));
        verify(usuarioService).salvarUsuario(any(Usuario.class));
    }

    @Test
    void deveRetornar400QuandoUsernameJaExiste() throws Exception {
        when(usuarioService.salvarUsuario(any(Usuario.class))).thenThrow(new IllegalArgumentException("Já existe um usuário com esse username."));
        mockMvc.perform(post("/api/usuarios").with(user("admin").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(JSON_USUARIO_VALIDO)).andExpect(status().isBadRequest());
        verify(usuarioService).salvarUsuario(any(Usuario.class));
    }

    @Test
    void deveRetornar400QuandoJsonForInvalido() throws Exception {
        mockMvc.perform(post("/api/usuarios").with(user("admin").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"username\":")).andExpect(status().isBadRequest());
        verifyNoInteractions(usuarioService);
    }

    @Test
    void deveRetornar400QuandoBodyEstiverAusente() throws Exception {
        mockMvc.perform(post("/api/usuarios").with(user("admin").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
        verifyNoInteractions(usuarioService);
    }

    @Test
    void deveAtualizarUsuarioComoAdmin() throws Exception {
        Usuario atualizado = criarUsuario("1", "maria", "hashNovo", Perfil.ATENDENTE);
        when(usuarioService.atualizarUsuario(eq("1"), any(Usuario.class), eq("admin"))).thenReturn(atualizado);
        mockMvc.perform(put("/api/usuarios/1").with(user("admin").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(JSON_USUARIO_VALIDO)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value("1")).andExpect(jsonPath("$.username").value("maria")).andExpect(jsonPath("$.role").value("ATENDENTE"));
        verify(usuarioService).atualizarUsuario(eq("1"), any(Usuario.class), eq("admin"));
    }

    @Test
    void deveRetornar400QuandoUsuarioNaoExisteParaAtualizacao() throws Exception {
        when(usuarioService.atualizarUsuario(eq("99"), any(Usuario.class), eq("admin"))).thenThrow(new IllegalArgumentException("Usuário não encontrado."));
        mockMvc.perform(put("/api/usuarios/99").with(user("admin").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(JSON_USUARIO_VALIDO)).andExpect(status().isBadRequest()).andExpect(content().string("Usuário não encontrado."));
    }

    @Test
    void deveRetornar400QuandoAdminTentaAlterarProprioPerfil() throws Exception {
        when(usuarioService.atualizarUsuario(eq("1"), any(Usuario.class), eq("admin"))).thenThrow(new IllegalArgumentException("Você não pode alterar o seu próprio perfil."));
        mockMvc.perform(put("/api/usuarios/1").with(user("admin").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "username": "admin",
                                "role": "GERENTE"
                            }
                            """)).andExpect(status().isBadRequest()).andExpect(content().string("Você não pode alterar o seu próprio perfil."));
    }

    @Test
    void deveExcluirOutroUsuarioComoAdmin() throws Exception {
        mockMvc.perform(delete("/api/usuarios/2").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isNoContent());
        verify(usuarioService).deletarUsuario("2", "admin");
    }

    @Test
    void deveRetornar400QuandoAdminTentaExcluirPropriaConta() throws Exception {
        doThrow(new IllegalArgumentException("Você não pode excluir o seu próprio usuário.")).when(usuarioService).deletarUsuario("1", "admin");
        mockMvc.perform(delete("/api/usuarios/1").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isBadRequest()).andExpect(content().string("Você não pode excluir o seu próprio usuário."));
        verify(usuarioService).deletarUsuario("1", "admin");
    }

    @Test
    void deveRetornar400QuandoUsuarioNaoExisteParaExclusao() throws Exception {
        doThrow(new IllegalArgumentException("Usuário não encontrado.")).when(usuarioService).deletarUsuario("99", "admin");
        mockMvc.perform(delete("/api/usuarios/99").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isBadRequest()).andExpect(content().string("Usuário não encontrado."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"GERENTE", "ATENDENTE"})
    void deveImpedirOutrosPerfisDeListarUsuarios(String perfil) throws Exception {
        mockMvc.perform(get("/api/usuarios").with(user("teste").roles(perfil))).andExpect(status().isForbidden());
        verifyNoInteractions(usuarioService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"GERENTE", "ATENDENTE"})
    void deveImpedirOutrosPerfisDeCadastrarUsuario(String perfil) throws Exception {
        mockMvc.perform(post("/api/usuarios").with(user("teste").roles(perfil)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(JSON_USUARIO_VALIDO)).andExpect(status().isForbidden());
        verifyNoInteractions(usuarioService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"GERENTE", "ATENDENTE"})
    void deveImpedirOutrosPerfisDeAtualizarUsuario(String perfil) throws Exception {
        mockMvc.perform(put("/api/usuarios/1").with(user("teste").roles(perfil)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(JSON_USUARIO_VALIDO)).andExpect(status().isForbidden());
        verifyNoInteractions(usuarioService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"GERENTE", "ATENDENTE"})
    void deveImpedirOutrosPerfisDeExcluirUsuario(String perfil) throws Exception {
        mockMvc.perform(delete("/api/usuarios/1").with(user("teste").roles(perfil)).with(csrf())).andExpect(status().isForbidden());
        verifyNoInteractions(usuarioService);
    }

    @Test
    void deveRedirecionarUsuarioNaoAutenticado() throws Exception {
        mockMvc.perform(get("/api/usuarios")).andExpect(status().is3xxRedirection());
        verifyNoInteractions(usuarioService);
    }

    @Test
    void deveImpedirCadastroSemCsrf() throws Exception {
        mockMvc.perform(post("/api/usuarios").with(user("admin").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(JSON_USUARIO_VALIDO)).andExpect(status().isForbidden());
        verifyNoInteractions(usuarioService);
    }

    @Test
    void deveImpedirAtualizacaoComCsrfInvalido() throws Exception {
        mockMvc.perform(put("/api/usuarios/1").with(user("admin").roles("ADMIN")).with(csrf().useInvalidToken()).contentType(MediaType.APPLICATION_JSON).content(JSON_USUARIO_VALIDO)).andExpect(status().isForbidden());
        verifyNoInteractions(usuarioService);
    }

    @Test
    void deveImpedirExclusaoSemCsrf() throws Exception {
        mockMvc.perform(delete("/api/usuarios/1").with(user("admin").roles("ADMIN"))).andExpect(status().isForbidden());
        verifyNoInteractions(usuarioService);
    }
}