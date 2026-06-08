package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.annotation.Auditar;
import br.com.cremepe.jeton.dominio.Regras;
import br.com.cremepe.jeton.dominio.RegrasConjuntas;
import br.com.cremepe.jeton.repository.RegrasConjuntasRepository;

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

    // =========================================================================
    // OPERAÇÕES DE ESCRITA
    // =========================================================================

    @Auditar(tabela = "regras_conjuntas", acao = "CRIAR", descricao = "Criação de novo agrupamento de regras (regras conjuntas)", dadosParametros = "{ 'nomeRegra': #regra.nomeRegra, 'inTipoLimite': #regra.inTipoLimite, 'pontosLimite': #regra.pontosLimite }", capturarEstadoAnterior = false, auditarExcecao = true, incluirRetorno = false)
    @Transactional
    public RegrasConjuntas criar(RegrasConjuntas regra) {
        regra.setIdRegraConjunta(null);
        return salvar(regra, true);
    }

    @Auditar(tabela = "regras_conjuntas", acao = "ATUALIZAR", descricao = "Atualização de agrupamento de regras existente", dadosParametros = "{ 'id': #regra.idRegraConjunta, 'nomeRegra': #regra.nomeRegra, 'inTipoLimite': #regra.inTipoLimite, 'pontosLimite': #regra.pontosLimite }", capturarEstadoAnterior = false, auditarExcecao = true, incluirRetorno = false)
    @Transactional
    public RegrasConjuntas atualizar(RegrasConjuntas regra) {
        if (regra.getIdRegraConjunta() == null) {
            throw new RuntimeException("ID da regra conjunta não informado para atualização.");
        }
        if (!repository.existsById(regra.getIdRegraConjunta())) {
            throw new RuntimeException("Regra conjunta não encontrada para atualização.");
        }
        return salvar(regra, false);
    }

    /**
     * Método privado com a lógica comum de persistência.
     * 
     * @param isNovo true para criação, false para atualização
     */
    private RegrasConjuntas salvar(RegrasConjuntas regra, boolean isNovo) {
        validarNomeUnico(regra, isNovo ? null : regra.getIdRegraConjunta());
        normalizarFlags(regra);

        // Para atualização, precisamos carregar a entidade existente para preservar as
        // associações
        RegrasConjuntas regraParaSalvar;
        if (isNovo) {
            regraParaSalvar = regra;
        } else {
            regraParaSalvar = repository.findById(regra.getIdRegraConjunta())
                    .orElseThrow(() -> new RuntimeException("Regra conjunta não encontrada para atualização."));
            // Atualiza os campos permitidos
            regraParaSalvar.setNomeRegra(regra.getNomeRegra());
            regraParaSalvar.setInTipoLimite(regra.getInTipoLimite());
            regraParaSalvar.setPontosLimite(regra.getPontosLimite());
            // Preserva as regras agrupadas (não alteramos a lista diretamente por aqui)
        }

        RegrasConjuntas salva = repository.save(regraParaSalvar);
        log.info("Regra Conjunta {}: id={}, nome='{}', tipoLimite={}, pontosLimite={}",
                isNovo ? "criada" : "atualizada",
                salva.getIdRegraConjunta(), salva.getNomeRegra(),
                salva.getInTipoLimite(), salva.getPontosLimite());
        return salva;
    }

    private void validarNomeUnico(RegrasConjuntas regra, Integer idAtual) {
        String nome = regra.getNomeRegra();
        if (nome == null || nome.trim().isEmpty())
            return;
        boolean existe = repository.existsByNomeRegraAndIdRegraConjuntaNot(nome.trim(), idAtual != null ? idAtual : 0);
        if (existe) {
            throw new RuntimeException("Já existe uma regra conjunta cadastrada com o nome '" + nome + "'.");
        }
    }

    private void normalizarFlags(RegrasConjuntas regra) {
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
    }

    // =========================================================================
    // EXCLUSÃO
    // =========================================================================
    @Auditar(tabela = "regras_conjuntas", acao = "EXCLUIR", descricao = "Exclusão de agrupamento de regras (remove também as associações)", dadosParametros = "{ 'id': #id }", capturarEstadoAnterior = false, auditarExcecao = true, incluirRetorno = false)
    @Transactional
    public void excluir(Integer id) {
        RegrasConjuntas regra = buscarOuFalhar(id);
        String nome = regra.getNomeRegra();
        String tipoLimite = regra.getInTipoLimite();
        Integer pontosLimite = regra.getPontosLimite();

        // Coleta os nomes das regras associadas antes de remover
        String regrasVinculadas = "";
        if (regra.getRegrasAgrupadas() != null && !regra.getRegrasAgrupadas().isEmpty()) {
            regrasVinculadas = regra.getRegrasAgrupadas().stream()
                    .map(Regras::getNomeRegra)
                    .collect(Collectors.joining("; "));
            // Remove todas as associações (limpa a tabela de ligação)
            regra.getRegrasAgrupadas().clear();
            repository.save(regra); // persistir a remoção das associações
            log.info("Associações de regras removidas para o agrupamento id={}, regras=[{}]", id, regrasVinculadas);
        }

        repository.deleteById(id);
        log.info("Regra Conjunta excluída: id={}, nome='{}', tipoLimite={}, pontosLimite={}",
                id, nome, tipoLimite, pontosLimite);
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