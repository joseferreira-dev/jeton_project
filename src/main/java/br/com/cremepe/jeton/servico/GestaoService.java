package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.Gestao;
import br.com.cremepe.jeton.repositorio.GestaoRepository;
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

    @Autowired
    private GestaoRepository gestaoRepository;

    // =========================================================================
    // OPERAÇÕES DE ESCRITA
    // =========================================================================

    @Transactional
    public Gestao salvar(Gestao gestao) {
        validarDatas(gestao);
        validarNomeUnico(gestao);
        validarSobreposicao(gestao);
        return gestaoRepository.save(gestao);
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
    public void excluir(Integer id) {
        // Aqui você pode adicionar verificações de integridade (ex: se existem
        // conselheiros ou atividades vinculados)
        // Antes de excluir, o controller já trata a mensagem de erro, mas você pode
        // lançar exceção aqui.
        gestaoRepository.deleteById(id);
    }
}