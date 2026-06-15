package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.domain.Parametros;
import br.com.cremepe.jeton.repository.ParametrosRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParametrosService {

    private static final Logger log = LoggerFactory.getLogger(ParametrosService.class);

    private final ParametrosRepository repository;
    private final LogJetonService logJetonService;

    public ParametrosService(ParametrosRepository repository, LogJetonService logJetonService) {
        this.repository = repository;
        this.logJetonService = logJetonService;
    }

    private Parametros getParametros() {
        return repository.findById(1).orElseGet(() -> {
            Parametros novo = new Parametros();
            novo.setBloqueaSistema("N");
            return repository.save(novo);
        });
    }

    @Transactional(readOnly = true)
    public boolean isSistemaBloqueado() {
        return "S".equals(getParametros().getBloqueaSistema());
    }

    @Transactional(readOnly = true)
    public String obterStatus() {
        return getParametros().getBloqueaSistema();
    }

    @Transactional
    public String alternarBloqueio() {
        Parametros params = repository.findById(1).orElseGet(Parametros::new);
        String statusAntigo = params.getBloqueaSistema();
        if (statusAntigo == null)
            statusAntigo = "N";

        String statusNovo = "S".equals(statusAntigo) ? "N" : "S";
        params.setBloqueaSistema(statusNovo);
        repository.save(params);

        logJetonService.logBloqueioAlternado(statusAntigo, statusNovo);

        String statusDescricao = "S".equals(statusNovo) ? "BLOQUEADO" : "LIBERADO";
        log.info("Status do bloqueio alterado para: {}", statusDescricao);
        return statusDescricao;
    }
}