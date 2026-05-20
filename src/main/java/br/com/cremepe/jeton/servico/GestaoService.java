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
        if (gestao.getDtInicio() == null || gestao.getDtFim() == null) {
            throw new RuntimeException("As datas de início e fim são obrigatórias.");
        }

        if (gestao.getDtFim().isBefore(gestao.getDtInicio())) {
            throw new RuntimeException("A data de fim não pode ser anterior ao início.");
        }

        if (gestao.getNomeGestao() != null && !gestao.getNomeGestao().trim().isEmpty()) {
            Optional<Gestao> gestaoExistente = gestaoRepository.findByNomeGestao(gestao.getNomeGestao().trim());
            if (gestaoExistente.isPresent()) {
                if (gestao.getIdGestao() == null || !gestao.getIdGestao().equals(gestaoExistente.get().getIdGestao())) {
                    throw new RuntimeException(
                            "Já existe uma gestão cadastrada com o nome '" + gestao.getNomeGestao() + "'.");
                }
            }
        }

        if (gestaoRepository.existeSobreposicao(gestao.getIdGestao(), gestao.getDtInicio(), gestao.getDtFim())) {
            throw new RuntimeException("O período selecionado coincide com uma gestão já cadastrada.");
        }

        return gestaoRepository.save(gestao);
    }

    @Transactional(readOnly = true)
    public Page<Gestao> listarComPaginacaoEPesquisa(String termo, int page, int size, String sortField,
            String sortDir) {
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