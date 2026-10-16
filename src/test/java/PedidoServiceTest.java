import com.umc.pediupatrao.dto.ItemPedidoDTO;
import com.umc.pediupatrao.dto.PedidoRequestDTO;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import com.umc.pediupatrao.service.PedidoService;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.repository.PedidoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.core.Authentication;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.any;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import org.springframework.security.core.GrantedAuthority;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {
    @Mock
    private PedidoRepository pedidoRepository;
    @InjectMocks
    private PedidoService pedidoService;
    @Mock
    private Authentication authentication;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private ProdutoRepository produtoRepository;

    @Test
    void rejeitarPedidoNoFinalizado() {
        Pedido pedido = new Pedido();
        pedido.setStatus(StatusPedido.RECEBIDO);

        when(pedidoRepository.findById("123"))
                .thenReturn(Optional.of(pedido));

        assertThrows(
                IllegalArgumentException.class,
                () -> pedidoService.atualizarStatus(
                        "123",
                        StatusPedido.FINALIZADO,
                        authentication
                )
        );

        verify(pedidoRepository, never()).save(pedido);
    }

    @Test
    void alterarPedidoEmPreparo() {
        Pedido pedido = new Pedido();
        pedido.setStatus(StatusPedido.RECEBIDO);

        when(pedidoRepository.findById("123"))
                .thenReturn(Optional.of(pedido));

        when(pedidoRepository.save(pedido))
                .thenReturn(pedido);

        Pedido pedidoAtualizado = pedidoService.atualizarStatus(
                "123",
                StatusPedido.EM_PREPARO,
                authentication
        );

        assertEquals(
                StatusPedido.EM_PREPARO,
                pedidoAtualizado.getStatus()
        );

        verify(pedidoRepository).save(pedido);
    }

    @Test
    void registrarResponsavelEHorarioSairParaEntrega() {
        Pedido pedido = new Pedido();
        pedido.setStatus(StatusPedido.PRONTO);

        when(pedidoRepository.findById("123"))
                .thenReturn(Optional.of(pedido));

        when(authentication.getName())
                .thenReturn("entregador01");

        when(pedidoRepository.save(pedido))
                .thenReturn(pedido);

        Pedido pedidoAtualizado = pedidoService.atualizarStatus(
                "123",
                StatusPedido.SAIU_PARA_ENTREGA,
                authentication
        );

        assertEquals(
                StatusPedido.SAIU_PARA_ENTREGA,
                pedidoAtualizado.getStatus()
        );

        assertEquals(
                "entregador01",
                pedidoAtualizado.getResponsavelSaidaId()
        );

        assertNotNull(pedidoAtualizado.getHorarioSaida());

        verify(pedidoRepository).save(pedido);
    }

    @Test
    void impedirSaidaResponsavelNaoIdentificado() {
        Pedido pedido = new Pedido();
        pedido.setStatus(StatusPedido.PRONTO);

        when(pedidoRepository.findById("123"))
                .thenReturn(Optional.of(pedido));

        when(authentication.getName())
                .thenReturn(null);

        assertThrows(
                IllegalArgumentException.class,
                () -> pedidoService.atualizarStatus(
                        "123",
                        StatusPedido.SAIU_PARA_ENTREGA,
                        authentication
                )
        );

        verify(pedidoRepository, never()).save(pedido);
    }

    @Test
    void finalizarPedidoSaiuParaEntrega() {
        Pedido pedido = new Pedido();
        pedido.setStatus(StatusPedido.SAIU_PARA_ENTREGA);

        when(pedidoRepository.findById("123"))
                .thenReturn(Optional.of(pedido));

        when(pedidoRepository.save(pedido))
                .thenReturn(pedido);

        Pedido pedidoAtualizado = pedidoService.atualizarStatus(
                "123",
                StatusPedido.FINALIZADO,
                authentication
        );

        assertEquals(
                StatusPedido.FINALIZADO,
                pedidoAtualizado.getStatus()
        );

        verify(pedidoRepository).save(pedido);
    }

    @Test
    void naoAlterarPedidoFinalizado() {
        Pedido pedido = new Pedido();
        pedido.setStatus(StatusPedido.FINALIZADO);

        when(pedidoRepository.findById("123"))
                .thenReturn(Optional.of(pedido));

        assertThrows(
                IllegalArgumentException.class,
                () -> pedidoService.atualizarStatus(
                        "123",
                        StatusPedido.EM_PREPARO,
                        authentication
                )
        );

        verify(pedidoRepository, never()).save(pedido);
    }

    @Test
    void alterarPedidoEmPreparoParaPronto() {
        Pedido pedido = new Pedido();
        pedido.setStatus(StatusPedido.EM_PREPARO);

        when(pedidoRepository.findById("pedido1"))
                .thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(pedido))
                .thenReturn(pedido);

        Pedido resultado = pedidoService.atualizarStatus(
                "pedido1",
                StatusPedido.PRONTO,
                authentication
        );

        org.junit.jupiter.api.Assertions.assertEquals(
                StatusPedido.PRONTO,
                resultado.getStatus()
        );

        verify(pedidoRepository).save(pedido);
    }


    @Test
    void criarPedidoComStatusHorarioEResponsavel() {
        PedidoRequestDTO dto = new PedidoRequestDTO();
        dto.setClienteId("cliente1");

        ItemPedidoDTO itemDto = new ItemPedidoDTO();
        itemDto.setProdutoId("produto1");
        itemDto.setQuantidade(2);

        dto.setItens(List.of(itemDto));

        // 2. Simular a existência do cliente
        Cliente cliente = new Cliente();

        when(clienteRepository.findById("cliente1"))
                .thenReturn(Optional.of(cliente));

        Produto produto = new Produto();
        produto.setId("produto1");
        produto.setNome("Pizza");
        produto.setPreco(30.0);
        produto.setAtivo(true);

        when(produtoRepository.findById("produto1"))
                .thenReturn(Optional.of(produto));

        when(authentication.getName()).thenReturn("atendente01");

        when(pedidoRepository.save(any(Pedido.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Pedido pedidoCriado = pedidoService.criarPedido(dto, authentication);

        org.junit.jupiter.api.Assertions.assertEquals(
                StatusPedido.RECEBIDO,
                pedidoCriado.getStatus()
        );

        org.junit.jupiter.api.Assertions.assertEquals(
                "atendente01",
                pedidoCriado.getResponsavelId()
        );

        assertNotNull(pedidoCriado.getHorarioEntrada());

        org.junit.jupiter.api.Assertions.assertEquals(
                60.0,
                pedidoCriado.getValorTotal(),
                0.001
        );

        verify(pedidoRepository).save(any(Pedido.class));
    }


    @Test
    void naoPularEtapaDeRecebidoParaPronto() {
        Pedido pedido = new Pedido();
        pedido.setStatus(StatusPedido.RECEBIDO);

        when(pedidoRepository.findById("pedido1"))
                .thenReturn(Optional.of(pedido));

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> pedidoService.atualizarStatus(
                        "pedido1",
                        StatusPedido.PRONTO,
                        authentication
                )
        );

        verify(pedidoRepository, never()).save(pedido);
    }

    @Test
    void cancelarPedidoComGerenteEJustificativa() {
        Pedido pedido = new Pedido();
        pedido.setStatus(StatusPedido.RECEBIDO);

        when(pedidoRepository.findById("pedido1"))
                .thenReturn(Optional.of(pedido));

        org.springframework.security.core.authority.SimpleGrantedAuthority gerente =
                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_GERENTE");

        org.mockito.Mockito.doReturn(java.util.List.of(gerente))
                .when(authentication)
                .getAuthorities();

        when(pedidoRepository.save(pedido))
                .thenReturn(pedido);

        Pedido resultado = pedidoService.cancelarPedido(
                "pedido1",
                "Cliente solicitou o cancelamento",
                authentication
        );

        assertEquals(StatusPedido.CANCELADO, resultado.getStatus());
        assertEquals(
                "Cliente solicitou o cancelamento",
                resultado.getCancelamentoPedido()
        );

        verify(pedidoRepository).save(pedido);
    }
}
