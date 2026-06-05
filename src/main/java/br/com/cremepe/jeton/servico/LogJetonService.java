package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.LogJeton;
import br.com.cremepe.jeton.repositorio.LogJetonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LogJetonService {

    private static final Logger log = LoggerFactory.getLogger(LogJetonService.class);

    @Autowired
    private LogJetonRepository logRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarLog(String nomeTabela, Integer idUsuario, String textoLog) {
        if (idUsuario == null) {
            log.warn("Tentativa de registrar log sem usuário associado. Tabela: {}, Texto: {}", nomeTabela, textoLog);
            return;
        }

        LogJeton logJeton = new LogJeton();
        logJeton.setNomeTabela(nomeTabela);
        logJeton.setIdUsuario(idUsuario);
        logJeton.setDataHoraLog(LocalDateTime.now());
        logJeton.setTextoLog(textoLog);

        logRepository.save(logJeton);
        log.debug("Log registrado: tabela={}, usuario={}", nomeTabela, idUsuario);
    }

    @Transactional(readOnly = true)
    public List<LogJeton> listarLogsPorUsuario(Integer idUsuario) {
        return logRepository.findByIdUsuarioOrderByDataHoraLogDesc(idUsuario);
    }

    @Transactional(readOnly = true)
    public List<LogJeton> listarLogsPorTabelaEPeriodo(String nomeTabela, LocalDateTime inicio, LocalDateTime fim) {
        return logRepository.findByNomeTabelaAndDataHoraLogBetween(nomeTabela, inicio, fim);
    }

    @Transactional(readOnly = true)
    public Page<LogJeton> listarComFiltros(String nomeTabela, LocalDateTime dataInicio, LocalDateTime dataFim,
            Pageable pageable) {
        return logRepository.pesquisarComFiltros(nomeTabela, dataInicio, dataFim, pageable);
    }
}