package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.Resolucao;
import br.com.cremepe.jeton.repositorio.RegrasRepository;
import br.com.cremepe.jeton.repositorio.ResolucaoRepository;
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
public class ResolucaoService {

    private static final Logger log = LoggerFactory.getLogger(ResolucaoService.class);

    @Autowired
    private ResolucaoRepository repository;
    @Autowired
    private RegrasRepository regrasRepository;

    // =========================================================================
    // OPERAÇÕES DE ESCRITA
    // =========================================================================

    @Transactional
    public Resolucao salvar(Resolucao resolucao) {
        validarUnicidade(resolucao);
        validarSobreposicao(resolucao);
        if (resolucao.getInRevogado() == null || resolucao.getInRevogado().trim().isEmpty()) {
            resolucao.setInRevogado(Resolucao.REVOGADO_NAO);
        }
        Resolucao salva = repository.save(resolucao);
        log.info("Resolução salva: id={}, número={}/{}", salva.getIdResolucao(), salva.getNumero(), salva.getAno());
        return salva;
    }

    private void validarUnicidade(Resolucao resolucao) {
        Integer idAtual = resolucao.getIdResolucao();
        Integer numero = resolucao.getNumero();
        Integer ano = resolucao.getAno();
        if (numero == null || ano == null)
            return;
        boolean existe = repository.existsByNumeroAndAnoAndIdResolucaoNot(numero, ano, idAtual != null ? idAtual : 0);
        if (existe) {
            throw new RuntimeException("Já existe uma resolução cadastrada com o número " + numero + "/" + ano);
        }
    }

    private void validarSobreposicao(Resolucao resolucao) {
        LocalDate inicio = resolucao.getDtInicioVigencia();
        LocalDate fim = resolucao.getDtFimVigencia();
        if (inicio == null) {
            return;
        }
        // Se fim for nulo, considerar uma data distante (ex: 31/12/9999)
        LocalDate fimParaValidacao = (fim != null) ? fim : LocalDate.of(9999, 12, 31);

        Integer idResolucao = (resolucao.getIdResolucao() != null) ? resolucao.getIdResolucao() : 0;
        boolean sobrepoe = repository.existePeriodoSobreposto(idResolucao, inicio, fimParaValidacao);
        if (sobrepoe) {
            throw new RuntimeException(
                    "Já existe uma resolução cadastrada cujo período de vigência coincide com o informado. Verifique as datas.");
        }
    }

    @Transactional
    public void revogar(Integer id) {
        Resolucao resolucao = buscarOuFalhar(id);
        if (resolucao.isRevogado()) {
            throw new RuntimeException("A resolução já está revogada.");
        }
        resolucao.setInRevogado(Resolucao.REVOGADO_SIM);
        repository.save(resolucao);
        regrasRepository.revogarRegrasPorResolucao(id);
        log.info("Resolução revogada: id={}, número={}/{}", id, resolucao.getNumero(), resolucao.getAno());
    }

    @Transactional
    public void restaurar(Integer id) {
        Resolucao resolucao = buscarOuFalhar(id);
        if (!resolucao.isRevogado()) {
            throw new RuntimeException("A resolução já está em vigor.");
        }
        resolucao.setInRevogado(Resolucao.REVOGADO_NAO);
        repository.save(resolucao);
        regrasRepository.restaurarRegrasPorResolucao(id);
        log.info("Resolução restaurada (e suas regras vinculadas): id={}", id);
    }

    @Transactional
    public void excluirFisicamente(Integer id) {
        Resolucao resolucao = buscarOuFalhar(id);
        if (!resolucao.isRevogado()) {
            throw new RuntimeException("Para excluir fisicamente, a resolução deve estar revogada primeiro.");
        }
        long countRegras = regrasRepository.countByResolucaoIdResolucao(id);
        if (countRegras > 0) {
            throw new RuntimeException("Não é possível excluir a resolução pois existem " + countRegras +
                    " regra(s) vinculada(s). Revogue-as ou exclua-as antes.");
        }
        repository.deleteById(id);
        log.info("Resolução excluída fisicamente: id={}", id);
    }

    // =========================================================================
    // OPERAÇÕES DE LEITURA
    // =========================================================================

    @Transactional(readOnly = true)
    public List<Resolucao> listarTodos() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Resolucao> buscarPorId(Integer id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public Resolucao buscarOuFalhar(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Resolução não encontrada com ID: " + id));
    }

    @Transactional(readOnly = true)
    public Page<Resolucao> listarComPaginacaoEPesquisa(String termo, String situacao, int page, int size,
            String sortField, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();
        Pageable pageable = (size == 0) ? Pageable.unpaged(sort) : PageRequest.of(page, size, sort);
        return repository.pesquisarPaginado(termo, situacao, pageable);
    }
}