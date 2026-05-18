package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.*;
import br.com.cremepe.jeton.dto.PontosRemanescentesDTO;
import br.com.cremepe.jeton.repositorio.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class JetonService {

    @Autowired private JetonRepository jetonRepository;
    @Autowired private PontosSaldoRepository pontosSaldoRepository;
    @Autowired private AtividadeConselhalRepository atividadeRepository;
    @Autowired private ResolucaoRepository resolucaoRepository;
    @Autowired private GestaoConselheiroRepository gestaoConselheiroRepository;

    @Transactional(readOnly = true)
    public List<PontosRemanescentesDTO> listarSaldosAgrupados() {
        return pontosSaldoRepository.buscarSaldosAgrupadosPorConselheiro();
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
    public void excluirJeton(Integer id) {
        jetonRepository.deleteById(id);
    }

    /**
     * O MOTOR DE CÁLCULO FINANCEIRO (FECHAMENTO DE FOLHA)
     * Este método processa todos os conselheiros ativos numa gestão para um determinado mês/ano.
     */
    @Transactional(rollbackFor = Exception.class)
    public void processarFechamentoMensal(Gestao gestao, Integer mes, Integer ano) {
        
        // 1. Obtém a Resolução Ativa (Regra Financeira Magna)
        List<Resolucao> resolucoesAtivas = resolucaoRepository.findByInRevogado("N");
        if (resolucoesAtivas.isEmpty()) {
            throw new RuntimeException("Não existe nenhuma Resolução financeira em vigor para basear os cálculos.");
        }
        Resolucao resolucao = resolucoesAtivas.get(0); // Assume a principal
        Integer pontosPorJeton = resolucao.getPontosPorJeton();
        Integer tetoMensalJetons = resolucao.getMaxJetonsMes();
        BigDecimal valorJeton = resolucao.getValorJeton();

        // 2. Obtém todos os conselheiros daquela Gestão
        List<GestaoConselheiro> vinculos = gestaoConselheiroRepository.findByGestaoIdGestao(gestao.getIdGestao());

        for (GestaoConselheiro vinculo : vinculos) {
            Conselheiro conselheiro = vinculo.getConselheiro();

            // Proteção contra processamento duplicado no mesmo mês
            if (jetonRepository.findByConselheiroIdPessoaAndMesAndAno(conselheiro.getIdPessoa(), mes, ano).isPresent()) {
                continue; // Já foi processado este mês, salta para o próximo conselheiro
            }

            // 3. Recolher a matéria-prima (Saldos antigos + Atividades novas com comprovativo)
            List<PontosSaldo> saldosAntigos = pontosSaldoRepository.findSaldosAtivosPorConselheiro(conselheiro.getIdPessoa());
            List<AtividadeConselhal> atividadesNovas = atividadeRepository.findPendentesParaProcessamento(conselheiro.getIdPessoa(), mes, ano);

            if (saldosAntigos.isEmpty() && atividadesNovas.isEmpty()) {
                continue; // Conselheiro não tem nada a receber, salta.
            }

            // 4. Somar todos os pontos disponíveis
            double totalPontosAcumulados = 0;
            
            for (PontosSaldo saldo : saldosAntigos) {
                totalPontosAcumulados += saldo.getPontosSobrando();
                // O saldo antigo é "consumido" na totalidade para este processamento
                saldo.setPontosUtilizados(saldo.getPontosSobrando());
                saldo.setPontosSobrando(0);
                saldo.setInSituacao("I"); // Inativo (Consumido)
                pontosSaldoRepository.save(saldo);
            }

            for (AtividadeConselhal atividade : atividadesNovas) {
                // Multiplica a quantidade de atividade pelo valor de pontos da regra
                totalPontosAcumulados += (atividade.getQtdAtividade() * atividade.getRegra().getPontos());
                // Marca a atividade como Processada/Concluída
                atividade.setInSituacao("C"); 
                atividadeRepository.save(atividade);
            }

            // 5. A Matemática dos Jetons
            int jetonsConvertidos = (int) (totalPontosAcumulados / pontosPorJeton);
            double pontosRestantes = totalPontosAcumulados % pontosPorJeton;

            // 6. Aplicação do Teto (Tesoura Financeira)
            int jetonsAPagar = Math.min(jetonsConvertidos, tetoMensalJetons);
            
            // Se o limite foi atingido, os Jetons que ultrapassaram o teto voltam a ser convertidos em pontos de sobra
            if (jetonsConvertidos > tetoMensalJetons) {
                int jetonsCortados = jetonsConvertidos - tetoMensalJetons;
                pontosRestantes += (jetonsCortados * pontosPorJeton);
            }

            // 7. Gerar o Pagamento (se houver Jetons inteiros para pagar)
            Jeton jetonCriado = null;
            if (jetonsAPagar > 0) {
                Jeton j = new Jeton();
                j.setGestao(gestao);
                j.setConselheiro(conselheiro);
                j.setMes(mes);
                j.setAno(ano);
                j.setTotalJeton(jetonsAPagar);
                j.setValor(valorJeton.multiply(new BigDecimal(jetonsAPagar)));
                j.setInSituacao("A"); // Ativo / Processado
                jetonCriado = jetonRepository.save(j);
            }

            // 8. Guardar os Pontos Remanescentes (Sobras para o mês seguinte)
            if (pontosRestantes > 0) {
                PontosSaldo novoSaldo = new PontosSaldo();
                // Usa a última atividade apenas como referência de ligação, tal como no legado
                if (!atividadesNovas.isEmpty()) {
                    novoSaldo.setAtividade(atividadesNovas.get(atividadesNovas.size() - 1));
                }
                novoSaldo.setJeton(jetonCriado); // Pode ser nulo se só gerou sobra
                novoSaldo.setDataHora(LocalDateTime.now());
                novoSaldo.setPontosSobrando((int) pontosRestantes);
                novoSaldo.setPontosUtilizados(0);
                novoSaldo.setInSituacao("A"); // Ativo para o próximo mês
                pontosSaldoRepository.save(novoSaldo);
            }
        }
    }
}