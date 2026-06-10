package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.annotation.Auditar;
import br.com.cremepe.jeton.domain.*;
import br.com.cremepe.jeton.dto.*;
import br.com.cremepe.jeton.repository.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
    @Autowired
    private ConselheiroRepository conselheiroRepository;
    @Autowired
    private GestaoRepository gestaoRepository;
    @Autowired
    private ConselheiroService conselheiroService;

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

    @Auditar(tabela = "jeton", acao = "PROCESSAR_FOLHA", descricao = "Processamento mensal de folha de jetons", dadosParametros = "{ 'idGestao': #gestao.idGestao, 'nomeGestao': #gestao.nomeGestao, 'mes': #mes, 'ano': #ano }", capturarEstadoAnterior = false, auditarExcecao = true, incluirRetorno = false)
    @Transactional
    public void processarFechamentoMensal(Gestao gestao, Integer mes, Integer ano) {
        String nomeGestao = gestao.getNomeGestao();
        log.info("Iniciando processamento mensal: gestão '{}' (ID={}), competência {}/{}",
                nomeGestao, gestao.getIdGestao(), mes, ano);

        // Validações preliminares
        validarPreProcessamento(gestao.getIdGestao(), mes, ano);
        limparProcessamentosAnteriores(gestao.getIdGestao(), mes, ano);

        // Resolução vigente no último dia do mês
        Resolucao resolucaoTeto = obterResolucaoVigente(gestao.getIdGestao(), mes, ano);
        int maxJetonsPermitidos = (resolucaoTeto.getMaxJetonsMes() != null && resolucaoTeto.getMaxJetonsMes() > 0)
                ? resolucaoTeto.getMaxJetonsMes()
                : 22;

        List<GestaoConselheiro> vinculos = gestaoConselheiroRepository.findByGestaoIdGestao(gestao.getIdGestao());
        int totalConselheirosProcessados = 0;
        int totalJetonsGeradosGeral = 0;
        BigDecimal totalValorPagoGeral = BigDecimal.ZERO;

        for (GestaoConselheiro vinculo : vinculos) {
            Conselheiro conselheiro = vinculo.getConselheiro();
            ProcessamentoResultado resultado = processarConselheiro(conselheiro, gestao, mes, ano, resolucaoTeto,
                    maxJetonsPermitidos);
            if (resultado != null) {
                totalConselheirosProcessados++;
                totalJetonsGeradosGeral += resultado.totalJetons;
                totalValorPagoGeral = totalValorPagoGeral.add(resultado.valorTotal);
            }
        }

        log.info("Processamento mensal finalizado: gestão '{}', {}/{} - {} conselheiros, {} jetons, valor total {}",
                nomeGestao, mes, ano, totalConselheirosProcessados, totalJetonsGeradosGeral, totalValorPagoGeral);
    }

    private void validarPreProcessamento(Integer idGestao, Integer mes, Integer ano) {
        // Trava 1: Impede recálculo de mês já homologado
        if (isFolhaHomologada(idGestao, mes, ano)) {
            throw new RuntimeException("A folha do período " + mes + "/" + ano +
                    " já foi homologada e não pode ser recalculada.");
        }

        // Trava 2: Nenhuma atividade pendente
        long pendentesNoMes = atividadeRepository.contarAtividadesPendentesNoMes(idGestao, mes, ano);
        if (pendentesNoMes > 0) {
            throw new RuntimeException("Cálculo bloqueado: existem " + pendentesNoMes +
                    " atividade(s) pendentes no período. Valide-as ou exclua-as antes de processar.");
        }

        // Trava 3: Meses anteriores devem estar fechados
        LocalDateTime inicioDoMes = LocalDate.of(ano, mes, 1).atStartOfDay();
        long anterioresNaoFechadas = atividadeRepository.contarAtividadesAnterioresNaoFechadas(idGestao, inicioDoMes);
        if (anterioresNaoFechadas > 0) {
            throw new RuntimeException("Cálculo bloqueado: existem " + anterioresNaoFechadas +
                    " atividade(s) de meses anteriores ainda não homologadas.");
        }
    }

    private void limparProcessamentosAnteriores(Integer idGestao, Integer mes, Integer ano) {
        List<Jeton> jetonsExistentes = jetonRepository.findByGestaoIdGestaoAndMesAndAno(idGestao, mes, ano);
        List<AtividadeConselhal> atividadesComputadas = atividadeRepository.findComputadasDoMes(idGestao, mes, ano);
        Set<Integer> conselheirosParaEstorno = new HashSet<>();
        jetonsExistentes.forEach(j -> conselheirosParaEstorno.add(j.getConselheiro().getIdPessoa()));
        atividadesComputadas.forEach(a -> conselheirosParaEstorno.add(a.getConselheiro().getIdPessoa()));

        for (Integer idPessoa : conselheirosParaEstorno) {
            estornarFolhaDoConselheiro(idPessoa, idGestao, mes, ano);
        }
    }

    private Resolucao obterResolucaoVigente(Integer idGestao, Integer mes, Integer ano) {
        LocalDate ultimoDiaMes = LocalDate.of(ano, mes, 1).with(java.time.temporal.TemporalAdjusters.lastDayOfMonth());
        List<Resolucao> resolucoesVigentes = resolucaoRepository.findResolucoesVigentesNaData(ultimoDiaMes);
        if (resolucoesVigentes.isEmpty()) {
            throw new RuntimeException("Nenhuma Resolução ativa cadastrada para a competência " + mes + "/" + ano);
        }
        return resolucoesVigentes.get(0);
    }

    private ProcessamentoResultado processarConselheiro(Conselheiro conselheiro, Gestao gestao,
            Integer mes, Integer ano,
            Resolucao resolucaoTeto, int maxJetonsPermitidos) {
        String nomeConselheiro = conselheiro.getPessoa().getNome();
        List<PontosSaldo> saldosAntigos = pontosSaldoRepository.buscarSaldosDisponiveisOrdenadosFIFO(
                conselheiro.getIdPessoa(), gestao.getIdGestao());
        List<AtividadeConselhal> novasAtividades = atividadeRepository.findHomologadasParaCalculo(
                conselheiro.getIdPessoa(), mes, ano);

        if (saldosAntigos.isEmpty() && novasAtividades.isEmpty())
            return null;

        log.debug("Processando conselheiro: {} (ID={})", nomeConselheiro, conselheiro.getIdPessoa());

        List<PontosSaldo> filaUnificada = new ArrayList<>(saldosAntigos);
        List<Integer> idsAtividadesProcessadas = new ArrayList<>();

        // Converte atividades em saldos e adiciona à fila
        for (AtividadeConselhal atividade : novasAtividades) {
            PontosSaldo novoSaldo = converterAtividadeEmSaldo(atividade, conselheiro, gestao, resolucaoTeto);
            filaUnificada.add(pontosSaldoRepository.save(novoSaldo));
            idsAtividadesProcessadas.add(atividade.getIdAtividade());
        }

        filaUnificada.sort(Comparator.comparing(PontosSaldo::getDataHora)
                .thenComparing(PontosSaldo::getIdPontosSaldo));

        aplicarLimitesTurno(filaUnificada, conselheiro.getIdPessoa());

        // Cálculo dos jetons (absorção FIFO)
        ResultadoAbsorcao absorcao = calcularJetonsParaConselheiro(filaUnificada, maxJetonsPermitidos);

        if (absorcao.totalJetons == 0 && idsAtividadesProcessadas.isEmpty()) {
            return null; // nenhum jeton gerado e nenhuma atividade nova
        }

        // Gera os registros financeiros (Jetons)
        List<Jeton> jetonsGerados = gerarRegistrosJetons(absorcao.demonstrativo, gestao, conselheiro, mes, ano,
                novasAtividades);
        Jeton primeiroJeton = jetonsGerados.isEmpty() ? null : jetonsGerados.get(0);

        // Consumo fracionado dos pontos (splitting)
        consumirPontosFracionados(filaUnificada, absorcao.totalPontosConsumidos, primeiroJeton);

        if (!idsAtividadesProcessadas.isEmpty()) {
            atividadeRepository.marcarComoComputadaEmLote(idsAtividadesProcessadas);
        }

        BigDecimal valorTotal = jetonsGerados.stream().map(Jeton::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ProcessamentoResultado(absorcao.totalJetons, valorTotal);
    }

    private PontosSaldo converterAtividadeEmSaldo(AtividadeConselhal atividade, Conselheiro conselheiro,
            Gestao gestao, Resolucao resolucaoTeto) {
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

        return novoSaldo;
    }

    private ResultadoAbsorcao calcularJetonsParaConselheiro(List<PontosSaldo> filaUnificada, int maxJetonsPermitidos) {
        int bufferPontos = 0;
        int totalJetons = 0;
        int totalPontosConsumidos = 0;
        Map<Resolucao, Integer> demonstrativo = new LinkedHashMap<>();

        for (PontosSaldo saldo : filaUnificada) {
            bufferPontos += saldo.getPontosSobrando();
            Resolucao norma = saldo.getResolucao();
            int pontosPorJeton = (norma.getPontosPorJeton() != null && norma.getPontosPorJeton() > 0)
                    ? norma.getPontosPorJeton()
                    : 3;

            int jetonsPossiveis = bufferPontos / pontosPorJeton;
            int jetonsEfetivos = Math.min(jetonsPossiveis, maxJetonsPermitidos - totalJetons);

            if (jetonsEfetivos > 0) {
                demonstrativo.put(norma, demonstrativo.getOrDefault(norma, 0) + jetonsEfetivos);
                totalJetons += jetonsEfetivos;
                int consumidos = jetonsEfetivos * pontosPorJeton;
                bufferPontos -= consumidos;
                totalPontosConsumidos += consumidos;
            }
        }

        ResultadoAbsorcao resultado = new ResultadoAbsorcao();
        resultado.demonstrativo = demonstrativo;
        resultado.totalJetons = totalJetons;
        resultado.totalPontosConsumidos = totalPontosConsumidos;
        return resultado;
    }

    private List<Jeton> gerarRegistrosJetons(Map<Resolucao, Integer> demonstrativo, Gestao gestao,
            Conselheiro conselheiro, Integer mes, Integer ano,
            List<AtividadeConselhal> novasAtividades) {
        List<Jeton> jetonsSalvos = new ArrayList<>();
        for (Map.Entry<Resolucao, Integer> entry : demonstrativo.entrySet()) {
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
            jetonsSalvos.add(jetonRepository.save(jeton));
            log.debug("   Gerado {} jetons para {} no valor total de {} ({} por jeton)",
                    entry.getValue(), conselheiro.getPessoa().getNome(), jeton.getValor().toString(),
                    valorBase.toString());
        }
        return jetonsSalvos;
    }

    private void consumirPontosFracionados(List<PontosSaldo> filaUnificada, int pontosParaConsumir, Jeton jetonAlvo) {
        if (pontosParaConsumir <= 0 || jetonAlvo == null)
            return;

        for (PontosSaldo saldo : filaUnificada) {
            if (pontosParaConsumir <= 0)
                break;
            int disponivel = saldo.getPontosSobrando();
            if (disponivel == 0)
                continue;

            int consumir = Math.min(disponivel, pontosParaConsumir);
            if (consumir < disponivel) {
                // Divide o saldo
                saldo.setPontosSobrando(0);
                saldo.setPontosUtilizados(saldo.getPontosUtilizados() + consumir);
                saldo.setInSituacao(PontosSaldo.SITUACAO_INATIVO);
                saldo.setJeton(jetonAlvo);
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
                saldo.setJeton(jetonAlvo);
                pontosSaldoRepository.save(saldo);
            }
            pontosParaConsumir -= consumir;
        }
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

    private void estornarFolhaDoConselheiro(Integer idPessoa, Integer idGestao, Integer mes, Integer ano) {
        String nomeConselheiro = conselheiroRepository.findById(idPessoa)
                .map(c -> c.getPessoa().getNome())
                .orElse("ID " + idPessoa);
        log.debug("Estornando folha do conselheiro {} para gestão ID {}, competência {}/{}",
                nomeConselheiro, idGestao, mes, ano);

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
        log.debug("Folha estornada para {} na competência {}/{}", nomeConselheiro, mes, ano);
    }

    @Auditar(tabela = "jeton", acao = "ESTORNAR_PONTUAL", descricao = "Estorno pontual de um Jeton (exclusão lógica)", dadosParametros = "{ 'idJeton': #idJeton }", capturarEstadoAnterior = true, auditarExcecao = true, incluirRetorno = false)
    @Transactional
    public void estornarJetonPontual(Integer idJeton) {
        Jeton jeton = jetonRepository.findById(idJeton)
                .orElseThrow(() -> new RuntimeException("Registro financeiro não encontrado."));

        if (Jeton.SITUACAO_PAGO.equals(jeton.getInSituacao())) {
            throw new RuntimeException("Estorno negado: este pagamento já foi homologado em folha definitiva.");
        }

        String nomeConselheiro = jeton.getConselheiro().getPessoa().getNome();
        String nomeGestao = jeton.getGestao().getNomeGestao();

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

        log.info("Jeton estornado pontualmente: ID {} - conselheiro '{}', gestão '{}', {}/{}",
                idJeton, nomeConselheiro, nomeGestao, jeton.getMes(), jeton.getAno());
    }

    @Auditar(tabela = "jeton", acao = "HOMOLOGAR_FOLHA", descricao = "Homologação e fechamento definitivo da folha mensal", dadosParametros = "{ 'idGestao': #gestao.idGestao, 'nomeGestao': #gestao.nomeGestao, 'mes': #mes, 'ano': #ano }", capturarEstadoAnterior = false, auditarExcecao = true, incluirRetorno = false)
    @Transactional
    public void realizarFechamentoDefinitivoFolha(Gestao gestao, Integer mes, Integer ano) {
        String nomeGestao = gestao.getNomeGestao();

        int totalAtualizado = atividadeRepository.fecharAtividadesEmFolha(gestao.getIdGestao(), mes, ano);
        if (totalAtualizado == 0) {
            throw new RuntimeException("Não foram encontradas atividades validadas e computadas para fechar.");
        }

        List<Jeton> jetonsDoMes = jetonRepository.findByGestaoIdGestaoAndMesAndAno(gestao.getIdGestao(), mes, ano);
        for (Jeton j : jetonsDoMes) {
            j.setInSituacao(Jeton.SITUACAO_EXCLUIDO);
            jetonRepository.save(j);
        }

        log.info("Folha fechada definitivamente: gestão '{}', {}/{} - {} atividades fechadas, {} jetons afetados",
                nomeGestao, mes, ano, totalAtualizado, jetonsDoMes.size());
    }

    @Auditar(tabela = "jeton", acao = "EXCLUIR", descricao = "Exclusão física de um registro de Jeton", dadosParametros = "{ 'idJeton': #id }", capturarEstadoAnterior = true, auditarExcecao = true, incluirRetorno = false)
    @Transactional
    public void excluirJeton(Integer id) {
        Jeton jeton = jetonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Jeton não encontrado com ID: " + id));

        String nomeConselheiro = jeton.getConselheiro().getPessoa().getNome();
        String nomeGestao = jeton.getGestao().getNomeGestao();

        jetonRepository.deleteById(id);
        log.info("Jeton excluído fisicamente: ID {} - conselheiro '{}', gestão '{}', {}/{}",
                id, nomeConselheiro, nomeGestao, jeton.getMes(), jeton.getAno());
    }

    private boolean isFolhaHomologada(Integer idGestao, Integer mes, Integer ano) {
        List<Jeton> jetonsHomologados = jetonRepository.findByGestaoIdGestaoAndMesAndAno(idGestao, mes, ano)
                .stream().filter(j -> Jeton.SITUACAO_EXCLUIDO.equals(j.getInSituacao()))
                .toList();
        if (!jetonsHomologados.isEmpty())
            return true;

        long atividadesFechadas = atividadeRepository.countAtividadesFechadasNoPeriodo(idGestao, mes, ano);
        return atividadesFechadas > 0;
    }

    public List<Jeton> listarPorConselheiro(Integer idPessoa) {
        return jetonRepository.findByConselheiroIdPessoaOrderByAnoDescMesDesc(idPessoa);
    }

    public List<Jeton> listarPorConselheiro(Integer idPessoa, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "ano", "mes"));
        Page<Jeton> page = jetonRepository.findByConselheiroIdPessoa(idPessoa, pageable);
        return page.getContent();
    }

    @Transactional(readOnly = true)
    public RelatorioGeralDTO gerarRelatorioGeral(Integer idGestao, Integer mes, Integer ano) {
        Gestao gestao = gestaoRepository.findById(idGestao)
                .orElseThrow(() -> new RuntimeException("Gestão não encontrada"));

        List<Jeton> jetons = jetonRepository.findByGestaoIdGestaoAndMesAndAno(idGestao, mes, ano);
        Map<Integer, ConselheiroRelatorioDTO> mapa = new LinkedHashMap<>();

        for (Jeton j : jetons) {
            ConselheiroRelatorioDTO dto = mapa.computeIfAbsent(j.getConselheiro().getIdPessoa(), k -> {
                ConselheiroRelatorioDTO novo = new ConselheiroRelatorioDTO();
                novo.setIdPessoa(k);
                novo.setNome(j.getConselheiro().getPessoa().getNome());
                novo.setTotalJetons(0);
                novo.setValor(BigDecimal.ZERO);
                novo.setSaldoAnterior(0);
                novo.setPontosAcumuladosMes(0);
                novo.setSaldoFuturo(0);
                novo.setAtividades(new ArrayList<>());
                return novo;
            });
            dto.setTotalJetons(dto.getTotalJetons() + j.getTotalJeton());
            dto.setValor(dto.getValor().add(j.getValor()));
        }

        for (Map.Entry<Integer, ConselheiroRelatorioDTO> entry : mapa.entrySet()) {
            Integer idPessoa = entry.getKey();
            ConselheiroRelatorioDTO dto = entry.getValue();

            List<Jeton> jetonsCons = jetons.stream()
                    .filter(j -> j.getConselheiro().getIdPessoa().equals(idPessoa))
                    .collect(Collectors.toList());
            List<PontosSaldo> todosSaldos = new ArrayList<>();
            for (Jeton j : jetonsCons) {
                todosSaldos.addAll(pontosSaldoRepository.findByJetonIdJeton(j.getIdJeton()));
            }

            int saldoAnterior = 0;
            int pontosAcumuladosMes = 0;
            for (PontosSaldo ps : todosSaldos) {
                boolean doMesAtual = false;
                if (ps.getAtividade() != null) {
                    LocalDate dataAtv = ps.getAtividade().getDataHoraAtividade().toLocalDate();
                    if (dataAtv.getYear() == ano && dataAtv.getMonthValue() == mes) {
                        doMesAtual = true;
                    }
                }
                if (doMesAtual) {
                    pontosAcumuladosMes += ps.getPontosUtilizados();
                } else {
                    saldoAnterior += ps.getPontosUtilizados();
                }
            }
            dto.setSaldoAnterior(saldoAnterior);
            dto.setPontosAcumuladosMes(pontosAcumuladosMes);

            Integer saldoFuturo = pontosSaldoRepository.somarPontosSobrandoAtivos(idPessoa, idGestao);
            dto.setSaldoFuturo(saldoFuturo != null ? saldoFuturo : 0);

            List<AtividadeConselhal> atividadesMes = atividadeRepository.findComputadasPorConselheiroEMes(idPessoa, mes,
                    ano);
            List<AtividadeRelatorioDTO> atividadesDTO = new ArrayList<>();
            for (AtividadeConselhal at : atividadesMes) {
                AtividadeRelatorioDTO atDTO = new AtividadeRelatorioDTO();
                atDTO.setRegra(at.getRegra().getNomeRegra());
                atDTO.setData(at.getDataHoraAtividade().toLocalDate());
                atDTO.setQuantidade(at.getQtdAtividade());
                atividadesDTO.add(atDTO);
            }
            dto.setAtividades(atividadesDTO);
        }

        RelatorioGeralDTO geral = new RelatorioGeralDTO();
        geral.setIdGestao(idGestao);
        geral.setNomeGestao(gestao.getNomeGestao());
        geral.setMes(mes);
        geral.setAno(ano);
        geral.setConselheiros(new ArrayList<>(mapa.values()));

        int totalJetons = geral.getConselheiros().stream().mapToInt(ConselheiroRelatorioDTO::getTotalJetons).sum();
        BigDecimal totalValor = geral.getConselheiros().stream()
                .map(ConselheiroRelatorioDTO::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        geral.setTotalGeralJetons(totalJetons);
        geral.setTotalGeralValor(totalValor);

        return geral;
    }

    public BigDecimal sumValorRecebidoPorConselheiro(Integer idPessoa) {
        BigDecimal valor = jetonRepository.sumValorRecebidoPorConselheiro(idPessoa);
        return valor != null ? valor : BigDecimal.ZERO;
    }

    public List<JetonAgrupadoDTO> listarJetonsAgrupadosPorGestaoEMes(Integer idGestao, Integer mes, Integer ano) {
        List<Jeton> listaBruta = jetonRepository.findByGestaoIdGestaoAndMesAndAno(idGestao, mes, ano);
        Map<Integer, JetonAgrupadoDTO> agrupado = new LinkedHashMap<>();
        for (Jeton j : listaBruta) {
            Integer idPessoa = j.getConselheiro().getIdPessoa();
            JetonAgrupadoDTO dto = agrupado.get(idPessoa);
            if (dto == null) {
                dto = new JetonAgrupadoDTO(j.getConselheiro(), j.getGestao(), j.getMes(), j.getAno());
                agrupado.put(idPessoa, dto);
            }
            dto.adicionarJeton(j);
        }
        return new ArrayList<>(agrupado.values());
    }

    public Map<String, Object> gerarRelatorioIndividualConselheiro(Integer idPessoa, Integer idGestao, Integer mes,
            Integer ano) {
        Conselheiro conselheiro = conselheiroService.buscarPorId(idPessoa)
                .orElseThrow(() -> new RuntimeException("Conselheiro não encontrado"));

        List<Map<String, Object>> atividades = listarAtividadesAgrupadasPorConselheiro(idPessoa, idGestao, mes, ano);

        List<Jeton> jetons = jetonRepository.findByGestaoIdGestaoAndMesAndAno(idGestao, mes, ano).stream()
                .filter(j -> j.getConselheiro().getIdPessoa().equals(idPessoa))
                .toList();

        int saldoAnterior = 0;
        int pontosAcumuladosMes = 0;
        for (Jeton j : jetons) {
            List<PontosSaldo> pontosList = pontosSaldoRepository.findByJetonIdJeton(j.getIdJeton());
            for (PontosSaldo ps : pontosList) {
                boolean doMesAtual = false;
                if (ps.getAtividade() != null) {
                    LocalDate dataAtv = ps.getAtividade().getDataHoraAtividade().toLocalDate();
                    if (dataAtv.getYear() == ano && dataAtv.getMonthValue() == mes) {
                        doMesAtual = true;
                    }
                }
                if (doMesAtual) {
                    pontosAcumuladosMes += ps.getPontosUtilizados();
                } else {
                    saldoAnterior += ps.getPontosUtilizados();
                }
            }
        }
        Integer saldoFuturo = pontosSaldoRepository.somarPontosSobrandoAtivos(idPessoa, idGestao);

        Map<String, Object> resposta = new HashMap<>();
        resposta.put("nomeConselheiro", conselheiro.getPessoa().getNome());
        resposta.put("atividades", atividades);
        resposta.put("saldoAnterior", saldoAnterior);
        resposta.put("pontosAcumuladosMes", pontosAcumuladosMes);
        resposta.put("saldoFuturo", saldoFuturo != null ? saldoFuturo : 0);
        return resposta;
    }

    private static class ProcessamentoResultado {
        int totalJetons;
        BigDecimal valorTotal;

        ProcessamentoResultado(int totalJetons, BigDecimal valorTotal) {
            this.totalJetons = totalJetons;
            this.valorTotal = valorTotal;
        }
    }

    private static class ResultadoAbsorcao {
        Map<Resolucao, Integer> demonstrativo = new LinkedHashMap<>();
        int totalJetons = 0;
        int totalPontosConsumidos = 0;
    }
}