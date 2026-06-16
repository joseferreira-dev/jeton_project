package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.domain.Resolucao;
import br.com.cremepe.jeton.repository.RegrasRepository;
import br.com.cremepe.jeton.repository.ResolucaoRepository;
import br.com.cremepe.jeton.util.NormativaValidator;
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
public class ResolucaoService {

    private static final Logger log = LoggerFactory.getLogger(ResolucaoService.class);

    private final ResolucaoRepository repository;
    private final RegrasRepository regrasRepository;
    private final LogJetonService logJetonService;
    private final NormativaValidator normativaValidator;

    public ResolucaoService(ResolucaoRepository repository,
            RegrasRepository regrasRepository,
            LogJetonService logJetonService,
            NormativaValidator normativaValidator) {
        this.repository = repository;
        this.regrasRepository = regrasRepository;
        this.logJetonService = logJetonService;
        this.normativaValidator = normativaValidator;
    }

    @Transactional
    public Resolucao criar(Resolucao resolucao) {
        resolucao.setIdResolucao(null);
        normalizarResolucao(resolucao);
        normativaValidator.validarResolucao(
                resolucao.getNumero(),
                resolucao.getAno(),
                resolucao.getDtInicioVigencia(),
                resolucao.getDtFimVigencia(),
                null);

        Resolucao salva = repository.save(resolucao);
        log.info("Resolução criada: id={}, número={}/{}, vigência={} até {}, revogado={}",
                salva.getIdResolucao(), salva.getNumero(), salva.getAno(),
                salva.getDtInicioVigencia(), salva.getDtFimVigencia(), salva.getInRevogado());
        logJetonService.logResolucaoCriada(salva);
        return salva;
    }

    @Transactional
    public Resolucao atualizar(Resolucao resolucao) {
        if (resolucao.getIdResolucao() == null) {
            throw new RuntimeException("ID da resolução não informado para atualização.");
        }
        if (!repository.existsById(resolucao.getIdResolucao())) {
            throw new RuntimeException("Resolução não encontrada para atualização.");
        }

        Resolucao antiga = repository.findById(resolucao.getIdResolucao())
                .orElseThrow(() -> new RuntimeException("Resolução não encontrada."));
        Resolucao copia = copiarResolucao(antiga);

        normalizarResolucao(resolucao);
        normativaValidator.validarResolucao(
                resolucao.getNumero(),
                resolucao.getAno(),
                resolucao.getDtInicioVigencia(),
                resolucao.getDtFimVigencia(),
                resolucao.getIdResolucao());

        antiga.setNumero(resolucao.getNumero());
        antiga.setAno(resolucao.getAno());
        antiga.setDtInicioVigencia(resolucao.getDtInicioVigencia());
        antiga.setDtFimVigencia(resolucao.getDtFimVigencia());
        antiga.setLinkPublicado(resolucao.getLinkPublicado());
        antiga.setInRevogado(resolucao.getInRevogado());
        antiga.setEmenta(resolucao.getEmenta());
        antiga.setPontosPorJeton(resolucao.getPontosPorJeton());
        antiga.setMaxJetonsDia(resolucao.getMaxJetonsDia());
        antiga.setMaxJetonsPeriodo(resolucao.getMaxJetonsPeriodo());
        antiga.setMaxJetonsMes(resolucao.getMaxJetonsMes());
        antiga.setValorJeton(resolucao.getValorJeton());

        Resolucao atualizada = repository.save(antiga);
        log.info("Resolução atualizada: id={}, número={}/{}, vigência={} até {}, revogado={}",
                atualizada.getIdResolucao(), atualizada.getNumero(), atualizada.getAno(),
                atualizada.getDtInicioVigencia(), atualizada.getDtFimVigencia(), atualizada.getInRevogado());

        logJetonService.logResolucaoAtualizada(copia, atualizada);
        return atualizada;
    }

    @Transactional
    public void revogar(Integer id) {
        Resolucao resolucao = buscarOuFalhar(id);
        if (resolucao.isRevogado()) {
            throw new RuntimeException("A resolução já está revogada.");
        }
        Resolucao copia = copiarResolucao(resolucao);

        resolucao.setInRevogado(Resolucao.REVOGADO_SIM);
        repository.save(resolucao);
        regrasRepository.revogarRegrasPorResolucao(id);

        logJetonService.logResolucaoRevogada(copia);
        log.info("Resolução revogada: id={}, número={}/{}", id, resolucao.getNumero(), resolucao.getAno());
    }

    @Transactional
    public void restaurar(Integer id) {
        Resolucao resolucao = buscarOuFalhar(id);
        if (!resolucao.isRevogado()) {
            throw new RuntimeException("A resolução já está em vigor.");
        }
        Resolucao copia = copiarResolucao(resolucao);

        resolucao.setInRevogado(Resolucao.REVOGADO_NAO);
        repository.save(resolucao);
        regrasRepository.restaurarRegrasPorResolucao(id);

        logJetonService.logResolucaoRestaurada(copia);
        log.info("Resolução restaurada: id={}, número={}/{}", id, resolucao.getNumero(), resolucao.getAno());
    }

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

        Resolucao copia = copiarResolucao(resolucao);
        repository.deleteById(id);

        logJetonService.logResolucaoExcluida(copia);
        log.info("Resolução excluída: id={}, número={}/{}", id, resolucao.getNumero(), resolucao.getAno());
    }

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

    private void normalizarResolucao(Resolucao resolucao) {
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
        if (resolucao.getPontosPorJeton() == null) {
            resolucao.setPontosPorJeton(3);
        }
        if (resolucao.getMaxJetonsDia() == null) {
            resolucao.setMaxJetonsDia(3);
        }
        if (resolucao.getMaxJetonsPeriodo() == null) {
            resolucao.setMaxJetonsPeriodo(1);
        }
        if (resolucao.getMaxJetonsMes() == null) {
            resolucao.setMaxJetonsMes(22);
        }
        if (resolucao.getValorJeton() == null) {
            resolucao.setValorJeton(java.math.BigDecimal.ZERO);
        }
    }

    private Resolucao copiarResolucao(Resolucao original) {
        Resolucao copia = new Resolucao();
        copia.setIdResolucao(original.getIdResolucao());
        copia.setNumero(original.getNumero());
        copia.setAno(original.getAno());
        copia.setDtInicioVigencia(original.getDtInicioVigencia());
        copia.setDtFimVigencia(original.getDtFimVigencia());
        copia.setLinkPublicado(original.getLinkPublicado());
        copia.setInRevogado(original.getInRevogado());
        copia.setEmenta(original.getEmenta());
        copia.setPontosPorJeton(original.getPontosPorJeton());
        copia.setMaxJetonsDia(original.getMaxJetonsDia());
        copia.setMaxJetonsPeriodo(original.getMaxJetonsPeriodo());
        copia.setMaxJetonsMes(original.getMaxJetonsMes());
        copia.setValorJeton(original.getValorJeton());
        return copia;
    }
}