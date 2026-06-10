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

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GestaoConselheiroService {

    private static final Logger log = LoggerFactory.getLogger(GestaoConselheiroService.class);

    @Autowired
    private GestaoConselheiroRepository gestaoConselheiroRepository;
    @Autowired
    private GestaoRepository gestaoRepository;
    @Autowired
    private ConselheiroRepository conselheiroRepository;
    @Autowired
    private AtividadeConselhalRepository atividadeRepository;
    @Autowired
    private PessoaRepository pessoaRepository;

    @Auditar(tabela = "gestao_conselheiro", acao = "CRIAR", descricao = "Criação de vínculo entre conselheiro e gestão", dadosParametros = "{ 'idGestao': #vinculo.gestao.idGestao, 'idPessoa': #vinculo.conselheiro.idPessoa, 'situacao': #vinculo.inSituacao }", dadosRetorno = "#result", capturarEstadoAnterior = false, auditarExcecao = true)
    @Transactional
    public GestaoConselheiro criar(GestaoConselheiro vinculo) {
        vinculo.setId(null);
        return salvarVinculo(vinculo, true);
    }

    @Auditar(tabela = "gestao_conselheiro", acao = "ATUALIZAR", descricao = "Atualização de vínculo entre conselheiro e gestão (apenas situação)", dadosParametros = "{ 'idGestao': #vinculo.gestao.idGestao, 'idPessoa': #vinculo.conselheiro.idPessoa, 'novaSituacao': #vinculo.inSituacao }", dadosRetorno = "#result", capturarEstadoAnterior = true, auditarExcecao = true)
    @Transactional
    public GestaoConselheiro atualizar(GestaoConselheiro vinculo) {
        GestaoConselheiroId id = new GestaoConselheiroId(
                vinculo.getGestao().getIdGestao(),
                vinculo.getConselheiro().getIdPessoa());
        if (!gestaoConselheiroRepository.existsById(id)) {
            throw new RuntimeException("Vínculo não encontrado para atualização.");
        }
        return salvarVinculo(vinculo, false);
    }

    private GestaoConselheiro salvarVinculo(GestaoConselheiro vinculo, boolean isNovo) {
        GestaoConselheiro vinculoExistente = null;
        if (!isNovo) {
            vinculoExistente = gestaoConselheiroRepository.findById(new GestaoConselheiroId(
                    vinculo.getGestao().getIdGestao(),
                    vinculo.getConselheiro().getIdPessoa()))
                    .orElseThrow(() -> new RuntimeException("Vínculo não encontrado para edição."));
        }

        if (isNovo) {
            validarVinculoDuplicado(vinculo.getGestao().getIdGestao(), vinculo.getConselheiro().getIdPessoa());
        }

        Integer idGestao = vinculo.getGestao().getIdGestao();
        Integer idPessoa = vinculo.getConselheiro().getIdPessoa();
        String nomeGestao = gestaoRepository.findById(idGestao)
                .map(Gestao::getNomeGestao)
                .orElse("Gestão ID " + idGestao);
        String nomeConselheiro = pessoaRepository.findById(idPessoa)
                .map(Pessoa::getNome)
                .orElse("Conselheiro ID " + idPessoa);

        // Se o vínculo está sendo ativado, inativa outros vínculos
        if (GestaoConselheiro.SITUACAO_ATIVO.equals(vinculo.getInSituacao())) {
            inativarOutrosVinculosAtivos(idGestao, idPessoa);
        }

        GestaoConselheiro salvo;
        if (isNovo) {
            vinculo.setId(new GestaoConselheiroId(idGestao, idPessoa));
            salvo = gestaoConselheiroRepository.save(vinculo);
        } else {
            vinculoExistente.setInSituacao(vinculo.getInSituacao());
            salvo = gestaoConselheiroRepository.save(vinculoExistente);
        }

        log.info("Vínculo {}: gestão='{}' (ID={}), conselheiro='{}' (ID={}), situação={}",
                isNovo ? "criado" : "atualizado",
                nomeGestao, idGestao, nomeConselheiro, idPessoa, salvo.getInSituacao());

        return salvo;
    }

    private void inativarOutrosVinculosAtivos(Integer idGestaoAtual, Integer idPessoa) {
        List<GestaoConselheiro> outrosVinculos = gestaoConselheiroRepository
                .findByConselheiroIdPessoaAndInSituacao(idPessoa, GestaoConselheiro.SITUACAO_ATIVO);
        for (GestaoConselheiro v : outrosVinculos) {
            if (!v.getId().getIdGestao().equals(idGestaoAtual)) {
                v.setInSituacao(GestaoConselheiro.SITUACAO_INATIVO);
                gestaoConselheiroRepository.save(v);
                log.debug("Vínculo inativado: gestão={}, conselheiro={}", v.getId().getIdGestao(), idPessoa);
            }
        }
    }

    @Auditar(tabela = "gestao_conselheiro", acao = "ATUALIZAR_EM_MASSA", descricao = "Atualização em massa de vínculos de conselheiros para uma gestão", dadosParametros = "{ 'idGestao': #idGestao }", dadosRetorno = "#result", capturarEstadoAnterior = false, auditarExcecao = true, incluirRetorno = true)
    @Transactional
    public Map<String, List<Integer>> atualizarVinculosEmMassa(Integer idGestao, List<Integer> idsPessoasSelecionadas) {
        if (idsPessoasSelecionadas == null) {
            idsPessoasSelecionadas = new ArrayList<>();
        }

        Gestao gestao = gestaoRepository.findById(idGestao)
                .orElseThrow(() -> new RuntimeException("Gestão não encontrada: " + idGestao));

        List<GestaoConselheiro> vinculosAtuais = gestaoConselheiroRepository.findByIdIdGestao(idGestao);
        Set<Integer> idsAtuais = vinculosAtuais.stream()
                .map(v -> v.getId().getIdPessoa())
                .collect(Collectors.toSet());

        Set<Integer> idsNovos = new HashSet<>(idsPessoasSelecionadas);

        // IDs a serem removidos (estavam na lista mas não estão mais)
        Set<Integer> idsRemover = new HashSet<>(idsAtuais);
        idsRemover.removeAll(idsNovos);

        List<Integer> idsRemovidos = new ArrayList<>();
        for (Integer idPessoa : idsRemover) {
            if (removerVinculoSeSemAtividade(idGestao, idPessoa)) {
                idsRemovidos.add(idPessoa);
                log.info("Vínculo removido: gestão={}, conselheiro={}", idGestao, idPessoa);
            }
        }

        // IDs a serem adicionados (não existiam antes)
        Set<Integer> idsAdicionar = new HashSet<>(idsNovos);
        idsAdicionar.removeAll(idsAtuais);
        List<Integer> idsAdicionados = new ArrayList<>();

        for (Integer idPessoa : idsAdicionar) {
            Conselheiro conselheiro = conselheiroRepository.findById(idPessoa)
                    .orElseThrow(() -> new RuntimeException("Conselheiro não encontrado: " + idPessoa));

            // Inativa outros vínculos ativos deste conselheiro
            inativarOutrosVinculosAtivos(idGestao, idPessoa);

            GestaoConselheiro novoVinculo = new GestaoConselheiro();
            novoVinculo.setId(new GestaoConselheiroId(idGestao, idPessoa));
            novoVinculo.setGestao(gestao);
            novoVinculo.setConselheiro(conselheiro);
            novoVinculo.setInSituacao(GestaoConselheiro.SITUACAO_ATIVO);
            gestaoConselheiroRepository.save(novoVinculo);
            idsAdicionados.add(idPessoa);
            log.info("Vínculo criado: gestão={}, conselheiro={}", idGestao, idPessoa);
        }

        Map<String, List<Integer>> resultado = new HashMap<>();
        resultado.put("removidos", idsRemovidos);
        resultado.put("adicionados", idsAdicionados);
        return resultado;
    }

    private boolean removerVinculoSeSemAtividade(Integer idGestao, Integer idPessoa) {
        long atividades = atividadeRepository.countByGestaoIdGestaoAndConselheiroIdPessoa(idGestao, idPessoa);
        if (atividades == 0) {
            gestaoConselheiroRepository.deleteById(new GestaoConselheiroId(idGestao, idPessoa));
            return true;
        }
        log.debug("Conselheiro {} possui {} atividade(s) na gestão {} – vínculo mantido", idPessoa, atividades,
                idGestao);
        return false;
    }

    @Auditar(tabela = "gestao_conselheiro", acao = "ATIVAR", descricao = "Ativação de vínculo entre conselheiro e gestão", dadosParametros = "{ 'idGestao': #idGestao, 'idPessoa': #idPessoa }", capturarEstadoAnterior = false, auditarExcecao = true, incluirRetorno = false)
    @Transactional
    public void ativarVinculo(Integer idGestao, Integer idPessoa) {
        GestaoConselheiro vinculo = buscarOuFalhar(idGestao, idPessoa);
        if (vinculo.isAtivo())
            return;
        vinculo.setInSituacao(GestaoConselheiro.SITUACAO_ATIVO);
        inativarOutrosVinculosAtivos(idGestao, idPessoa);
        gestaoConselheiroRepository.save(vinculo);
        log.info("Vínculo ativado: gestão={}, conselheiro={}", idGestao, idPessoa);
    }

    @Auditar(tabela = "gestao_conselheiro", acao = "INATIVAR", descricao = "Inativação de vínculo entre conselheiro e gestão", dadosParametros = "{ 'idGestao': #idGestao, 'idPessoa': #idPessoa }", capturarEstadoAnterior = false, auditarExcecao = true, incluirRetorno = false)
    @Transactional
    public void inativarVinculo(Integer idGestao, Integer idPessoa) {
        GestaoConselheiro vinculo = buscarOuFalhar(idGestao, idPessoa);
        if (vinculo.isInativo())
            return;
        vinculo.setInSituacao(GestaoConselheiro.SITUACAO_INATIVO);
        gestaoConselheiroRepository.save(vinculo);
        log.info("Vínculo inativado: gestão={}, conselheiro={}", idGestao, idPessoa);
    }

    @Auditar(tabela = "gestao_conselheiro", acao = "EXCLUIR", descricao = "Exclusão de vínculo", dadosParametros = "{ 'idGestao': #idGestao, 'idPessoa': #idPessoa }", capturarEstadoAnterior = false, auditarExcecao = true, incluirRetorno = false)
    @Transactional
    public void excluir(Integer idGestao, Integer idPessoa) {
        if (!removerVinculoSeSemAtividade(idGestao, idPessoa)) {
            throw new RuntimeException("Não é possível excluir o vínculo pois existem atividades associadas.");
        }
        log.info("Vínculo excluído permanentemente: gestão={}, conselheiro={}", idGestao, idPessoa);
    }

    @Transactional(readOnly = true)
    public Page<GestaoConselheiro> listarComPaginacaoEPesquisa(String termo, String situacao, int page, int size,
            String sortField, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();
        Pageable pageable = (size == 0) ? Pageable.unpaged(sort) : PageRequest.of(page, size, sort);
        return gestaoConselheiroRepository.pesquisarPaginado(termo, situacao, pageable);
    }

    @Transactional(readOnly = true)
    public List<GestaoConselheiro> listarTodos() {
        return gestaoConselheiroRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<GestaoConselheiro> buscarPorId(Integer idGestao, Integer idPessoa) {
        return gestaoConselheiroRepository.findById(new GestaoConselheiroId(idGestao, idPessoa));
    }

    @Transactional(readOnly = true)
    public GestaoConselheiro buscarOuFalhar(Integer idGestao, Integer idPessoa) {
        return gestaoConselheiroRepository.findById(new GestaoConselheiroId(idGestao, idPessoa))
                .orElseThrow(() -> new RuntimeException(
                        "Vínculo não encontrado para gestão " + idGestao + " e conselheiro " + idPessoa));
    }

    @Transactional(readOnly = true)
    public Optional<GestaoConselheiro> buscarPorConselheiroEStatus(Integer idPessoa, String situacao) {
        List<GestaoConselheiro> lista = gestaoConselheiroRepository
                .findByConselheiroIdPessoaAndInSituacao(idPessoa, situacao);
        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }

    @Transactional(readOnly = true)
    public boolean existeVinculoParaConselheiro(Integer idPessoa) {
        return gestaoConselheiroRepository.existsByConselheiroIdPessoa(idPessoa);
    }

    @Transactional(readOnly = true)
    public List<Integer> findConselheirosComAtividadesNaGestao(Integer idGestao) {
        return gestaoConselheiroRepository.findByIdIdGestao(idGestao).stream()
                .map(gc -> gc.getId().getIdPessoa())
                .filter(idPessoa -> atividadeRepository.countByGestaoIdGestaoAndConselheiroIdPessoa(idGestao,
                        idPessoa) > 0)
                .collect(Collectors.toList());
    }

    private void validarVinculoDuplicado(Integer idGestao, Integer idPessoa) {
        if (gestaoConselheiroRepository.existsByGestaoAndConselheiro(idGestao, idPessoa)) {
            throw new RuntimeException("Já existe um vínculo entre a gestão informada e este conselheiro.");
        }
    }
}