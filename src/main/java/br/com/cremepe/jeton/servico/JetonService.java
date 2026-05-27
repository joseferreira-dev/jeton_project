package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.*;
import br.com.cremepe.jeton.dto.PontosRemanescentesDTO;
import br.com.cremepe.jeton.repositorio.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class JetonService {

    private static final Logger log = LoggerFactory.getLogger(JetonService.class);

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
    @Autowired
    private RegrasService regrasService;

    // =========================================================================
    // LEITURA
    // =========================================================================

    @Transactional(readOnly = true)
    public List<PontosRemanescentesDTO> listarSaldosAgrupados() {
        return pontosSaldoRepository.buscarSaldosAgrupadosPorConselheiro();
    }

    @Transactional(readOnly = true)
    public List<Jeton> listarTodos() {
        return jetonRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Jeton> pesquisarHistorico(Integer idGestao, Integer mes, Integer ano, String termo) {
        String termoBusca = (termo != null && !termo.trim().isEmpty()) ? termo.trim() : null;
        return jetonRepository.pesquisarHistorico(idGestao, mes, ano, termoBusca);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarAtividadesAgrupadasPorConselheiro(Integer idPessoa, Integer idGestao,
            Integer mes, Integer ano) {
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

    // =========================================================================
    // PROCESSAMENTO
    // =========================================================================

    @Transactional
    public void processarFechamentoMensal(Gestao gestao, Integer mes, Integer ano) {
        log.info("Iniciando processamento mensal: gestão={}, competência={}/{}", gestao.getIdGestao(), mes, ano);

        // Trava 1: Nenhuma atividade pendente
        long pendentesNoMes = atividadeRepository.contarAtividadesPendentesNoMes(gestao.getIdGestao(), mes, ano);
        if (pendentesNoMes > 0) {
            throw new RuntimeException("Cálculo bloqueado: existem " + pendentesNoMes +
                    " atividade(s) pendentes no período. Valide-as ou exclua-as antes de processar.");
        }

        // Trava 2: Meses anteriores devem estar fechados
        LocalDateTime inicioDoMes = LocalDate.of(ano, mes, 1).atStartOfDay();
        long anterioresNaoFechadas = atividadeRepository.contarAtividadesAnterioresNaoFechadas(gestao.getIdGestao(),
                inicioDoMes);
        if (anterioresNaoFechadas > 0) {
            throw new RuntimeException("Cálculo bloqueado: existem " + anterioresNaoFechadas +
                    " atividade(s) de meses anteriores ainda não homologadas.");
        }

        // Auto-limpeza: estorna processamentos antigos da mesma competência
        List<Jeton> jetonsExistentes = jetonRepository.findByGestaoIdGestaoAndMesAndAno(gestao.getIdGestao(), mes, ano);
        List<AtividadeConselhal> atividadesComputadas = atividadeRepository.findComputadasDoMes(gestao.getIdGestao(),
                mes, ano);
        Set<Integer> conselheirosParaEstorno = new HashSet<>();
        jetonsExistentes.forEach(j -> conselheirosParaEstorno.add(j.getConselheiro().getIdPessoa()));
        atividadesComputadas.forEach(a -> conselheirosParaEstorno.add(a.getConselheiro().getIdPessoa()));
        for (Integer idPessoa : conselheirosParaEstorno) {
            estornarFolhaDoConselheiro(idPessoa, gestao.getIdGestao(), mes, ano);
        }

        // Obter resolução vigente no último dia do mês
        LocalDate ultimoDiaMes = LocalDate.of(ano, mes, 1).with(java.time.temporal.TemporalAdjusters.lastDayOfMonth());
        List<Resolucao> resolucoesVigentes = resolucaoRepository.findResolucoesVigentesNaData(ultimoDiaMes);
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

            // Transformar novas atividades em saldos
            for (AtividadeConselhal atividade : novasAtividades) {
                List<Resolucao> resAtvList = resolucaoRepository
                        .findResolucoesVigentesNaData(atividade.getDataHoraAtividade().toLocalDate());
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
                int totalPontos = qtdAtv * multRegra;

                novoSaldo.setPontosTrabalhados(totalPontos);
                novoSaldo.setPontosUtilizados(0);
                novoSaldo.setPontosSobrando(totalPontos);
                novoSaldo.setInSituacao(PontosSaldo.SITUACAO_ATIVO);

                filaUnificada.add(pontosSaldoRepository.save(novoSaldo));
                idsAtividadesProcessadas.add(atividade.getIdAtividade());
            }

            filaUnificada.sort(Comparator.comparing(PontosSaldo::getDataHora)
                    .thenComparing(PontosSaldo::getIdPontosSaldo));

            aplicarLimitesTurno(filaUnificada, conselheiro.getIdPessoa());

            // Absorção matemática (FIFO)
            int bufferPontos = 0;
            int totalJetonsGerados = 0;
            int totalPontosConsumidos = 0;
            Map<Resolucao, Integer> demonstrativoJetons = new LinkedHashMap<>();

            for (PontosSaldo saldo : filaUnificada) {
                bufferPontos += saldo.getPontosSobrando();
                Resolucao norma = saldo.getResolucao();
                int pontosPorJeton = (norma.getPontosPorJeton() != null && norma.getPontosPorJeton() > 0)
                        ? norma.getPontosPorJeton()
                        : 3;

                int jetonsPossiveis = bufferPontos / pontosPorJeton;
                int jetonsEfetivos = Math.min(jetonsPossiveis, maxJetonsPermitidos - totalJetonsGerados);

                if (jetonsEfetivos > 0) {
                    demonstrativoJetons.put(norma, demonstrativoJetons.getOrDefault(norma, 0) + jetonsEfetivos);
                    totalJetonsGerados += jetonsEfetivos;
                    int consumidos = jetonsEfetivos * pontosPorJeton;
                    bufferPontos -= consumidos;
                    totalPontosConsumidos += consumidos;
                }
            }

            // Gerar registros financeiros
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
                jeton.setInSituacao(Jeton.SITUACAO_ATIVO);
                jeton.getAtividades().addAll(novasAtividades);
                jeton = jetonRepository.save(jeton);
                if (primeiroJetonSalvo == null)
                    primeiroJetonSalvo = jeton;
            }

            // Consumo fracionado (splitting)
            int pontosParaConsumir = totalPontosConsumidos;
            if (pontosParaConsumir > 0 && primeiroJetonSalvo != null) {
                for (PontosSaldo saldo : filaUnificada) {
                    if (pontosParaConsumir <= 0)
                        break;
                    int disponivel = saldo.getPontosSobrando();
                    if (disponivel == 0)
                        continue;

                    int consumir = Math.min(disponivel, pontosParaConsumir);
                    if (consumir < disponivel) {
                        // divide
                        saldo.setPontosSobrando(0);
                        saldo.setPontosUtilizados(saldo.getPontosUtilizados() + consumir);
                        saldo.setInSituacao(PontosSaldo.SITUACAO_INATIVO);
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
                        remainder.setInSituacao(PontosSaldo.SITUACAO_ATIVO);
                        pontosSaldoRepository.save(remainder);
                    } else {
                        saldo.setPontosSobrando(0);
                        saldo.setPontosUtilizados(saldo.getPontosUtilizados() + consumir);
                        saldo.setInSituacao(PontosSaldo.SITUACAO_INATIVO);
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
        log.info("Processamento mensal concluído: gestão={}, competência={}/{}", gestao.getIdGestao(), mes, ano);
    }

    private void aplicarLimitesTurno(List<PontosSaldo> saldos, Integer idPessoa) {
        Map<String, List<PontosSaldo>> grupos = new LinkedHashMap<>();
        for (PontosSaldo saldo : saldos) {
            if (saldo.getAtividade() == null)
                continue;
            LocalDate data = saldo.getDataHora().toLocalDate();
            String turno = saldo.getAtividade().getInTurno();
            String chave = data.toString() + "_" + turno;
            grupos.computeIfAbsent(chave, k -> new ArrayList<>()).add(saldo);
        }

        for (Map.Entry<String, List<PontosSaldo>> entry : grupos.entrySet()) {
            String[] partes = entry.getKey().split("_");
            LocalDate data = LocalDate.parse(partes[0]);
            String turno = partes[1];

            Integer pagosAnterior = pontosSaldoRepository.sumPontosUtilizadosPorConselheiroDataTurno(idPessoa, data,
                    turno);
            int acumulado = (pagosAnterior != null) ? pagosAnterior : 0;

            Optional<Resolucao> optRes = regrasService.buscarResolucaoPorData(data);
            if (optRes.isEmpty())
                continue;
            int limite = optRes.get().getPontosPorJeton();

            List<PontosSaldo> saldosGrupo = entry.getValue();
            saldosGrupo.sort(Comparator.comparing(PontosSaldo::getDataHora)
                    .thenComparing(PontosSaldo::getIdPontosSaldo));

            for (PontosSaldo saldo : saldosGrupo) {
                int disponivel = saldo.getPontosSobrando();
                if (disponivel == 0)
                    continue;

                if (acumulado >= limite) {
                    saldo.setPontosSobrando(0);
                    pontosSaldoRepository.save(saldo);
                } else {
                    int sobra = limite - acumulado;
                    if (disponivel > sobra) {
                        saldo.setPontosSobrando(sobra);
                        acumulado = limite;
                    } else {
                        acumulado += disponivel;
                    }
                    pontosSaldoRepository.save(saldo);
                }
            }
        }
    }

    @Transactional
    public void estornarFolhaDoConselheiro(Integer idPessoa, Integer idGestao, Integer mes, Integer ano) {
        List<Jeton> jetons = jetonRepository.findByGestaoIdGestaoAndMesAndAno(idGestao, mes, ano).stream()
                .filter(j -> j.getConselheiro().getIdPessoa().equals(idPessoa))
                .toList();

        for (Jeton j : jetons) {
            List<PontosSaldo> consumidos = pontosSaldoRepository.findByJetonIdJeton(j.getIdJeton());
            for (PontosSaldo s : consumidos) {
                s.setPontosSobrando(s.getPontosSobrando() + s.getPontosUtilizados());
                s.setPontosUtilizados(0);
                s.setInSituacao(PontosSaldo.SITUACAO_ATIVO);
                s.setJeton(null);
                pontosSaldoRepository.save(s);
            }
            j.getAtividades().clear();
            jetonRepository.delete(j);
        }

        List<PontosSaldo> saldosAtividadesDesteMes = pontosSaldoRepository.buscarSaldosDeAtividadesDoMes(idPessoa, mes,
                ano);
        pontosSaldoRepository.deleteAll(saldosAtividadesDesteMes);

        atividadeRepository.reverterAtividadesComputadas(idPessoa, idGestao, mes, ano);
        log.debug("Folha estornada para conselheiro {} na competência {}/{}", idPessoa, mes, ano);
    }

    @Transactional
    public void estornarJetonPontual(Integer idJeton) {
        Jeton jeton = jetonRepository.findById(idJeton)
                .orElseThrow(() -> new RuntimeException("Registro financeiro não encontrado."));

        if (Jeton.SITUACAO_PAGO.equals(jeton.getInSituacao())) {
            throw new RuntimeException("Estorno negado: este pagamento já foi homologado em folha definitiva.");
        }

        List<PontosSaldo> saldos = pontosSaldoRepository.findByJetonIdJeton(idJeton);
        for (PontosSaldo saldo : saldos) {
            saldo.setPontosSobrando(saldo.getPontosSobrando() + saldo.getPontosUtilizados());
            saldo.setPontosUtilizados(0);
            saldo.setInSituacao(PontosSaldo.SITUACAO_ATIVO);
            saldo.setJeton(null);
            pontosSaldoRepository.save(saldo);
        }

        atividadeRepository.reverterAtividadesComputadas(
                jeton.getConselheiro().getIdPessoa(),
                jeton.getGestao().getIdGestao(),
                jeton.getMes(),
                jeton.getAno());

        jetonRepository.delete(jeton);
        log.info("Jeton estornado pontualmente: id={}", idJeton);
    }

    @Transactional
    public void realizarFechamentoDefinitivoFolha(Gestao gestao, Integer mes, Integer ano) {
        int totalAtualizado = atividadeRepository.fecharAtividadesEmFolha(gestao.getIdGestao(), mes, ano);
        if (totalAtualizado == 0) {
            throw new RuntimeException("Não foram encontradas atividades validadas e computadas para fechar.");
        }

        List<Jeton> jetonsDoMes = jetonRepository.findByGestaoIdGestaoAndMesAndAno(gestao.getIdGestao(), mes, ano);
        for (Jeton j : jetonsDoMes) {
            j.setInSituacao(Jeton.SITUACAO_EXCLUIDO);
            jetonRepository.save(j);
        }
        log.info("Folha fechada definitivamente: gestão={}, competência={}/{}", gestao.getIdGestao(), mes, ano);
    }

    @Transactional
    public void excluirJeton(Integer id) {
        jetonRepository.deleteById(id);
        log.info("Jeton excluído fisicamente: id={}", id);
    }
}