package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.domain.Portaria;
import br.com.cremepe.jeton.repository.PortariaRepository;
import br.com.cremepe.jeton.repository.RegrasRepository;
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
public class PortariaService {

    private static final Logger log = LoggerFactory.getLogger(PortariaService.class);

    private final PortariaRepository repository;
    private final RegrasRepository regrasRepository;
    private final LogJetonService logJetonService;
    private final NormativaValidator normativaValidator;

    public PortariaService(PortariaRepository repository,
            RegrasRepository regrasRepository,
            LogJetonService logJetonService,
            NormativaValidator normativaValidator) {
        this.repository = repository;
        this.regrasRepository = regrasRepository;
        this.logJetonService = logJetonService;
        this.normativaValidator = normativaValidator;
    }

    @Transactional
    public Portaria criar(Portaria portaria) {
        portaria.setIdPortaria(null);
        normalizarPortaria(portaria);
        normativaValidator.validarPortaria(
                portaria.getNumero(),
                portaria.getAno(),
                portaria.getDtInicioVigencia(),
                portaria.getDtFimVigencia(),
                null);

        Portaria salva = repository.save(portaria);
        log.info("Portaria criada: id={}, número={}/{}, vigência={} até {}, revogado={}",
                salva.getIdPortaria(), salva.getNumero(), salva.getAno(),
                salva.getDtInicioVigencia(), salva.getDtFimVigencia(), salva.getInRevogado());
        logJetonService.logPortariaCriada(salva);
        return salva;
    }

    @Transactional
    public Portaria atualizar(Portaria portaria) {
        if (portaria.getIdPortaria() == null) {
            throw new RuntimeException("ID da portaria não informado para atualização.");
        }
        if (!repository.existsById(portaria.getIdPortaria())) {
            throw new RuntimeException("Portaria não encontrada para atualização.");
        }

        Portaria antiga = repository.findById(portaria.getIdPortaria())
                .orElseThrow(() -> new RuntimeException("Portaria não encontrada."));
        Portaria copia = copiarPortaria(antiga);

        normalizarPortaria(portaria);
        normativaValidator.validarPortaria(
                portaria.getNumero(),
                portaria.getAno(),
                portaria.getDtInicioVigencia(),
                portaria.getDtFimVigencia(),
                portaria.getIdPortaria());

        antiga.setNumero(portaria.getNumero());
        antiga.setAno(portaria.getAno());
        antiga.setDtInicioVigencia(portaria.getDtInicioVigencia());
        antiga.setDtFimVigencia(portaria.getDtFimVigencia());
        antiga.setLinkPublicado(portaria.getLinkPublicado());
        antiga.setInRevogado(portaria.getInRevogado());

        Portaria atualizada = repository.save(antiga);
        log.info("Portaria atualizada: id={}, número={}/{}, vigência={} até {}, revogado={}",
                atualizada.getIdPortaria(), atualizada.getNumero(), atualizada.getAno(),
                atualizada.getDtInicioVigencia(), atualizada.getDtFimVigencia(), atualizada.getInRevogado());

        logJetonService.logPortariaAtualizada(copia, atualizada);
        return atualizada;
    }

    @Transactional
    public void revogar(Integer id) {
        Portaria portaria = buscarOuFalhar(id);
        if (portaria.isRevogado()) {
            throw new RuntimeException("A portaria já está revogada.");
        }
        Portaria copia = copiarPortaria(portaria);

        portaria.setInRevogado(Portaria.REVOGADO_SIM);
        repository.save(portaria);
        regrasRepository.revogarRegrasPorPortaria(id);

        logJetonService.logPortariaRevogada(copia);
        log.info("Portaria revogada: id={}, número={}/{}", id, portaria.getNumero(), portaria.getAno());
    }

    @Transactional
    public void restaurar(Integer id) {
        Portaria portaria = buscarOuFalhar(id);
        if (!portaria.isRevogado()) {
            throw new RuntimeException("A portaria já está em vigor.");
        }
        Portaria copia = copiarPortaria(portaria);

        portaria.setInRevogado(Portaria.REVOGADO_NAO);
        repository.save(portaria);

        logJetonService.logPortariaRestaurada(copia);
        log.info("Portaria restaurada: id={}, número={}/{}", id, portaria.getNumero(), portaria.getAno());
    }

    @Transactional
    public void excluir(Integer id) {
        Portaria portaria = buscarOuFalhar(id);
        if (!portaria.isRevogado()) {
            throw new RuntimeException("Para excluir, a portaria deve estar revogada primeiro.");
        }
        long countRegras = regrasRepository.countByPortariaIdPortaria(id);
        if (countRegras > 0) {
            throw new RuntimeException("Não é possível excluir a portaria pois existem " + countRegras +
                    " regra(s) vinculada(s). Revogue-as ou exclua-as antes.");
        }

        Portaria copia = copiarPortaria(portaria);
        repository.deleteById(id);

        logJetonService.logPortariaExcluida(copia);
        log.info("Portaria excluída: id={}, número={}/{}", id, portaria.getNumero(), portaria.getAno());
    }

    @Transactional(readOnly = true)
    public List<Portaria> listarTodos() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Portaria> buscarPorId(Integer id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public Portaria buscarOuFalhar(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Portaria não encontrada com ID: " + id));
    }

    @Transactional(readOnly = true)
    public Page<Portaria> listarComPaginacaoEPesquisa(String termo, String situacao, int page, int size,
            String sortField, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();
        Pageable pageable = (size == 0) ? Pageable.unpaged(sort) : PageRequest.of(page, size, sort);
        return repository.findAllByFilters(termo, situacao, pageable);
    }

    private void normalizarPortaria(Portaria portaria) {
        if (portaria.getInRevogado() == null || portaria.getInRevogado().trim().isEmpty()) {
            portaria.setInRevogado(Portaria.REVOGADO_NAO);
        } else {
            portaria.setInRevogado(portaria.getInRevogado().toUpperCase());
        }
        if (portaria.getLinkPublicado() != null) {
            portaria.setLinkPublicado(portaria.getLinkPublicado().trim());
        }
    }

    private Portaria copiarPortaria(Portaria original) {
        Portaria copia = new Portaria();
        copia.setIdPortaria(original.getIdPortaria());
        copia.setNumero(original.getNumero());
        copia.setAno(original.getAno());
        copia.setDtInicioVigencia(original.getDtInicioVigencia());
        copia.setDtFimVigencia(original.getDtFimVigencia());
        copia.setLinkPublicado(original.getLinkPublicado());
        copia.setInRevogado(original.getInRevogado());
        return copia;
    }
}