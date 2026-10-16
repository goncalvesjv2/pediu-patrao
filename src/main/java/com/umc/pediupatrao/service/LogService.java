package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Log;
import com.umc.pediupatrao.repository.LogRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LogService {
    private final LogRepository logRepository;

    public LogService(LogRepository logRepository) {
        this.logRepository = logRepository;
    }

    public Log registrar(Authentication authentication, String operacao, String entidade, String entidadeId, List<Log.AlteracaoCampo> alteracoes, String justificativa) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("É necessário identificar o usuário para registrar a auditoria.");
        }

        Log log = new Log();

        log.setUsuario(authentication.getName());
        log.setPerfil(obterPerfil(authentication));
        log.setDataHora(LocalDateTime.now());
        log.setOperacao(operacao);
        log.setEntidade(entidade);
        log.setEntidadeId(entidadeId);
        log.setAlteracoes(alteracoes == null ? List.of() : alteracoes);
        log.setJustificativa(justificativa);

        return logRepository.save(log);
    }

    private String obterPerfil(Authentication authentication) {
        return authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(autoridade -> autoridade.startsWith("ROLE_"))
                .map(autoridade -> autoridade.substring(5))
                .findFirst()
                .orElse("NAO_IDENTIFICADO");
    }
}
