package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.GestaoConselheiro;
import br.com.cremepe.jeton.dominio.GestaoConselheiroId;
import br.com.cremepe.jeton.repositorio.GestaoConselheiroRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class GestaoConselheiroService {

    @Autowired
    private GestaoConselheiroRepository repository;

    @Transactional(readOnly = true)
    public List<GestaoConselheiro> listarTodos() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<GestaoConselheiro> buscarPorId(GestaoConselheiroId id) {
        return repository.findById(id);
    }

    @Transactional
    public GestaoConselheiro salvar(GestaoConselheiro vinculo) {
        return repository.save(vinculo);
    }

    @Transactional
    public void excluir(GestaoConselheiroId id) {
        repository.deleteById(id);
    }
}