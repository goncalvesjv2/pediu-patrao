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
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        // Arrange: prepara um pedido recebido
        Pedido pedido = new Pedido();
        pedido.setStatus(StatusPedido.RECEBIDO);

        when(pedidoRepository.findById("123"))
                .thenReturn(Optional.of(pedido));

        // Act + Assert: a transição deve gerar uma exceção
        assertThrows(
                IllegalArgumentException.class,
                () -> pedidoService.atualizarStatus(
                        "123",
                        StatusPedido.FINALIZADO,
                        authentication
                )
        );

        // Confirma que o pedido não foi salvo
        verify(pedidoRepository, never()).save(pedido);
    }
}
