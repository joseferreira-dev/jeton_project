package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.annotation.Auditar;
import br.com.cremepe.jeton.dominio.Resolucao;
import br.com.cremepe.jeton.repository.RegrasRepository;
import br.com.cremepe.jeton.repository.ResolucaoRepository;

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

    @Auditar(tabela = "resolucao", acao = "CRIAR", descricao = "Criação de nova resolução", dadosParametros = "{ 'numero': #resolucao.numero, 'ano': #resolucao.ano, 'dtInicioVigencia': #resolucao.dtInicioVigencia, 'dtFimVigencia': #resolucao.dtFimVigencia, 'ementa': #resolucao.ementa, 'pontosPorJeton': #resolucao.pontosPorJeton, 'maxJetonsDia': #resolucao.maxJetonsDia, 'maxJetonsPeriodo': #resolucao.maxJetonsPeriodo, 'maxJetonsMes': #resolucao.maxJetonsMes, 'valorJeton': #resolucao.valorJeton, 'linkPublicado': #resolucao.linkPublicado }", dadosRetorno = "#result", capturarEstadoAnterior = false, auditarExcecao = true)
    @Transactional
    public Resolucao criar(Resolucao resolucao) {
        resolucao.setIdResolucao(null);
        return salvarResolucao(resolucao, true);
    }

    @Auditar(tabela = "resolucao", acao = "ATUALIZAR", descricao = "Atualização de resolução existente", dadosParametros = "{ 'id': #resolucao.idResolucao, 'numero': #resolucao.numero, 'ano': #resolucao.ano, 'dtInicioVigencia': #resolucao.dtInicioVigencia, 'dtFimVigencia': #resolucao.dtFimVigencia, 'ementa': #resolucao.ementa, 'pontosPorJeton': #resolucao.pontosPorJeton, 'maxJetonsDia': #resolucao.maxJetonsDia, 'maxJetonsPeriodo': #resolucao.maxJetonsPeriodo, 'maxJetonsMes': #resolucao.maxJetonsMes, 'valorJeton': #resolucao.valorJeton, 'linkPublicado': #resolucao.linkPublicado }", dadosRetorno = "#result", capturarEstadoAnterior = true, auditarExcecao = true)
    @Transactional
    public Resolucao atualizar(Resolucao resolucao) {
        if (resolucao.getIdResolucao() == null) {
            throw new RuntimeException("ID da resolução não informado para atualização.");
        }
        if (!repository.existsById(resolucao.getIdResolucao())) {
            throw new RuntimeException("Resolução não encontrada para atualização.");
        }
        return salvarResolucao(resolucao, false);
    }

    /**
     * Método privado com a lógica comum de persistência.
     * 
     * @param isNovo true para criação, false para atualização
     */
    private Resolucao salvarResolucao(Resolucao resolucao, boolean isNovo) {
        validarUnicidade(resolucao);
        validarSobreposicao(resolucao);
        normalizarFlags(resolucao);

        Resolucao salva = repository.save(resolucao);
        log.info("Resolução {}: id={}, número={}/{}, vigência={} até {}, revogado={}",
                isNovo ? "criada" : "atualizada",
                salva.getIdResolucao(), salva.getNumero(), salva.getAno(),
                salva.getDtInicioVigencia(), salva.getDtFimVigencia(), salva.getInRevogado());
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
        if (inicio == null)
            return;
        LocalDate fimValidacao = (fim != null) ? fim : LocalDate.of(9999, 12, 31);
        Integer idResolucao = (resolucao.getIdResolucao() != null) ? resolucao.getIdResolucao() : 0;
        boolean sobrepoe = repository.existePeriodoSobreposto(idResolucao, inicio, fimValidacao);
        if (sobrepoe) {
            throw new RuntimeException(
                    "Já existe uma resolução cadastrada cujo período de vigência coincide com o informado. Verifique as datas.");
        }
    }

    private void normalizarFlags(Resolucao resolucao) {
        if (resolucao.getInRevogado() == null || resolucao.getInRevogado().trim().isEmpty()) {
            resolucao.setInRevogado(Resolucao.REVOGADO_NAO);
        } else {
            resolucao.setInRevogado(resolucao.getInRevogado().toUpperCase());
        }
        if (resolucao.getLinkPublicado() != null) {
            resolucao.setLinkPublicado(resolucao.getLinkPublicado().trim());
        }
        if (resolucao.getEmenta() != null) {
            resolucao.setEmenta(resolucao.getEmenta().trim());
        }
        // Garante que valores numéricos não sejam nulos
        if (resolucao.getPontosPorJeton() == null)
            resolucao.setPontosPorJeton(3);
        if (resolucao.getMaxJetonsDia() == null)
            resolucao.setMaxJetonsDia(3);
        if (resolucao.getMaxJetonsPeriodo() == null)
            resolucao.setMaxJetonsPeriodo(1);
        if (resolucao.getMaxJetonsMes() == null)
            resolucao.setMaxJetonsMes(22);
        if (resolucao.getValorJeton() == null)
            resolucao.setValorJeton(java.math.BigDecimal.ZERO);
    }

    // =========================================================================
    // REVOGAÇÃO
    // =========================================================================

    @Auditar(tabela = "resolucao", acao = "REVOGAR", descricao = "Revogação de resolução", dadosParametros = "{ 'id': #id }", capturarEstadoAnterior = true, auditarExcecao = true, incluirRetorno = false)
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

    // =========================================================================
    // RESTAURAÇÃO
    // =========================================================================

    @Auditar(tabela = "resolucao", acao = "RESTAURAR", descricao = "Restauração de resolução revogada (volta a ficar em vigor)", dadosParametros = "{ 'id': #id }", capturarEstadoAnterior = true, auditarExcecao = true, incluirRetorno = false)
    @Transactional
    public void restaurar(Integer id) {
        Resolucao resolucao = buscarOuFalhar(id);
        if (!resolucao.isRevogado()) {
            throw new RuntimeException("A resolução já está em vigor.");
        }
        resolucao.setInRevogado(Resolucao.REVOGADO_NAO);
        repository.save(resolucao);
        regrasRepository.restaurarRegrasPorResolucao(id);
        log.info("Resolução restaurada: id={}, número={}/{}", id, resolucao.getNumero(), resolucao.getAno());
    }

    // =========================================================================
    // EXCLUSÃO
    // =========================================================================

    @Auditar(tabela = "resolucao", acao = "EXCLUIR", descricao = "Exclusão permanente de resolução", dadosParametros = "{ 'id': #id }", capturarEstadoAnterior = true, auditarExcecao = true, incluirRetorno = false)
    @Transactional
    public void excluir(Integer id) {
        Resolucao resolucao = buscarOuFalhar(id);
        if (!resolucao.isRevogado()) {
            throw new RuntimeException("Para excluir, a resolução deve estar revogada primeiro.");
        }
        long countRegras = regrasRepository.countByResolucaoIdResolucao(id);
        if (countRegras > 0) {
            throw new RuntimeException("Não é possível excluir a resolução pois existem " + countRegras +
                    " regra(s) vinculada(s). Revogue-as ou exclua-as antes.");
        }
        repository.deleteById(id);
        log.info("Resolução excluída fisicamente: id={}, número={}/{}", id, resolucao.getNumero(), resolucao.getAno());
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