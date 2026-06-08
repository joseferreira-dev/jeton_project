package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.annotation.Auditar;
import br.com.cremepe.jeton.domain.*;
import br.com.cremepe.jeton.repository.*;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    // =========================================================================
    // OPERAÇÕES DE ESCRITA
    // =========================================================================

    @Auditar(tabela = "gestao_conselheiro", acao = "CRIAR", descricao = "Criação de vínculo entre conselheiro e gestão", dadosParametros = "{ 'idGestao': #vinculo.gestao.idGestao, 'idPessoa': #vinculo.conselheiro.idPessoa, 'situacao': #vinculo.inSituacao }", dadosRetorno = "#result", capturarEstadoAnterior = false, auditarExcecao = true)
    @Transactional
    public GestaoConselheiro criar(GestaoConselheiro vinculo) {
        // Garante que o ID composto seja nulo para criação
        vinculo.setId(null);
        return salvarVinculo(vinculo, true);
    }

    @Auditar(tabela = "gestao_conselheiro", acao = "ATUALIZAR", descricao = "Atualização de vínculo entre conselheiro e gestão (apenas situação)", dadosParametros = "{ 'idGestao': #vinculo.gestao.idGestao, 'idPessoa': #vinculo.conselheiro.idPessoa, 'novaSituacao': #vinculo.inSituacao }", dadosRetorno = "#result", capturarEstadoAnterior = true, auditarExcecao = true)
    @Transactional
    public GestaoConselheiro atualizar(GestaoConselheiro vinculo) {
        // Verifica se o vínculo existe
        GestaoConselheiroId id = new GestaoConselheiroId(
                vinculo.getGestao().getIdGestao(),
                vinculo.getConselheiro().getIdPessoa());
        if (!repository.existsById(id)) {
            throw new RuntimeException("Vínculo não encontrado para atualização.");
        }
        return salvarVinculo(vinculo, false);
    }

    /**
     * Método privado que contém a lógica comum de persistência.
     * 
     * @param isNovo true para criação, false para atualização
     */
    private GestaoConselheiro salvarVinculo(GestaoConselheiro vinculo, boolean isNovo) {
        // Para edição, carrega o vínculo existente (garante entidade gerenciada)
        GestaoConselheiro vinculoExistente = null;
        if (!isNovo) {
            vinculoExistente = repository.findById(new GestaoConselheiroId(
                    vinculo.getGestao().getIdGestao(),
                    vinculo.getConselheiro().getIdPessoa()))
                    .orElseThrow(() -> new RuntimeException("Vínculo não encontrado para edição."));
        }

        // Validação de duplicidade (apenas para criação)
        if (isNovo) {
            validarVinculoDuplicado(vinculo.getGestao().getIdGestao(), vinculo.getConselheiro().getIdPessoa());
        }

        // Busca os nomes diretamente dos repositórios para evitar lazy loading
        // problemático
        Integer idGestao = vinculo.getGestao().getIdGestao();
        Integer idPessoa = vinculo.getConselheiro().getIdPessoa();
        String nomeGestao = gestaoRepository.findById(idGestao)
                .map(Gestao::getNomeGestao)
                .orElse("Gestão ID " + idGestao);
        String nomeConselheiro = pessoaRepository.findById(idPessoa)
                .map(Pessoa::getNome)
                .orElse("Conselheiro ID " + idPessoa);

        // Se o vínculo está sendo ativado, inativa outros vínculos ativos do mesmo
        // conselheiro
        if (GestaoConselheiro.SITUACAO_ATIVO.equals(vinculo.getInSituacao())) {
            inativarOutrosVinculosAtivos(idGestao, idPessoa);
        }

        GestaoConselheiro salvo;
        if (isNovo) {
            // Criação: define o ID composto e salva
            vinculo.setId(new GestaoConselheiroId(idGestao, idPessoa));
            salvo = repository.save(vinculo);
        } else {
            // Atualização: apenas altera a situação
            vinculoExistente.setInSituacao(vinculo.getInSituacao());
            salvo = repository.save(vinculoExistente);
        }

        log.info("Vínculo {}: gestão='{}' (ID={}), conselheiro='{}' (ID={}), situação={}",
                isNovo ? "criado" : "atualizado",
                nomeGestao, idGestao, nomeConselheiro, idPessoa, salvo.getInSituacao());

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

    // =========================================================================
    // OPERAÇÕES EM MASSA
    // =========================================================================

    @Auditar(tabela = "gestao_conselheiro", acao = "ATUALIZAR_EM_MASSA", descricao = "Atualização em massa de vínculos de conselheiros para uma gestão", dadosParametros = "{ 'idGestao': #idGestao }", dadosRetorno = "#result", capturarEstadoAnterior = false, auditarExcecao = true, incluirRetorno = true)
    @Transactional
    public Map<String, List<Integer>> atualizarVinculosEmMassa(Integer idGestao, List<Integer> idsPessoasSelecionadas) {
        if (idsPessoasSelecionadas == null)
            idsPessoasSelecionadas = new ArrayList<>();

        Gestao gestao = gestaoRepository.findById(idGestao)
                .orElseThrow(() -> new RuntimeException("Gestão não encontrada: " + idGestao));
        String nomeGestao = gestao.getNomeGestao();

        List<GestaoConselheiro> vinculosAtuais = repository.findByIdIdGestao(idGestao);
        List<Integer> idsAtuais = vinculosAtuais.stream()
                .map(v -> v.getId().getIdPessoa())
                .collect(Collectors.toList());

        List<Integer> idsRemovidos = new ArrayList<>();
        List<Integer> idsInativados = new ArrayList<>();
        List<Integer> idsAdicionados = new ArrayList<>();
        List<Integer> idsReativados = new ArrayList<>();

        // Remover ou inativar os desmarcados
        for (GestaoConselheiro vinculo : vinculosAtuais) {
            Integer idPessoa = vinculo.getId().getIdPessoa();
            if (!idsPessoasSelecionadas.contains(idPessoa)) {
                long qtdAtividades = atividadeRepository.countByGestaoIdGestaoAndConselheiroIdPessoa(idGestao,
                        idPessoa);
                if (qtdAtividades == 0) {
                    repository.delete(vinculo);
                    idsRemovidos.add(idPessoa);
                } else {
                    if (GestaoConselheiro.SITUACAO_ATIVO.equals(vinculo.getInSituacao())) {
                        vinculo.setInSituacao(GestaoConselheiro.SITUACAO_INATIVO);
                        repository.save(vinculo);
                        idsInativados.add(idPessoa);
                    }
                }
            }
        }

        // Adicionar ou reativar novos
        if (!idsPessoasSelecionadas.isEmpty()) {
            for (Integer idPessoa : idsPessoasSelecionadas) {
                if (!idsAtuais.contains(idPessoa)) {
                    Conselheiro conselheiro = conselheiroRepository.findById(idPessoa)
                            .orElseThrow(() -> new RuntimeException("Conselheiro não encontrado: " + idPessoa));
                    GestaoConselheiro novo = new GestaoConselheiro();
                    novo.setId(new GestaoConselheiroId(idGestao, idPessoa));
                    novo.setGestao(gestao);
                    novo.setConselheiro(conselheiro);
                    novo.setInSituacao(GestaoConselheiro.SITUACAO_ATIVO);
                    inativarOutrosVinculosAtivos(idGestao, idPessoa);
                    repository.save(novo);
                    idsAdicionados.add(idPessoa);
                } else {
                    Optional<GestaoConselheiro> existente = repository
                            .findById(new GestaoConselheiroId(idGestao, idPessoa));
                    if (existente.isPresent()
                            && GestaoConselheiro.SITUACAO_INATIVO.equals(existente.get().getInSituacao())) {
                        existente.get().setInSituacao(GestaoConselheiro.SITUACAO_ATIVO);
                        inativarOutrosVinculosAtivos(idGestao, idPessoa);
                        repository.save(existente.get());
                        idsReativados.add(idPessoa);
                    }
                }
            }
        }

        // Log informativo apenas para debug
        log.info(
                "Atualização em massa de vínculos para gestão '{}' (ID={}). Removidos: {}, Inativados: {}, Adicionados: {}, Reativados: {}",
                nomeGestao, idGestao, idsRemovidos.size(), idsInativados.size(), idsAdicionados.size(),
                idsReativados.size());

        // Retorna um mapa com as listas
        Map<String, List<Integer>> resultado = new HashMap<>();
        resultado.put("removidos", idsRemovidos);
        resultado.put("inativados", idsInativados);
        resultado.put("adicionados", idsAdicionados);
        resultado.put("reativados", idsReativados);
        return resultado;
    }

    // =========================================================================
    // OPERAÇÕES DE ATIVAÇÃO / INATIVAÇÃO
    // =========================================================================

    @Auditar(tabela = "gestao_conselheiro", acao = "ATIVAR", descricao = "Ativação de vínculo entre conselheiro e gestão", dadosParametros = "{ 'idGestao': #idGestao, 'idPessoa': #idPessoa }", capturarEstadoAnterior = false, auditarExcecao = true, incluirRetorno = false)
    @Transactional
    public void ativarVinculo(Integer idGestao, Integer idPessoa) {
        GestaoConselheiro vinculo = buscarOuFalhar(idGestao, idPessoa);
        if (vinculo.isAtivo())
            return;
        vinculo.setInSituacao(GestaoConselheiro.SITUACAO_ATIVO);
        inativarOutrosVinculosAtivos(idGestao, idPessoa);
        repository.save(vinculo);
        log.info("Vínculo ativado: gestão={}, conselheiro={}", idGestao, idPessoa);
    }

    @Auditar(tabela = "gestao_conselheiro", acao = "INATIVAR", descricao = "Inativação de vínculo entre conselheiro e gestão", dadosParametros = "{ 'idGestao': #idGestao, 'idPessoa': #idPessoa }", capturarEstadoAnterior = false, auditarExcecao = true, incluirRetorno = false)
    @Transactional
    public void inativarVinculo(Integer idGestao, Integer idPessoa) {
        GestaoConselheiro vinculo = buscarOuFalhar(idGestao, idPessoa);
        if (vinculo.isInativo())
            return;
        vinculo.setInSituacao(GestaoConselheiro.SITUACAO_INATIVO);
        repository.save(vinculo);
        log.info("Vínculo inativado: gestão={}, conselheiro={}", idGestao, idPessoa);
    }

    @Auditar(tabela = "gestao_conselheiro", acao = "EXCLUIR", descricao = "Exclusão de vínculo", dadosParametros = "{ 'idGestao': #idGestao, 'idPessoa': #idPessoa }", capturarEstadoAnterior = false, auditarExcecao = true, incluirRetorno = false)
    @Transactional
    public void excluir(Integer idGestao, Integer idPessoa) {
        long atividades = atividadeRepository.countByGestaoIdGestaoAndConselheiroIdPessoa(idGestao, idPessoa);
        if (atividades > 0) {
            throw new RuntimeException("Não é possível excluir o vínculo pois existem atividades associadas.");
        }
        repository.deleteById(new GestaoConselheiroId(idGestao, idPessoa));
        log.info("Vínculo excluído permanentemente: gestão={}, conselheiro={}", idGestao, idPessoa);
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

    @Transactional(readOnly = true)
    public Optional<GestaoConselheiro> buscarPorConselheiroEStatus(Integer idPessoa, String situacao) {
        List<GestaoConselheiro> lista = repository.findByConselheiroIdPessoaAndInSituacao(idPessoa, situacao);
        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }

    @Transactional(readOnly = true)
    public boolean existeVinculoParaConselheiro(Integer idPessoa) {
        return repository.existsByConselheiroIdPessoa(idPessoa);
    }

    // =========================================================================
    // MÉTODOS AUXILIARES
    // =========================================================================

    private void validarVinculoDuplicado(Integer idGestao, Integer idPessoa) {
        if (repository.existsByGestaoAndConselheiro(idGestao, idPessoa)) {
            throw new RuntimeException("Já existe um vínculo entre a gestão informada e este conselheiro.");
        }
    }
}