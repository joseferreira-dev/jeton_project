package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.domain.Regras;
import br.com.cremepe.jeton.domain.RegrasConjuntas;
import br.com.cremepe.jeton.repository.RegrasConjuntasRepository;
import br.com.cremepe.jeton.util.RegraValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RegrasConjuntasService {

    private static final Logger log = LoggerFactory.getLogger(RegrasConjuntasService.class);

    private final RegrasConjuntasRepository repository;
    private final LogJetonService logJetonService;
    private final RegrasService regrasService;
    private final RegraValidator regraValidator;

    public RegrasConjuntasService(RegrasConjuntasRepository repository,
            LogJetonService logJetonService,
            RegrasService regrasService,
            RegraValidator regraValidator) {
        this.repository = repository;
        this.logJetonService = logJetonService;
        this.regrasService = regrasService;
        this.regraValidator = regraValidator;
    }

    @Transactional
    public RegrasConjuntas criar(RegrasConjuntas regra) {
        regra.setIdRegraConjunta(null);
        RegrasConjuntas salva = salvar(regra, true);
        logJetonService.logRegraConjuntaCriada(salva);
        return salva;
    }

    @Transactional
    public RegrasConjuntas atualizar(RegrasConjuntas regra) {
        if (regra.getIdRegraConjunta() == null) {
            throw new RuntimeException("ID da regra conjunta não informado para atualização.");
        }
        if (!repository.existsById(regra.getIdRegraConjunta())) {
            throw new RuntimeException("Regra conjunta não encontrada para atualização.");
        }
        RegrasConjuntas antiga = repository.findById(regra.getIdRegraConjunta())
                .orElseThrow(() -> new RuntimeException("Regra conjunta não encontrada."));
        RegrasConjuntas copia = copiarRegraConjunta(antiga);

        RegrasConjuntas atualizada = salvar(regra, false);
        logJetonService.logRegraConjuntaAtualizada(copia, atualizada);
        return atualizada;
    }

    private RegrasConjuntas salvar(RegrasConjuntas regra, boolean isNovo) {
        // Validações
        regraValidator.validarNomeRegraConjuntaUnico(regra.getNomeRegra(), isNovo ? null : regra.getIdRegraConjunta());
        regraValidator.validarPontosLimitePositivo(regra.getPontosLimite());
        regraValidator.validarRegrasAgrupadasNaoVazias(regra);

        // Normaliza flags
        if (regra.getNomeRegra() != null) {
            regra.setNomeRegra(regra.getNomeRegra().trim());
        }
        if (regra.getInTipoLimite() == null || regra.getInTipoLimite().trim().isEmpty()) {
            regra.setInTipoLimite(RegrasConjuntas.TIPO_LIMITE_DIARIO);
        } else {
            regra.setInTipoLimite(regra.getInTipoLimite().toUpperCase());
        }
        if (regra.getPontosLimite() == null) {
            regra.setPontosLimite(0);
        }

        RegrasConjuntas regraParaSalvar;
        if (isNovo) {
            regraParaSalvar = regra;
        } else {
            // Carrega a entidade existente
            regraParaSalvar = repository.findById(regra.getIdRegraConjunta())
                    .orElseThrow(() -> new RuntimeException("Regra conjunta não encontrada para atualização."));
            // Atualiza campos simples
            regraParaSalvar.setNomeRegra(regra.getNomeRegra());
            regraParaSalvar.setInTipoLimite(regra.getInTipoLimite());
            regraParaSalvar.setPontosLimite(regra.getPontosLimite());
            // Atualiza a lista de regras agrupadas
            regraParaSalvar.getRegrasAgrupadas().clear();
            if (regra.getRegrasAgrupadas() != null) {
                regraParaSalvar.getRegrasAgrupadas().addAll(regra.getRegrasAgrupadas());
            }
        }

        RegrasConjuntas salva = repository.save(regraParaSalvar);
        log.info("Regra Conjunta {}: id={}, nome='{}', tipoLimite={}, pontosLimite={}",
                isNovo ? "criada" : "atualizada",
                salva.getIdRegraConjunta(), salva.getNomeRegra(),
                salva.getInTipoLimite(), salva.getPontosLimite());
        return salva;
    }

    @Transactional
    public void excluir(Integer id) {
        RegrasConjuntas regra = buscarOuFalhar(id);
        RegrasConjuntas copia = copiarRegraConjunta(regra);

        // Coleta os nomes das regras associadas antes de remover
        String regrasVinculadas = "";
        if (regra.getRegrasAgrupadas() != null && !regra.getRegrasAgrupadas().isEmpty()) {
            regrasVinculadas = regra.getRegrasAgrupadas().stream()
                    .map(Regras::getNomeRegra)
                    .collect(Collectors.joining("; "));
            // Remove todas as associações (limpa a tabela de ligação)
            regra.getRegrasAgrupadas().clear();
            repository.save(regra);
            log.info("Associações de regras removidas para o agrupamento id={}, regras=[{}]", id, regrasVinculadas);
        }

        repository.deleteById(id);
        log.info("Regra Conjunta excluída: id={}, nome='{}', tipoLimite={}, pontosLimite={}",
                id, copia.getNomeRegra(), copia.getInTipoLimite(), copia.getPontosLimite());

        logJetonService.logRegraConjuntaExcluida(copia, regrasVinculadas);
    }

    @Transactional(readOnly = true)
    public List<RegrasConjuntas> listarTodos() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<RegrasConjuntas> buscarPorId(Integer id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public RegrasConjuntas buscarOuFalhar(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Regra Conjunta não encontrada com ID: " + id));
    }

    @Transactional(readOnly = true)
    public Page<RegrasConjuntas> listarComPaginacaoEPesquisa(String termo, String tipoLimite,
            int page, int size, String sortField, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();
        Pageable pageable = (size == 0) ? Pageable.unpaged(sort) : PageRequest.of(page, size, sort);
        return repository.findAllByFilters(termo, tipoLimite, pageable);
    }

    private RegrasConjuntas copiarRegraConjunta(RegrasConjuntas original) {
        RegrasConjuntas copia = new RegrasConjuntas();
        copia.setIdRegraConjunta(original.getIdRegraConjunta());
        copia.setNomeRegra(original.getNomeRegra());
        copia.setInTipoLimite(original.getInTipoLimite());
        copia.setPontosLimite(original.getPontosLimite());
        if (original.getRegrasAgrupadas() != null) {
            copia.setRegrasAgrupadas(new ArrayList<>(original.getRegrasAgrupadas()));
        }
        return copia;
    }
}