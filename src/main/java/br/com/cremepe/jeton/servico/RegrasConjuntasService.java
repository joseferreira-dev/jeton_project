package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.RegrasConjuntas;
import br.com.cremepe.jeton.repositorio.RegrasConjuntasRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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

    @Transactional(readOnly = true)
    public Page<RegrasConjuntas> listarComPaginacaoEPesquisa(String termo, String tipoLimite, int page, int size, String sortField, String sortDir) {
        org.springframework.data.domain.Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            org.springframework.data.domain.Sort.by(sortField).descending() : org.springframework.data.domain.Sort.by(sortField).ascending();
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, sort);
        return repository.pesquisarPaginado(termo, tipoLimite, pageable);
    }
}