package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.PontosSaldo;
import br.com.cremepe.jeton.repositorio.PontosSaldoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PontosRemanescentesService {

    private static final Logger log = LoggerFactory.getLogger(PontosRemanescentesService.class);

    @Autowired
    private PontosSaldoRepository repository;
    @Autowired
    private LogJetonService logJetonService;

    // =========================================================================
    // OPERAÇÕES DE ESCRITA
    // =========================================================================

    @Transactional
    public PontosSaldo salvar(PontosSaldo pontos, Integer idUsuarioLogado) {
        boolean isNovo = pontos.getIdPontosSaldo() == null;
        Integer idConselheiro = pontos.getConselheiro() != null ? pontos.getConselheiro().getIdPessoa() : null;
        Integer idGestao = pontos.getGestao() != null ? pontos.getGestao().getIdGestao() : null;
        Integer pontosTrabalhados = pontos.getPontosTrabalhados();
        Integer pontosUtilizados = pontos.getPontosUtilizados();
        Integer pontosSobrando = pontos.getPontosSobrando();
        String situacao = pontos.getInSituacao();

        validarIntegridade(pontos);
        normalizar(pontos);
        PontosSaldo salvo = repository.save(pontos);

        log.info(
                "Saldo de pontos {}: id={}, conselheiro={}, gestão={}, pontosTrabalhados={}, pontosUtilizados={}, pontosSobrando={}, situação={}",
                isNovo ? "criado" : "atualizado",
                salvo.getIdPontosSaldo(), idConselheiro, idGestao,
                pontosTrabalhados, pontosUtilizados, pontosSobrando, situacao);

        String textoLog = String.format(
                "Saldo de pontos %s: ID=%d, Conselheiro ID=%d, Gestão ID=%d, Pontos Trabalhados=%d, Pontos Utilizados=%d, Pontos Sobrando=%d, Situação='%s'",
                isNovo ? "criado" : "atualizado",
                salvo.getIdPontosSaldo(), idConselheiro, idGestao,
                pontosTrabalhados, pontosUtilizados, pontosSobrando, situacao);
        logJetonService.registrarLog("pontos_saldo", idUsuarioLogado, textoLog);

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

    @Transactional
    public void excluir(Integer id, Integer idUsuarioLogado) {
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

        String textoLog = String.format(
                "Saldo de pontos excluído: ID=%d, Conselheiro ID=%d, Gestão ID=%d, Pontos Sobrando=%d, Situação='%s'",
                id, idConselheiro, idGestao, pontosSobrando, situacao);
        logJetonService.registrarLog("pontos_saldo", idUsuarioLogado, textoLog);
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