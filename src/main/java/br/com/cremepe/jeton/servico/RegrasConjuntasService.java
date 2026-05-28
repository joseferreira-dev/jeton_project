package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.Regras;
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
import java.util.stream.Collectors;

@Service
public class RegrasConjuntasService {

    private static final Logger log = LoggerFactory.getLogger(RegrasConjuntasService.class);

    @Autowired
    private RegrasConjuntasRepository repository;
    @Autowired
    private LogJetonService logJetonService;

    // =========================================================================
    // OPERAÇÕES DE ESCRITA
    // =========================================================================

    @Transactional
    public RegrasConjuntas salvar(RegrasConjuntas regra, Integer idUsuarioLogado) {
        boolean isNovo = regra.getIdRegraConjunta() == null;
        String nome = regra.getNomeRegra();
        String tipoLimite = regra.getInTipoLimite();
        Integer pontosLimite = regra.getPontosLimite();

        validarNomeUnico(regra);
        normalizarFlags(regra);
        RegrasConjuntas salva = repository.save(regra);

        // Coleta os nomes (ou IDs) das regras associadas a este grupo
        String regrasVinculadas = "";
        if (salva.getRegrasAgrupadas() != null && !salva.getRegrasAgrupadas().isEmpty()) {
            regrasVinculadas = salva.getRegrasAgrupadas().stream()
                    .map(Regras::getNomeRegra)
                    .collect(Collectors.joining("; "));
        }

        log.info("Regra Conjunta {}: id={}, nome='{}', tipoLimite={}, pontosLimite={}, regras=[{}]",
                isNovo ? "criada" : "atualizada",
                salva.getIdRegraConjunta(), nome, tipoLimite, pontosLimite, regrasVinculadas);

        String textoLog = String.format(
                "Regra Conjunta %s: ID=%d, Nome='%s', TipoLimite='%s', PontosLimite=%d, RegrasVinculadas=[%s]",
                isNovo ? "criada" : "atualizada",
                salva.getIdRegraConjunta(), nome, tipoLimite, pontosLimite, regrasVinculadas);
        logJetonService.registrarLog("regras_conjuntas", idUsuarioLogado, textoLog);

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
    public void excluir(Integer id, Integer idUsuarioLogado) {
        RegrasConjuntas regra = buscarOuFalhar(id);
        String nome = regra.getNomeRegra();
        String tipoLimite = regra.getInTipoLimite();
        Integer pontosLimite = regra.getPontosLimite();

        // Coleta os nomes (ou IDs) das regras associadas antes de remover as
        // associações
        String regrasVinculadas = "";
        if (regra.getRegrasAgrupadas() != null && !regra.getRegrasAgrupadas().isEmpty()) {
            regrasVinculadas = regra.getRegrasAgrupadas().stream()
                    .map(Regras::getNomeRegra)
                    .collect(Collectors.joining("; "));
        }

        // Remove todas as associações com regras (limpa a tabela de ligação)
        if (regra.getRegrasAgrupadas() != null && !regra.getRegrasAgrupadas().isEmpty()) {
            regra.getRegrasAgrupadas().clear();
            repository.save(regra);
            log.info("Associações de regras removidas para o agrupamento id={}, regras=[{}]", id, regrasVinculadas);
        }

        repository.deleteById(id);
        log.info("Regra Conjunta excluída: id={}, nome='{}'", id, nome);

        String textoLog = String.format(
                "Regra Conjunta excluída: ID=%d, Nome='%s', TipoLimite='%s', PontosLimite=%d, RegrasVinculadas=[%s]",
                id, nome, tipoLimite, pontosLimite, regrasVinculadas);
        logJetonService.registrarLog("regras_conjuntas", idUsuarioLogado, textoLog);
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