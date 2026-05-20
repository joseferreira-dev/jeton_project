package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.*;
import br.com.cremepe.jeton.dto.PontosRemanescentesDTO;
import br.com.cremepe.jeton.repositorio.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class JetonService {

    @Autowired
    private JetonRepository jetonRepository;
    @Autowired
    private PontosSaldoRepository pontosSaldoRepository;
    @Autowired
    private AtividadeConselhalRepository atividadeRepository;
    @Autowired
    private ResolucaoRepository resolucaoRepository;
    @Autowired
    private GestaoConselheiroRepository gestaoConselheiroRepository;

    @Transactional(readOnly = true)
    public List<PontosRemanescentesDTO> listarSaldosAgrupados() {
        return pontosSaldoRepository.buscarSaldosAgrupadosPorConselheiro();
    }

    @Transactional(readOnly = true)
    public List<Jeton> listarTodos() {
        return jetonRepository.findAll();
    }

    @Transactional
    public void processarFechamentoMensal(Gestao gestao, Integer mes, Integer ano) {
        // 1. VARREDURA PREVENTIVA CONTRA LANÇAMENTOS NÃO AUDITADOS
        List<AtividadeConselhal> inconsistentes = atividadeRepository
                .findAtividadesInconsistentesDoMes(gestao.getIdGestao(), mes, ano);
        if (!inconsistentes.isEmpty()) {
            throw new RuntimeException("Fechamento negado: Existem atividades pendentes ou sem anexo no período.");
        }

        // 2. BUSCA A NORMATIVA VIGENTE DO MÊS PARA O TETO MENSAL MÁXIMO
        LocalDate dataReferencia = LocalDate.of(ano, mes, 1);
        List<Resolucao> resolucoesDoMes = resolucaoRepository.findResoluesVigentesNaData(dataReferencia);
        if (resolucoesDoMes.isEmpty()) {
            throw new RuntimeException("Nenhuma Resolução ativa cadastrada para a competência " + mes + "/" + ano);
        }
        Resolucao normaVigenteNoMes = resolucoesDoMes.get(0);
        int tetoMaximoJetonsMes = normaVigenteNoMes.getMaxJetonsMes();

        // 3. RECUPERA CONSELHEIROS ATIVOS DO MANDATO
        List<GestaoConselheiro> vinculos = gestaoConselheiroRepository.findByIdIdGestao(gestao.getIdGestao());

        for (GestaoConselheiro vinculo : vinculos) {
            Conselheiro conselheiro = vinculo.getConselheiro();

            // Evita reprocessamento acidental de folha idêntica
            if (jetonRepository.findByConselheiroIdPessoaAndMesAndAno(conselheiro.getIdPessoa(), mes, ano)
                    .isPresent()) {
                continue;
            }

            // ==============================================================================
            // PASSO 1: CAPTURA E AGRUPAMENTO DO SALDO DAS NOVAS ATIVIDADES VALIDADAS
            // ==============================================================================
            List<AtividadeConselhal> novasAtividadesAptas = atividadeRepository
                    .findHomologadasParaCalculo(conselheiro.getIdPessoa(), mes, ano);

            // Agrupa novas atividades por Resolução (conforme seu requisito de segregação)
            Map<Resolucao, Integer> pontosPorNormativaNovos = new LinkedHashMap<>();
            for (AtividadeConselhal at : novasAtividadesAptas) {
                if ("N".equals(at.getInComputada())) {
                    Resolucao r = at.getRegra().getResolucao();
                    int pts = at.getRegra().getPontos() * (at.getQtdAtividade() != null ? at.getQtdAtividade() : 1);
                    pontosPorNormativaNovos.merge(r, pts, Integer::sum);
                }
            }

            // Grava os novos saldos brutos gerados antes do consumo
            pontosPorNormativaNovos.forEach((res, totalPts) -> {
                PontosSaldo ps = new PontosSaldo();
                ps.setGestao(gestao);
                ps.setResolucao(res);
                ps.setDataHora(LocalDateTime.now());
                ps.setPontosTrabalhados(totalPts);
                ps.setPontosSobrando(totalPts);
                ps.setPontosUtilizados(0);
                ps.setInSituacao("A");
                if (!novasAtividadesAptas.isEmpty())
                    ps.setAtividade(novasAtividadesAptas.get(0));
                pontosSaldoRepository.save(ps);
            });

            // ==============================================================================
            // PASSO 2: EXECUÇÃO DO MOTOR EM CASCATA CRONOLÓGICA (ALGORITMO FIFO SOLICITADO)
            // ==============================================================================
            List<PontosSaldo> todosSaldosDisponiveis = pontosSaldoRepository
                    .findSaldosAtivosPorGestaoFifo(conselheiro.getIdPessoa(), gestao.getIdGestao());

            int jetonsAcumuladosNoMes = 0;
            int sobraPontosHerdada = 0;

            for (PontosSaldo saldoFiltrado : todosSaldosDisponiveis) {
                Resolucao resContexto = saldoFiltrado.getResolucao();
                int fatorConversao = resContexto.getPontosPorJeton();
                BigDecimal valorFinanceiroJeton = resContexto.getValorJeton();

                // Soma os pontos nativos deste saldo com a sobra herdada da normativa anterior
                int pontosDisponiveisNestaFase = saldoFiltrado.getPontosSobrando() + sobraPontosHerdada;
                saldoFiltrado.setPontosTrabalhados(pontosDisponiveisNestaFase);
                sobraPontosHerdada = 0; // Consumida

                int capacidadeJetonsDestaFase = pontosDisponiveisNestaFase / fatorConversao;
                int residuoFase = pontosDisponiveisNestaFase % fatorConversao;

                int jetonsAfecharAgora = 0;

                if (jetonsAcumuladosNoMes + capacidadeJetonsDestaFase <= tetoMaximoJetonsMes) {
                    // Consumo integral da capacidade dessa resolução
                    jetonsAfecharAgora = capacidadeJetonsDestaFase;
                    saldoFiltrado.setPontosUtilizados(jetonsAfecharAgora * fatorConversao);
                    saldoFiltrado.setPontosSobrando(residuoFase);
                    sobraPontosHerdada = residuoFase; // Passa o resto adiante (Efeito Cascata)
                } else {
                    // Atingiu o teto mensal máximo permitido
                    jetonsAfecharAgora = tetoMaximoJetonsMes - jetonsAcumuladosNoMes;
                    saldoFiltrado.setPontosUtilizados(jetonsAfecharAgora * fatorConversao);
                    saldoFiltrado.setPontosSobrando(pontosDisponiveisNestaFase - (jetonsAfecharAgora * fatorConversao));
                    sobraPontosHerdada = saldoFiltrado.getPontosSobrando();
                }

                jetonsAcumuladosNoMes += jetonsAfecharAgora;

                // Salva o pagamento correspondente à moeda da resolução processada
                if (jetonsAfecharAgora > 0) {
                    Jeton pagamento = new Jeton();
                    pagamento.setGestao(gestao);
                    pagamento.setConselheiro(conselheiro);
                    pagamento.setMes(mes);
                    pagamento.setAno(ano);
                    pagamento.setTotalJeton(jetonsAfecharAgora);
                    pagamento.setValor(valorFinanceiroJeton.multiply(new BigDecimal(jetonsAfecharAgora)));
                    pagamento.setInSituacao("A");
                    Jeton jetonSalvo = jetonRepository.save(pagamento);
                    saldoFiltrado.setJeton(jetonSalvo);
                }

                if (saldoFiltrado.getPontosSobrando() == 0) {
                    saldoFiltrado.setInSituacao("I"); // Totalmente exaurido
                }
                pontosSaldoRepository.save(saldoFiltrado);
            }

            // Marcar atividades como processadas para duplo bloqueio
            for (AtividadeConselhal at : novasAtividadesAptas) {
                at.setInComputada("S");
                atividadeRepository.save(at);
            }
        }
    }

    @Transactional
    public void excluirJeton(Integer id) {
        jetonRepository.deleteById(id);
    }
}