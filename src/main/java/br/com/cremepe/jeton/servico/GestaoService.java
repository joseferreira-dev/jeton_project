package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.anotacao.Auditar;
import br.com.cremepe.jeton.dominio.Gestao;
import br.com.cremepe.jeton.repository.GestaoRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private GestaoRepository gestaoRepository;

    // =========================================================================
    // OPERAÇÕES DE ESCRITA
    // =========================================================================

    @Auditar(tabela = "gestao", acao = "CRIAR", capturarEstadoAnterior = false, descricao = "Criação de nova gestão", auditarExcecao = true)
    @Transactional
    public Gestao criarGestao(Gestao gestao) {
        validarDatas(gestao);
        validarNomeUnico(gestao);
        validarSobreposicao(gestao);

        Gestao salva = gestaoRepository.save(gestao);
        log.info("Gestão criada: ID={}, nome='{}', período={} até {}",
                salva.getIdGestao(), salva.getNomeGestao(),
                salva.getDtInicio(), salva.getDtFim());
        return salva;
    }

    @Auditar(tabela = "gestao", acao = "ATUALIZAR", capturarEstadoAnterior = true, descricao = "Edição de gestão", auditarExcecao = true)
    @Transactional
    public Gestao atualizarGestao(Gestao gestao) {
        Gestao existente = buscarGestaoOuFalhar(gestao.getIdGestao());

        validarDatas(gestao);
        validarNomeUnico(gestao);
        validarSobreposicao(gestao);

        existente.setNomeGestao(gestao.getNomeGestao());
        existente.setDtInicio(gestao.getDtInicio());
        existente.setDtFim(gestao.getDtFim());

        Gestao atualizada = gestaoRepository.save(existente);
        log.info("Gestão atualizada: ID={}, nome='{}', período={} até {}",
                atualizada.getIdGestao(), atualizada.getNomeGestao(),
                atualizada.getDtInicio(), atualizada.getDtFim());
        return atualizada;
    }

    @Auditar(tabela = "gestao", acao = "EXCLUIR", capturarEstadoAnterior = true, descricao = "Exclusão de gestão", auditarExcecao = true)
    @Transactional
    public void excluirGestao(Gestao gestao) {
        Gestao existente = buscarGestaoOuFalhar(gestao.getIdGestao());
        gestaoRepository.deleteById(existente.getIdGestao());
        log.info("Gestão excluída: ID={}, nome='{}', período={} até {}",
                existente.getIdGestao(), existente.getNomeGestao(),
                existente.getDtInicio(), existente.getDtFim());
    }

    // =========================================================================
    // MÉTODOS DE VALIDAÇÃO E LEITURA
    // =========================================================================

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
}