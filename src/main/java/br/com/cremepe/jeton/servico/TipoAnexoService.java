package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.TipoAnexo;
import br.com.cremepe.jeton.repositorio.TipoAnexoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TipoAnexoService {

    @Autowired
    private TipoAnexoRepository repository;

    @Transactional(readOnly = true)
    public List<TipoAnexo> listarTodos() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<TipoAnexo> buscarPorId(Integer id) {
        return repository.findById(id);
    }

    @Transactional
    public TipoAnexo salvar(TipoAnexo tipoAnexo) {
        return repository.save(tipoAnexo);
    }

    @Transactional
    public void excluir(Integer id) {
        repository.deleteById(id);
    }
}