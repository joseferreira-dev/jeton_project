package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.Gestao;
import br.com.cremepe.jeton.repositorio.GestaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class GestaoService {

    private static final Logger log = LoggerFactory.getLogger(GestaoService.class);

    @Autowired
    private GestaoRepository gestaoRepository;
    @Autowired
    private LogJetonService logJetonService;

    // =========================================================================
    // OPERAÇÕES DE ESCRITA
    // =========================================================================

    @Transactional
    public Gestao salvar(Gestao gestao, Integer idUsuarioLogado) {
        boolean isNovo = gestao.getIdGestao() == null;

        validarDatas(gestao);
        validarNomeUnico(gestao);
        validarSobreposicao(gestao);

        Gestao salva = gestaoRepository.save(gestao);

        log.info("Gestão {}: ID={}, nome='{}', período={} até {}",
                isNovo ? "criada" : "atualizada",
                salva.getIdGestao(), salva.getNomeGestao(), salva.getDtInicio(), salva.getDtFim());

        String textoLog = String.format(
                "Gestão %s: ID=%d, Nome='%s', Início=%s, Fim=%s",
                isNovo ? "criada" : "atualizada",
                salva.getIdGestao(), salva.getNomeGestao(), salva.getDtInicio(), salva.getDtFim());
        logJetonService.registrarLog("gestao", idUsuarioLogado, textoLog);

        return salva;
    }

    private void validarDatas(Gestao gestao) {
        if (gestao.getDtInicio() == null || gestao.getDtFim() == null) {
            throw new RuntimeException("As datas de início e fim são obrigatórias.");
        }
        if (!gestao.getDtFim().isAfter(gestao.getDtInicio())) {
            throw new RuntimeException("A data de fim deve ser posterior à data de início.");
        }
    }

    private void validarNomeUnico(Gestao gestao) {
        if (gestao.getNomeGestao() != null && !gestao.getNomeGestao().trim().isEmpty()) {
            boolean existe = gestaoRepository.existsByNomeGestaoIgnorandoId(
                    gestao.getNomeGestao().trim(), gestao.getIdGestao());
            if (existe) {
                throw new RuntimeException(
                        "Já existe uma gestão cadastrada com o nome '" + gestao.getNomeGestao() + "'.");
            }
        }
    }

    private void validarSobreposicao(Gestao gestao) {
        boolean sobrepoe = gestaoRepository.existsPeriodoSobreposto(
                gestao.getIdGestao(), gestao.getDtInicio(), gestao.getDtFim());
        if (sobrepoe) {
            throw new RuntimeException("O período selecionado coincide com uma gestão já cadastrada.");
        }
    }

    // =========================================================================
    // OPERAÇÕES DE LEITURA
    // =========================================================================

    @Transactional(readOnly = true)
    public Page<Gestao> listarComPaginacaoEPesquisa(String termo, int page, int size,
            String sortField, String sortDir) {
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

    @Transactional(readOnly = true)
    public Gestao buscarGestaoOuFalhar(Integer id) {
        return gestaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Gestão não encontrada com ID: " + id));
    }

    // =========================================================================
    // EXCLUSÃO
    // =========================================================================

    @Transactional
    public void excluir(Integer id, Integer idUsuarioLogado) {
        // Busca a gestão antes de excluir para obter dados para o log
        Optional<Gestao> gestaoOpt = gestaoRepository.findById(id);
        if (gestaoOpt.isEmpty()) {
            log.warn("Tentativa de excluir gestão inexistente ID={}", id);
            throw new RuntimeException("Gestão não encontrada para exclusão.");
        }
        Gestao gestao = gestaoOpt.get();
        String nome = gestao.getNomeGestao();
        LocalDate dtInicio = gestao.getDtInicio();
        LocalDate dtFim = gestao.getDtFim();

        gestaoRepository.deleteById(id);

        log.info("Gestão excluída: ID={}, nome='{}', período={} até {}", id, nome, dtInicio, dtFim);

        String textoLog = String.format(
                "Gestão excluída: ID=%d, Nome='%s', Início=%s, Fim=%s",
                id, nome, dtInicio, dtFim);
        logJetonService.registrarLog("gestao", idUsuarioLogado, textoLog);
    }
}