package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.Log;
import com.umc.pediupatrao.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Objects;

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
                                    "nome", null, clienteSalvo.getNome()
                            )
                    ),
                    null
            );

            return clienteSalvo;
        }

        Cliente anterior = clienteRepository.findById(cliente.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cliente não encontrado para atualização."
                ));

        List<Log.AlteracaoCampo> alteracoes = new ArrayList<>();

        adicionarAlteracao(alteracoes, "nome", anterior.getNome(), cliente.getNome());
        adicionarAlteracao(alteracoes, "telefone", anterior.getTelefone(), cliente.getTelefone());
        adicionarAlteracao(alteracoes, "cep", anterior.getCep(), cliente.getCep());
        adicionarAlteracao(alteracoes, "logradouro", anterior.getLogradouro(), cliente.getLogradouro());
        adicionarAlteracao(alteracoes, "numero", anterior.getNumero(), cliente.getNumero());
        adicionarAlteracao(alteracoes, "complemento", anterior.getComplemento(), cliente.getComplemento());
        adicionarAlteracao(alteracoes, "bairro", anterior.getBairro(), cliente.getBairro());
        adicionarAlteracao(alteracoes, "cidade", anterior.getCidade(), cliente.getCidade());
        adicionarAlteracao(alteracoes, "estado", anterior.getEstado(), cliente.getEstado());
        adicionarAlteracao(alteracoes, "endereco", anterior.getEndereco(), cliente.getEndereco());

        Cliente atualizado = clienteRepository.save(cliente);

        if (!alteracoes.isEmpty()) {
            logService.registrar(
                    authentication,
                    "ALTERACAO_CLIENTE",
                    "Cliente",
                    atualizado.getId(),
                    alteracoes,
                    null
            );
        }

        return atualizado;
    }

    private void adicionarAlteracao(
            List<Log.AlteracaoCampo> alteracoes,
            String campo,
            String valorAnterior,
            String valorNovo) {

        if (!Objects.equals(valorAnterior, valorNovo)) {
            alteracoes.add(
                    new Log.AlteracaoCampo(campo, valorAnterior, valorNovo)
            );
        }
    }
}
