package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.Portaria;
import br.com.cremepe.jeton.dominio.Regras;
import br.com.cremepe.jeton.dominio.Resolucao;
import br.com.cremepe.jeton.repositorio.RegrasRepository;
import br.com.cremepe.jeton.repositorio.AtividadeConselhalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class RegrasService {

    @Autowired private RegrasRepository repository;
    @Autowired private AtividadeConselhalRepository atividadeRepository;

    @Transactional(readOnly = true)
    public List<Regras> listarTodos() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Regras> buscarPorId(Integer id) {
        return repository.findById(id);
    }

    @Transactional
    public Regras salvar(Regras regra) {
        if (regra.getInRevogado() == null || regra.getInRevogado().trim().isEmpty()) {
            regra.setInRevogado("N");
        }
        return repository.save(regra);
    }

    @Transactional
    public void revogar(Integer id) {
        repository.findById(id).ifPresent(r -> {
            r.setInRevogado("S");
            repository.save(r);
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
        if (atividadeRepository.countByRegraIdRegra(id) > 0) {
            throw new RuntimeException("Não é possível excluir: existem atividades de conselheiros lançadas com esta Regra. Use a opção 'Revogar'.");
        }
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Resolucao> listarResolucoesComRegras() { 
        return repository.findResolucoesComRegras(); 
    }

    @Transactional(readOnly = true)
    public List<Portaria> listarPortariasComRegras() { 
        return repository.findPortariasComRegras(); 
    }

    @Transactional(readOnly = true)
    public List<Portaria> listarPortariasCompativeis(Integer idResolucao) { 
        return repository.findPortariasCompativeis(idResolucao); 
    }

    @Transactional(readOnly = true)
    public List<Resolucao> listarResolucoesCompativeis(Integer idPortaria) { 
        return repository.findResolucoesCompativeis(idPortaria); 
    }

    @Transactional(readOnly = true)
    public List<Regras> listarRegrasExatas(Integer idResolucao, Integer idPortaria) { 
        return repository.findRegrasExatas(idResolucao, idPortaria); 
    }

    @Transactional(readOnly = true)
    public Page<Regras> listarComPaginacaoEPesquisa(String termo, String situacao, int page, int size, String sortField, String sortDir) {
        org.springframework.data.domain.Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            org.springframework.data.domain.Sort.by(sortField).descending() : org.springframework.data.domain.Sort.by(sortField).ascending();
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, sort);
        return repository.pesquisarPaginado(termo, situacao, pageable);
    }
}