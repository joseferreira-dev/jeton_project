package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.NivelAcesso;
import br.com.cremepe.jeton.repositorio.NivelAcessoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class NivelAcessoService {

    @Autowired
    private NivelAcessoRepository repository;

    @Transactional(readOnly = true)
    public List<NivelAcesso> listarTodos() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<NivelAcesso> buscarPorId(String id) {
        return repository.findById(id);
    }

    @Transactional
    public NivelAcesso salvar(NivelAcesso nivel) {
        return repository.save(nivel);
    }

    @Transactional
    public void excluir(String id) {
        repository.deleteById(id);
    }
}