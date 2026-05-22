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
import java.util.stream.Collectors;

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
        // 0. AUTO-LIMPEZA E PROTEÇÃO DE RE-PROCESSAMENTO
        List<Jeton> jetonsExistentes = jetonRepository.findByGestaoIdGestaoAndMesAndAno(gestao.getIdGestao(), mes, ano);
        if (!jetonsExistentes.isEmpty()) {
            Set<Integer> conselheirosParaEstorno = jetonsExistentes.stream()
                    .map(j -> j.getConselheiro().getIdPessoa())
                    .collect(Collectors.toSet());
            for (Integer idPessoa : conselheirosParaEstorno) {
                estornarFolhaDoConselheiro(idPessoa, gestao.getIdGestao(), mes, ano);
            }
        }

        // 1. Identificar o Teto da Competência
        LocalDate ultimoDiaMes = LocalDate.of(ano, mes, 1).with(java.time.temporal.TemporalAdjusters.lastDayOfMonth());
        List<Resolucao> resolucoesVigentes = resolucaoRepository.findResoluesVigentesNaData(ultimoDiaMes);

        if (resolucoesVigentes.isEmpty()) {
            throw new RuntimeException("Nenhuma Resolução ativa cadastrada para a competência " + mes + "/" + ano);
        }
        Resolucao resolucaoTeto = resolucoesVigentes.get(0);
        int maxJetonsPermitidos = (resolucaoTeto.getMaxJetonsMes() != null && resolucaoTeto.getMaxJetonsMes() > 0)
                ? resolucaoTeto.getMaxJetonsMes()
                : 22;

        List<GestaoConselheiro> vinculos = gestaoConselheiroRepository.findByGestaoIdGestao(gestao.getIdGestao());

        for (GestaoConselheiro vinculo : vinculos) {
            Conselheiro conselheiro = vinculo.getConselheiro();

            // A. Recolher saldos antigos e novas atividades
            List<PontosSaldo> saldosAntigos = pontosSaldoRepository.buscarSaldosDisponiveisOrdenadosFIFO(
                    conselheiro.getIdPessoa(), gestao.getIdGestao());
            List<AtividadeConselhal> novasAtividades = atividadeRepository.findHomologadasParaCalculo(
                    conselheiro.getIdPessoa(), mes, ano);

            if (saldosAntigos.isEmpty() && novasAtividades.isEmpty())
                continue;

            List<PontosSaldo> filaUnificada = new ArrayList<>(saldosAntigos);
            List<Integer> idsAtividadesProcessadas = new ArrayList<>();

            // B. Converter atividades em Pontos na fila FIFO
            for (AtividadeConselhal atividade : novasAtividades) {
                List<Resolucao> resAtvList = resolucaoRepository
                        .findResoluesVigentesNaData(atividade.getDataHoraAtividade().toLocalDate());
                Resolucao resolucaoAtividade = resAtvList.isEmpty() ? resolucaoTeto : resAtvList.get(0);

                PontosSaldo novoSaldo = new PontosSaldo();
                novoSaldo.setAtividade(atividade);
                novoSaldo.setConselheiro(conselheiro);
                novoSaldo.setGestao(gestao);
                novoSaldo.setResolucao(resolucaoAtividade);
                novoSaldo.setDataHora(atividade.getDataHoraAtividade());

                int multRegra = (atividade.getRegra() != null && atividade.getRegra().getPontos() != null)
                        ? atividade.getRegra().getPontos()
                        : 1;
                int qtdAtv = (atividade.getQtdAtividade() != null) ? atividade.getQtdAtividade() : 1;
                int totalPontosAtividade = qtdAtv * multRegra;

                novoSaldo.setPontosTrabalhados(totalPontosAtividade);
                novoSaldo.setPontosUtilizados(0);
                novoSaldo.setPontosSobrando(totalPontosAtividade);
                novoSaldo.setInSituacao("A");

                filaUnificada.add(pontosSaldoRepository.save(novoSaldo));
                idsAtividadesProcessadas.add(atividade.getIdAtividade());
            }

            filaUnificada
                    .sort(Comparator.comparing(PontosSaldo::getDataHora).thenComparing(PontosSaldo::getIdPontosSaldo));

            // C. Módulo Matemático (Calculadora de Absorção Baseada em Teto)
            int pontosAcumuladosJanela = 0;
            int totalJetonsGeradosConselheiro = 0;
            Map<Resolucao, Integer> demonstrativoJetons = new LinkedHashMap<>();

            for (PontosSaldo saldo : filaUnificada) {
                pontosAcumuladosJanela += saldo.getPontosSobrando();
                Resolucao normaVigenteSaldo = saldo.getResolucao();
                int pontosPorJeton = (normaVigenteSaldo.getPontosPorJeton() != null
                        && normaVigenteSaldo.getPontosPorJeton() > 0)
                                ? normaVigenteSaldo.getPontosPorJeton()
                                : 3;

                int jetonsPossiveis = pontosAcumuladosJanela / pontosPorJeton;
                int jetonsEfetivos = Math.min(jetonsPossiveis, maxJetonsPermitidos - totalJetonsGeradosConselheiro);

                if (jetonsEfetivos > 0) {
                    demonstrativoJetons.put(normaVigenteSaldo,
                            demonstrativoJetons.getOrDefault(normaVigenteSaldo, 0) + jetonsEfetivos);
                    totalJetonsGeradosConselheiro += jetonsEfetivos;
                    pontosAcumuladosJanela -= (jetonsEfetivos * pontosPorJeton);
                }
            }

            // D. Gerar Documentos Financeiros (Jetons) e Relacionamentos
            Jeton primeiroJetonSalvo = null;
            for (Map.Entry<Resolucao, Integer> entry : demonstrativoJetons.entrySet()) {
                Jeton jeton = new Jeton();
                jeton.setGestao(gestao);
                jeton.setConselheiro(conselheiro);
                jeton.setMes(mes);
                jeton.setAno(ano);
                jeton.setTotalJeton(entry.getValue());

                BigDecimal valorBase = (entry.getKey().getValorJeton() != null) ? entry.getKey().getValorJeton()
                        : BigDecimal.ZERO;
                jeton.setValor(valorBase.multiply(BigDecimal.valueOf(entry.getValue())));
                jeton.setInSituacao("A");

                // CORREÇÃO CRÍTICA: Preenche a tabela jeton_atividade!
                if (jeton.getAtividades() == null) {
                    jeton.setAtividades(new ArrayList<>());
                }
                jeton.getAtividades().addAll(novasAtividades);

                jeton = jetonRepository.save(jeton);
                if (primeiroJetonSalvo == null)
                    primeiroJetonSalvo = jeton;
            }

            // E. Queimar os pontos da fila consumidos
            for (PontosSaldo saldo : filaUnificada) {
                saldo.setJeton(primeiroJetonSalvo);
                saldo.setPontosUtilizados(saldo.getPontosUtilizados() + saldo.getPontosSobrando());
                saldo.setPontosSobrando(0);
                saldo.setInSituacao("I");
                pontosSaldoRepository.save(saldo);
            }

            // F. Gerar a "Sobra" consolidada para o próximo mês
            if (pontosAcumuladosJanela > 0) {
                PontosSaldo sobraSaldo = new PontosSaldo();
                sobraSaldo.setAtividade(null);
                sobraSaldo.setConselheiro(conselheiro);
                sobraSaldo.setGestao(gestao);
                sobraSaldo.setResolucao(resolucaoTeto);
                sobraSaldo.setDataHora(ultimoDiaMes.atTime(23, 59, 59));
                sobraSaldo.setPontosTrabalhados(pontosAcumuladosJanela);
                sobraSaldo.setPontosUtilizados(0);
                sobraSaldo.setPontosSobrando(pontosAcumuladosJanela);
                sobraSaldo.setInSituacao("A");
                pontosSaldoRepository.save(sobraSaldo);
            }

            // G. CORREÇÃO CRÍTICA: Persistência direta no DB para fechar as Atividades
            if (!idsAtividadesProcessadas.isEmpty()) {
                atividadeRepository.marcarComoComputadaEmLote(idsAtividadesProcessadas);
            }
        }
    }

    @Transactional
    public void estornarFolhaDoConselheiro(Integer idPessoa, Integer idGestao, Integer mes, Integer ano) {
        List<Jeton> jetons = jetonRepository.findByGestaoIdGestaoAndMesAndAno(idGestao, mes, ano).stream()
                .filter(j -> j.getConselheiro().getIdPessoa().equals(idPessoa))
                .toList();
        if (jetons.isEmpty())
            return;

        // 1. Apaga a "Sobra" que foi gerada e jogada para o próximo mês
        LocalDate ultimoDia = LocalDate.of(ano, mes, 1).with(java.time.temporal.TemporalAdjusters.lastDayOfMonth());
        LocalDateTime dataSobra = ultimoDia.atTime(23, 59, 59);
        List<PontosSaldo> sobras = pontosSaldoRepository.buscarSaldosDisponiveisOrdenadosFIFO(idPessoa, idGestao)
                .stream()
                .filter(ps -> ps.getAtividade() == null && ps.getDataHora().equals(dataSobra))
                .toList();
        pontosSaldoRepository.deleteAll(sobras);

        // 2. Desfaz a associação de Pontos e Jetons de forma cirúrgica
        for (Jeton j : jetons) {
            List<PontosSaldo> consumidos = pontosSaldoRepository.findByJetonIdJeton(j.getIdJeton());
            for (PontosSaldo s : consumidos) {
                s.setJeton(null); // Quebra o vínculo para não dar erro de chave estrangeira
                pontosSaldoRepository.save(s);

                // SE O SALDO VEIO DE UMA ATIVIDADE DESTE MÊS: Nós a apagamos para ela não se
                // duplicar ao reprocessar
                if (s.getAtividade() != null &&
                        s.getAtividade().getDataHoraAtividade().getMonthValue() == mes &&
                        s.getAtividade().getDataHoraAtividade().getYear() == ano) {

                    pontosSaldoRepository.delete(s);
                }
                // SE FOR UM SALDO HISTÓRICO (Carga Inicial ou Sobra anterior): Restauramos ele
                // para o estado Ativo
                else {
                    s.setPontosSobrando(s.getPontosTrabalhados());
                    s.setPontosUtilizados(0);
                    s.setInSituacao("A");
                    pontosSaldoRepository.save(s);
                }
            }
            j.getAtividades().clear(); // Limpa as dependências da tabela jeton_atividade
            jetonRepository.delete(j);
        }

        // 3. Devolve as atividades reais para o status original (C + N)
        atividadeRepository.reverterAtividadesComputadas(idPessoa, idGestao, mes, ano);
    }

    @Transactional
    public void estornarFolhaEmLote(Integer idGestao, Integer mes, Integer ano) {
        List<Jeton> jetons = jetonRepository.findByGestaoIdGestaoAndMesAndAno(idGestao, mes, ano);

        if (jetons.isEmpty()) {
            throw new RuntimeException(
                    "Não há pagamentos processados para estornar nesta competência " + mes + "/" + ano + ".");
        }

        // Pega os conselheiros únicos e invoca o estorno seguro para cada um deles
        Set<Integer> conselheirosIds = jetons.stream()
                .map(j -> j.getConselheiro().getIdPessoa())
                .collect(Collectors.toSet());

        for (Integer idPessoa : conselheirosIds) {
            estornarFolhaDoConselheiro(idPessoa, idGestao, mes, ano);
        }
    }

    @Transactional
    public void estornarJetonPontual(Integer idJeton) {
        Jeton jeton = jetonRepository.findById(idJeton)
                .orElseThrow(() -> new RuntimeException("Registro financeiro não encontrado."));

        if ("P".equals(jeton.getInSituacao())) {
            throw new RuntimeException("Estorno negado: Este pagamento já foi HOMOLOGADO em folha definitiva.");
        }

        List<PontosSaldo> saldos = pontosSaldoRepository.findByJetonIdJeton(idJeton);
        for (PontosSaldo saldo : saldos) {
            saldo.setPontosSobrando(saldo.getPontosSobrando() + saldo.getPontosUtilizados());
            saldo.setPontosUtilizados(0);
            saldo.setInSituacao("A");
            saldo.setJeton(null);
            pontosSaldoRepository.save(saldo);
        }

        atividadeRepository.reverterAtividadesComputadas(
                jeton.getConselheiro().getIdPessoa(),
                jeton.getGestao().getIdGestao(),
                jeton.getMes(),
                jeton.getAno());

        jetonRepository.delete(jeton);
    }

    @Transactional
    public void excluirJeton(Integer id) {
        jetonRepository.deleteById(id);
    }

    @Transactional
    public void realizarFechamentoDefinitivoFolha(Gestao gestao, Integer mes, Integer ano) {
        int totalAtualizado = atividadeRepository.fecharAtividadesEmFolha(gestao.getIdGestao(), mes, ano);

        if (totalAtualizado == 0) {
            throw new RuntimeException(
                    "Não foram encontradas atividades validadas e computadas (C+S) para fechar na competência " + mes
                            + "/" + ano + ".");
        }

        List<Jeton> jetonsDoMes = jetonRepository.findByGestaoIdGestaoAndMesAndAno(gestao.getIdGestao(), mes, ano);
        for (Jeton j : jetonsDoMes) {
            j.setInSituacao("I"); // Jeton Inativo (Fechado/Pago definitivo)
            jetonRepository.save(j);
        }
    }
}