import com.umc.pediupatrao.controller.LogController;
import com.umc.pediupatrao.repository.LogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.umc.pediupatrao.PizzariaPedidosApplication;
import org.springframework.test.context.ContextConfiguration;
import com.umc.pediupatrao.config.SecurityConfig;
import com.umc.pediupatrao.service.UsuarioDetailsService;

@WebMvcTest(LogController.class)
@ContextConfiguration(classes = { PizzariaPedidosApplication.class, SecurityConfig.class })
class LogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LogRepository logRepository;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPodeConsultarLogs() throws Exception {
        when(logRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/logs"))
                .andExpect(status().isOk());

        verify(logRepository).findAll();
    }

    @Test
    @WithMockUser(roles = "GERENTE")
    void gerentePodeConsultarLogs() throws Exception {
        when(logRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/logs"))
                .andExpect(status().isOk());

        verify(logRepository).findAll();
    }

    @Test
    @WithMockUser(roles = "ATENDENTE")
    void atendenteNaoPodeConsultarLogs() throws Exception {
        mockMvc.perform(get("/api/logs"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ENTREGADOR")
    void entregadorNaoPodeConsultarLogs() throws Exception {
        mockMvc.perform(get("/api/logs"))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuarioNaoAutenticadoNaoPodeConsultarLogs() throws Exception {
        mockMvc.perform(get("/api/logs").with(anonymous()))
                .andExpect(status().is3xxRedirection());
    }
}
