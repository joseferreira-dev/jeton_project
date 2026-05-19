package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.Resolucao;
import br.com.cremepe.jeton.repositorio.RegrasRepository;
import br.com.cremepe.jeton.repositorio.ResolucaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

@Service
public class ResolucaoService {

    @Autowired private ResolucaoRepository repository;
    @Autowired private RegrasRepository regrasRepository;

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
        if (resolucao.getInRevogado() == null || resolucao.getInRevogado().trim().isEmpty()) {
            resolucao.setInRevogado("N");
        }
        return repository.save(resolucao);
    }

    @Transactional
    public void revogar(Integer id) {
        repository.findById(id).ifPresent(r -> {
            r.setInRevogado("S");
            repository.save(r);
            // Revoga as regras associadas
            regrasRepository.revogarRegrasPorResolucao(id);
        });
    }

    @Transactional
    public void restaurar(Integer id) {
        repository.findById(id).ifPresent(r -> {
            r.setInRevogado("N");
            repository.save(r);
        });
    }

    @Transactional
    public void excluirFisicamente(Integer id) {
        if (regrasRepository.countByResolucaoIdResolucao(id) > 0) {
            throw new RuntimeException("Não é possível excluir: existem regras vinculadas a esta Resolução. Use a opção 'Revogar' em vez disso.");
        }
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Page<Resolucao> listarComPaginacaoEPesquisa(String termo, String situacao, int page, int size, String sortField, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();
        Pageable pageable = (size == 0) ? Pageable.unpaged(sort) : PageRequest.of(page, size, sort);
        return repository.pesquisarPaginado(termo, situacao, pageable);
    }
}