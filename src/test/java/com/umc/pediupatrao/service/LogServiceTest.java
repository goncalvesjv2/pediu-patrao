package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Log;
import com.umc.pediupatrao.repository.LogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogServiceTest {
    @Mock
    private LogRepository logRepository;

    @InjectMocks
    private LogService logService;

    @Captor
    private ArgumentCaptor<Log> logCaptor;

    private Authentication criarAutenticacao(String username, String... autoridades) {
        List<SimpleGrantedAuthority> permissoes = Arrays.stream(autoridades).map(SimpleGrantedAuthority::new).toList();
        return new UsernamePasswordAuthenticationToken(username, "senha", permissoes);
    }

    private Log registrarComSucesso(
            Authentication autenticacao,
            String operacao,
            String entidade,
            String entidadeId,
            List<Log.AlteracaoCampo> alteracoes,
            String justificativa) {

        when(logRepository.save(any(Log.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        return logService.registrar(
                autenticacao,
                operacao,
                entidade,
                entidadeId,
                alteracoes,
                justificativa
        );
    }

    @Test
    void deveRegistrarLogComUsuarioAutenticado() {
        Authentication autenticacao = criarAutenticacao("joao", "ROLE_ADMIN");

        Log resultado = registrarComSucesso(
                autenticacao,
                "CRIACAO_CLIENTE",
                "Cliente",
                "10",
                List.of(),
                null
        );

        assertNotNull(resultado);
        assertEquals("joao", resultado.getUsuario());
        verify(logRepository).save(logCaptor.capture());
        assertSame(resultado, logCaptor.getValue());
    }

    @Test
    void deveImpedirRegistroSemAuthentication() {
        SecurityException erro = assertThrows(SecurityException.class, () -> logService.registrar(null, "CRIACAO_CLIENTE", "Cliente", "10", List.of(), null));
        assertEquals("É necessário identificar o usuário para registrar a auditoria.", erro.getMessage());
        verifyNoInteractions(logRepository);
    }

    @Test
    void deveImpedirRegistroComUsuarioNaoAutenticado() {
        Authentication autenticacao = new UsernamePasswordAuthenticationToken("joao", "senha");
        assertFalse(autenticacao.isAuthenticated());

        SecurityException erro = assertThrows(SecurityException.class, () -> logService.registrar(autenticacao, "CRIACAO_CLIENTE", "Cliente", "10", List.of(), null));

        assertEquals("É necessário identificar o usuário para registrar a auditoria.", erro.getMessage());
        verifyNoInteractions(logRepository);
    }

    @Test
    void deveRegistrarPerfilAdmin() {
        Authentication autenticacao = criarAutenticacao("joao", "ROLE_ADMIN");
        Log resultado = registrarComSucesso(autenticacao, "LOGIN", "Usuario", "1", List.of(), null);
        assertEquals("ADMIN", resultado.getPerfil());
    }

    @Test
    void deveRegistrarPerfilGerente() {
        Authentication autenticacao = criarAutenticacao("maria", "ROLE_GERENTE");
        Log resultado = registrarComSucesso(autenticacao, "ALTERACAO_PRODUTO", "Produto", "2", List.of(), null);
        assertEquals("GERENTE", resultado.getPerfil());
    }

    @Test
    void deveRegistrarPerfilAtendente() {
        Authentication autenticacao = criarAutenticacao("pedro", "ROLE_ATENDENTE");

        Log resultado = registrarComSucesso(
                autenticacao,
                "CRIACAO_CLIENTE",
                "Cliente",
                "3",
                List.of(),
                null
        );

        assertEquals("ATENDENTE", resultado.getPerfil());
    }

    @Test
    void deveRegistrarPerfilNaoIdentificado() {
        Authentication autenticacao = criarAutenticacao("joao", "SCOPE_read");

        Log resultado = registrarComSucesso(
                autenticacao,
                "CONSULTA",
                "Cliente",
                "1",
                List.of(),
                null
        );

        assertEquals("NAO_IDENTIFICADO", resultado.getPerfil());
    }

    @Test
    void deveRegistrarDataHora() {
        Authentication autenticacao = criarAutenticacao("joao", "ROLE_ADMIN");
        LocalDateTime inicio = LocalDateTime.now();

        Log resultado = registrarComSucesso(
                autenticacao,
                "CRIACAO_CLIENTE",
                "Cliente",
                "10",
                List.of(),
                null
        );

        LocalDateTime fim = LocalDateTime.now();

        assertNotNull(resultado.getDataHora());
        assertFalse(resultado.getDataHora().isBefore(inicio));
        assertFalse(resultado.getDataHora().isAfter(fim));
    }

    @Test
    void deveRegistrarOperacaoEntidadeEId() {
        Authentication autenticacao = criarAutenticacao("joao", "ROLE_ADMIN");

        Log resultado = registrarComSucesso(
                autenticacao,
                "ALTERACAO_CLIENTE",
                "Cliente",
                "25",
                List.of(),
                null
        );

        assertEquals("ALTERACAO_CLIENTE", resultado.getOperacao());
        assertEquals("Cliente", resultado.getEntidade());
        assertEquals("25", resultado.getEntidadeId());
    }

    @Test
    void deveRegistrarCamposAlterados() {
        Authentication autenticacao = criarAutenticacao("joao", "ROLE_ADMIN");

        Log.AlteracaoCampo alteracao = new Log.AlteracaoCampo("nome", "Maria", "Maria Silva");
        List<Log.AlteracaoCampo> alteracoes = List.of(alteracao);

        Log resultado = registrarComSucesso(
                autenticacao,
                "ALTERACAO_CLIENTE",
                "Cliente",
                "10",
                alteracoes,
                null
        );

        assertEquals(1, resultado.getAlteracoes().size());
        Log.AlteracaoCampo campo = resultado.getAlteracoes().get(0);

        assertEquals("nome", campo.getCampo());
        assertEquals("Maria", campo.getValorAnterior());
        assertEquals("Maria Silva", campo.getValorNovo());
    }

    @Test
    void deveUtilizarListaVaziaQuandoAlteracoesForemNulas() {
        Authentication autenticacao = criarAutenticacao("joao", "ROLE_ADMIN");

        Log resultado = registrarComSucesso(
                autenticacao,
                "CRIACAO_CLIENTE",
                "Cliente",
                "10",
                null,
                null
        );

        assertNotNull(resultado.getAlteracoes());
        assertTrue(resultado.getAlteracoes().isEmpty());
    }

    @Test
    void deveRegistrarJustificativa() {
        Authentication autenticacao = criarAutenticacao("maria", "ROLE_GERENTE");

        Log resultado = registrarComSucesso(
                autenticacao,
                "CANCELAMENTO_PEDIDO",
                "Pedido",
                "5",
                List.of(),
                "Solicitação do cliente"
        );

        assertEquals("Solicitação do cliente", resultado.getJustificativa());
    }

    @Test
    void deveInformarFalhaAoSalvarLog() {
        Authentication autenticacao = criarAutenticacao("joao", "ROLE_ADMIN");
        IllegalStateException falha = new IllegalStateException("Falha ao salvar log");
        when(logRepository.save(any(Log.class))).thenThrow(falha);
        IllegalStateException erro = assertThrows(IllegalStateException.class, () -> logService.registrar(autenticacao, "CRIACAO_CLIENTE", "Cliente", "10", List.of(), null));
        assertSame(falha, erro);
        verify(logRepository).save(any(Log.class));
    }
}
