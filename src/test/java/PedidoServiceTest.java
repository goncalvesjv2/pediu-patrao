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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {
    @Mock
    private PedidoRepository pedidoRepository;
    @InjectMocks
    private PedidoService pedidoService;
    @Mock
    private Authentication authentication;

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
}
