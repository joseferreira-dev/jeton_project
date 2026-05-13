package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.Jeton;
import br.com.cremepe.jeton.dto.PontosRemanescentesDTO;
import br.com.cremepe.jeton.repositorio.JetonRepository;
import br.com.cremepe.jeton.repositorio.PontosSaldoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Serviço responsável pela lógica de liquidação financeira (Jetons) e gestão de saldos.
 * Substitui as antigas FachadaJeton e FachadaPontosRemanescentes.
 */
@Service
public class JetonService {

    @Autowired
    private JetonRepository jetonRepository;

    @Autowired
    private PontosSaldoRepository pontosSaldoRepository;

    /**
     * Lista todos os saldos de pontos agrupados por conselheiro para exibição em ecrã.
     * Utiliza a query otimizada que criámos no Passo 4.
     */
    @Transactional(readOnly = true)
    public List<PontosRemanescentesDTO> listarSaldosAgrupados() {
        return pontosSaldoRepository.buscarSaldosAgrupadosPorConselheiro();
    }

    /**
     * Recupera o histórico de pagamentos de um conselheiro num ano específico.
     */
    @Transactional(readOnly = true)
    public List<Jeton> listarHistoricoPorConselheiro(Integer idPessoa, Integer ano) {
        return jetonRepository.findByConselheiroIdPessoaAndAnoOrderByMesDesc(idPessoa, ano);
    }

    /**
     * Realiza o fecho mensal (Processamento do Jeton).
     * @Transactional garante que, se houver erro ao salvar, nada será gravado no banco (Rollback).
     */
    @Transactional
    public Jeton processarPagamentoMensal(Jeton jeton) {
        // REGRA DE NEGÓCIO: Impedir duplicação de pagamento para o mesmo mês/ano
        Optional<Jeton> existente = jetonRepository.findByConselheiroIdPessoaAndMesAndAno(
                jeton.getConselheiro().getIdPessoa(), 
                jeton.getMes(), 
                jeton.getAno());

        if (existente.isPresent()) {
            throw new RuntimeException("Já existe um Jeton processado para este conselheiro neste período.");
        }

        // Aqui você pode inserir a lógica de cálculo baseada na Resolução vigente
        // Exemplo: jeton.setValor(jeton.getTotalJeton().multiply(valorBaseDaResolucao));

        return jetonRepository.save(jeton);
    }

    @Transactional
    public void excluirJeton(Integer id) {
        // Antes de excluir o jeton, a regra de negócio pode exigir libertar os pontos_saldo associados
        jetonRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Jeton> listarTodos() {
        return jetonRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Jeton> buscarPorId(Integer id) {
        return jetonRepository.findById(id);
    }

    @Transactional
    public Jeton salvar(Jeton jeton) {
        return jetonRepository.save(jeton);
    }
}