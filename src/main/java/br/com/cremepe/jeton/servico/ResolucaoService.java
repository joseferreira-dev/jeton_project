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
    @Autowired
    private LogJetonService logJetonService;

    // =========================================================================
    // OPERAÇÕES DE ESCRITA
    // =========================================================================

    @Transactional
    public Resolucao salvar(Resolucao resolucao, Integer idUsuarioLogado) {
        boolean isNovo = resolucao.getIdResolucao() == null;
        Integer numero = resolucao.getNumero();
        Integer ano = resolucao.getAno();
        LocalDate dtInicio = resolucao.getDtInicioVigencia();
        LocalDate dtFim = resolucao.getDtFimVigencia();
        String ementa = resolucao.getEmenta();
        Integer pontosPorJeton = resolucao.getPontosPorJeton();
        Integer maxJetonsDia = resolucao.getMaxJetonsDia();
        Integer maxJetonsPeriodo = resolucao.getMaxJetonsPeriodo();
        Integer maxJetonsMes = resolucao.getMaxJetonsMes();
        String valorJeton = resolucao.getValorJeton() != null ? resolucao.getValorJeton().toString() : "null";

        validarUnicidade(resolucao);
        validarSobreposicao(resolucao);
        if (resolucao.getInRevogado() == null || resolucao.getInRevogado().trim().isEmpty()) {
            resolucao.setInRevogado(Resolucao.REVOGADO_NAO);
        }

        Resolucao salva = repository.save(resolucao);

        log.info("Resolução {}: id={}, número={}/{}, vigência={} até {}, revogado={}",
                isNovo ? "criada" : "atualizada",
                salva.getIdResolucao(), numero, ano, dtInicio, dtFim, salva.getInRevogado());

        String textoLog = String.format(
                "Resolução %s: ID=%d, Número=%d/%d, Início Vigência=%s, Fim Vigência=%s, Ementa=%s, Pontos por Jeton=%d, MaxJetonsDia=%d, MaxJetonsPeriodo=%d, MaxJetonsMes=%d, Valor Jeton=%s, Revogado='%s'",
                isNovo ? "criada" : "atualizada",
                salva.getIdResolucao(), numero, ano,
                dtInicio != null ? dtInicio : "null",
                dtFim != null ? dtFim : "null",
                ementa != null ? ementa.substring(0, Math.min(ementa.length(), 100)) : "null",
                pontosPorJeton, maxJetonsDia, maxJetonsPeriodo, maxJetonsMes, valorJeton,
                salva.getInRevogado());
        logJetonService.registrarLog("resolucao", idUsuarioLogado, textoLog);

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
    public void revogar(Integer id, Integer idUsuarioLogado) {
        Resolucao resolucao = buscarOuFalhar(id);
        if (resolucao.isRevogado()) {
            throw new RuntimeException("A resolução já está revogada.");
        }
        String numeroAno = resolucao.getNumero() + "/" + resolucao.getAno();
        resolucao.setInRevogado(Resolucao.REVOGADO_SIM);
        repository.save(resolucao);
        regrasRepository.revogarRegrasPorResolucao(id);
        log.info("Resolução revogada: id={}, número={}", id, numeroAno);

        String textoLog = String.format(
                "Resolução revogada: ID=%d, Número=%d/%d, Início Vigência=%s, Fim Vigência=%s",
                id, resolucao.getNumero(), resolucao.getAno(),
                resolucao.getDtInicioVigencia(), resolucao.getDtFimVigencia());
        logJetonService.registrarLog("resolucao", idUsuarioLogado, textoLog);
    }

    @Transactional
    public void restaurar(Integer id, Integer idUsuarioLogado) {
        Resolucao resolucao = buscarOuFalhar(id);
        if (!resolucao.isRevogado()) {
            throw new RuntimeException("A resolução já está em vigor.");
        }
        resolucao.setInRevogado(Resolucao.REVOGADO_NAO);
        repository.save(resolucao);
        regrasRepository.restaurarRegrasPorResolucao(id);
        log.info("Resolução restaurada: id={}, número={}/{}", id, resolucao.getNumero(), resolucao.getAno());

        String textoLog = String.format(
                "Resolução restaurada (e suas regras vinculadas): ID=%d, Número=%d/%d",
                id, resolucao.getNumero(), resolucao.getAno());
        logJetonService.registrarLog("resolucao", idUsuarioLogado, textoLog);
    }

    @Transactional
    public void excluirFisicamente(Integer id, Integer idUsuarioLogado) {
        Resolucao resolucao = buscarOuFalhar(id);
        if (!resolucao.isRevogado()) {
            throw new RuntimeException("Para excluir, a resolução deve estar revogada primeiro.");
        }
        long countRegras = regrasRepository.countByResolucaoIdResolucao(id);
        if (countRegras > 0) {
            throw new RuntimeException("Não é possível excluir a resolução pois existem " + countRegras +
                    " regra(s) vinculada(s). Revogue-as ou exclua-as antes.");
        }
        String numeroAno = resolucao.getNumero() + "/" + resolucao.getAno();
        repository.deleteById(id);
        log.info("Resolução excluída: id={}, número={}", id, numeroAno);

        String textoLog = String.format(
                "Resolução excluída: ID=%d, Número=%d/%d",
                id, resolucao.getNumero(), resolucao.getAno());
        logJetonService.registrarLog("resolucao", idUsuarioLogado, textoLog);
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