package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.Portaria;
import br.com.cremepe.jeton.dominio.Regras;
import br.com.cremepe.jeton.dominio.Resolucao;
import br.com.cremepe.jeton.repositorio.AtividadeConselhalRepository;
import br.com.cremepe.jeton.repositorio.PortariaRepository;
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
    @Autowired
    private LogJetonService logJetonService;

    // =========================================================================
    // OPERAÇÕES DE ESCRITA (CRUD)
    // =========================================================================

    @Transactional
    public Regras salvar(Regras regra, Integer idUsuarioLogado) {
        boolean isNovo = regra.getIdRegra() == null;
        String nomeRegra = regra.getNomeRegra();
        Integer pontos = regra.getPontos();
        Integer limiteTurno = regra.getPontosLimitesTurno();
        String judicante = regra.getInJudicante();
        String revogado = regra.getInRevogado();

        // Carrega a Resolução gerenciada do banco
        Resolucao resolucaoManaged = carregarResolucaoGerenciada(regra.getResolucao());
        regra.setResolucao(resolucaoManaged);

        // Carrega a Portaria gerenciada (se existir)
        if (regra.getPortaria() != null && regra.getPortaria().getIdPortaria() != null) {
            Portaria portariaManaged = carregarPortariaGerenciada(regra.getPortaria().getIdPortaria());
            regra.setPortaria(portariaManaged);
        } else {
            regra.setPortaria(null);
        }

        // Validações
        validarPortaria(regra);
        validarDuplicidade(regra);
        normalizarFlags(regra);

        // Salva a regra
        Regras salva = repository.save(regra);
        log.info("Regra {}: id={}, nome='{}', pontos={}, limiteTurno={}, judicante={}, revogado={}",
                isNovo ? "criada" : "atualizada",
                salva.getIdRegra(), nomeRegra, pontos, limiteTurno, judicante, revogado);

        // Log de auditoria
        String textoLog = String.format(
                "Regra %s: ID=%d, Nome='%s', Pontos=%d, LimiteTurno=%d, Judicante='%s', Revogado='%s', Resolução='%s' (ID=%d)%s",
                isNovo ? "criada" : "atualizada",
                salva.getIdRegra(), nomeRegra, pontos, limiteTurno, judicante, revogado,
                resolucaoManaged.getNumero() + "/" + resolucaoManaged.getAno(), resolucaoManaged.getIdResolucao(),
                regra.getPortaria() != null
                        ? String.format(", Portaria='%d/%d' (ID=%d)", regra.getPortaria().getNumero(),
                                regra.getPortaria().getAno(), regra.getPortaria().getIdPortaria())
                        : "");
        logJetonService.registrarLog("regras", idUsuarioLogado, textoLog);

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
        }
        if (regra.getInJudicante() == null || regra.getInJudicante().trim().isEmpty()) {
            regra.setInJudicante(Regras.JUDICANTE_NAO);
        }
        if (regra.getPontos() == null)
            regra.setPontos(0);
        if (regra.getPontosLimitesTurno() == null)
            regra.setPontosLimitesTurno(0);
    }

    @Transactional
    public void revogar(Integer id, Integer idUsuarioLogado) {
        Regras regra = buscarOuFalhar(id);
        if (regra.isRevogado()) {
            throw new RuntimeException("A regra já está revogada.");
        }
        String nome = regra.getNomeRegra();
        regra.setInRevogado(Regras.REVOGADO_SIM);
        repository.save(regra);
        log.info("Regra revogada: id={}, nome={}", id, nome);

        String textoLog = String.format("Regra revogada: ID=%d, Nome='%s'", id, nome);
        logJetonService.registrarLog("regras", idUsuarioLogado, textoLog);
    }

    @Transactional
    public void restaurar(Integer id, Integer idUsuarioLogado) {
        Regras regra = buscarOuFalhar(id);
        if (!regra.isRevogado()) {
            throw new RuntimeException("A regra já está em vigor.");
        }
        String nome = regra.getNomeRegra();
        regra.setInRevogado(Regras.REVOGADO_NAO);
        repository.save(regra);
        log.info("Regra restaurada: id={}, nome={}", id, nome);

        String textoLog = String.format("Regra restaurada: ID=%d, Nome='%s'", id, nome);
        logJetonService.registrarLog("regras", idUsuarioLogado, textoLog);
    }

    @Transactional
    public void excluirFisicamente(Integer id, Integer idUsuarioLogado) {
        Regras regra = buscarOuFalhar(id);
        if (!regra.isRevogado()) {
            throw new RuntimeException("Para excluir, a regra deve estar revogada primeiro.");
        }
        long countAtividades = atividadeRepository.countByRegraIdRegra(id);
        if (countAtividades > 0) {
            throw new RuntimeException("Não é possível excluir a regra pois existem " + countAtividades +
                    " atividade(s) vinculada(s) a ela. Revogue-a ou use 'Revogar' em vez disso.");
        }
        String nome = regra.getNomeRegra();
        repository.deleteById(id);
        log.info("Regra excluída: id={}, nome={}", id, nome);

        String textoLog = String.format("Regra excluída: ID=%d, Nome='%s'", id, nome);
        logJetonService.registrarLog("regras", idUsuarioLogado, textoLog);
    }

    // =========================================================================
    // OPERAÇÕES DE LEITURA (CONSULTAS)
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
}