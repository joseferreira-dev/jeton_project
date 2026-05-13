package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.RegrasConjuntas;
import br.com.cremepe.jeton.repositorio.RegrasConjuntasRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class RegrasConjuntasService {

    @Autowired
    private RegrasConjuntasRepository repository;

    @Transactional(readOnly = true)
    public List<RegrasConjuntas> listarTodos() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<RegrasConjuntas> buscarPorId(Integer id) {
        return repository.findById(id);
    }

    @Transactional
    public RegrasConjuntas salvar(RegrasConjuntas regra) {
        return repository.save(regra);
    }

    @Transactional
    public void excluir(Integer id) {
        repository.deleteById(id);
    }
}