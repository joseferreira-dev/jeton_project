package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.RegrasConjuntas;
import br.com.cremepe.jeton.repositorio.RegrasConjuntasRepository;
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
public class RegrasConjuntasService {

    private static final Logger log = LoggerFactory.getLogger(RegrasConjuntasService.class);

    @Autowired
    private RegrasConjuntasRepository repository;

    // =========================================================================
    // OPERAÇÕES DE ESCRITA
    // =========================================================================

    @Transactional
    public RegrasConjuntas salvar(RegrasConjuntas regra) {
        validarNomeUnico(regra);
        normalizarFlags(regra);
        RegrasConjuntas salva = repository.save(regra);
        log.info("Regra Conjunta salva: id={}, nome={}", salva.getIdRegraConjunta(), salva.getNomeRegra());
        return salva;
    }

    private void validarNomeUnico(RegrasConjuntas regra) {
        Integer idAtual = regra.getIdRegraConjunta() != null ? regra.getIdRegraConjunta() : 0;
        boolean existe = repository.existsByNomeRegraAndIdRegraConjuntaNot(regra.getNomeRegra(), idAtual);
        if (existe) {
            throw new RuntimeException(
                    "Já existe uma regra conjunta cadastrada com o nome '" + regra.getNomeRegra() + "'.");
        }
    }

    private void normalizarFlags(RegrasConjuntas regra) {
        if (regra.getInTipoLimite() == null || regra.getInTipoLimite().trim().isEmpty()) {
            regra.setInTipoLimite(RegrasConjuntas.TIPO_LIMITE_DIARIO);
        }
        if (regra.getPontosLimite() == null) {
            regra.setPontosLimite(0);
        }
    }

    @Transactional
    public void excluir(Integer id) {
        RegrasConjuntas regra = buscarOuFalhar(id);

        // Remove todas as associações com regras (limpa a tabela de ligação)
        if (regra.getRegrasAgrupadas() != null && !regra.getRegrasAgrupadas().isEmpty()) {
            regra.getRegrasAgrupadas().clear();
            repository.save(regra);
            log.info("Associações de regras removidas para o agrupamento id={}", id);
        }

        // Agora pode excluir o agrupamento
        repository.deleteById(id);
        log.info("Regra Conjunta excluída: id={}", id);
    }

    // =========================================================================
    // OPERAÇÕES DE LEITURA
    // =========================================================================

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
        return repository.pesquisarPaginado(termo, tipoLimite, pageable);
    }
}