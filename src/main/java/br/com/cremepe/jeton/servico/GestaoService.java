package br.com.cremepe.jeton.servico;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.cremepe.jeton.dominio.Gestao;
import br.com.cremepe.jeton.repositorio.GestaoRepository;

@Service
public class GestaoService {

    @Autowired
    private GestaoRepository gestaoRepository;

    @Transactional
    public Gestao salvar(Gestao gestao) {
        
        // 1. Validação de Nome Duplicado
        if (gestao.getNomeGestao() != null && !gestao.getNomeGestao().trim().isEmpty()) {
            Optional<Gestao> existente = gestaoRepository.findByNomeGestao(gestao.getNomeGestao().trim());
            
            // Se encontrou uma gestão com esse nome, verifica se não é a própria gestão que estamos a editar
            if (existente.isPresent() && 
               (gestao.getIdGestao() == null || !gestao.getIdGestao().equals(existente.get().getIdGestao()))) {
                throw new RuntimeException("Já existe uma gestão cadastrada com o nome '" + gestao.getNomeGestao() + "'.");
            }
        }

        // 2. Validação de datas
        if (gestao.getDtInicio() != null && gestao.getDtFim() != null) {
            if (gestao.getDtFim().isBefore(gestao.getDtInicio())) {
                throw new RuntimeException("A data de fim não pode ser anterior à data de início da gestão.");
            }
        }
        
        return gestaoRepository.save(gestao);
    }

    @Transactional(readOnly = true)
    public Page<Gestao> listarComPaginacaoEPesquisa(String termo, int page, int size, String sortField, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();
        Pageable pageable = (size == 0) ? Pageable.unpaged(sort) : PageRequest.of(page, size, sort);
        return gestaoRepository.pesquisarPaginado(termo, pageable);
    }

    @Transactional(readOnly = true)
    public List<Gestao> listarTodos() {
        return gestaoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Gestao> buscarPorId(Integer id) {
        return gestaoRepository.findById(id);
    }

    @Transactional
    public void excluir(Integer id) {
        gestaoRepository.deleteById(id);
    }
}