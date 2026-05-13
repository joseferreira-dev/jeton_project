package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.Parametros;
import br.com.cremepe.jeton.repositorio.ParametrosRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ParametrosService {

    @Autowired
    private ParametrosRepository repository;

    @Transactional(readOnly = true)
    public List<Parametros> listarTodos() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Parametros> buscarPorId(String id) {
        return repository.findById(id);
    }

    @Transactional
    public Parametros salvar(Parametros parametro) {
        return repository.save(parametro);
    }
}