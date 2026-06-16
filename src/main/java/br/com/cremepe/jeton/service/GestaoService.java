package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.domain.Gestao;
import br.com.cremepe.jeton.repository.GestaoRepository;
import br.com.cremepe.jeton.util.GestaoValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class GestaoService {

    private static final Logger log = LoggerFactory.getLogger(GestaoService.class);

    private final GestaoRepository gestaoRepository;
    private final LogJetonService logJetonService;
    private final GestaoValidator gestaoValidator;

    public GestaoService(GestaoRepository gestaoRepository,
            LogJetonService logJetonService,
            GestaoValidator gestaoValidator) {
        this.gestaoRepository = gestaoRepository;
        this.logJetonService = logJetonService;
        this.gestaoValidator = gestaoValidator;
    }

    @Transactional
    public Gestao criar(Gestao gestao) {
        gestao.setIdGestao(null);
        gestaoValidator.validarGestao(gestao);

        Gestao salva = gestaoRepository.save(gestao);
        log.info("Gestão criada: ID={}, nome='{}', período={} até {}",
                salva.getIdGestao(), salva.getNomeGestao(),
                salva.getDtInicio(), salva.getDtFim());

        logJetonService.logGestaoCriada(salva);
        return salva;
    }

    @Transactional
    public Gestao atualizar(Gestao gestao) {
        if (gestao.getIdGestao() == null) {
            throw new RuntimeException("ID da gestão não informado para atualização.");
        }

        Gestao existente = buscarGestaoOuFalhar(gestao.getIdGestao());
        Gestao copia = copiarGestao(existente);

        gestaoValidator.validarGestao(gestao);

        existente.setNomeGestao(gestao.getNomeGestao());
        existente.setDtInicio(gestao.getDtInicio());
        existente.setDtFim(gestao.getDtFim());

        Gestao atualizada = gestaoRepository.save(existente);
        log.info("Gestão atualizada: ID={}, nome='{}', período={} até {}",
                atualizada.getIdGestao(), atualizada.getNomeGestao(),
                atualizada.getDtInicio(), atualizada.getDtFim());

        logJetonService.logGestaoAtualizada(copia, atualizada);
        return atualizada;
    }

    @Transactional
    public void excluir(Integer id) {
        Gestao existente = buscarGestaoOuFalhar(id);
        Gestao copia = copiarGestao(existente);

        gestaoRepository.deleteById(id);
        log.info("Gestão excluída: ID={}, nome='{}', período={} até {}",
                existente.getIdGestao(), existente.getNomeGestao(),
                existente.getDtInicio(), existente.getDtFim());

        logJetonService.logGestaoExcluida(copia);
    }

    @Transactional(readOnly = true)
    public Page<Gestao> listarComPaginacaoEPesquisa(String termo, int page, int size,
            String sortField, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();
        Pageable pageable = (size == 0) ? Pageable.unpaged(sort) : PageRequest.of(page, size, sort);
        return gestaoRepository.findByNomeGestaoContainingIgnoreCase(termo, pageable);
    }

    @Transactional(readOnly = true)
    public List<Gestao> listarTodos() {
        return gestaoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Gestao> buscarPorId(Integer id) {
        return gestaoRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Gestao buscarGestaoOuFalhar(Integer id) {
        return gestaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Gestão não encontrada com ID: " + id));
    }

    private Gestao copiarGestao(Gestao original) {
        Gestao copia = new Gestao();
        copia.setIdGestao(original.getIdGestao());
        copia.setNomeGestao(original.getNomeGestao());
        copia.setDtInicio(original.getDtInicio());
        copia.setDtFim(original.getDtFim());
        return copia;
    }
}