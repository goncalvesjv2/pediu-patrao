
package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioDetailsServiceTest {
    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioDetailsService usuarioDetailsService;

    // Método auxiliar para criar usuários
    private Usuario criarUsuario(String username, String password, Perfil perfil) {
        Usuario usuario = new Usuario();

        usuario.setUsername(username);
        usuario.setPassword(password);
        usuario.setRole(perfil);

        return usuario;
    }

    @Test
    void deveCarregarUsuarioExistente() {
        Usuario usuario = criarUsuario(
                "joao",
                "senhaCriptografada123",
                Perfil.ADMIN
        );

        when(usuarioRepository.findByUsername("joao")).thenReturn(Optional.of(usuario));

        UserDetails resultado = usuarioDetailsService.loadUserByUsername("joao");

        assertNotNull(resultado);
        assertEquals("joao", resultado.getUsername());
        assertEquals("senhaCriptografada123", resultado.getPassword());
        verify(usuarioRepository).findByUsername("joao");
    }

    @ParameterizedTest
    @EnumSource(Perfil.class)
    void deveCarregarUsuarioComPerfilCorreto(Perfil perfil) {
        Usuario usuario = criarUsuario(
                "joao",
                "senhaCriptografada123",
                perfil
        );

        when(usuarioRepository.findByUsername("joao")).thenReturn(Optional.of(usuario));

        UserDetails resultado = usuarioDetailsService.loadUserByUsername("joao");

        String autoridadeEsperada = "ROLE_" + perfil.name();

        assertTrue(resultado.getAuthorities().contains(new SimpleGrantedAuthority(autoridadeEsperada)));
        assertEquals(1, resultado.getAuthorities().size());
        verify(usuarioRepository).findByUsername("joao");
    }

    @Test
    void devePreservarSenhaArmazenada() {
        String senhaArmazenada = "$2a$10$hashDeExemplo";

        Usuario usuario = criarUsuario(
                "maria",
                senhaArmazenada,
                Perfil.GERENTE
        );

        when(usuarioRepository.findByUsername("maria")).thenReturn(Optional.of(usuario));
        UserDetails resultado = usuarioDetailsService.loadUserByUsername("maria");
        assertEquals(senhaArmazenada, resultado.getPassword());
        verify(usuarioRepository).findByUsername("maria");
    }

    @Test
    void deveLancarExcecaoQuandoUsuarioNaoExiste() {
        when(usuarioRepository.findByUsername("inexistente")).thenReturn(Optional.empty());
        UsernameNotFoundException erro = assertThrows(UsernameNotFoundException.class, () -> usuarioDetailsService.loadUserByUsername("inexistente"));
        assertEquals("Usuário não encontrado", erro.getMessage());
        verify(usuarioRepository).findByUsername("inexistente");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void deveLancarExcecaoQuandoUsernameVazioNaoEncontrado(String username) {
        when(usuarioRepository.findByUsername(username)).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class, () -> usuarioDetailsService.loadUserByUsername(username));
        verify(usuarioRepository).findByUsername(username);
    }

    @Test
    void deveConsultarRepositorioComUsernameInformado() {
        Usuario usuario = criarUsuario(
                "carlos",
                "senhaCriptografada123",
                Perfil.ATENDENTE
        );

        when(usuarioRepository.findByUsername("carlos")).thenReturn(Optional.of(usuario));
        usuarioDetailsService.loadUserByUsername("carlos");
        verify(usuarioRepository, times(1)).findByUsername("carlos");
        verifyNoMoreInteractions(usuarioRepository);
    }

    @Test
    void deveRepassarFalhaDoRepositorio() {
        IllegalStateException erro = new IllegalStateException("Falha ao consultar usuário");
        when(usuarioRepository.findByUsername("joao")).thenThrow(erro);
        IllegalStateException resultado = assertThrows(IllegalStateException.class, () -> usuarioDetailsService.loadUserByUsername("joao"));
        assertSame(erro, resultado);
        verify(usuarioRepository).findByUsername("joao");
    }
}
