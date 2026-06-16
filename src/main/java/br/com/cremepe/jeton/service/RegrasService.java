package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.domain.Portaria;
import br.com.cremepe.jeton.domain.Regras;
import br.com.cremepe.jeton.domain.Resolucao;
import br.com.cremepe.jeton.repository.AtividadeConselhalRepository;
import br.com.cremepe.jeton.repository.PortariaRepository;
import br.com.cremepe.jeton.repository.RegrasRepository;
import br.com.cremepe.jeton.repository.ResolucaoRepository;
import br.com.cremepe.jeton.util.RegraValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class RegrasService {

    private static final Logger log = LoggerFactory.getLogger(RegrasService.class);

    private final RegrasRepository repository;
    private final ResolucaoRepository resolucaoRepository;
    private final PortariaRepository portariaRepository;
    private final AtividadeConselhalRepository atividadeRepository;
    private final LogJetonService logJetonService;
    private final RegraValidator regraValidator;

    public RegrasService(RegrasRepository repository,
            ResolucaoRepository resolucaoRepository,
            PortariaRepository portariaRepository,
            AtividadeConselhalRepository atividadeRepository,
            LogJetonService logJetonService,
            RegraValidator regraValidator) {
        this.repository = repository;
        this.resolucaoRepository = resolucaoRepository;
        this.portariaRepository = portariaRepository;
        this.atividadeRepository = atividadeRepository;
        this.logJetonService = logJetonService;
        this.regraValidator = regraValidator;
    }

    @Transactional
    public Regras criar(Regras regra) {
        regra.setIdRegra(null);
        Regras salva = salvarRegra(regra, true);
        logJetonService.logRegraCriada(salva);
        return salva;
    }

    @Transactional
    public Regras atualizar(Regras regra) {
        if (regra.getIdRegra() == null) {
            throw new RuntimeException("ID da regra não informado para atualização.");
        }
        if (!repository.existsById(regra.getIdRegra())) {
            throw new RuntimeException("Regra não encontrada para atualização.");
        }
        Regras antiga = repository.findById(regra.getIdRegra())
                .orElseThrow(() -> new RuntimeException("Regra não encontrada."));
        Regras copia = copiarRegra(antiga);

        Regras atualizada = salvarRegra(regra, false);
        logJetonService.logRegraAtualizada(copia, atualizada);
        return atualizada;
    }

    private Regras salvarRegra(Regras regra, boolean isNovo) {
        if (regra.getResolucao() == null || regra.getResolucao().getIdResolucao() == null) {
            throw new RuntimeException("A resolução é obrigatória. Selecione uma resolução válida.");
        }
        Resolucao resolucaoManaged = resolucaoRepository.findById(regra.getResolucao().getIdResolucao())
                .orElseThrow(() -> new RuntimeException(
                        "Resolução não encontrada com ID: " + regra.getResolucao().getIdResolucao()));
        regra.setResolucao(resolucaoManaged);

        if (regra.getPortaria() != null && regra.getPortaria().getIdPortaria() != null) {
            Portaria portariaManaged = portariaRepository.findById(regra.getPortaria().getIdPortaria())
                    .orElseThrow(() -> new RuntimeException(
                            "Portaria não encontrada com ID: " + regra.getPortaria().getIdPortaria()));
            regra.setPortaria(portariaManaged);
        } else {
            regra.setPortaria(null);
        }

        regraValidator.validarNomeRegraUnico(regra.getNomeRegra(), isNovo ? null : regra.getIdRegra());
        regraValidator.validarPortariaNaoRevogada(regra.getPortaria());

        // Normaliza flags
        if (regra.getInRevogado() == null || regra.getInRevogado().trim().isEmpty()) {
            regra.setInRevogado(Regras.REVOGADO_NAO);
        } else {
            regra.setInRevogado(regra.getInRevogado().toUpperCase());
        }
        if (regra.getInJudicante() == null || regra.getInJudicante().trim().isEmpty()) {
            regra.setInJudicante(Regras.JUDICANTE_NAO);
        } else {
            regra.setInJudicante(regra.getInJudicante().toUpperCase());
        }
        if (regra.getPontos() == null)
            regra.setPontos(0);
        if (regra.getPontosLimitesTurno() == null)
            regra.setPontosLimitesTurno(0);
        if (regra.getNomeRegra() != null)
            regra.setNomeRegra(regra.getNomeRegra().trim());

        Regras salva = repository.save(regra);
        log.info("Regra {}: id={}, nome='{}', pontos={}, limiteTurno={}, judicante={}, revogado={}",
                isNovo ? "criada" : "atualizada",
                salva.getIdRegra(), salva.getNomeRegra(), salva.getPontos(),
                salva.getPontosLimitesTurno(), salva.getInJudicante(), salva.getInRevogado());
        return salva;
    }

    @Transactional
    public void revogar(Integer id) {
        Regras regra = buscarOuFalhar(id);
        regraValidator.validarRegraNaoRevogada(regra);
        Regras copia = copiarRegra(regra);
        regra.setInRevogado(Regras.REVOGADO_SIM);
        repository.save(regra);
        log.info("Regra revogada: id={}, nome={}", id, regra.getNomeRegra());
        logJetonService.logRegraRevogada(copia);
    }

    @Transactional
    public void restaurar(Integer id) {
        Regras regra = buscarOuFalhar(id);
        regraValidator.validarRegraRevogada(regra);
        Regras copia = copiarRegra(regra);
        regra.setInRevogado(Regras.REVOGADO_NAO);
        repository.save(regra);
        log.info("Regra restaurada: id={}, nome={}", id, regra.getNomeRegra());
        logJetonService.logRegraRestaurada(copia);
    }

    @Transactional
    public void excluir(Integer id) {
        Regras regra = buscarOuFalhar(id);
        long countAtividades = atividadeRepository.countByRegraIdRegra(id);
        regraValidator.validarExclusaoRegra(regra, countAtividades);
        Regras copia = copiarRegra(regra);
        repository.deleteById(id);
        log.info("Regra excluída: id={}, nome={}", id, regra.getNomeRegra());
        logJetonService.logRegraExcluida(copia);
    }

    @Transactional(readOnly = true)
    public List<Regras> listarTodos() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Regras> buscarPorId(Integer id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public Regras buscarOuFalhar(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Regra não encontrada com ID: " + id));
    }

    @Transactional(readOnly = true)
    public Page<Regras> listarComPaginacaoEPesquisa(String termo, String situacao, String judicante,
            int page, int size, String sortField, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();
        Pageable pageable = (size == 0) ? Pageable.unpaged(sort) : PageRequest.of(page, size, sort);
        return repository.pesquisarPaginado(termo, situacao, judicante, pageable);
    }

    @Transactional(readOnly = true)
    public List<Resolucao> listarResolucoesComRegras() {
        return repository.findResolucoesComRegras();
    }

    @Transactional(readOnly = true)
    public List<Portaria> listarPortariasComRegras() {
        return repository.findPortariasComRegras();
    }

    @Transactional(readOnly = true)
    public List<Portaria> listarPortariasCompativeis(Integer idResolucao) {
        return repository.findPortariasCompativeis(idResolucao);
    }

    @Transactional(readOnly = true)
    public List<Resolucao> listarResolucoesCompativeis(Integer idPortaria) {
        return repository.findResolucoesCompativeis(idPortaria);
    }

    @Transactional(readOnly = true)
    public List<Regras> listarRegrasExatas(Integer idResolucao, Integer idPortaria) {
        return repository.findRegrasExatas(idResolucao, idPortaria);
    }

    @Transactional(readOnly = true)
    public List<Regras> listarRegrasPorNormativasInclusiveRevogadas(Integer idResolucao, Integer idPortaria) {
        return repository.findRegrasPorNormativasInclusiveRevogadas(idResolucao, idPortaria);
    }

    @Transactional(readOnly = true)
    public Optional<Resolucao> buscarResolucaoPorData(LocalDate data) {
        List<Resolucao> lista = repository.findResolucaoPorData(data);
        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }

    @Transactional(readOnly = true)
    public Optional<Portaria> buscarPortariaPorData(LocalDate data) {
        List<Portaria> lista = repository.findPortariaPorData(data);
        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }

    @Transactional(readOnly = true)
    public List<Regras> listarRegrasPorResolucao(Integer resolucaoId) {
        if (resolucaoId == null) {
            return repository.findAll();
        }
        return repository.findByResolucaoIdResolucao(resolucaoId);
    }

    private Regras copiarRegra(Regras original) {
        Regras copia = new Regras();
        copia.setIdRegra(original.getIdRegra());
        copia.setNomeRegra(original.getNomeRegra());
        copia.setDescricao(original.getDescricao());
        copia.setPontos(original.getPontos());
        copia.setInRevogado(original.getInRevogado());
        copia.setPontosLimitesTurno(original.getPontosLimitesTurno());
        copia.setInJudicante(original.getInJudicante());
        copia.setResolucao(original.getResolucao());
        copia.setPortaria(original.getPortaria());
        return copia;
    }
}