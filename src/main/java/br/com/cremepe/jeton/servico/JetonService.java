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
        // =========================================================================
        // 0. TRAVAS DE SEGURANÇA E REGRAS DE NEGÓCIO
        // =========================================================================

        // Trava 1: Nenhuma atividade Pendente ('P') no mês atual
        long pendentesNoMes = atividadeRepository.contarAtividadesPendentesNoMes(gestao.getIdGestao(), mes, ano);
        if (pendentesNoMes > 0) {
            throw new RuntimeException(
                    "Cálculo Bloqueado: Existem " + pendentesNoMes + " atividade(s) com status 'Pendente' no período "
                            + mes + "/" + ano + ". Valide-as ou exclua-as antes de processar os Jetons.");
        }

        // Trava 2: Cronologia rigorosa (Meses anteriores DEVEM estar 100% Fechados em
        // Folha Definitiva)
        LocalDateTime inicioDoMes = LocalDate.of(ano, mes, 1).atStartOfDay();
        long anterioresNaoFechadas = atividadeRepository.contarAtividadesAnterioresNaoFechadas(gestao.getIdGestao(),
                inicioDoMes);
        if (anterioresNaoFechadas > 0) {
            throw new RuntimeException("Cálculo Bloqueado: Existem " + anterioresNaoFechadas
                    + " atividade(s) de meses anteriores que ainda não foram homologadas/fechadas definitivamente. Você deve realizar o fechamento definitivo das folhas anteriores.");
        }

        // =========================================================================
        // 1. AUTO-LIMPEZA BLINDADA
        // =========================================================================
        List<Jeton> jetonsExistentes = jetonRepository.findByGestaoIdGestaoAndMesAndAno(gestao.getIdGestao(), mes, ano);
        List<AtividadeConselhal> atividadesComputadas = atividadeRepository.findComputadasDoMes(gestao.getIdGestao(),
                mes, ano);

        Set<Integer> conselheirosParaEstorno = new HashSet<>();
        jetonsExistentes.forEach(j -> conselheirosParaEstorno.add(j.getConselheiro().getIdPessoa()));
        atividadesComputadas.forEach(a -> conselheirosParaEstorno.add(a.getConselheiro().getIdPessoa()));

        for (Integer idPessoa : conselheirosParaEstorno) {
            estornarFolhaDoConselheiro(idPessoa, gestao.getIdGestao(), mes, ano);
        }

        // 1. Identificar Teto da Competência
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

            List<PontosSaldo> saldosAntigos = pontosSaldoRepository.buscarSaldosDisponiveisOrdenadosFIFO(
                    conselheiro.getIdPessoa(), gestao.getIdGestao());
            List<AtividadeConselhal> novasAtividades = atividadeRepository.findHomologadasParaCalculo(
                    conselheiro.getIdPessoa(), mes, ano);

            if (saldosAntigos.isEmpty() && novasAtividades.isEmpty())
                continue;

            List<PontosSaldo> filaUnificada = new ArrayList<>(saldosAntigos);
            List<Integer> idsAtividadesProcessadas = new ArrayList<>();

            // Transformar novas atividades em saldos na fila
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

            // D. Módulo de Absorção Matemática
            int bufferPontos = 0;
            int totalJetonsGeradosConselheiro = 0;
            int totalPontosConsumidosGeral = 0;
            Map<Resolucao, Integer> demonstrativoJetons = new LinkedHashMap<>();

            for (PontosSaldo saldo : filaUnificada) {
                bufferPontos += saldo.getPontosSobrando();
                Resolucao normaVigenteSaldo = saldo.getResolucao();
                int pontosPorJeton = (normaVigenteSaldo.getPontosPorJeton() != null
                        && normaVigenteSaldo.getPontosPorJeton() > 0)
                                ? normaVigenteSaldo.getPontosPorJeton()
                                : 3;

                int jetonsPossiveis = bufferPontos / pontosPorJeton;
                int jetonsEfetivos = Math.min(jetonsPossiveis, maxJetonsPermitidos - totalJetonsGeradosConselheiro);

                if (jetonsEfetivos > 0) {
                    demonstrativoJetons.put(normaVigenteSaldo,
                            demonstrativoJetons.getOrDefault(normaVigenteSaldo, 0) + jetonsEfetivos);
                    totalJetonsGeradosConselheiro += jetonsEfetivos;
                    int consumidosAqui = jetonsEfetivos * pontosPorJeton;
                    bufferPontos -= consumidosAqui;
                    totalPontosConsumidosGeral += consumidosAqui;
                }
            }

            // E. Gerar Registos Financeiros
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

                if (jeton.getAtividades() == null)
                    jeton.setAtividades(new ArrayList<>());
                jeton.getAtividades().addAll(novasAtividades);

                jeton = jetonRepository.save(jeton);
                if (primeiroJetonSalvo == null)
                    primeiroJetonSalvo = jeton;
            }

            // F. UTXO Coin Splitting (Queima Fracionada de Pontos)
            int pontosParaConsumir = totalPontosConsumidosGeral;

            if (pontosParaConsumir > 0 && primeiroJetonSalvo != null) {
                for (PontosSaldo saldo : filaUnificada) {
                    if (pontosParaConsumir <= 0)
                        break;

                    int disponivel = saldo.getPontosSobrando();
                    if (disponivel == 0)
                        continue;

                    int consumir = Math.min(disponivel, pontosParaConsumir);

                    if (consumir < disponivel) {
                        // Partição do Saldo (Atual guarda o consumo, Nova linha retém a sobra)
                        saldo.setPontosSobrando(0);
                        saldo.setPontosUtilizados(saldo.getPontosUtilizados() + consumir);
                        saldo.setInSituacao("I");
                        saldo.setJeton(primeiroJetonSalvo);
                        pontosSaldoRepository.save(saldo);

                        PontosSaldo remainder = new PontosSaldo();
                        remainder.setAtividade(saldo.getAtividade());
                        remainder.setConselheiro(saldo.getConselheiro());
                        remainder.setGestao(saldo.getGestao());
                        remainder.setResolucao(saldo.getResolucao());
                        remainder.setDataHora(saldo.getDataHora());
                        remainder.setPontosTrabalhados(disponivel - consumir);
                        remainder.setPontosUtilizados(0);
                        remainder.setPontosSobrando(disponivel - consumir);
                        remainder.setInSituacao("A");
                        pontosSaldoRepository.save(remainder);
                    } else {
                        // Consumo Integral
                        saldo.setPontosSobrando(0);
                        saldo.setPontosUtilizados(saldo.getPontosUtilizados() + consumir);
                        saldo.setInSituacao("I");
                        saldo.setJeton(primeiroJetonSalvo);
                        pontosSaldoRepository.save(saldo);
                    }
                    pontosParaConsumir -= consumir;
                }
            }

            if (!idsAtividadesProcessadas.isEmpty()) {
                atividadeRepository.marcarComoComputadaEmLote(idsAtividadesProcessadas);
            }
        }
    }

    @Transactional
    public void estornarFolhaDoConselheiro(Integer idPessoa, Integer idGestao, Integer mes, Integer ano) {
        // 1. Encontrar pagamentos e devolver pontos à origem sem perder o histórico
        List<Jeton> jetons = jetonRepository.findByGestaoIdGestaoAndMesAndAno(idGestao, mes, ano).stream()
                .filter(j -> j.getConselheiro().getIdPessoa().equals(idPessoa))
                .toList();

        for (Jeton j : jetons) {
            List<PontosSaldo> consumidos = pontosSaldoRepository.findByJetonIdJeton(j.getIdJeton());
            for (PontosSaldo s : consumidos) {
                s.setPontosSobrando(s.getPontosSobrando() + s.getPontosUtilizados());
                s.setPontosUtilizados(0);
                s.setInSituacao("A");
                s.setJeton(null);
                pontosSaldoRepository.save(s);
            }
            j.getAtividades().clear();
            jetonRepository.delete(j);
        }

        // 2. Apagar saldos espelho de atividades que ocorreram estritamente neste mês
        List<PontosSaldo> saldosAtividadesDesteMes = pontosSaldoRepository.buscarSaldosDeAtividadesDoMes(idPessoa, mes,
                ano);
        pontosSaldoRepository.deleteAll(saldosAtividadesDesteMes);

        // 3. Reverter as atividades
        atividadeRepository.reverterAtividadesComputadas(idPessoa, idGestao, mes, ano);
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
                    "Não foram encontradas atividades validadas e computadas para fechar na competência " + mes
                            + "/" + ano + ".");
        }

        List<Jeton> jetonsDoMes = jetonRepository.findByGestaoIdGestaoAndMesAndAno(gestao.getIdGestao(), mes, ano);
        for (Jeton j : jetonsDoMes) {
            j.setInSituacao("E");
            jetonRepository.save(j);
        }
    }

    @Transactional(readOnly = true)
    public List<Jeton> pesquisarHistorico(Integer idGestao, Integer mes, Integer ano, String termo) {
        String termoBusca = (termo != null && !termo.trim().isEmpty()) ? termo.trim() : null;
        return jetonRepository.pesquisarHistorico(idGestao, mes, ano, termoBusca);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarAtividadesAgrupadasPorConselheiro(Integer idPessoa, Integer idGestao,
            Integer mes, Integer ano) {
        // Busca todos os jetons daquele conselheiro no mês especificado
        List<Jeton> jetons = jetonRepository.findByGestaoIdGestaoAndMesAndAno(idGestao, mes, ano).stream()
                .filter(j -> j.getConselheiro().getIdPessoa().equals(idPessoa))
                .toList();

        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Jeton jeton : jetons) {
            if (jeton.getAtividades() != null) {
                for (AtividadeConselhal at : jeton.getAtividades()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("regra", at.getRegra() != null ? at.getRegra().getNomeRegra() : "Regra não identificada");
                    map.put("data", at.getDataHoraAtividade().toLocalDate().toString());
                    map.put("qtd", at.getQtdAtividade());
                    resultado.add(map);
                }
            }
        }
        return resultado;
    }
}