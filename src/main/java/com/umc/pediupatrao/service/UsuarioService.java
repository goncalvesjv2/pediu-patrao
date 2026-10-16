package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Busca um usuário por ID
    public Optional<Usuario> buscarPorId(String id) {
        return usuarioRepository.findById(id);
    }

    // Busca um usuário por username
    public Optional<Usuario> buscarPorUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }

    // Salva um novo usuário (criptografando a senha)
    @PreAuthorize("hasRole('ADMIN')")
    public Usuario salvarUsuario(Usuario usuario) {
        validar(usuario);
        if (usuario.getPassword() == null || usuario.getPassword().isBlank()) {
            throw new IllegalArgumentException("A senha é obrigatória.");
        }
        String username = usuario.getUsername().trim();

        if (usuarioRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Já existe um usuário com esse username.");
        }
        usuario.setId(null);
        usuario.setUsername(username);
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword())); // Criptografa a senha
        return usuarioRepository.save(usuario);
    }

    // Deleta um usuário pelo ID
    @PreAuthorize("hasRole('ADMIN')")
    public void deletarUsuario(String id, String usernameLogado) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        if (usuario.getUsername().equals(usernameLogado)) {
            throw new IllegalArgumentException("Você não pode excluir o seu próprio usuário.");
        }
        usuarioRepository.deleteById(id);
    }

    // Lista todos os usuários
    public Iterable<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    // Atualiza o usuário usuários
    @PreAuthorize("hasRole('ADMIN')")
    public Usuario atualizarUsuario(String id, Usuario usuarioAtualizado, String usernameLogado) {
        Usuario isExist = usuarioRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        validar(usuarioAtualizado);
        boolean changeProfile = isExist.getRole() != usuarioAtualizado.getRole();

        if (changeProfile && isExist.getUsername().equals(usernameLogado)) {
            throw new IllegalArgumentException("Você não pode alterar o seu próprio perfil.");
        }

        String newUsername = usuarioAtualizado.getUsername().trim();

        if (!isExist.getUsername().equals(newUsername) && usuarioRepository.existsByUsername(newUsername)) {
            throw new IllegalArgumentException("Já existe um usuário com esse username.");
        }

        isExist.setUsername(newUsername);
        isExist.setRole(usuarioAtualizado.getRole());

        // Só atualiza a senha se uma nova foi informada
        if (usuarioAtualizado.getPassword() != null && !usuarioAtualizado.getPassword().isBlank()) {
            isExist.setPassword(passwordEncoder.encode(usuarioAtualizado.getPassword()));
        }

        return usuarioRepository.save(isExist);
    }

    // Validações de cadastro e edição
    private void validar(Usuario usuario) {
        if (usuario.getUsername() == null || usuario.getUsername().isBlank()) {
            throw new IllegalArgumentException("O username é obrigatório.");
        }
        if (usuario.getRole() == null) {
            throw new IllegalArgumentException("O perfil é obrigatório.");
        }
    }
}
