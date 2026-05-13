package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.PontosSaldo;
import br.com.cremepe.jeton.repositorio.PontosSaldoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PontosRemanescentesService {

    @Autowired
    private PontosSaldoRepository repository;

    @Transactional(readOnly = true)
    public List<PontosSaldo> listarTodos() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<PontosSaldo> buscarPorId(Integer id) {
        return repository.findById(id);
    }

    @Transactional
    public PontosSaldo salvar(PontosSaldo pontos) {
        return repository.save(pontos);
    }

    @Transactional
    public void excluir(Integer id) {
        repository.deleteById(id);
    }
}