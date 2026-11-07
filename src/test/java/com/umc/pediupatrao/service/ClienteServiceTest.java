package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.Log;
import com.umc.pediupatrao.repository.ClienteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {
    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private LogService logService;

    @InjectMocks
    private ClienteService clienteService;

    @Captor
    private ArgumentCaptor<List<Log.AlteracaoCampo>> alteracoesCaptor;

    private Cliente criarCliente(String id, String nome) {
        Cliente cliente = new Cliente();
        cliente.setId(id);
        cliente.setNome(nome);
        return cliente;
    }

    private Cliente criarClienteCompleto(String id, String prefixo) {
        Cliente cliente = criarCliente(id, prefixo + "-nome");
        cliente.setTelefone(prefixo + "-telefone");
        cliente.setCep(prefixo + "-cep");
        cliente.setLogradouro(prefixo + "-logradouro");
        cliente.setNumero(prefixo + "-numero");
        cliente.setComplemento(prefixo + "-complemento");
        cliente.setBairro(prefixo + "-bairro");
        cliente.setCidade(prefixo + "-cidade");
        cliente.setEstado(prefixo + "-estado");
        cliente.setEndereco(prefixo + "-endereco");

        return cliente;
    }

    private void verificarAlteracao(
            Log.AlteracaoCampo alteracao,
            String campo,
            String valorAnterior,
            String valorNovo) {

        assertEquals(campo, alteracao.getCampo());
        assertEquals(valorAnterior, alteracao.getValorAnterior());
        assertEquals(valorNovo, alteracao.getValorNovo());
    }

    @Test
    void deveCadastrarNovoCliente() {
        Cliente cliente = criarCliente(null, "Maria");
        when(clienteRepository.save(cliente)).thenReturn(cliente);

        Cliente resultado = clienteService.novoCliente(cliente);

        assertSame(cliente, resultado);
        verify(clienteRepository).save(cliente);
    }

    @Test
    void deveInformarFalhaAoCadastrarCliente() {
        Cliente cliente = criarCliente(null, "Maria");
        IllegalStateException falha = new IllegalStateException("Falha ao salvar cliente");
        when(clienteRepository.save(cliente)).thenThrow(falha);
        IllegalStateException erro = assertThrows(IllegalStateException.class, () -> clienteService.novoCliente(cliente));
        assertSame(falha, erro);
    }

    @Test
    void deveListarClientesCadastrados() {
        Cliente maria = criarCliente("1", "Maria");
        Cliente joao = criarCliente("2", "João");

        when(clienteRepository.findAll()).thenReturn(List.of(maria, joao));
        List<Cliente> resultado = clienteService.listarClientes();

        assertEquals(2, resultado.size());
        assertEquals(List.of(maria, joao), resultado);
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHaClientes() {
        when(clienteRepository.findAll()).thenReturn(List.of());
        List<Cliente> resultado = clienteService.listarClientes();
        assertTrue(resultado.isEmpty());
    }

    @Test
    void deveExcluirClientePorId() {
        clienteService.excluir("1");
        verify(clienteRepository).deleteById("1");
    }

    @Test
    void deveInformarFalhaAoExcluirCliente() {
        IllegalStateException falha = new IllegalStateException("Falha ao excluir cliente");
        doThrow(falha).when(clienteRepository).deleteById("1");
        IllegalStateException erro = assertThrows(IllegalStateException.class, () -> clienteService.excluir("1"));
        assertSame(falha, erro);
    }

    @Test
    void deveBuscarClienteExistentePorId() {
        Cliente cliente = criarCliente("1", "Maria");

        when(clienteRepository.findById("1")).thenReturn(Optional.of(cliente));
        Optional<Cliente> resultado = clienteService.buscarPorId("1");

        assertTrue(resultado.isPresent());
        assertSame(cliente, resultado.orElseThrow());

        verify(clienteRepository).findById("1");
    }

    @Test
    void deveRetornarVazioAoBuscarClienteInexistente() {
        when(clienteRepository.findById("99")).thenReturn(Optional.empty());
        Optional<Cliente> resultado = clienteService.buscarPorId("99");
        assertTrue(resultado.isEmpty());
    }

    @Test
    void deveCadastrarClienteERegistrarAuditoria() {
        Cliente novo = criarCliente(null, "Maria");
        Cliente salvo = criarCliente("10", "Maria");
        Authentication autenticacao = mock(Authentication.class);

        when(clienteRepository.save(novo)).thenReturn(salvo);
        Cliente resultado = clienteService.salvar(novo, autenticacao);
        assertSame(salvo, resultado);

        verify(clienteRepository).save(novo);
        verify(logService).registrar(same(autenticacao), eq("CRIACAO_CLIENTE"), eq("Cliente"), eq("10"), alteracoesCaptor.capture(), isNull());

        List<Log.AlteracaoCampo> alteracoes = alteracoesCaptor.getValue();
        assertEquals(1, alteracoes.size());
        verificarAlteracao(alteracoes.get(0), "nome", null, "Maria");
    }

    @Test
    void naoDeveRegistrarAuditoriaQuandoCadastroFalha() {
        Cliente novo = criarCliente(null, "Maria");

        Authentication autenticacao = mock(Authentication.class);
        IllegalStateException falha = new IllegalStateException("Falha ao salvar");

        when(clienteRepository.save(novo)).thenThrow(falha);
        IllegalStateException erro = assertThrows(IllegalStateException.class, () -> clienteService.salvar(novo, autenticacao));

        assertSame(falha, erro);
        verifyNoInteractions(logService);
    }

    @Test
    void deveInformarFalhaNaAuditoriaDoCadastro() {
        Cliente novo = criarCliente(null, "Maria");
        Cliente salvo = criarCliente("10", "Maria");

        Authentication autenticacao = mock(Authentication.class);
        IllegalStateException falha = new IllegalStateException("Falha na auditoria");

        when(clienteRepository.save(novo)).thenReturn(salvo);
        when(logService.registrar(same(autenticacao), eq("CRIACAO_CLIENTE"), eq("Cliente"), eq("10"), anyList(), isNull())).thenThrow(falha);

        IllegalStateException erro = assertThrows(IllegalStateException.class, () -> clienteService.salvar(novo, autenticacao));
        assertSame(falha, erro);
        verify(clienteRepository).save(novo);
    }

    @Test
    void deveAtualizarNomeERegistrarAlteracao() {
        Cliente anterior = criarCliente("1", "Maria");
        Cliente atualizado = criarCliente("1", "Maria Silva");

        Authentication autenticacao = mock(Authentication.class);

        when(clienteRepository.findById("1")).thenReturn(Optional.of(anterior));
        when(clienteRepository.save(atualizado)).thenReturn(atualizado);

        Cliente resultado = clienteService.salvar(atualizado, autenticacao);
        assertSame(atualizado, resultado);

        verify(logService).registrar(same(autenticacao), eq("ALTERACAO_CLIENTE"), eq("Cliente"), eq("1"), alteracoesCaptor.capture(), isNull());

        List<Log.AlteracaoCampo> alteracoes = alteracoesCaptor.getValue();
        assertEquals(1, alteracoes.size());

        verificarAlteracao(alteracoes.get(0), "nome", "Maria", "Maria Silva");
    }

    @Test
    void deveRegistrarTodosOsCamposModificados() {
        Cliente anterior = criarClienteCompleto("1", "antes");
        Cliente atualizado = criarClienteCompleto("1", "depois");

        Authentication autenticacao = mock(Authentication.class);

        when(clienteRepository.findById("1")).thenReturn(Optional.of(anterior));
        when(clienteRepository.save(atualizado)).thenReturn(atualizado);

        clienteService.salvar(atualizado, autenticacao);
        verify(logService).registrar(same(autenticacao), eq("ALTERACAO_CLIENTE"), eq("Cliente"), eq("1"), alteracoesCaptor.capture(), isNull());

        List<Log.AlteracaoCampo> alteracoes = alteracoesCaptor.getValue();

        String[] campos = {
                "nome",
                "telefone",
                "cep",
                "logradouro",
                "numero",
                "complemento",
                "bairro",
                "cidade",
                "estado",
                "endereco"
        };

        assertEquals(campos.length, alteracoes.size());

        for (int i = 0; i < campos.length; i++) {
            verificarAlteracao(alteracoes.get(i), campos[i], "antes-" + campos[i], "depois-" + campos[i]);
        }
    }

    @Test
    void naoDeveRegistrarAuditoriaQuandoNadaMudou() {
        Cliente anterior = criarClienteCompleto("1", "mesmos-dados");
        Cliente atualizado = criarClienteCompleto("1", "mesmos-dados");
        Authentication autenticacao = mock(Authentication.class);

        when(clienteRepository.findById("1")).thenReturn(Optional.of(anterior));
        when(clienteRepository.save(atualizado)).thenReturn(atualizado);

        Cliente resultado = clienteService.salvar(atualizado, autenticacao);
        assertSame(atualizado, resultado);

        verify(clienteRepository).save(atualizado);
        verifyNoInteractions(logService);
    }

    @Test
    void deveImpedirAtualizacaoDeClienteInexistente() {
        Cliente atualizado = criarCliente("99", "Maria");
        Authentication autenticacao = mock(Authentication.class);

        when(clienteRepository.findById("99")).thenReturn(Optional.empty());
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> clienteService.salvar(atualizado, autenticacao));

        assertEquals("Cliente não encontrado para atualização.", erro.getMessage());
        verify(clienteRepository, never()).save(any(Cliente.class));
        verifyNoInteractions(logService);
    }

    @Test
    void naoDeveRegistrarAuditoriaQuandoAtualizacaoFalha() {
        Cliente anterior = criarCliente("1", "Maria");
        Cliente atualizado = criarCliente("1", "Maria Silva");

        Authentication autenticacao = mock(Authentication.class);
        IllegalStateException falha = new IllegalStateException("Falha ao atualizar");

        when(clienteRepository.findById("1")).thenReturn(Optional.of(anterior));
        when(clienteRepository.save(atualizado)).thenThrow(falha);

        IllegalStateException erro = assertThrows(IllegalStateException.class, () -> clienteService.salvar(atualizado, autenticacao));

        assertSame(falha, erro);
        verifyNoInteractions(logService);
    }

    @Test
    void deveInformarFalhaNaAuditoriaDaAtualizacao() {
        Cliente anterior = criarCliente("1", "Maria");
        Cliente atualizado = criarCliente("1", "Maria Silva");

        Authentication autenticacao = mock(Authentication.class);
        IllegalStateException falha = new IllegalStateException("Falha na auditoria");

        when(clienteRepository.findById("1")).thenReturn(Optional.of(anterior));
        when(clienteRepository.save(atualizado)).thenReturn(atualizado);
        when(logService.registrar(same(autenticacao), eq("ALTERACAO_CLIENTE"), eq("Cliente"), eq("1"), anyList(), isNull())).thenThrow(falha);

        IllegalStateException erro = assertThrows(IllegalStateException.class, () -> clienteService.salvar(atualizado, autenticacao));
        assertSame(falha, erro);
        verify(clienteRepository).save(atualizado);
    }

    @Test
    void deveRegistrarMudancaDeNuloParaValor() {
        Cliente anterior = criarCliente("1", "Maria");
        Cliente atualizado = criarCliente("1", "Maria");

        atualizado.setTelefone("11999999999");
        Authentication autenticacao = mock(Authentication.class);

        when(clienteRepository.findById("1")).thenReturn(Optional.of(anterior));
        when(clienteRepository.save(atualizado)).thenReturn(atualizado);
        clienteService.salvar(atualizado, autenticacao);

        verify(logService).registrar(same(autenticacao), eq("ALTERACAO_CLIENTE"), eq("Cliente"), eq("1"), alteracoesCaptor.capture(), isNull());

        List<Log.AlteracaoCampo> alteracoes = alteracoesCaptor.getValue();

        assertEquals(1, alteracoes.size());
        verificarAlteracao(alteracoes.get(0), "telefone", null, "11999999999");
    }

    @Test
    void deveRegistrarMudancaDeValorParaNulo() {
        Cliente anterior = criarCliente("1", "Maria");
        anterior.setTelefone("11999999999");

        Cliente atualizado = criarCliente("1", "Maria");
        Authentication autenticacao = mock(Authentication.class);

        when(clienteRepository.findById("1")).thenReturn(Optional.of(anterior));
        when(clienteRepository.save(atualizado)).thenReturn(atualizado);

        clienteService.salvar(atualizado, autenticacao);

        verify(logService).registrar(same(autenticacao), eq("ALTERACAO_CLIENTE"), eq("Cliente"), eq("1"), alteracoesCaptor.capture(), isNull());
        List<Log.AlteracaoCampo> alteracoes = alteracoesCaptor.getValue();

        assertEquals(1, alteracoes.size());
        verificarAlteracao(alteracoes.get(0), "telefone", "11999999999", null);
    }
}
