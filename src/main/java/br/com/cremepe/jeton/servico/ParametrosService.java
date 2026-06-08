package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.annotation.Auditar;
import br.com.cremepe.jeton.dominio.Parametros;
import br.com.cremepe.jeton.repository.ParametrosRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParametrosService {

    private static final Logger log = LoggerFactory.getLogger(ParametrosService.class);

    @Autowired
    private ParametrosRepository repository;

    @Transactional(readOnly = true)
    public boolean isSistemaBloqueado() {
        Parametros params = repository.findById(1).orElseGet(() -> {
            Parametros novo = new Parametros();
            novo.setBloqueaSistema("N");
            return repository.save(novo);
        });
        return "S".equals(params.getBloqueaSistema());
    }

    @Transactional(readOnly = true)
    public String obterStatus() {
        Parametros params = repository.findById(1).orElseGet(() -> {
            Parametros novo = new Parametros();
            novo.setBloqueaSistema("N");
            return repository.save(novo);
        });
        return params.getBloqueaSistema();
    }

    @Auditar(tabela = "parametros", acao = "ALTERAR_BLOQUEIO", descricao = "Alterna o bloqueio do sistema", capturarEstadoAnterior = true, auditarExcecao = true, incluirRetorno = true)
    @Transactional
    public String alternarBloqueio() {
        Parametros params = repository.findById(1).orElseGet(Parametros::new);
        String novoStatus = "S".equals(params.getBloqueaSistema()) ? "N" : "S";
        params.setBloqueaSistema(novoStatus);
        repository.save(params);
        log.info("Status do bloqueio alterado para: {}", "S".equals(novoStatus) ? "BLOQUEADO" : "LIBERADO");
        return novoStatus == "S" ? "BLOQUEADO" : "LIBERADO";
    }
}