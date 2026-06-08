package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.annotation.Auditar;
import br.com.cremepe.jeton.dominio.PontosSaldo;
import br.com.cremepe.jeton.repository.PontosSaldoRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PontosSaldoService {

    private static final Logger log = LoggerFactory.getLogger(PontosSaldoService.class);

    @Autowired
    private PontosSaldoRepository repository;

    // =========================================================================
    // OPERAÇÕES DE ESCRITA
    // =========================================================================

    @Auditar(tabela = "pontos_saldo", acao = "CRIAR", descricao = "Criação de registro de saldo de pontos", dadosParametros = "{ 'conselheiroId': #pontos.conselheiro?.idPessoa, 'gestaoId': #pontos.gestao?.idGestao, 'pontosTrabalhados': #pontos.pontosTrabalhados, 'pontosUtilizados': #pontos.pontosUtilizados, 'pontosSobrando': #pontos.pontosSobrando, 'situacao': #pontos.inSituacao, 'atividadeId': #pontos.atividade?.idAtividade, 'jetonId': #pontos.jeton?.idJeton, 'resolucaoId': #pontos.resolucao?.idResolucao }", dadosRetorno = "#result", capturarEstadoAnterior = false, auditarExcecao = true)
    @Transactional
    public PontosSaldo criar(PontosSaldo pontos) {
        pontos.setIdPontosSaldo(null);
        return salvarPontos(pontos, true);
    }

    @Auditar(tabela = "pontos_saldo", acao = "ATUALIZAR", descricao = "Atualização de registro de saldo de pontos", dadosParametros = "{ 'id': #pontos.idPontosSaldo, 'conselheiroId': #pontos.conselheiro?.idPessoa, 'gestaoId': #pontos.gestao?.idGestao, 'pontosTrabalhados': #pontos.pontosTrabalhados, 'pontosUtilizados': #pontos.pontosUtilizados, 'pontosSobrando': #pontos.pontosSobrando, 'situacao': #pontos.inSituacao, 'atividadeId': #pontos.atividade?.idAtividade, 'jetonId': #pontos.jeton?.idJeton, 'resolucaoId': #pontos.resolucao?.idResolucao }", dadosRetorno = "#result", capturarEstadoAnterior = true, auditarExcecao = true)
    @Transactional
    public PontosSaldo atualizar(PontosSaldo pontos) {
        if (pontos.getIdPontosSaldo() == null) {
            throw new RuntimeException("ID do saldo de pontos não informado para atualização.");
        }
        if (!repository.existsById(pontos.getIdPontosSaldo())) {
            throw new RuntimeException("Saldo de pontos não encontrado para atualização.");
        }
        return salvarPontos(pontos, false);
    }

    /**
     * Método privado com a lógica comum de persistência.
     * 
     * @param isNovo true para criação, false para atualização
     */
    private PontosSaldo salvarPontos(PontosSaldo pontos, boolean isNovo) {
        validarIntegridade(pontos);
        normalizar(pontos);

        PontosSaldo salvo = repository.save(pontos);
        log.info(
                "Saldo de pontos {}: id={}, conselheiro={}, gestão={}, pontosTrabalhados={}, pontosUtilizados={}, pontosSobrando={}, situação={}",
                isNovo ? "criado" : "atualizado",
                salvo.getIdPontosSaldo(),
                salvo.getConselheiro() != null ? salvo.getConselheiro().getIdPessoa() : null,
                salvo.getGestao() != null ? salvo.getGestao().getIdGestao() : null,
                salvo.getPontosTrabalhados(), salvo.getPontosUtilizados(),
                salvo.getPontosSobrando(), salvo.getInSituacao());
        return salvo;
    }

    private void validarIntegridade(PontosSaldo pontos) {
        if (pontos.getIdPontosSaldo() != null) {
            Optional<PontosSaldo> existente = repository.findById(pontos.getIdPontosSaldo());
            if (existente.isPresent()) {
                PontosSaldo original = existente.get();
                if (original.isUtilizado() || original.isExcluido()) {
                    throw new RuntimeException("Não é possível alterar um saldo que já foi utilizado ou excluído.");
                }
            }
        }
        if ((pontos.getPontosTrabalhados() != null && pontos.getPontosTrabalhados() < 0) ||
                (pontos.getPontosUtilizados() != null && pontos.getPontosUtilizados() < 0) ||
                (pontos.getPontosSobrando() != null && pontos.getPontosSobrando() < 0)) {
            throw new RuntimeException("Os valores de pontos não podem ser negativos.");
        }
    }

    private void normalizar(PontosSaldo pontos) {
        if (pontos.getDataHora() == null) {
            pontos.setDataHora(java.time.LocalDateTime.now());
        }
        if (pontos.getInSituacao() == null || pontos.getInSituacao().trim().isEmpty()) {
            pontos.setInSituacao(PontosSaldo.SITUACAO_ATIVO);
        }
        if (pontos.getPontosTrabalhados() == null)
            pontos.setPontosTrabalhados(0);
        if (pontos.getPontosUtilizados() == null)
            pontos.setPontosUtilizados(0);
        if (pontos.getPontosSobrando() == null)
            pontos.setPontosSobrando(0);
    }

    // =========================================================================
    // EXCLUSÃO
    // =========================================================================

    @Auditar(tabela = "pontos_saldo", acao = "EXCLUIR", descricao = "Exclusão de registro de saldo de pontos (apenas se não utilizado)", dadosParametros = "{ 'id': #id }", capturarEstadoAnterior = false, auditarExcecao = true, incluirRetorno = false)
    @Transactional
    public void excluir(Integer id) {
        PontosSaldo saldo = buscarOuFalhar(id);
        Integer idConselheiro = saldo.getConselheiro() != null ? saldo.getConselheiro().getIdPessoa() : null;
        Integer idGestao = saldo.getGestao() != null ? saldo.getGestao().getIdGestao() : null;
        Integer pontosSobrando = saldo.getPontosSobrando();
        String situacao = saldo.getInSituacao();

        if (saldo.isUtilizado()) {
            throw new RuntimeException("Não é possível excluir um saldo que já foi utilizado em pagamentos.");
        }
        if (saldo.isAtivo() && saldo.getPontosSobrando() > 0) {
            throw new RuntimeException(
                    "Não é possível excluir um saldo ativo com pontos remanescentes. Considere inativá-lo.");
        }
        repository.deleteById(id);
        log.info(
                "Saldo de pontos excluído fisicamente: id={}, conselheiro={}, gestão={}, pontosSobrando={}, situação={}",
                id, idConselheiro, idGestao, pontosSobrando, situacao);
    }

    // =========================================================================
    // OPERAÇÕES DE LEITURA
    // =========================================================================

    @Transactional(readOnly = true)
    public List<PontosSaldo> listarTodos() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<PontosSaldo> buscarPorId(Integer id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public PontosSaldo buscarOuFalhar(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Saldo de pontos não encontrado com ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<PontosSaldo> buscarSaldosDisponiveis(Integer idPessoa, Integer idGestao) {
        return repository.buscarSaldosDisponiveisOrdenadosFIFO(idPessoa, idGestao);
    }

    @Transactional(readOnly = true)
    public boolean existeSaldoAtivoParaConselheiroGestao(Integer idPessoa, Integer idGestao) {
        return repository.existsByConselheiroIdPessoaAndGestaoIdGestaoAndInSituacao(idPessoa, idGestao,
                PontosSaldo.SITUACAO_ATIVO);
    }
}