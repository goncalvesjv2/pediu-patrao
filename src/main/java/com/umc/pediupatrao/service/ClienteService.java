package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.Log;
import com.umc.pediupatrao.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private LogService logService;

    public Cliente novoCliente(Cliente cliente) {
        return clienteRepository.save(cliente);
    }

    public List<Cliente> listarClientes() {
        return clienteRepository.findAll();
    }

    // Método para excluir cliente
    public void excluir(String id) {
        clienteRepository.deleteById(id);
    }

    public Optional<Cliente> buscarPorId(String id) {
        return clienteRepository.findById(id);
    }

    // Método para salvar um novo cliente ou atualizar um cliente existente
    public Cliente salvar(Cliente cliente, Authentication authentication) {
        if (cliente.getId() == null) {
            Cliente clienteSalvo = clienteRepository.save(cliente);

            logService.registrar(
                    authentication,
                    "CRIACAO_CLIENTE",
                    "Cliente",
                    clienteSalvo.getId(),
                    List.of(
                            new Log.AlteracaoCampo(
                                    "nome",
                                    null,
                                    clienteSalvo.getNome()
                            )
                    ),
                    null
            );
            return clienteSalvo;
        } else {
            if (clienteRepository.existsById(cliente.getId())) {
                return clienteRepository.save(cliente);  // Atualiza o cliente existente
            } else {
                throw new IllegalArgumentException("Cliente não encontrado para atualização.");
            }
        }
    }

}
