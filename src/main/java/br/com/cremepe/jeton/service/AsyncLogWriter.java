package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.domain.LogJeton;
import br.com.cremepe.jeton.domain.Usuario;
import br.com.cremepe.jeton.repository.LogJetonRepository;
import br.com.cremepe.jeton.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AsyncLogWriter {

    private static final Logger log = LoggerFactory.getLogger(AsyncLogWriter.class);

    private final LogJetonRepository logRepository;
    private final UsuarioRepository usuarioRepository;

    public AsyncLogWriter(LogJetonRepository logRepository, UsuarioRepository usuarioRepository) {
        this.logRepository = logRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Async("taskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeLog(String nomeTabela, Integer idUsuario, String textoLog) {
        try {
            Usuario usuario = usuarioRepository.getReferenceById(idUsuario);
            LogJeton logJeton = new LogJeton();
            logJeton.setNomeTabela(nomeTabela);
            logJeton.setUsuario(usuario);
            logJeton.setDataHoraLog(LocalDateTime.now());
            logJeton.setTextoLog(textoLog);
            logRepository.save(logJeton);
            log.debug("Log assíncrono registrado: tabela={}, usuário={}", nomeTabela, idUsuario);
        } catch (Exception e) {
            log.error("Falha ao registrar log assíncrono", e);
        }
    }
}