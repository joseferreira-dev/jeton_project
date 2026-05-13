package br.com.cremepe.jeton.servico;
import br.com.cremepe.jeton.dominio.Gestao;
import br.com.cremepe.jeton.repositorio.GestaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class GestaoService {
    @Autowired private GestaoRepository repository;
    public List<Gestao> listarTodos() { return repository.findAll(); }
    public Optional<Gestao> buscarPorId(Integer id) { return repository.findById(id); }
    public Gestao salvar(Gestao obj) { return repository.save(obj); }
    public void excluir(Integer id) { repository.deleteById(id); }
}