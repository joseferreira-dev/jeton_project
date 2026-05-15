package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.Portaria;
import br.com.cremepe.jeton.repositorio.PortariaRepository;
import br.com.cremepe.jeton.repositorio.RegrasRepository;

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
public class PortariaService {

    @Autowired private PortariaRepository repository;
    @Autowired private RegrasRepository regrasRepository;

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
        if (portaria.getInRevogado() == null || portaria.getInRevogado().trim().isEmpty()) {
            portaria.setInRevogado("N");
        }
        return repository.save(portaria);
    }

    @Transactional
    public void revogar(Integer id) {
        repository.findById(id).ifPresent(p -> {
            p.setInRevogado("S");
            repository.save(p);
        });
    }

    @Transactional
    public void restaurar(Integer id) {
        repository.findById(id).ifPresent(p -> {
            p.setInRevogado("N");
            repository.save(p);
        });
    }

    @Transactional
    public void excluirFisicamente(Integer id) {
        if (regrasRepository.countByPortariaIdPortaria(id) > 0) {
            throw new RuntimeException("Não é possível excluir: existem regras vinculadas a esta Portaria. Use a opção 'Revogar' em vez disso.");
        }
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Page<Portaria> listarComPaginacaoEPesquisa(String termo, String situacao, int page, int size, String sortField, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();
        Pageable pageable = (size == 0) ? Pageable.unpaged(sort) : PageRequest.of(page, size, sort);
        return repository.pesquisarPaginado(termo, situacao, pageable);
    }    
}