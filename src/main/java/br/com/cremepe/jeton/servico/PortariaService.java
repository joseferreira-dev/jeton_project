package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.Portaria;
import br.com.cremepe.jeton.repositorio.PortariaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PortariaService {

    @Autowired
    private PortariaRepository repository;

    @Transactional(readOnly = true)
    public List<Portaria> listarTodos() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Portaria> buscarPorId(Integer id) {
        return repository.findById(id);
    }

    @Transactional
    public Portaria salvar(Portaria portaria) {
        return repository.save(portaria);
    }

    @Transactional
    public void excluir(Integer id) {
        repository.deleteById(id);
    }
}