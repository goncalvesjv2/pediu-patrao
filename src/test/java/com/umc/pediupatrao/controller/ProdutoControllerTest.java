package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.service.ProdutoService;
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
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProdutoController.class)
@Import(SecurityConfig.class)
class ProdutoControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProdutoService produtoService;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    private static final String JSON_PRODUTO_VALIDO = """
            {
                "nome": "Pizza Calabresa",
                "tipo": "Pizza",
                "preco": 49.90,
                "ativo": true
            }
            """;

    private Produto criarProduto(String id, String nome, String tipo, Double preco, Boolean ativo) {
        Produto produto = new Produto();
        produto.setId(id);
        produto.setNome(nome);
        produto.setTipo(tipo);
        produto.setPreco(preco);
        produto.setAtivo(ativo);
        return produto;
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "GERENTE", "ATENDENTE"})
    void deveListarProdutosParaPerfisAutorizados(String perfil) throws Exception {
        Produto produto1 = criarProduto("1", "Pizza Calabresa", "Pizza", 49.90, true);
        Produto produto2 = criarProduto("2", "Refrigerante", "Bebida", 8.00, true);

        when(produtoService.listarProdutos()).thenReturn(List.of(produto1, produto2));

        mockMvc.perform(get("/api/produtos").with(user("teste").roles(perfil)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[0].nome").value("Pizza Calabresa"))
                .andExpect(jsonPath("$[0].preco").value(49.90))
                .andExpect(jsonPath("$[1].id").value("2"))
                .andExpect(jsonPath("$[1].nome").value("Refrigerante"));

        verify(produtoService).listarProdutos();
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoExistemProdutos() throws Exception {
        when(produtoService.listarProdutos()).thenReturn(List.of());
        mockMvc.perform(get("/api/produtos").with(user("gerente").roles("GERENTE"))).andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(produtoService).listarProdutos();
    }

    @Test
    void devePermitirGerenteCadastrarProduto() throws Exception {
        Produto produtoSalvo = criarProduto("1", "Pizza Calabresa", "Pizza", 49.90, true);
        when(produtoService.salvar(any(Produto.class))).thenReturn(produtoSalvo);
        mockMvc.perform(post("/api/produtos").with(user("gerente").roles("GERENTE")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(JSON_PRODUTO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.nome").value("Pizza Calabresa"))
                .andExpect(jsonPath("$.tipo").value("Pizza"))
                .andExpect(jsonPath("$.preco").value(49.90))
                .andExpect(jsonPath("$.ativo").value(true));

        verify(produtoService).salvar(argThat(produto -> produto.getId() == null && "Pizza Calabresa".equals(produto.getNome()) && "Pizza".equals(produto.getTipo()) && Double.valueOf(49.90).equals(produto.getPreco()) && Boolean.TRUE.equals(produto.getAtivo())));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "ATENDENTE"})
    void deveImpedirOutrosPerfisDeCadastrarProduto(String perfil) throws Exception {
        mockMvc.perform(post("/api/produtos").with(user("teste").roles(perfil)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(JSON_PRODUTO_VALIDO)).andExpect(status().isForbidden());
        verifyNoInteractions(produtoService);
    }

    @Test
    void deveRetornar400QuandoJsonDoCadastroForInvalido() throws Exception {
        mockMvc.perform(post("/api/produtos").with(user("gerente").roles("GERENTE")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"nome\":")).andExpect(status().isBadRequest());
        verifyNoInteractions(produtoService);
    }

    @Test
    void deveRetornar400QuandoCadastroNaoPossuiBody() throws Exception {
        mockMvc.perform(post("/api/produtos").with(user("gerente").roles("GERENTE")).with(csrf()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
        verifyNoInteractions(produtoService);
    }

    @Test
    void deveRetornar400QuandoPrecoNaoForNumerico() throws Exception {
        mockMvc.perform(post("/api/produtos").with(user("gerente").roles("GERENTE")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "nome": "Pizza",
                                    "tipo": "Pizza",
                                    "preco": "valorInvalido",
                                    "ativo": true
                                }
                        """)
                )
                .andExpect(status().isBadRequest());
        verifyNoInteractions(produtoService);
    }

    @Test
    void deveRejeitarCadastroComTipoDeConteudoInvalido() throws Exception {
        mockMvc.perform(post("/api/produtos").with(user("gerente").roles("GERENTE")).with(csrf()).contentType(MediaType.TEXT_PLAIN).content("nome=Pizza")).andExpect(status().isUnsupportedMediaType());
        verifyNoInteractions(produtoService);
    }

    @Test
    void devePermitirGerenteAtualizarProduto() throws Exception {
        when(produtoService.salvar(any(Produto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        mockMvc.perform(put("/api/produtos/1").with(user("gerente").roles("GERENTE")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "nome": "Pizza Atualizada",
                                    "tipo": "Pizza",
                                    "preco": 59.90,
                                    "ativo": true
                                }
                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.nome").value("Pizza Atualizada"))
                .andExpect(jsonPath("$.preco").value(59.90));

        verify(produtoService).salvar(argThat(produto -> "1".equals(produto.getId()) && "Pizza Atualizada".equals(produto.getNome()))
        );
    }

    @Test
    void deveUtilizarIdDaUrlAoAtualizarProduto() throws Exception {
        when(produtoService.salvar(any(Produto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        mockMvc.perform(put("/api/produtos/1").with(user("gerente").roles("GERENTE")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "id": "999",
                                    "nome": "Pizza",
                                    "tipo": "Pizza",
                                    "preco": 39.90,
                                    "ativo": true
                                }
                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"));

        verify(produtoService).salvar(argThat(produto -> "1".equals(produto.getId())));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "ATENDENTE"})
    void deveImpedirOutrosPerfisDeAtualizarProduto(String perfil) throws Exception {
        mockMvc.perform(put("/api/produtos/1").with(user("teste").roles(perfil)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(JSON_PRODUTO_VALIDO)).andExpect(status().isForbidden());
        verifyNoInteractions(produtoService);
    }

    @Test
    void deveRetornar400QuandoAtualizacaoNaoPossuiBody() throws Exception {
        mockMvc.perform(put("/api/produtos/1").with(user("gerente").roles("GERENTE")).with(csrf()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
        verifyNoInteractions(produtoService);
    }

    @Test
    void deveRetornar400QuandoJsonDaAtualizacaoForInvalido() throws Exception {
        mockMvc.perform(put("/api/produtos/1").with(user("gerente").roles("GERENTE")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"nome\":")).andExpect(status().isBadRequest());
        verifyNoInteractions(produtoService);
    }

    @Test
    void devePermitirGerenteExcluirProduto() throws Exception {
        mockMvc.perform(delete("/api/produtos/1").with(user("gerente").roles("GERENTE")).with(csrf())).andExpect(status().isNoContent());
        verify(produtoService).excluir("1");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "ATENDENTE"})
    void deveImpedirOutrosPerfisDeExcluirProduto(String perfil) throws Exception {
        mockMvc.perform(delete("/api/produtos/1").with(user("teste").roles(perfil)).with(csrf())).andExpect(status().isForbidden());
        verifyNoInteractions(produtoService);
    }

    @Test
    void deveRedirecionarUsuarioNaoAutenticado() throws Exception {
        mockMvc.perform(get("/api/produtos")).andExpect(status().is3xxRedirection());
        verifyNoInteractions(produtoService);
    }

    @Test
    void deveImpedirCadastroSemCsrf() throws Exception {
        mockMvc.perform(post("/api/produtos").with(user("gerente").roles("GERENTE")).contentType(MediaType.APPLICATION_JSON).content(JSON_PRODUTO_VALIDO)).andExpect(status().isForbidden());
        verifyNoInteractions(produtoService);
    }

    @Test
    void deveImpedirAtualizacaoSemCsrf() throws Exception {
        mockMvc.perform(put("/api/produtos/1").with(user("gerente").roles("GERENTE")).contentType(MediaType.APPLICATION_JSON).content(JSON_PRODUTO_VALIDO)).andExpect(status().isForbidden());
        verifyNoInteractions(produtoService);
    }

    @Test
    void deveImpedirExclusaoSemCsrf() throws Exception {
        mockMvc.perform(delete("/api/produtos/1").with(user("gerente").roles("GERENTE"))).andExpect(status().isForbidden());
        verifyNoInteractions(produtoService);
    }

    @Test
    void deveImpedirCadastroComCsrfInvalido() throws Exception {
        mockMvc.perform(post("/api/produtos").with(user("gerente").roles("GERENTE")).with(csrf().useInvalidToken()).contentType(MediaType.APPLICATION_JSON).content(JSON_PRODUTO_VALIDO)).andExpect(status().isForbidden());
        verifyNoInteractions(produtoService);
    }
}
