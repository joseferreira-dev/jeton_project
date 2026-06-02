package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.Parametros;
import br.com.cremepe.jeton.repositorio.ParametrosRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParametrosService {

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

    @Transactional
    public void alternarBloqueio() {
        Parametros params = repository.findById(1).orElseGet(Parametros::new);
        String novoStatus = "S".equals(params.getBloqueaSistema()) ? "N" : "S";
        params.setBloqueaSistema(novoStatus);
        repository.save(params);
    }
}