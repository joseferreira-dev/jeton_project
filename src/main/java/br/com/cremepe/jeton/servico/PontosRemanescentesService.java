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

    // =========================================================================
    // OPERAÇÕES DE ESCRITA
    // =========================================================================

    @Transactional
    public PontosSaldo salvar(PontosSaldo pontos) {
        validarIntegridade(pontos);
        normalizar(pontos);
        PontosSaldo salvo = repository.save(pontos);
        log.info("Saldo de pontos salvo: id={}, conselheiro={}, pontosSobrando={}",
                salvo.getIdPontosSaldo(),
                salvo.getConselheiro() != null ? salvo.getConselheiro().getIdPessoa() : null,
                salvo.getPontosSobrando());
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
    public void excluir(Integer id) {
        PontosSaldo saldo = buscarOuFalhar(id);
        if (saldo.isUtilizado()) {
            throw new RuntimeException("Não é possível excluir um saldo que já foi utilizado em pagamentos.");
        }
        if (saldo.isAtivo() && saldo.getPontosSobrando() > 0) {
            throw new RuntimeException(
                    "Não é possível excluir um saldo ativo com pontos remanescentes. Considere inativá-lo.");
        }
        repository.deleteById(id);
        log.info("Saldo de pontos excluído fisicamente: id={}", id);
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