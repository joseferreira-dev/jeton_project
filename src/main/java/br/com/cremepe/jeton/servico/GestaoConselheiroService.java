package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.Conselheiro;
import br.com.cremepe.jeton.dominio.Gestao;
import br.com.cremepe.jeton.dominio.GestaoConselheiro;
import br.com.cremepe.jeton.dominio.GestaoConselheiroId;
import br.com.cremepe.jeton.dominio.Pessoa;
import br.com.cremepe.jeton.repositorio.AtividadeConselhalRepository;
import br.com.cremepe.jeton.repositorio.ConselheiroRepository;
import br.com.cremepe.jeton.repositorio.GestaoConselheiroRepository;
import br.com.cremepe.jeton.repositorio.GestaoRepository;
import br.com.cremepe.jeton.repositorio.PessoaRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
public class GestaoConselheiroService {

    private static final Logger log = LoggerFactory.getLogger(GestaoConselheiroService.class);

    @Autowired
    private GestaoConselheiroRepository repository;
    @Autowired
    private GestaoRepository gestaoRepository;
    @Autowired
    private ConselheiroRepository conselheiroRepository;
    @Autowired
    private AtividadeConselhalRepository atividadeRepository;
    @Autowired
    private PessoaRepository pessoaRepository;
    @Autowired
    private LogJetonService logJetonService;

    // =========================================================================
    // OPERAÇÕES DE ESCRITA
    // =========================================================================

    @Transactional
    public GestaoConselheiro salvar(GestaoConselheiro vinculo, Integer idUsuarioLogado) {
        if (vinculo.getId() == null) {
            vinculo.setId(new GestaoConselheiroId());
        }
        Integer idGestao = vinculo.getGestao().getIdGestao();
        Integer idPessoa = vinculo.getConselheiro().getIdPessoa();
        vinculo.getId().setIdGestao(idGestao);
        vinculo.getId().setIdPessoa(idPessoa);

        validarVinculoDuplicado(idGestao, idPessoa);

        // Busca os nomes diretamente dos repositórios para evitar lazy loading
        // problemático
        String nomeGestao = gestaoRepository.findById(idGestao)
                .map(Gestao::getNomeGestao)
                .orElse("Gestão ID " + idGestao);
        String nomeConselheiro = pessoaRepository.findById(idPessoa)
                .map(Pessoa::getNome)
                .orElse("Conselheiro ID " + idPessoa);

        boolean isNovo = !repository.existsById(vinculo.getId());

        if (GestaoConselheiro.SITUACAO_ATIVO.equals(vinculo.getInSituacao())) {
            inativarOutrosVinculosAtivos(idGestao, idPessoa);
        }

        GestaoConselheiro salvo = repository.save(vinculo);
        log.info("Vínculo {}: gestão='{}' (ID={}), conselheiro='{}' (ID={}), situação={}",
                isNovo ? "criado" : "atualizado",
                nomeGestao, idGestao, nomeConselheiro, idPessoa, salvo.getInSituacao());

        String textoLog = String.format(
                "Vínculo %s: Gestão='%s' (ID=%d), Conselheiro='%s' (ID=%d), Situação='%s'",
                isNovo ? "criado" : "atualizado",
                nomeGestao, idGestao, nomeConselheiro, idPessoa, salvo.getInSituacao());
        logJetonService.registrarLog("gestao_conselheiro", idUsuarioLogado, textoLog);

        return salvo;
    }

    private void inativarOutrosVinculosAtivos(Integer idGestaoAtual, Integer idPessoa) {
        List<GestaoConselheiro> outrosVinculos = repository.findByConselheiroIdPessoaAndInSituacao(
                idPessoa, GestaoConselheiro.SITUACAO_ATIVO);
        for (GestaoConselheiro v : outrosVinculos) {
            if (!v.getId().getIdGestao().equals(idGestaoAtual)) {
                v.setInSituacao(GestaoConselheiro.SITUACAO_INATIVO);
                repository.save(v);
                log.debug("Vínculo inativado: gestão={}, conselheiro={}", v.getId().getIdGestao(), idPessoa);
            }
        }
    }

    @Transactional
    public void atualizarVinculosEmMassa(Integer idGestao, List<Integer> idsPessoasSelecionadas,
            Integer idUsuarioLogado) {
        if (idsPessoasSelecionadas == null)
            idsPessoasSelecionadas = new ArrayList<>();

        // Busca dados da gestão para o log
        Gestao gestao = gestaoRepository.findById(idGestao)
                .orElseThrow(() -> new RuntimeException("Gestão não encontrada: " + idGestao));
        String nomeGestao = gestao.getNomeGestao();

        // 1. Vínculos atuais (todos, independente do status)
        List<GestaoConselheiro> vinculosAtuais = repository.findByIdIdGestao(idGestao);
        List<Integer> idsAtuais = vinculosAtuais.stream()
                .map(v -> v.getId().getIdPessoa())
                .collect(Collectors.toList());

        List<Integer> idsRemovidos = new ArrayList<>();
        List<Integer> idsInativados = new ArrayList<>();
        List<Integer> idsAdicionados = new ArrayList<>();
        List<Integer> idsReativados = new ArrayList<>();

        // 2. Remover ou inativar os que foram desmarcados
        for (GestaoConselheiro vinculo : vinculosAtuais) {
            Integer idPessoa = vinculo.getId().getIdPessoa();
            if (!idsPessoasSelecionadas.contains(idPessoa)) {
                long qtdAtividades = atividadeRepository.countByGestaoIdGestaoAndConselheiroIdPessoa(idGestao,
                        idPessoa);
                if (qtdAtividades == 0) {
                    repository.delete(vinculo);
                    log.info("Vínculo removido (sem atividades): gestão={}, conselheiro={}", idGestao, idPessoa);
                    idsRemovidos.add(idPessoa);
                } else {
                    if (GestaoConselheiro.SITUACAO_ATIVO.equals(vinculo.getInSituacao())) {
                        vinculo.setInSituacao(GestaoConselheiro.SITUACAO_INATIVO);
                        repository.save(vinculo);
                        log.info("Vínculo inativado (possui atividades): gestão={}, conselheiro={}", idGestao,
                                idPessoa);
                        idsInativados.add(idPessoa);
                    }
                }
            }
        }

        // 3. Adicionar novos vínculos ou reativar existentes
        if (!idsPessoasSelecionadas.isEmpty()) {
            for (Integer idPessoa : idsPessoasSelecionadas) {
                if (!idsAtuais.contains(idPessoa)) {
                    // Novo vínculo
                    Conselheiro conselheiro = conselheiroRepository.findById(idPessoa)
                            .orElseThrow(() -> new RuntimeException("Conselheiro não encontrado: " + idPessoa));
                    GestaoConselheiro novo = new GestaoConselheiro();
                    novo.setId(new GestaoConselheiroId(idGestao, idPessoa));
                    novo.setGestao(gestao);
                    novo.setConselheiro(conselheiro);
                    novo.setInSituacao(GestaoConselheiro.SITUACAO_ATIVO);
                    inativarOutrosVinculosAtivos(idGestao, idPessoa);
                    repository.save(novo);
                    log.info("Novo vínculo ativo criado: gestão={}, conselheiro={}", idGestao, idPessoa);
                    idsAdicionados.add(idPessoa);
                } else {
                    // Já existia, talvez inativo: reativar se necessário
                    Optional<GestaoConselheiro> existente = repository
                            .findById(new GestaoConselheiroId(idGestao, idPessoa));
                    if (existente.isPresent()
                            && GestaoConselheiro.SITUACAO_INATIVO.equals(existente.get().getInSituacao())) {
                        existente.get().setInSituacao(GestaoConselheiro.SITUACAO_ATIVO);
                        inativarOutrosVinculosAtivos(idGestao, idPessoa);
                        repository.save(existente.get());
                        log.info("Vínculo reativado: gestão={}, conselheiro={}", idGestao, idPessoa);
                        idsReativados.add(idPessoa);
                    }
                }
            }
        }

        // Log de auditoria da operação em massa
        if (!idsRemovidos.isEmpty() || !idsInativados.isEmpty() || !idsAdicionados.isEmpty()
                || !idsReativados.isEmpty()) {
            String textoLog = String.format(
                    "Atualização em massa de vínculos para gestão '%s' (ID=%d). " +
                            "Removidos: %s, Inativados: %s, Adicionados: %s, Reativados: %s",
                    nomeGestao, idGestao,
                    idsRemovidos.toString(), idsInativados.toString(),
                    idsAdicionados.toString(), idsReativados.toString());
            logJetonService.registrarLog("gestao_conselheiro", idUsuarioLogado, textoLog);
        }
    }

    @Transactional
    public void ativarVinculo(Integer idGestao, Integer idPessoa, Integer idUsuarioLogado) {
        GestaoConselheiro vinculo = buscarOuFalhar(idGestao, idPessoa);
        if (vinculo.isAtivo())
            return;
        vinculo.setInSituacao(GestaoConselheiro.SITUACAO_ATIVO);
        inativarOutrosVinculosAtivos(idGestao, idPessoa);
        repository.save(vinculo);
        log.info("Vínculo ativado: gestão={}, conselheiro={}", idGestao, idPessoa);

        String textoLog = String.format(
                "Vínculo ativado: Gestão ID=%d, Conselheiro ID=%d",
                idGestao, idPessoa);
        logJetonService.registrarLog("gestao_conselheiro", idUsuarioLogado, textoLog);
    }

    @Transactional
    public void inativarVinculo(Integer idGestao, Integer idPessoa, Integer idUsuarioLogado) {
        GestaoConselheiro vinculo = buscarOuFalhar(idGestao, idPessoa);
        if (vinculo.isInativo())
            return;
        vinculo.setInSituacao(GestaoConselheiro.SITUACAO_INATIVO);
        repository.save(vinculo);
        log.info("Vínculo inativado: gestão={}, conselheiro={}", idGestao, idPessoa);

        String textoLog = String.format(
                "Vínculo inativado: Gestão ID=%d, Conselheiro ID=%d",
                idGestao, idPessoa);
        logJetonService.registrarLog("gestao_conselheiro", idUsuarioLogado, textoLog);
    }

    // =========================================================================
    // OPERAÇÕES DE LEITURA
    // =========================================================================

    @Transactional(readOnly = true)
    public Page<GestaoConselheiro> listarComPaginacaoEPesquisa(String termo, String situacao, int page, int size,
            String sortField, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();
        Pageable pageable = (size == 0) ? Pageable.unpaged(sort) : PageRequest.of(page, size, sort);
        return repository.pesquisarPaginado(termo, situacao, pageable);
    }

    @Transactional(readOnly = true)
    public List<GestaoConselheiro> listarTodos() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<GestaoConselheiro> buscarPorId(Integer idGestao, Integer idPessoa) {
        return repository.findById(new GestaoConselheiroId(idGestao, idPessoa));
    }

    @Transactional(readOnly = true)
    public GestaoConselheiro buscarOuFalhar(Integer idGestao, Integer idPessoa) {
        return repository.findById(new GestaoConselheiroId(idGestao, idPessoa))
                .orElseThrow(() -> new RuntimeException(
                        "Vínculo não encontrado para gestão " + idGestao + " e conselheiro " + idPessoa));
    }

    // =========================================================================
    // EXCLUSÃO
    // =========================================================================
    @Transactional
    public void excluir(Integer idGestao, Integer idPessoa, Integer idUsuarioLogado) {
        // Verifica se existem atividades antes de excluir
        long atividades = atividadeRepository.countByGestaoIdGestaoAndConselheiroIdPessoa(idGestao, idPessoa);
        if (atividades > 0) {
            throw new RuntimeException("Não é possível excluir o vínculo pois existem atividades associadas.");
        }

        // Coleta informações para o log
        Optional<GestaoConselheiro> vinculoOpt = repository.findById(new GestaoConselheiroId(idGestao, idPessoa));
        String detalhes = "";
        if (vinculoOpt.isPresent()) {
            GestaoConselheiro v = vinculoOpt.get();
            String nomeGestao = v.getGestao().getNomeGestao();
            String nomeConselheiro = v.getConselheiro().getPessoa().getNome();
            detalhes = String.format("Gestão='%s' (ID=%d), Conselheiro='%s' (ID=%d)",
                    nomeGestao, idGestao, nomeConselheiro, idPessoa);
        } else {
            detalhes = String.format("Gestão ID=%d, Conselheiro ID=%d", idGestao, idPessoa);
        }

        repository.deleteById(new GestaoConselheiroId(idGestao, idPessoa));
        log.info("Vínculo excluído permanentemente: gestão={}, conselheiro={}", idGestao, idPessoa);

        String textoLog = String.format("Vínculo excluído: %s", detalhes);
        logJetonService.registrarLog("gestao_conselheiro", idUsuarioLogado, textoLog);
    }

    // =========================================================================
    // MÉTODOS PRIVADOS AUXILIARES
    // =========================================================================
    private void validarVinculoDuplicado(Integer idGestao, Integer idPessoa) {
        if (repository.existsByGestaoAndConselheiro(idGestao, idPessoa)) {
            throw new RuntimeException("Já existe um vínculo entre a gestão informada e este conselheiro.");
        }
    }
}