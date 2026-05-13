package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.Regras;
import br.com.cremepe.jeton.repositorio.RegrasRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class RegrasService {

    @Autowired
    private RegrasRepository repository;

    @Transactional(readOnly = true)
    public List<Regras> listarTodos() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Regras> buscarPorId(Integer id) {
        return repository.findById(id);
    }

    @Transactional
    public Regras salvar(Regras regra) {
        return repository.save(regra);
    }

    @Transactional
    public void excluir(Integer id) {
        repository.deleteById(id);
    }
}