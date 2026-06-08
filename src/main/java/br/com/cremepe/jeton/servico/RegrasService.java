package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.anotacao.Auditar;
import br.com.cremepe.jeton.dominio.Portaria;
import br.com.cremepe.jeton.dominio.Regras;
import br.com.cremepe.jeton.dominio.Resolucao;
import br.com.cremepe.jeton.repository.AtividadeConselhalRepository;
import br.com.cremepe.jeton.repository.PortariaRepository;
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
public class RegrasService {

    private static final Logger log = LoggerFactory.getLogger(RegrasService.class);

    @Autowired
    private RegrasRepository repository;
    @Autowired
    private ResolucaoRepository resolucaoRepository;
    @Autowired
    private PortariaRepository portariaRepository;
    @Autowired
    private AtividadeConselhalRepository atividadeRepository;

    // =========================================================================
    // OPERAÇÕES DE ESCRITA
    // =========================================================================

    @Auditar(tabela = "regras", acao = "CRIAR", descricao = "Criação de nova regra de pontuação", dadosParametros = "{ 'nomeRegra': #regra.nomeRegra, 'pontos': #regra.pontos, 'pontosLimitesTurno': #regra.pontosLimitesTurno, 'inJudicante': #regra.inJudicante, 'idResolucao': #regra.resolucao?.idResolucao, 'idPortaria': #regra.portaria?.idPortaria }", dadosRetorno = "#result", capturarEstadoAnterior = false, auditarExcecao = true)
    @Transactional
    public Regras criar(Regras regra) {
        regra.setIdRegra(null); // força criação
        return salvarRegra(regra, true);
    }

    @Auditar(tabela = "regras", acao = "ATUALIZAR", descricao = "Atualização de regra de pontuação existente", dadosParametros = "{ 'id': #regra.idRegra, 'nomeRegra': #regra.nomeRegra, 'pontos': #regra.pontos, 'pontosLimitesTurno': #regra.pontosLimitesTurno, 'inJudicante': #regra.inJudicante, 'idResolucao': #regra.resolucao?.idResolucao, 'idPortaria': #regra.portaria?.idPortaria }", dadosRetorno = "#result", capturarEstadoAnterior = true, auditarExcecao = true)
    @Transactional
    public Regras atualizar(Regras regra) {
        if (regra.getIdRegra() == null) {
            throw new RuntimeException("ID da regra não informado para atualização.");
        }
        if (!repository.existsById(regra.getIdRegra())) {
            throw new RuntimeException("Regra não encontrada para atualização.");
        }
        return salvarRegra(regra, false);
    }

    /**
     * Método privado com a lógica comum de persistência.
     * 
     * @param isNovo true para criação, false para atualização
     */
    private Regras salvarRegra(Regras regra, boolean isNovo) {
        // Carrega a Resolução gerenciada
        Resolucao resolucaoManaged = carregarResolucaoGerenciada(regra.getResolucao());
        regra.setResolucao(resolucaoManaged);

        // Carrega a Portaria gerenciada (se existir)
        if (regra.getPortaria() != null && regra.getPortaria().getIdPortaria() != null) {
            Portaria portariaManaged = carregarPortariaGerenciada(regra.getPortaria().getIdPortaria());
            regra.setPortaria(portariaManaged);
        } else {
            regra.setPortaria(null);
        }

        validarPortaria(regra);
        validarDuplicidade(regra);
        normalizarFlags(regra);

        Regras salva = repository.save(regra);
        log.info("Regra {}: id={}, nome='{}', pontos={}, limiteTurno={}, judicante={}, revogado={}",
                isNovo ? "criada" : "atualizada",
                salva.getIdRegra(), salva.getNomeRegra(), salva.getPontos(),
                salva.getPontosLimitesTurno(), salva.getInJudicante(), salva.getInRevogado());
        return salva;
    }

    private Resolucao carregarResolucaoGerenciada(Resolucao resolucao) {
        if (resolucao == null || resolucao.getIdResolucao() == null) {
            throw new RuntimeException("A resolução é obrigatória. Selecione uma resolução válida.");
        }
        return resolucaoRepository.findById(resolucao.getIdResolucao())
                .orElseThrow(
                        () -> new RuntimeException("Resolução não encontrada com ID: " + resolucao.getIdResolucao()));
    }

    private Portaria carregarPortariaGerenciada(Integer idPortaria) {
        if (idPortaria == null)
            return null;
        return portariaRepository.findById(idPortaria)
                .orElseThrow(() -> new RuntimeException("Portaria não encontrada com ID: " + idPortaria));
    }

    private void validarPortaria(Regras regra) {
        if (regra.getPortaria() != null && regra.getPortaria().isRevogado()) {
            throw new RuntimeException("Não é possível vincular a regra a uma portaria revogada.");
        }
    }

    private void validarDuplicidade(Regras regra) {
        Integer idAtual = regra.getIdRegra() != null ? regra.getIdRegra() : 0;
        boolean existe = repository.existsByNomeRegraAndIdRegraNot(regra.getNomeRegra(), idAtual);
        if (existe) {
            throw new RuntimeException("Já existe uma regra cadastrada com o nome '" + regra.getNomeRegra() + "'.");
        }
    }

    private void normalizarFlags(Regras regra) {
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
    }

    // =========================================================================
    // REVOGAÇÃO
    // =========================================================================

    @Auditar(tabela = "regras", acao = "REVOGAR", descricao = "Revogação de regra de pontuação", dadosParametros = "{ 'id': #id }", capturarEstadoAnterior = true, auditarExcecao = true, incluirRetorno = false)
    @Transactional
    public void revogar(Integer id) {
        Regras regra = buscarOuFalhar(id);
        if (regra.isRevogado()) {
            throw new RuntimeException("A regra já está revogada.");
        }
        regra.setInRevogado(Regras.REVOGADO_SIM);
        repository.save(regra);
        log.info("Regra revogada: id={}, nome={}", id, regra.getNomeRegra());
    }

    // =========================================================================
    // RESTAURAÇÃO
    // =========================================================================

    @Auditar(tabela = "regras", acao = "RESTAURAR", descricao = "Restauração de regra revogada (volta a ficar em vigor)", dadosParametros = "{ 'id': #id }", capturarEstadoAnterior = true, auditarExcecao = true, incluirRetorno = false)
    @Transactional
    public void restaurar(Integer id) {
        Regras regra = buscarOuFalhar(id);
        if (!regra.isRevogado()) {
            throw new RuntimeException("A regra já está em vigor.");
        }
        regra.setInRevogado(Regras.REVOGADO_NAO);
        repository.save(regra);
        log.info("Regra restaurada: id={}, nome={}", id, regra.getNomeRegra());
    }

    // =========================================================================
    // EXCLUSÃO
    // =========================================================================

    @Auditar(tabela = "regras", acao = "EXCLUIR", descricao = "Exclusão permanente de regra", dadosParametros = "{ 'id': #id }", capturarEstadoAnterior = true, auditarExcecao = true, incluirRetorno = false)
    @Transactional
    public void excluir(Integer id) {
        Regras regra = buscarOuFalhar(id);
        if (!regra.isRevogado()) {
            throw new RuntimeException("Para excluir, a regra deve estar revogada primeiro.");
        }
        long countAtividades = atividadeRepository.countByRegraIdRegra(id);
        if (countAtividades > 0) {
            throw new RuntimeException("Não é possível excluir a regra pois existem " + countAtividades +
                    " atividade(s) vinculada(s) a ela. Revogue-a ou use 'Revogar' em vez disso.");
        }
        repository.deleteById(id);
        log.info("Regra excluída: id={}, nome={}", id, regra.getNomeRegra());
    }

    // =========================================================================
    // OPERAÇÕES DE LEITURA
    // =========================================================================

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

    // =========================================================================
    // MÉTODOS AUXILIARES UTILIZADOS POR OUTROS SERVIÇOS
    // =========================================================================

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
}