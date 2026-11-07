package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {
    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    void deveBuscarUsuarioPorId() {
        Usuario usuario = mock(Usuario.class);
        when(usuarioRepository.findById("1")).thenReturn(Optional.of(usuario));
        Optional<Usuario> resultado = usuarioService.buscarPorId("1");
        assertTrue(resultado.isPresent());
        assertSame(usuario, resultado.get());
        verify(usuarioRepository).findById("1");
    }

    @Test
    void deveRetornarVazioQuandoUsuarioNaoExiste() {
        when(usuarioRepository.findById("99")).thenReturn(Optional.empty());
        Optional<Usuario> resultado = usuarioService.buscarPorId("99");
        assertTrue(resultado.isEmpty());
    }

    @Test
    void deveBuscarUsuarioPorUsername() {
        Usuario usuario = mock(Usuario.class);
        when(usuarioRepository.findByUsername("joao")).thenReturn(Optional.of(usuario));
        Optional<Usuario> resultado = usuarioService.buscarPorUsername("joao");
        assertTrue(resultado.isPresent());
        assertSame(usuario, resultado.get());
    }

    @Test
    void deveRetornarVazioQuandoUsernameNaoExiste() {
        when(usuarioRepository.findByUsername("inexistente")).thenReturn(Optional.empty());
        Optional<Usuario> resultado = usuarioService.buscarPorUsername("inexistente");
        assertTrue(resultado.isEmpty());
    }

    @Test
    void deveListarUsuarios() {
        Usuario usuario = mock(Usuario.class);
        when(usuarioRepository.findAll()).thenReturn(List.of(usuario));
        Iterable<Usuario> resultado = usuarioService.listarTodos();
        assertIterableEquals(List.of(usuario), resultado);
    }

    @Test
    void deveRetornarListaVazia() {
        when(usuarioRepository.findAll()).thenReturn(List.of());
        Iterable<Usuario> resultado = usuarioService.listarTodos();
        assertFalse(resultado.iterator().hasNext());
    }

    @Test
    void deveExcluirOutroUsuario() {
        Usuario usuario = mock(Usuario.class);
        when(usuario.getUsername()).thenReturn("maria");
        when(usuarioRepository.findById("2")).thenReturn(Optional.of(usuario));
        usuarioService.deletarUsuario("2", "joao");
        verify(usuarioRepository).deleteById("2");
    }

    @Test
    void deveImpedirExclusaoDoProprioUsuario() {
        Usuario usuario = mock(Usuario.class);
        when(usuario.getUsername()).thenReturn("joao");
        when(usuarioRepository.findById("1")).thenReturn(Optional.of(usuario));
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> usuarioService.deletarUsuario("1", "joao"));
        assertEquals("Você não pode excluir o seu próprio usuário.", erro.getMessage());
        verify(usuarioRepository, never()).deleteById("1");
    }

    @Test
    void deveImpedirExclusaoDeUsuarioInexistente() {
        when(usuarioRepository.findById("99")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> usuarioService.deletarUsuario("99", "joao"));
        verify(usuarioRepository, never()).deleteById(anyString());
    }

    @Test
    void deveImpedirCadastroSemUsername() {
        Usuario usuario = mock(Usuario.class);
        when(usuario.getUsername()).thenReturn(" ");
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> usuarioService.salvarUsuario(usuario));
        assertEquals("O username é obrigatório.", erro.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deveImpedirCadastroSemPerfil() {
        Usuario usuario = mock(Usuario.class);
        when(usuario.getUsername()).thenReturn("joao");
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> usuarioService.salvarUsuario(usuario));
        assertEquals("O perfil é obrigatório.", erro.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deveImpedirAtualizacaoDeUsuarioInexistente() {
        Usuario usuarioAtualizado = mock(Usuario.class);
        when(usuarioRepository.findById("99")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> usuarioService.atualizarUsuario("99", usuarioAtualizado, "admin"));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deveCadastrarUsuarioComSucesso() {
        Usuario usuario = new Usuario();
        usuario.setUsername(" joao ");
        usuario.setPassword("senha123");
        usuario.setRole(Perfil.ATENDENTE);

        when(passwordEncoder.encode("senha123")).thenReturn("senhaCriptografada");
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        Usuario resultado = usuarioService.salvarUsuario(usuario);

        assertNotNull(resultado);
        assertEquals("joao", resultado.getUsername());
        assertEquals("senhaCriptografada", resultado.getPassword());

        verify(passwordEncoder).encode("senha123");
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void deveImpedirCadastroComUsernameDuplicado() {
        Usuario usuario = new Usuario();
        usuario.setUsername("joao");
        usuario.setPassword("senha123");
        usuario.setRole(Perfil.ATENDENTE);

        when(usuarioRepository.existsByUsername("joao")).thenReturn(true);
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> usuarioService.salvarUsuario(usuario));

        assertEquals("Já existe um usuário com esse username.", erro.getMessage());
        verify(usuarioRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void deveImpedirCadastroSemSenha() {
        Usuario usuario = new Usuario();
        usuario.setUsername("joao");
        usuario.setRole(Perfil.ATENDENTE);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> usuarioService.salvarUsuario(usuario));
        assertEquals("A senha é obrigatória.", erro.getMessage());
        verify(usuarioRepository, never()).save(any());
    }


    @Test
    void deveAtualizarUsuarioComSucesso() {
        Usuario existente = new Usuario();
        existente.setId("2");
        existente.setUsername("maria");
        existente.setPassword("hashAntigo");
        existente.setRole(Perfil.ATENDENTE);

        Usuario atualizado = new Usuario();
        atualizado.setUsername("mariaNova");
        atualizado.setRole(Perfil.GERENTE);

        when(usuarioRepository.findById("2")).thenReturn(Optional.of(existente));
        when(usuarioRepository.save(existente)).thenReturn(existente);
        Usuario resultado = usuarioService.atualizarUsuario("2", atualizado, "joao");

        assertEquals("mariaNova", resultado.getUsername());
        assertEquals(Perfil.GERENTE, resultado.getRole());
        assertEquals("hashAntigo", resultado.getPassword());
        verify(usuarioRepository).save(existente);
    }

    @Test
    void deveAtualizarSenhaComSucesso() {
        Usuario existente = new Usuario();
        existente.setId("2");
        existente.setUsername("maria");
        existente.setPassword("hashAntigo");
        existente.setRole(Perfil.ATENDENTE);

        Usuario atualizado = new Usuario();
        atualizado.setUsername("maria");
        atualizado.setPassword("novaSenha");
        atualizado.setRole(Perfil.ATENDENTE);

        when(usuarioRepository.findById("2")).thenReturn(Optional.of(existente));
        when(passwordEncoder.encode("novaSenha")).thenReturn("hashNovo");
        when(usuarioRepository.save(existente)).thenReturn(existente);

        Usuario resultado = usuarioService.atualizarUsuario("2", atualizado, "admin");
        assertEquals("hashNovo", resultado.getPassword());

        verify(passwordEncoder).encode("novaSenha");
        verify(usuarioRepository).save(existente);
    }

    @Test
    void deveManterSenhaQuandoNaoInformarNovaSenha() {
        Usuario existente = new Usuario();
        existente.setId("2");
        existente.setUsername("maria");
        existente.setPassword("hashAntigo");
        existente.setRole(Perfil.ATENDENTE);

        Usuario atualizado = new Usuario();
        atualizado.setUsername("maria");
        atualizado.setRole(Perfil.ATENDENTE);

        when(usuarioRepository.findById("2")).thenReturn(Optional.of(existente));
        when(usuarioRepository.save(existente)).thenReturn(existente);
        Usuario resultado = usuarioService.atualizarUsuario("2", atualizado, "admin");

        assertEquals("hashAntigo", resultado.getPassword());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void deveImpedirAlteracaoDoProprioPerfil() {
        Usuario existente = new Usuario();
        existente.setId("1");
        existente.setUsername("joao");
        existente.setRole(Perfil.ADMIN);

        Usuario atualizado = new Usuario();
        atualizado.setUsername("joao");
        atualizado.setRole(Perfil.ATENDENTE);

        when(usuarioRepository.findById("1")).thenReturn(Optional.of(existente));
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> usuarioService.atualizarUsuario("1", atualizado, "joao"));
        assertEquals("Você não pode alterar o seu próprio perfil.", erro.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deveImpedirAtualizacaoComUsernameDuplicado() {
        Usuario existente = new Usuario();
        existente.setId("2");
        existente.setUsername("maria");
        existente.setRole(Perfil.ATENDENTE);

        Usuario atualizado = new Usuario();
        atualizado.setUsername("joao");
        atualizado.setRole(Perfil.ATENDENTE);

        when(usuarioRepository.findById("2")).thenReturn(Optional.of(existente));
        when(usuarioRepository.existsByUsername("joao")).thenReturn(true);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> usuarioService.atualizarUsuario("2", atualizado, "admin"));
        assertEquals("Já existe um usuário com esse username.", erro.getMessage());
        verify(usuarioRepository, never()).save(any());
    }
}