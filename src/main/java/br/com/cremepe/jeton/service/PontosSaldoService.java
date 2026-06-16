package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.domain.PontosSaldo;
import br.com.cremepe.jeton.repository.PontosSaldoRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PontosSaldoService {

    private static final Logger log = LoggerFactory.getLogger(PontosSaldoService.class);

    private final PontosSaldoRepository pontosSaldoRepository;
    private final LogJetonService logJetonService;

    public PontosSaldoService(PontosSaldoRepository pontosSaldoRepository,
            LogJetonService logJetonService) {
        this.pontosSaldoRepository = pontosSaldoRepository;
        this.logJetonService = logJetonService;
    }

    @Transactional
    public PontosSaldo criar(PontosSaldo pontos) {
        pontos.setIdPontosSaldo(null);
        PontosSaldo salvo = salvarPontos(pontos, true);
        logJetonService.logPontosSaldoCriado(salvo);
        return salvo;
    }

    @Transactional
    public PontosSaldo atualizar(PontosSaldo pontos) {
        if (pontos.getIdPontosSaldo() == null) {
            throw new RuntimeException("ID do saldo de pontos não informado para atualização.");
        }
        if (!pontosSaldoRepository.existsById(pontos.getIdPontosSaldo())) {
            throw new RuntimeException("Saldo de pontos não encontrado para atualização.");
        }
        PontosSaldo antigo = pontosSaldoRepository.findById(pontos.getIdPontosSaldo()).orElseThrow();
        PontosSaldo copia = copiarPontosSaldo(antigo);

        PontosSaldo atualizado = salvarPontos(pontos, false);
        logJetonService.logPontosSaldoAtualizado(copia, atualizado);
        return atualizado;
    }

    private PontosSaldo salvarPontos(PontosSaldo pontos, boolean isNovo) {
        validarIntegridade(pontos);
        normalizar(pontos);

        PontosSaldo salvo = pontosSaldoRepository.save(pontos);
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
            Optional<PontosSaldo> existente = pontosSaldoRepository.findById(pontos.getIdPontosSaldo());
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
        PontosSaldo copia = copiarPontosSaldo(saldo);
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
        pontosSaldoRepository.deleteById(id);
        log.info(
                "Saldo de pontos excluído: id={}, conselheiro={}, gestão={}, pontosSobrando={}, situação={}",
                id, idConselheiro, idGestao, pontosSobrando, situacao);
        logJetonService.logPontosSaldoExcluido(copia);
    }

    @Transactional(readOnly = true)
    public List<PontosSaldo> listarTodos() {
        return pontosSaldoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<PontosSaldo> buscarPorId(Integer id) {
        return pontosSaldoRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public PontosSaldo buscarOuFalhar(Integer id) {
        return pontosSaldoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Saldo de pontos não encontrado com ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<PontosSaldo> buscarSaldosDisponiveis(Integer idPessoa, Integer idGestao) {
        return pontosSaldoRepository.findSaldosDisponiveisOrderedFIFO(idPessoa, idGestao);
    }

    @Transactional(readOnly = true)
    public boolean existeSaldoAtivoParaConselheiroGestao(Integer idPessoa, Integer idGestao) {
        return pontosSaldoRepository.existsByConselheiroIdPessoaAndGestaoIdGestaoAndInSituacao(idPessoa, idGestao,
                PontosSaldo.SITUACAO_ATIVO);
    }

    public int somarPontosSobrandoTotal(Integer idPessoa) {
        Integer soma = pontosSaldoRepository.sumPontosSobrandoTotal(idPessoa);
        return soma != null ? soma : 0;
    }

    private PontosSaldo copiarPontosSaldo(PontosSaldo original) {
        PontosSaldo copia = new PontosSaldo();
        copia.setIdPontosSaldo(original.getIdPontosSaldo());
        copia.setAtividade(original.getAtividade());
        copia.setConselheiro(original.getConselheiro());
        copia.setJeton(original.getJeton());
        copia.setGestao(original.getGestao());
        copia.setResolucao(original.getResolucao());
        copia.setDataHora(original.getDataHora());
        copia.setPontosTrabalhados(original.getPontosTrabalhados());
        copia.setPontosUtilizados(original.getPontosUtilizados());
        copia.setPontosSobrando(original.getPontosSobrando());
        copia.setInSituacao(original.getInSituacao());
        return copia;
    }
}