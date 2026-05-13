package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.Resolucao;
import br.com.cremepe.jeton.repositorio.ResolucaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ResolucaoService {

    @Autowired
    private ResolucaoRepository repository;

    @Transactional(readOnly = true)
    public List<Resolucao> listarTodos() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Resolucao> buscarPorId(Integer id) {
        return repository.findById(id);
    }

    @Transactional
    public Resolucao salvar(Resolucao resolucao) {
        return repository.save(resolucao);
    }

    @Transactional
    public void excluir(Integer id) {
        repository.deleteById(id);
    }
}