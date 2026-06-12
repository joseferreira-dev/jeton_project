package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.annotation.Auditar;
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

    ParametrosService(ParametrosRepository repository) {
        this.repository = repository;
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

    @Auditar(tabela = "parametros", acao = "ALTERAR_BLOQUEIO", descricao = "Alterna o bloqueio do sistema", capturarEstadoAnterior = true, auditarExcecao = true, incluirRetorno = true)
    @Transactional
    public String alternarBloqueio() {
        Parametros params = repository.findById(1).orElseGet(Parametros::new);
        String novoStatus = "S".equals(params.getBloqueaSistema()) ? "N" : "S";
        params.setBloqueaSistema(novoStatus);
        repository.save(params);

        String statusDescricao = "S".equals(novoStatus) ? "BLOQUEADO" : "LIBERADO";
        log.info("Status do bloqueio alterado para: {}", statusDescricao);
        return statusDescricao;
    }
}