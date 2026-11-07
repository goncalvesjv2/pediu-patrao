package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.ItemPedidoDTO;
import com.umc.pediupatrao.dto.PedidoRequestDTO;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Log;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {
    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private LogService logService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private PedidoService pedidoService;

    @Captor
    private ArgumentCaptor<List<Log.AlteracaoCampo>> alteracoesCaptor;

    @BeforeEach
    void configurarAutenticacao() {
        lenient().when(authentication.isAuthenticated()).thenReturn(true);
        lenient().when(authentication.getName()).thenReturn("usuarioTeste");
    }

    private PedidoRequestDTO criarDtoValido() {
        PedidoRequestDTO dto = new PedidoRequestDTO();
        dto.setClienteId("cliente1");

        ItemPedidoDTO item = new ItemPedidoDTO();
        item.setProdutoId("produto1");
        item.setQuantidade(2);

        dto.setItens(List.of(item));

        return dto;
    }

    private void simularClienteExistente() {
        when(clienteRepository.findById("cliente1")).thenReturn(Optional.of(new Cliente()));
    }

    private Produto criarProdutoAtivo(String id, String nome, double preco) {
        Produto produto = new Produto();
        produto.setId(id);
        produto.setNome(nome);
        produto.setPreco(preco);
        produto.setAtivo(true);
        return produto;
    }

    private Pedido criarPedidoComStatus(StatusPedido status) {
        Pedido pedido = new Pedido();
        pedido.setId("pedido1");
        pedido.setStatus(status);
        return pedido;
    }

    private Pedido criarPedidoComItens() {
        Pedido pedido = criarPedidoComStatus(StatusPedido.RECEBIDO);
        pedido.setItens(List.of(new ItemPedido("produto1", "Pizza", 2, 50.0)));
        pedido.setValorTotal(100.0);
        return pedido;
    }

    private void simularGerente() {
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_GERENTE"))).when(authentication).getAuthorities();
    }

    private void simularAtendente() {
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_ATENDENTE"))).when(authentication).getAuthorities();
    }

    private void simularPedidoExistente(Pedido pedido) {
        when(pedidoRepository.findById("pedido1")).thenReturn(Optional.of(pedido));
    }

    @Test
    void criarPedidoComStatusHorarioEResponsavel() {
        PedidoRequestDTO dto = criarDtoValido();
        simularClienteExistente();
        Produto produto = criarProdutoAtivo("produto1", "Pizza", 30.0);
        when(produtoRepository.findById("produto1")).thenReturn(Optional.of(produto));
        when(authentication.getName()).thenReturn("atendente01");
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> {Pedido pedido = invocation.getArgument(0);pedido.setId("pedido1");return pedido;});

        Pedido resultado = pedidoService.criarPedido(dto, authentication);

        assertEquals(StatusPedido.RECEBIDO, resultado.getStatus());
        assertEquals("atendente01", resultado.getResponsavelId());
        assertEquals("cliente1", resultado.getClienteId());
        assertEquals(60.0, resultado.getValorTotal(), 0.001);
        assertNotNull(resultado.getHorarioEntrada());
        assertEquals(1, resultado.getItens().size());

        verify(pedidoRepository).save(any(Pedido.class));

        verify(logService).registrar(
                eq(authentication),
                eq("CRIACAO_PEDIDO"),
                eq("Pedido"),
                eq("pedido1"),
                anyList(),
                isNull()
        );
    }

    @Test
    void deveCriarPedidoComVariosItensERegistrarAuditoria() {
        PedidoRequestDTO dto = criarDtoValido();

        ItemPedidoDTO segundoItem = new ItemPedidoDTO();
        segundoItem.setProdutoId("produto2");
        segundoItem.setQuantidade(3);

        dto.setItens(List.of(dto.getItens().get(0), segundoItem));

        simularClienteExistente();

        Produto produto1 = criarProdutoAtivo("produto1", "Pizza", 10.0);
        Produto produto2 = criarProdutoAtivo("produto2", "Bebida", 5.0);

        when(produtoRepository.findById("produto1")).thenReturn(Optional.of(produto1));
        when(produtoRepository.findById("produto2")).thenReturn(Optional.of(produto2));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> {Pedido pedido = invocation.getArgument(0);pedido.setId("pedido1");return pedido;});

        Pedido resultado = pedidoService.criarPedido(dto, authentication);

        assertEquals(2, resultado.getItens().size());
        assertEquals(35.0, resultado.getValorTotal(), 0.001);
        assertEquals(StatusPedido.RECEBIDO, resultado.getStatus());

        verify(logService).registrar(
                eq(authentication),
                eq("CRIACAO_PEDIDO"),
                eq("Pedido"),
                eq("pedido1"),
                alteracoesCaptor.capture(),
                isNull()
        );

        List<Log.AlteracaoCampo> alteracoes = alteracoesCaptor.getValue();

        assertEquals(1, alteracoes.size());
        assertEquals("status", alteracoes.get(0).getCampo());
        assertNull(alteracoes.get(0).getValorAnterior());
        assertEquals("RECEBIDO", alteracoes.get(0).getValorNovo());
    }

    @Test
    void deveImpedirCriacaoComDtoNulo() {
        assertThrows(IllegalArgumentException.class, () -> pedidoService.criarPedido(null, authentication));
        verifyNoInteractions(pedidoRepository);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void deveImpedirCriacaoSemCliente(String clienteId) {
        PedidoRequestDTO dto = criarDtoValido();
        dto.setClienteId(clienteId);
        assertThrows(IllegalArgumentException.class, () -> pedidoService.criarPedido(dto, authentication));
        verifyNoInteractions(pedidoRepository);
    }

    @Test
    void deveImpedirCriacaoComClienteInexistente() {
        PedidoRequestDTO dto = criarDtoValido();
        when(clienteRepository.findById("cliente1")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> pedidoService.criarPedido(dto, authentication));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void deveImpedirCriacaoComItensNulos() {
        PedidoRequestDTO dto = criarDtoValido();
        dto.setItens(null);
        simularClienteExistente();
        assertThrows(IllegalArgumentException.class, () -> pedidoService.criarPedido(dto, authentication));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void deveImpedirCriacaoComListaDeItensVazia() {
        PedidoRequestDTO dto = criarDtoValido();
        dto.setItens(List.of());
        simularClienteExistente();
        assertThrows(IllegalArgumentException.class, () -> pedidoService.criarPedido(dto, authentication));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void deveImpedirCriacaoSemAuthentication() {
        PedidoRequestDTO dto = criarDtoValido();
        simularClienteExistente();
        assertThrows(IllegalArgumentException.class, () -> pedidoService.criarPedido(dto, null));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void deveImpedirCriacaoSemNomeDoResponsavel() {
        PedidoRequestDTO dto = criarDtoValido();
        simularClienteExistente();
        when(authentication.getName()).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> pedidoService.criarPedido(dto, authentication));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void deveImpedirCriacaoComItemNulo() {
        PedidoRequestDTO dto = criarDtoValido();
        dto.setItens(Collections.singletonList(null));
        simularClienteExistente();
        assertThrows(IllegalArgumentException.class, () -> pedidoService.criarPedido(dto, authentication));
        verify(pedidoRepository, never()).save(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void deveImpedirCriacaoSemProdutoId(String produtoId) {
        PedidoRequestDTO dto = criarDtoValido();
        dto.getItens().get(0).setProdutoId(produtoId);
        simularClienteExistente();
        assertThrows(IllegalArgumentException.class, () -> pedidoService.criarPedido(dto, authentication));
        verify(pedidoRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void deveImpedirCriacaoComQuantidadeInvalida(int quantidade) {
        PedidoRequestDTO dto = criarDtoValido();
        dto.getItens().get(0).setQuantidade(quantidade);
        simularClienteExistente();
        assertThrows(IllegalArgumentException.class, () -> pedidoService.criarPedido(dto, authentication));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void deveImpedirCriacaoComProdutoInexistente() {
        PedidoRequestDTO dto = criarDtoValido();
        simularClienteExistente();
        when(produtoRepository.findById("produto1")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> pedidoService.criarPedido(dto, authentication));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void deveImpedirCriacaoComProdutoInativo() {
        PedidoRequestDTO dto = criarDtoValido();
        simularClienteExistente();
        Produto produto = criarProdutoAtivo("produto1", "Pizza", 30.0);
        produto.setAtivo(false);
        when(produtoRepository.findById("produto1")).thenReturn(Optional.of(produto));
        assertThrows(IllegalArgumentException.class, () -> pedidoService.criarPedido(dto, authentication));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void deveListarPedidosCadastrados() {
        Pedido pedido1 = criarPedidoComStatus(StatusPedido.RECEBIDO);
        Pedido pedido2 = criarPedidoComStatus(StatusPedido.PRONTO);

        List<Pedido> pedidos = List.of(pedido1, pedido2);
        when(pedidoRepository.findAll()).thenReturn(pedidos);
        List<Pedido> resultado = pedidoService.listarPedidos();

        assertEquals(2, resultado.size());
        assertEquals(pedidos, resultado);
        verify(pedidoRepository).findAll();
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHaPedidos() {
        when(pedidoRepository.findAll()).thenReturn(List.of());
        List<Pedido> resultado = pedidoService.listarPedidos();
        assertTrue(resultado.isEmpty());
        verify(pedidoRepository).findAll();
    }

    @Test
    void alterarPedidoEmPreparo() {
        Pedido pedido = criarPedidoComStatus(StatusPedido.RECEBIDO);
        simularPedidoExistente(pedido);
        when(pedidoRepository.save(pedido)).thenReturn(pedido);
        Pedido resultado = pedidoService.atualizarStatus("pedido1", StatusPedido.EM_PREPARO, authentication);
        assertEquals(StatusPedido.EM_PREPARO, resultado.getStatus());
        verify(pedidoRepository).save(pedido);
    }

    @Test
    void alterarPedidoEmPreparoParaPronto() {
        Pedido pedido = criarPedidoComStatus(StatusPedido.EM_PREPARO);
        simularPedidoExistente(pedido);
        when(pedidoRepository.save(pedido)).thenReturn(pedido);
        Pedido resultado = pedidoService.atualizarStatus("pedido1", StatusPedido.PRONTO, authentication);
        assertEquals(StatusPedido.PRONTO, resultado.getStatus());
        verify(pedidoRepository).save(pedido);
    }

    @Test
    void registrarResponsavelEHorarioSairParaEntrega() {
        Pedido pedido = criarPedidoComStatus(StatusPedido.PRONTO);
        simularPedidoExistente(pedido);
        when(authentication.getName()).thenReturn("entregador01");
        when(pedidoRepository.save(pedido)).thenReturn(pedido);
        Pedido resultado = pedidoService.atualizarStatus("pedido1", StatusPedido.SAIU_PARA_ENTREGA, authentication);

        assertEquals(StatusPedido.SAIU_PARA_ENTREGA, resultado.getStatus());
        assertEquals("entregador01", resultado.getResponsavelSaidaId());
        assertNotNull(resultado.getHorarioSaida());
        verify(pedidoRepository).save(pedido);
    }

    @Test
    void finalizarPedidoSaiuParaEntrega() {
        Pedido pedido = criarPedidoComStatus(StatusPedido.SAIU_PARA_ENTREGA);
        simularPedidoExistente(pedido);
        when(pedidoRepository.save(pedido)).thenReturn(pedido);

        Pedido resultado = pedidoService.atualizarStatus("pedido1", StatusPedido.FINALIZADO, authentication);
        assertEquals(StatusPedido.FINALIZADO, resultado.getStatus());
        verify(pedidoRepository).save(pedido);
    }

    @Test
    void deveRegistrarAuditoriaNaAlteracaoDeStatus() {
        Pedido pedido = criarPedidoComStatus(StatusPedido.RECEBIDO);
        simularPedidoExistente(pedido);
        when(pedidoRepository.save(pedido)).thenReturn(pedido);
        pedidoService.atualizarStatus("pedido1", StatusPedido.EM_PREPARO, authentication);

        verify(logService).registrar(
                eq(authentication),
                eq("ALTERACAO_STATUS"),
                eq("Pedido"),
                eq("pedido1"),
                alteracoesCaptor.capture(),
                isNull()
        );

        List<Log.AlteracaoCampo> alteracoes = alteracoesCaptor.getValue();

        assertEquals(1, alteracoes.size());
        assertEquals("status", alteracoes.get(0).getCampo());
        assertEquals("RECEBIDO", alteracoes.get(0).getValorAnterior());
        assertEquals("EM_PREPARO", alteracoes.get(0).getValorNovo());
    }

    @Test
    void naoPularEtapaDeRecebidoParaPronto() {
        Pedido pedido = criarPedidoComStatus(StatusPedido.RECEBIDO);
        simularPedidoExistente(pedido);
        assertThrows(IllegalArgumentException.class, () -> pedidoService.atualizarStatus("pedido1", StatusPedido.PRONTO, authentication));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void rejeitarPedidoNoFinalizado() {
        Pedido pedido = criarPedidoComStatus(StatusPedido.RECEBIDO);
        simularPedidoExistente(pedido);
        assertThrows(IllegalArgumentException.class, () -> pedidoService.atualizarStatus("pedido1", StatusPedido.FINALIZADO, authentication));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void naoAlterarPedidoFinalizado() {
        Pedido pedido = criarPedidoComStatus(StatusPedido.FINALIZADO);
        simularPedidoExistente(pedido);
        assertThrows(IllegalArgumentException.class, () -> pedidoService.atualizarStatus("pedido1", StatusPedido.EM_PREPARO, authentication));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void impedirAlteracaoDePedidoCancelado() {
        Pedido pedido = criarPedidoComStatus(StatusPedido.CANCELADO);
        simularPedidoExistente(pedido);
        assertThrows(IllegalArgumentException.class, () -> pedidoService.atualizarStatus("pedido1", StatusPedido.EM_PREPARO, authentication));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void impedirSaidaResponsavelNaoIdentificado() {
        Pedido pedido = criarPedidoComStatus(StatusPedido.PRONTO);
        simularPedidoExistente(pedido);
        when(authentication.getName()).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> pedidoService.atualizarStatus("pedido1", StatusPedido.SAIU_PARA_ENTREGA, authentication));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void deveImpedirAtualizacaoComStatusNulo() {
        assertThrows(IllegalArgumentException.class, () -> pedidoService.atualizarStatus("pedido1", null, authentication));
        verifyNoInteractions(pedidoRepository);
    }

    @Test
    void deveImpedirAtualizacaoSemAuthentication() {
        assertThrows(SecurityException.class, () -> pedidoService.atualizarStatus("pedido1", StatusPedido.EM_PREPARO, null));
        verifyNoInteractions(pedidoRepository);
    }

    @Test
    void deveImpedirAtualizacaoSemAutenticacaoValida() {
        when(authentication.isAuthenticated()).thenReturn(false);
        assertThrows(SecurityException.class, () -> pedidoService.atualizarStatus("pedido1", StatusPedido.EM_PREPARO, authentication));
        verifyNoInteractions(pedidoRepository);
    }

    @Test
    void deveImpedirAtualizacaoDePedidoInexistente() {
        when(pedidoRepository.findById("pedido1")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> pedidoService.atualizarStatus("pedido1", StatusPedido.EM_PREPARO, authentication));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void cancelarPedidoComGerenteEJustificativa() {
        Pedido pedido = criarPedidoComStatus(StatusPedido.RECEBIDO);
        simularGerente();
        simularPedidoExistente(pedido);
        when(pedidoRepository.save(pedido)).thenReturn(pedido);
        Pedido resultado = pedidoService.cancelarPedido("pedido1", "Cliente desistiu", authentication);

        assertEquals(StatusPedido.CANCELADO, resultado.getStatus());
        assertEquals("Cliente desistiu", resultado.getCancelamentoPedido());
        verify(pedidoRepository).save(pedido);
    }

    @Test
    void deveRegistrarAuditoriaDoCancelamento() {
        Pedido pedido = criarPedidoComStatus(StatusPedido.RECEBIDO);
        simularGerente();
        simularPedidoExistente(pedido);
        when(pedidoRepository.save(pedido)).thenReturn(pedido);

        pedidoService.cancelarPedido("pedido1", "  Cliente desistiu  ", authentication);
        assertEquals("Cliente desistiu", pedido.getCancelamentoPedido());

        verify(logService).registrar(
                eq(authentication),
                eq("CANCELAMENTO_PEDIDO"),
                eq("Pedido"),
                eq("pedido1"),
                alteracoesCaptor.capture(),
                eq("Cliente desistiu")
        );

        List<Log.AlteracaoCampo> alteracoes = alteracoesCaptor.getValue();

        assertEquals(2, alteracoes.size());
        assertEquals("status", alteracoes.get(0).getCampo());
        assertEquals("RECEBIDO", alteracoes.get(0).getValorAnterior());
        assertEquals("CANCELADO", alteracoes.get(0).getValorNovo());
        assertEquals("cancelamentoPedido", alteracoes.get(1).getCampo());
        assertNull(alteracoes.get(1).getValorAnterior());
        assertEquals("Cliente desistiu", alteracoes.get(1).getValorNovo());
    }

    @Test
    void impedirCancelamentoPorAtendente() {
        simularAtendente();
        assertThrows(SecurityException.class, () -> pedidoService.cancelarPedido("pedido1", "Cliente desistiu", authentication));
        verifyNoInteractions(pedidoRepository);
    }

    @Test
    void impedirCancelamentoDePedidoFinalizado() {
        Pedido pedido = criarPedidoComStatus(StatusPedido.FINALIZADO);
        simularGerente();
        simularPedidoExistente(pedido);
        assertThrows(IllegalArgumentException.class, () -> pedidoService.cancelarPedido("pedido1", "Cliente desistiu", authentication));
        verify(pedidoRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = StatusPedido.class, names = {"SAIU_PARA_ENTREGA", "CANCELADO"})
    void deveImpedirCancelamentoEmStatusProibido(StatusPedido status) {
        Pedido pedido = criarPedidoComStatus(status);

        simularGerente();
        simularPedidoExistente(pedido);

        assertThrows(IllegalArgumentException.class, () -> pedidoService.cancelarPedido("pedido1", "Cliente desistiu", authentication));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void deveImpedirCancelamentoSemAuthentication() {
        assertThrows(SecurityException.class, () -> pedidoService.cancelarPedido("pedido1", "Cliente desistiu", null));
        verifyNoInteractions(pedidoRepository);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void deveImpedirCancelamentoSemJustificativa(String justificativa) {
        simularGerente();
        assertThrows(IllegalArgumentException.class, () -> pedidoService.cancelarPedido("pedido1", justificativa, authentication));
        verifyNoInteractions(pedidoRepository);
    }

    @Test
    void deveImpedirCancelamentoDePedidoInexistente() {
        simularGerente();
        when(pedidoRepository.findById("pedido1")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> pedidoService.cancelarPedido("pedido1", "Cliente desistiu", authentication));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void aplicarDesconto() {
        Pedido pedido = criarPedidoComItens();

        simularGerente();
        simularPedidoExistente(pedido);

        when(pedidoRepository.save(pedido)).thenReturn(pedido);
        Pedido resultado = pedidoService.aplicarDesconto("pedido1", 10.0, authentication);

        assertEquals(10.0, resultado.getPercentualDesconto(), 0.001);
        assertEquals(10.0, resultado.getValorDesconto(), 0.001);
        assertEquals(90.0, resultado.getValorTotal(), 0.001);
        verify(pedidoRepository).save(pedido);
    }

    @Test
    void devePermitirDescontoDeVintePorCento() {
        Pedido pedido = criarPedidoComItens();

        simularGerente();
        simularPedidoExistente(pedido);

        when(pedidoRepository.save(pedido)).thenReturn(pedido);
        Pedido resultado = pedidoService.aplicarDesconto("pedido1", 20.0, authentication);

        assertEquals(20.0, resultado.getPercentualDesconto(), 0.001);
        assertEquals(20.0, resultado.getValorDesconto(), 0.001);
        assertEquals(80.0, resultado.getValorTotal(), 0.001);
    }

    @Test
    void deveArredondarValoresDoDesconto() {
        Pedido pedido = criarPedidoComStatus(StatusPedido.RECEBIDO);

        pedido.setItens(List.of(new ItemPedido("produto1", "Pizza", 3, 19.99)));
        pedido.setValorTotal(59.97);

        simularGerente();
        simularPedidoExistente(pedido);

        when(pedidoRepository.save(pedido)).thenReturn(pedido);
        Pedido resultado = pedidoService.aplicarDesconto("pedido1", 15.0, authentication);

        assertEquals(9.00, resultado.getValorDesconto(), 0.001);
        assertEquals(50.97, resultado.getValorTotal(), 0.001);
    }

    @Test
    void deveRecalcularDescontoSobreValorOriginal() {
        Pedido pedido = criarPedidoComItens();

        simularGerente();
        simularPedidoExistente(pedido);

        when(pedidoRepository.save(pedido)).thenReturn(pedido);
        pedidoService.aplicarDesconto("pedido1", 10.0, authentication);
        assertEquals(90.0, pedido.getValorTotal(), 0.001);
        pedidoService.aplicarDesconto("pedido1", 20.0, authentication);

        assertEquals(20.0, pedido.getPercentualDesconto(), 0.001);
        assertEquals(20.0, pedido.getValorDesconto(), 0.001);
        assertEquals(80.0, pedido.getValorTotal(), 0.001);
        verify(pedidoRepository, times(2)).save(pedido);
    }

    @Test
    void deveRegistrarAuditoriaDoDesconto() {
        Pedido pedido = criarPedidoComItens();

        pedido.setPercentualDesconto(0.0);
        pedido.setValorDesconto(0.0);

        simularGerente();
        simularPedidoExistente(pedido);

        when(pedidoRepository.save(pedido)).thenReturn(pedido);

        pedidoService.aplicarDesconto("pedido1", 10.0, authentication);

        verify(logService).registrar(
                eq(authentication),
                eq("APLICACAO_DESCONTO"),
                eq("Pedido"),
                eq("pedido1"),
                alteracoesCaptor.capture(),
                isNull()
        );

        List<Log.AlteracaoCampo> alteracoes = alteracoesCaptor.getValue();

        assertEquals("percentualDesconto", alteracoes.get(0).getCampo());
        assertEquals("0.0", alteracoes.get(0).getValorAnterior());
        assertEquals("10.0", alteracoes.get(0).getValorNovo());

        assertEquals("valorDesconto", alteracoes.get(1).getCampo());
        assertEquals("0.0", alteracoes.get(1).getValorAnterior());
        assertEquals("10.0", alteracoes.get(1).getValorNovo());

        assertEquals("valorTotal", alteracoes.get(2).getCampo());
        assertEquals("100.0", alteracoes.get(2).getValorAnterior());
        assertEquals("90.0", alteracoes.get(2).getValorNovo());
    }

    @Test
    void impedirDescontoAcimaDeVintePorCento() {
        simularGerente();
        assertThrows(IllegalArgumentException.class, () -> pedidoService.aplicarDesconto("pedido1", 25.0, authentication));
        verifyNoInteractions(pedidoRepository);
    }

    @Test
    void impedirDescontoPorAtendente() {
        simularAtendente();
        assertThrows(SecurityException.class, () -> pedidoService.aplicarDesconto("pedido1", 10.0, authentication));
        verifyNoInteractions(pedidoRepository);
    }

    @Test
    void impedirDescontoAposSaidaParaEntrega() {
        Pedido pedido = criarPedidoComStatus(StatusPedido.SAIU_PARA_ENTREGA);
        simularGerente();
        simularPedidoExistente(pedido);
        assertThrows(IllegalArgumentException.class, () -> pedidoService.aplicarDesconto("pedido1", 10.0, authentication));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void deveImpedirDescontoSemAuthentication() {
        assertThrows(SecurityException.class, () -> pedidoService.aplicarDesconto("pedido1", 10.0, null));
        verifyNoInteractions(pedidoRepository);
    }

    @Test
    void deveImpedirDescontoSemAutoridades() {
        when(authentication.getAuthorities()).thenReturn(null);
        assertThrows(SecurityException.class, () -> pedidoService.aplicarDesconto("pedido1", 10.0, authentication));
        verifyNoInteractions(pedidoRepository);
    }

    @Test
    void deveImpedirPercentuaisDeDescontoInvalidos() {
        simularGerente();

        double[] percentuaisInvalidos = {
                0.0,
                -1.0,
                20.1,
                Double.NaN,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY
        };

        for (double percentual : percentuaisInvalidos) {
            assertThrows(IllegalArgumentException.class, () -> pedidoService.aplicarDesconto("pedido1", percentual, authentication));
        }

        verifyNoInteractions(pedidoRepository);
    }

    @Test
    void deveImpedirDescontoEmPedidoInexistente() {
        simularGerente();
        when(pedidoRepository.findById("pedido1")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> pedidoService.aplicarDesconto("pedido1", 10.0, authentication));
        verify(pedidoRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = StatusPedido.class, names = {"CANCELADO", "FINALIZADO"})
    void deveImpedirDescontoEmStatusProibido(StatusPedido status) {
        Pedido pedido = criarPedidoComStatus(status);
        simularGerente();
        simularPedidoExistente(pedido);
        assertThrows(IllegalArgumentException.class, () -> pedidoService.aplicarDesconto("pedido1", 10.0, authentication));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void deveImpedirDescontoSemListaDeItens() {
        Pedido pedido = criarPedidoComStatus(StatusPedido.RECEBIDO);
        pedido.setItens(null);
        simularGerente();
        simularPedidoExistente(pedido);
        assertThrows(IllegalArgumentException.class, () -> pedidoService.aplicarDesconto("pedido1", 10.0, authentication));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void deveImpedirDescontoComListaVazia() {
        Pedido pedido = criarPedidoComStatus(StatusPedido.RECEBIDO);
        pedido.setItens(List.of());
        simularGerente();
        simularPedidoExistente(pedido);
        assertThrows(IllegalArgumentException.class, () -> pedidoService.aplicarDesconto("pedido1", 10.0, authentication));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void deveManterStatusOriginalQuandoSaidaSemResponsavel() {
        Pedido pedido = criarPedidoComStatus(StatusPedido.PRONTO);

        simularPedidoExistente(pedido);
        when(authentication.getName()).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> pedidoService.atualizarStatus("pedido1", StatusPedido.SAIU_PARA_ENTREGA, authentication));
        assertEquals(StatusPedido.PRONTO, pedido.getStatus());
        verify(pedidoRepository, never()).save(any());
    }
}