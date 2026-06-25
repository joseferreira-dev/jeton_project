package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.domain.*;
import br.com.cremepe.jeton.dto.*;
import br.com.cremepe.jeton.mapper.AtividadeMapper;
import br.com.cremepe.jeton.mapper.JetonMapper;
import br.com.cremepe.jeton.repository.*;
import br.com.cremepe.jeton.service.JetonCalculator.ResultadoAbsorcao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do serviço de Jeton (Financeiro)")
class JetonServiceTest {

    @Mock
    private JetonRepository jetonRepositoryMock;

    @Mock
    private PontosSaldoRepository pontosSaldoRepositoryMock;

    @Mock
    private AtividadeConselhalRepository atividadeRepositoryMock;

    @Mock
    private ResolucaoRepository resolucaoRepositoryMock;

    @Mock
    private GestaoConselheiroRepository gestaoConselheiroRepositoryMock;

    @Mock
    private RegrasService regrasServiceMock;

    @Mock
    private ConselheiroRepository conselheiroRepositoryMock;

    @Mock
    private GestaoRepository gestaoRepositoryMock;

    @Mock
    private ConselheiroService conselheiroServiceMock;

    @Mock
    private JetonCalculator jetonCalculatorMock;

    @Mock
    private AtividadeMapper atividadeMapperMock;

    @Mock
    private JetonMapper jetonMapperMock;

    @Mock
    private LogJetonService logJetonServiceMock;

    @InjectMocks
    private JetonService service;

    // ========== HELPERS ==========

    private Gestao criarGestao(Integer id, String nome, LocalDate inicio, LocalDate fim) {
        Gestao g = new Gestao();
        g.setIdGestao(id);
        g.setNomeGestao(nome);
        g.setDtInicio(inicio);
        g.setDtFim(fim);
        return g;
    }

    private Conselheiro criarConselheiro(Integer id, String nome) {
        Pessoa p = new Pessoa();
        p.setIdPessoa(id);
        p.setNome(nome);
        Conselheiro c = new Conselheiro();
        c.setIdPessoa(id);
        c.setPessoa(p);
        c.setInSituacao(Conselheiro.SITUACAO_ATIVO);
        return c;
    }

    private Resolucao criarResolucao(Integer id, Integer pontosPorJeton, BigDecimal valorJeton) {
        Resolucao r = new Resolucao();
        r.setIdResolucao(id);
        r.setNumero(1);
        r.setAno(2026);
        r.setPontosPorJeton(pontosPorJeton != null ? pontosPorJeton : 3);
        r.setValorJeton(valorJeton != null ? valorJeton : BigDecimal.valueOf(100.00));
        r.setMaxJetonsMes(22);
        r.setMaxJetonsDia(3);
        r.setMaxJetonsPeriodo(1);
        return r;
    }

    private Jeton criarJeton(Integer id, Gestao gestao, Conselheiro conselheiro, Integer mes, Integer ano,
            Integer totalJeton, BigDecimal valor, String situacao) {
        Jeton j = new Jeton();
        j.setIdJeton(id);
        j.setGestao(gestao);
        j.setConselheiro(conselheiro);
        j.setMes(mes);
        j.setAno(ano);
        j.setTotalJeton(totalJeton);
        j.setValor(valor);
        j.setInSituacao(situacao);
        j.setAtividades(new ArrayList<>());
        return j;
    }

    private PontosSaldo criarSaldo(Integer id, Integer pontosSobrando, Resolucao resolucao,
            AtividadeConselhal atividade,
            Conselheiro conselheiro, Gestao gestao) {
        PontosSaldo ps = new PontosSaldo();
        ps.setIdPontosSaldo(id);
        ps.setPontosSobrando(pontosSobrando);
        ps.setResolucao(resolucao);
        ps.setAtividade(atividade);
        ps.setConselheiro(conselheiro);
        ps.setGestao(gestao);
        ps.setDataHora(LocalDateTime.now());
        ps.setInSituacao(PontosSaldo.SITUACAO_ATIVO);
        return ps;
    }

    private AtividadeConselhal criarAtividade(Integer id, Conselheiro conselheiro, Regras regra, Gestao gestao,
            LocalDateTime dataHora, String situacao, String computada) {
        AtividadeConselhal a = new AtividadeConselhal();
        a.setIdAtividade(id);
        a.setConselheiro(conselheiro);
        a.setRegra(regra);
        a.setGestao(gestao);
        a.setDataHoraAtividade(dataHora);
        a.setDataHoraRegistro(dataHora);
        a.setQtdAtividade(1);
        a.setInSituacao(situacao);
        a.setInComputada(computada);
        a.setInTurno(AtividadeConselhal.TURNO_MANHA);
        return a;
    }

    // ========== NOVOS TESTES DE PRÉ-PROCESSAMENTO ==========

    @Test
    @DisplayName("deve lançar exceção quando há meses anteriores não fechados")
    void deveLancarExcecaoMesesAnterioresNaoFechados() {
        // Dado
        Integer idGestao = 1;
        Integer mes = 6;
        Integer ano = 2026;
        Gestao gestao = criarGestao(idGestao, "Gestão Teste", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        when(atividadeRepositoryMock.countAtividadesFechadasNoPeriodo(idGestao, mes, ano)).thenReturn(0L);
        when(jetonRepositoryMock.findByGestaoIdGestaoAndMesAndAno(idGestao, mes, ano)).thenReturn(List.of());
        when(atividadeRepositoryMock.countAtividadesPendentesNoMes(idGestao, mes, ano)).thenReturn(0L);
        when(atividadeRepositoryMock.countAtividadesAnterioresNaoFechadas(eq(idGestao), any(LocalDateTime.class)))
                .thenReturn(5L); // meses anteriores não fechados

        // Quando / Então
        assertThatThrownBy(() -> service.processarFechamentoMensal(gestao, mes, ano))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("meses anteriores ainda não homologadas");

        verify(logJetonServiceMock, never()).logFolhaProcessada(any(), anyInt(), anyInt(), anyInt(), anyInt(), any());
    }

    @Test
    @DisplayName("deve limpar processamentos anteriores com jetons existentes")
    void deveLimparProcessamentosAnterioresComJetonsExistentes() throws Exception {
        // Dado
        Integer idGestao = 1;
        Integer mes = 6;
        Integer ano = 2026;
        Gestao gestao = criarGestao(idGestao, "Gestão Teste", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        Conselheiro conselheiro = criarConselheiro(100, "Dr. A");

        // Mocks para validações iniciais
        lenient().when(atividadeRepositoryMock.countAtividadesFechadasNoPeriodo(idGestao, mes, ano)).thenReturn(0L);
        lenient().when(atividadeRepositoryMock.countAtividadesPendentesNoMes(idGestao, mes, ano)).thenReturn(0L);
        lenient()
                .when(atividadeRepositoryMock.countAtividadesAnterioresNaoFechadas(eq(idGestao),
                        any(LocalDateTime.class)))
                .thenReturn(0L);

        // Resolução vigente
        Resolucao resolucao = criarResolucao(10, 3, BigDecimal.valueOf(100));
        LocalDate ultimoDiaMes = LocalDate.of(ano, mes, 1).withDayOfMonth(LocalDate.of(ano, mes, 1).lengthOfMonth());
        lenient().when(resolucaoRepositoryMock.findResolucoesVigentesNaData(ultimoDiaMes))
                .thenReturn(List.of(resolucao));

        // Vínculo do conselheiro
        GestaoConselheiro vinculo = mock(GestaoConselheiro.class);
        lenient().when(vinculo.getConselheiro()).thenReturn(conselheiro);
        lenient().when(gestaoConselheiroRepositoryMock.findByGestaoIdGestao(idGestao))
                .thenReturn(List.of(vinculo));

        // Jetons existentes no mês (para serem limpos)
        Jeton jetonExistente = criarJeton(1, gestao, conselheiro, mes, ano, 3, BigDecimal.valueOf(300),
                Jeton.SITUACAO_ATIVO);
        lenient().when(jetonRepositoryMock.findByGestaoIdGestaoAndMesAndAno(idGestao, mes, ano))
                .thenReturn(List.of(jetonExistente));

        // Atividades computadas no mês (para serem limpas)
        Regras regra = mock(Regras.class);
        lenient().when(regra.getPontos()).thenReturn(3);
        AtividadeConselhal atvComputada = criarAtividade(10, conselheiro, regra, gestao, LocalDateTime.now(),
                AtividadeConselhal.SITUACAO_VALIDADA, AtividadeConselhal.COMPUTADA_SIM);
        lenient().when(atividadeRepositoryMock.findComputadasDoMes(idGestao, mes, ano))
                .thenReturn(List.of(atvComputada));

        // Mocks para estorno do conselheiro (delete de jetons, restauração de saldos,
        // etc.)
        lenient().when(conselheiroRepositoryMock.findById(100)).thenReturn(Optional.of(conselheiro));
        lenient().when(pontosSaldoRepositoryMock.findByJetonIdJeton(1)).thenReturn(List.of());
        lenient().when(pontosSaldoRepositoryMock.findSaldosDeAtividadesByMes(100, mes, ano)).thenReturn(List.of());
        lenient().doNothing().when(atividadeRepositoryMock).reverterAtividadesComputadas(100, idGestao, mes, ano);

        // Mock para processar o conselheiro (após limpeza, ele não tem mais nada)
        lenient().when(pontosSaldoRepositoryMock.findSaldosDisponiveisOrderedFIFO(100, idGestao)).thenReturn(List.of());
        lenient().when(atividadeRepositoryMock.findHomologadasParaCalculo(100, mes, ano)).thenReturn(List.of());

        // Mock para save de jetons (caso haja)
        lenient().when(jetonRepositoryMock.save(any(Jeton.class))).thenAnswer(inv -> inv.getArgument(0));

        // Quando
        service.processarFechamentoMensal(gestao, mes, ano);

        // Então - verifica que o jeton foi deletado (via estorno)
        verify(jetonRepositoryMock, atLeastOnce()).delete(jetonExistente);
        verify(atividadeRepositoryMock).reverterAtividadesComputadas(100, idGestao, mes, ano);
    }

    @Test
    @DisplayName("deve aplicar limites de turno corretamente")
    void deveAplicarLimitesTurno() throws Exception {
        // Este teste testa a lógica de aplicarLimitesTurno via processarConselheiro.
        // Precisamos criar saldos com atividades que tenham turno e verificar redução.

        Integer idGestao = 1;
        Integer mes = 6;
        Integer ano = 2026;
        Gestao gestao = criarGestao(idGestao, "Gestão Teste", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        Conselheiro conselheiro = criarConselheiro(100, "Dr. A");
        Resolucao resolucao = criarResolucao(10, 3, BigDecimal.valueOf(100)); // 3 pts por jeton

        // Mocks de validação
        when(atividadeRepositoryMock.countAtividadesFechadasNoPeriodo(idGestao, mes, ano)).thenReturn(0L);
        when(atividadeRepositoryMock.countAtividadesPendentesNoMes(idGestao, mes, ano)).thenReturn(0L);
        when(atividadeRepositoryMock.countAtividadesAnterioresNaoFechadas(eq(idGestao), any(LocalDateTime.class)))
                .thenReturn(0L);

        LocalDate ultimoDiaMes = LocalDate.of(ano, mes, 1).withDayOfMonth(LocalDate.of(ano, mes, 1).lengthOfMonth());
        when(resolucaoRepositoryMock.findResolucoesVigentesNaData(ultimoDiaMes))
                .thenReturn(List.of(resolucao));

        GestaoConselheiro vinculo = mock(GestaoConselheiro.class);
        when(vinculo.getConselheiro()).thenReturn(conselheiro);
        when(gestaoConselheiroRepositoryMock.findByGestaoIdGestao(idGestao))
                .thenReturn(List.of(vinculo));

        // Sem jetons anteriores
        when(jetonRepositoryMock.findByGestaoIdGestaoAndMesAndAno(idGestao, mes, ano)).thenReturn(List.of());

        // Saldos antigos (não tem)
        when(pontosSaldoRepositoryMock.findSaldosDisponiveisOrderedFIFO(100, idGestao)).thenReturn(List.of());

        // Criar uma atividade que será convertida em saldo
        Regras regra = mock(Regras.class);
        when(regra.getPontos()).thenReturn(3);
        AtividadeConselhal atividade = criarAtividade(1, conselheiro, regra, gestao,
                LocalDateTime.of(2026, 6, 10, 8, 0), // manhã
                AtividadeConselhal.SITUACAO_VALIDADA, AtividadeConselhal.COMPUTADA_NAO);
        atividade.setInTurno(AtividadeConselhal.TURNO_MANHA);

        when(atividadeRepositoryMock.findHomologadasParaCalculo(100, mes, ano))
                .thenReturn(List.of(atividade));

        // Mock para save de PontosSaldo (durante conversão)
        when(pontosSaldoRepositoryMock.save(any(PontosSaldo.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Mock para sumPontosUtilizadosByConselheiroAndDataAndTurno (retorna 0)
        when(pontosSaldoRepositoryMock.sumPontosUtilizadosByConselheiroAndDataAndTurno(
                eq(100), any(LocalDate.class), eq("M")))
                .thenReturn(0);

        // Mock para buscarResolucaoPorData (usado para obter o limite)
        when(regrasServiceMock.buscarResolucaoPorData(any(LocalDate.class)))
                .thenReturn(Optional.of(resolucao));

        // Simular cálculo de jetons (absorção)
        Map<Resolucao, Integer> demo = new LinkedHashMap<>();
        demo.put(resolucao, 1);
        ResultadoAbsorcao resultadoAbsorcao = new ResultadoAbsorcao(demo, 1, 3);
        when(jetonCalculatorMock.calcularJetons(anyList(), anyInt()))
                .thenReturn(resultadoAbsorcao);

        // Mock para salvar Jeton
        when(jetonRepositoryMock.save(any(Jeton.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Quando
        service.processarFechamentoMensal(gestao, mes, ano);

        // Então - verificar que o método
        // sumPontosUtilizadosByConselheiroAndDataAndTurno foi chamado
        verify(pontosSaldoRepositoryMock, atLeastOnce())
                .sumPontosUtilizadosByConselheiroAndDataAndTurno(eq(100), any(LocalDate.class), eq("M"));
    }

    // ========== TESTES DE PROCESSAMENTO MENSAL ==========

    @Test
    @DisplayName("deve processar fechamento mensal com sucesso")
    void deveProcessarFechamentoMensalComSucesso() {
        // Dado
        Integer idGestao = 1;
        Integer mes = 6;
        Integer ano = 2026;
        Gestao gestao = criarGestao(idGestao, "Gestão Teste", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        // ✅ Garantir que não há homologação prévia
        when(atividadeRepositoryMock.countAtividadesFechadasNoPeriodo(idGestao, mes, ano)).thenReturn(0L);
        when(jetonRepositoryMock.findByGestaoIdGestaoAndMesAndAno(idGestao, mes, ano)).thenReturn(List.of());

        // Validações
        when(atividadeRepositoryMock.countAtividadesPendentesNoMes(idGestao, mes, ano)).thenReturn(0L);
        when(atividadeRepositoryMock.countAtividadesAnterioresNaoFechadas(eq(idGestao), any(LocalDateTime.class)))
                .thenReturn(0L);

        Resolucao resolucao = criarResolucao(10, 3, BigDecimal.valueOf(100));
        LocalDate ultimoDiaMes = LocalDate.of(ano, mes, 1).withDayOfMonth(LocalDate.of(ano, mes, 1).lengthOfMonth());
        when(resolucaoRepositoryMock.findResolucoesVigentesNaData(ultimoDiaMes))
                .thenReturn(List.of(resolucao));

        // Conselheiros
        Conselheiro cons1 = criarConselheiro(100, "Dr. A");
        Conselheiro cons2 = criarConselheiro(101, "Dr. B");
        GestaoConselheiro vinculo1 = mock(GestaoConselheiro.class);
        when(vinculo1.getConselheiro()).thenReturn(cons1);
        GestaoConselheiro vinculo2 = mock(GestaoConselheiro.class);
        when(vinculo2.getConselheiro()).thenReturn(cons2);
        when(gestaoConselheiroRepositoryMock.findByGestaoIdGestao(idGestao))
                .thenReturn(List.of(vinculo1, vinculo2));

        // ✅ Adicionar atividades para processar para cada conselheiro
        Regras regra = mock(Regras.class);
        when(regra.getPontos()).thenReturn(3);
        AtividadeConselhal atv1 = criarAtividade(10, cons1, regra, gestao, LocalDateTime.now(),
                AtividadeConselhal.SITUACAO_VALIDADA, AtividadeConselhal.COMPUTADA_NAO);
        AtividadeConselhal atv2 = criarAtividade(11, cons2, regra, gestao, LocalDateTime.now(),
                AtividadeConselhal.SITUACAO_VALIDADA, AtividadeConselhal.COMPUTADA_NAO);

        when(atividadeRepositoryMock.findHomologadasParaCalculo(eq(100), eq(mes), eq(ano)))
                .thenReturn(List.of(atv1));
        when(atividadeRepositoryMock.findHomologadasParaCalculo(eq(101), eq(mes), eq(ano)))
                .thenReturn(List.of(atv2));

        // Saldos existentes (vazios)
        when(pontosSaldoRepositoryMock.findSaldosDisponiveisOrderedFIFO(eq(100), eq(idGestao)))
                .thenReturn(List.of());
        when(pontosSaldoRepositoryMock.findSaldosDisponiveisOrderedFIFO(eq(101), eq(idGestao)))
                .thenReturn(List.of());

        // ✅ Mock do save de PontosSaldo (retorna o próprio objeto)
        when(pontosSaldoRepositoryMock.save(any(PontosSaldo.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Simula resultado do calculador para cada conselheiro
        Map<Resolucao, Integer> demo = new LinkedHashMap<>();
        demo.put(resolucao, 5);
        ResultadoAbsorcao resultadoAbsorcao = new ResultadoAbsorcao(demo, 5, 15);
        when(jetonCalculatorMock.calcularJetons(anyList(), anyInt()))
                .thenReturn(resultadoAbsorcao);

        // Mock para criar jetons
        when(jetonRepositoryMock.save(any(Jeton.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Mock para marcar atividades como computadas
        doNothing().when(atividadeRepositoryMock).marcarComoComputadaEmLote(anyList());

        // Quando
        service.processarFechamentoMensal(gestao, mes, ano);

        // Então
        verify(atividadeRepositoryMock).countAtividadesPendentesNoMes(idGestao, mes, ano);
        verify(atividadeRepositoryMock).countAtividadesAnterioresNaoFechadas(eq(idGestao), any(LocalDateTime.class));
        verify(resolucaoRepositoryMock).findResolucoesVigentesNaData(ultimoDiaMes);
        verify(gestaoConselheiroRepositoryMock).findByGestaoIdGestao(idGestao);
        // Verifica que o calculador foi chamado (pode ser duas vezes)
        verify(jetonCalculatorMock, atLeastOnce()).calcularJetons(anyList(), anyInt());
        // Verifica log com os valores corretos
        verify(logJetonServiceMock).logFolhaProcessada(eq(gestao), eq(mes), eq(ano), eq(2), eq(10),
                any(BigDecimal.class));
    }

    @Test
    @DisplayName("deve lançar exceção ao processar mês já homologado")
    void deveLancarExcecaoProcessarMesHomologado() {
        // Dado
        Integer idGestao = 1;
        Integer mes = 6;
        Integer ano = 2026;
        Gestao gestao = criarGestao(idGestao, "Gestão Teste", LocalDate.now().minusMonths(1),
                LocalDate.now().plusMonths(1));

        // Simula que há atividades fechadas (homologadas)
        when(atividadeRepositoryMock.countAtividadesFechadasNoPeriodo(idGestao, mes, ano))
                .thenReturn(5L);
        when(jetonRepositoryMock.findByGestaoIdGestaoAndMesAndAno(idGestao, mes, ano))
                .thenReturn(List.of());

        // Quando / Então
        assertThatThrownBy(() -> service.processarFechamentoMensal(gestao, mes, ano))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("A folha do período 6/2026 já foi homologada e não pode ser recalculada.");

        verify(logJetonServiceMock, never()).logFolhaProcessada(any(), anyInt(), anyInt(), anyInt(), anyInt(), any());
    }

    @Test
    @DisplayName("deve lançar exceção quando há atividades pendentes no período")
    void deveLancarExcecaoQuandoHaAtividadesPendentes() {
        // Dado
        Integer idGestao = 1;
        Integer mes = 6;
        Integer ano = 2026;
        Gestao gestao = criarGestao(idGestao, "Gestão Teste", LocalDate.now().minusMonths(1),
                LocalDate.now().plusMonths(1));

        when(atividadeRepositoryMock.countAtividadesPendentesNoMes(idGestao, mes, ano))
                .thenReturn(3L);

        // Quando / Então
        assertThatThrownBy(() -> service.processarFechamentoMensal(gestao, mes, ano))
                .isInstanceOf(RuntimeException.class)
                .hasMessage(
                        "Cálculo bloqueado: existem 3 atividade(s) pendentes no período. Valide-as ou exclua-as antes de processar.");
    }

    // ========== TESTES DE FECHAMENTO DEFINITIVO ==========

    @Test
    @DisplayName("deve realizar fechamento definitivo com sucesso")
    void deveRealizarFechamentoDefinitivoComSucesso() {
        // Dado
        Integer idGestao = 1;
        Integer mes = 6;
        Integer ano = 2026;
        Gestao gestao = criarGestao(idGestao, "Gestão Teste", LocalDate.now().minusMonths(1),
                LocalDate.now().plusMonths(1));

        when(atividadeRepositoryMock.fecharAtividadesEmFolha(idGestao, mes, ano))
                .thenReturn(10);

        Jeton jeton1 = criarJeton(1, gestao, criarConselheiro(100, "Dr. A"), mes, ano, 5, BigDecimal.valueOf(500),
                Jeton.SITUACAO_ATIVO);
        Jeton jeton2 = criarJeton(2, gestao, criarConselheiro(101, "Dr. B"), mes, ano, 3, BigDecimal.valueOf(300),
                Jeton.SITUACAO_ATIVO);
        when(jetonRepositoryMock.findByGestaoIdGestaoAndMesAndAno(idGestao, mes, ano))
                .thenReturn(List.of(jeton1, jeton2));

        when(jetonRepositoryMock.save(any(Jeton.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Quando
        service.realizarFechamentoDefinitivoFolha(gestao, mes, ano);

        // Então
        verify(atividadeRepositoryMock).fecharAtividadesEmFolha(idGestao, mes, ano);
        verify(jetonRepositoryMock, times(2)).save(any(Jeton.class));
        assertThat(jeton1.getInSituacao()).isEqualTo(Jeton.SITUACAO_EXCLUIDO);
        assertThat(jeton2.getInSituacao()).isEqualTo(Jeton.SITUACAO_EXCLUIDO);
        verify(logJetonServiceMock).logFolhaHomologada(eq(gestao), eq(mes), eq(ano), eq(10), eq(2));
    }

    @Test
    @DisplayName("deve lançar exceção ao fechar sem atividades computadas")
    void deveLancarExcecaoFecharSemAtividadesComputadas() {
        // Dado
        Integer idGestao = 1;
        Integer mes = 6;
        Integer ano = 2026;
        Gestao gestao = criarGestao(idGestao, "Gestão Teste", LocalDate.now().minusMonths(1),
                LocalDate.now().plusMonths(1));

        when(atividadeRepositoryMock.fecharAtividadesEmFolha(idGestao, mes, ano))
                .thenReturn(0);

        // Quando / Então
        assertThatThrownBy(() -> service.realizarFechamentoDefinitivoFolha(gestao, mes, ano))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Não foram encontradas atividades validadas e computadas para fechar.");

        verify(jetonRepositoryMock, never()).save(any());
    }

    // ========== TESTES DE ESTORNO ==========

    @Test
    @DisplayName("deve estornar jeton pontual com sucesso")
    void deveEstornarJetonPontualComSucesso() {
        // Dado
        Integer idJeton = 50;
        Gestao gestao = criarGestao(1, "Gestão", LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1));
        Conselheiro conselheiro = criarConselheiro(100, "Dr. Teste");
        Jeton jeton = criarJeton(idJeton, gestao, conselheiro, 6, 2026, 5, BigDecimal.valueOf(500),
                Jeton.SITUACAO_ATIVO);

        PontosSaldo saldo1 = criarSaldo(1, 10, criarResolucao(10, 3, BigDecimal.valueOf(100)), null, conselheiro,
                gestao);
        saldo1.setJeton(jeton);
        saldo1.setPontosUtilizados(3);
        saldo1.setPontosSobrando(7);
        saldo1.setInSituacao(PontosSaldo.SITUACAO_INATIVO);

        when(jetonRepositoryMock.findById(idJeton)).thenReturn(Optional.of(jeton));
        when(pontosSaldoRepositoryMock.findByJetonIdJeton(idJeton)).thenReturn(List.of(saldo1));

        // Quando
        service.estornarJetonPontual(idJeton);

        // Então
        // Verifica que o saldo foi restaurado
        assertThat(saldo1.getPontosSobrando()).isEqualTo(10); // 7 + 3
        assertThat(saldo1.getPontosUtilizados()).isZero();
        assertThat(saldo1.getInSituacao()).isEqualTo(PontosSaldo.SITUACAO_ATIVO);
        assertThat(saldo1.getJeton()).isNull();
        verify(pontosSaldoRepositoryMock).save(saldo1);

        // Verifica reversão das atividades computadas
        verify(atividadeRepositoryMock).reverterAtividadesComputadas(
                jeton.getConselheiro().getIdPessoa(),
                jeton.getGestao().getIdGestao(),
                jeton.getMes(),
                jeton.getAno());

        // Verifica exclusão do jeton
        verify(jetonRepositoryMock).delete(jeton);
        verify(logJetonServiceMock).logJetonEstornado(eq(idJeton), anyString(), anyString(), eq(6), eq(2026));
    }

    @Test
    @DisplayName("deve lançar exceção ao estornar jeton já pago/homologado")
    void deveLancarExcecaoEstornarJetonPago() {
        // Dado
        Integer idJeton = 50;
        Jeton jeton = criarJeton(idJeton, null, null, 6, 2026, 5, BigDecimal.valueOf(500), Jeton.SITUACAO_PAGO);
        when(jetonRepositoryMock.findById(idJeton)).thenReturn(Optional.of(jeton));

        // Quando / Então
        assertThatThrownBy(() -> service.estornarJetonPontual(idJeton))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Estorno negado: este pagamento já foi homologado em folha definitiva.");

        verify(jetonRepositoryMock, never()).delete(any());
    }

    // ========== TESTES DE RELATÓRIO GERAL ==========

    @Test
    @DisplayName("deve gerar relatório geral com sucesso")
    void deveGerarRelatorioGeralComSucesso() {
        // Dado
        Integer idGestao = 1;
        Integer mes = 6;
        Integer ano = 2026;
        Gestao gestao = criarGestao(idGestao, "Gestão Teste", LocalDate.now().minusMonths(1),
                LocalDate.now().plusMonths(1));
        Conselheiro cons1 = criarConselheiro(100, "Dr. A");
        Conselheiro cons2 = criarConselheiro(101, "Dr. B");

        Jeton j1 = criarJeton(1, gestao, cons1, mes, ano, 5, BigDecimal.valueOf(500), Jeton.SITUACAO_EXCLUIDO);
        Jeton j2 = criarJeton(2, gestao, cons2, mes, ano, 3, BigDecimal.valueOf(300), Jeton.SITUACAO_EXCLUIDO);

        when(gestaoRepositoryMock.findById(idGestao)).thenReturn(Optional.of(gestao));
        when(jetonRepositoryMock.findByGestaoIdGestaoAndMesAndAno(idGestao, mes, ano))
                .thenReturn(List.of(j1, j2));

        // Mocks para saldos por conselheiro
        when(pontosSaldoRepositoryMock.findByJetonIdJeton(1)).thenReturn(List.of());
        when(pontosSaldoRepositoryMock.findByJetonIdJeton(2)).thenReturn(List.of());

        when(pontosSaldoRepositoryMock.sumPontosSobrandoAtivos(100, idGestao)).thenReturn(10);
        when(pontosSaldoRepositoryMock.sumPontosSobrandoAtivos(101, idGestao)).thenReturn(15);

        when(atividadeRepositoryMock.findComputadasPorConselheiroEMes(eq(100), eq(mes), eq(ano)))
                .thenReturn(List.of());
        when(atividadeRepositoryMock.findComputadasPorConselheiroEMes(eq(101), eq(mes), eq(ano)))
                .thenReturn(List.of());

        // Quando
        RelatorioGeralDTO resultado = service.gerarRelatorioGeral(idGestao, mes, ano);

        // Então
        assertThat(resultado).isNotNull();
        assertThat(resultado.getIdGestao()).isEqualTo(idGestao);
        assertThat(resultado.getNomeGestao()).isEqualTo("Gestão Teste");
        assertThat(resultado.getMes()).isEqualTo(mes);
        assertThat(resultado.getAno()).isEqualTo(ano);
        assertThat(resultado.getConselheiros()).hasSize(2);
        assertThat(resultado.getTotalGeralJetons()).isEqualTo(8);
        assertThat(resultado.getTotalGeralValor()).isEqualTo(BigDecimal.valueOf(800));
    }

    // ========== TESTES DE SALDO POR CONSELHEIRO ==========

    @Test
    @DisplayName("deve obter saldo por conselheiro com sucesso")
    void deveObterSaldoPorConselheiroComSucesso() {
        // Dado
        Integer idPessoa = 100;

        PontosRemanescentesDTO dtoMock = new PontosRemanescentesDTO(idPessoa, "Dr. Teste", 20L,
                BigDecimal.valueOf(500));
        when(pontosSaldoRepositoryMock.findSaldoByConselheiro(idPessoa))
                .thenReturn(Optional.of(dtoMock));

        when(atividadeRepositoryMock.sumPontosAtividadesValidadasNaoComputadas(idPessoa))
                .thenReturn(15);

        // Quando
        PontosRemanescentesDTO resultado = service.obterSaldoPorConselheiro(idPessoa);

        // Então
        assertThat(resultado).isNotNull();
        assertThat(resultado.getIdPessoa()).isEqualTo(idPessoa);
        assertThat(resultado.getPontosRemanescentes()).isEqualTo(20L);
        assertThat(resultado.getPontosAtividadesValidadas()).isEqualTo(15L);
        assertThat(resultado.getSaldoPontos()).isEqualTo(35L);
        assertThat(resultado.getSomaJetons()).isEqualTo(BigDecimal.valueOf(500));
        assertThat(resultado.getPontosRemanescentesFormatado()).isNotNull();
        assertThat(resultado.getSaldoPontosFormatado()).isNotNull();
    }

    @Test
    @DisplayName("deve lançar exceção ao obter saldo de conselheiro inexistente")
    void deveLancarExcecaoObterSaldoConselheiroInexistente() {
        // Dado
        Integer idPessoa = 999;
        when(pontosSaldoRepositoryMock.findSaldoByConselheiro(idPessoa))
                .thenReturn(Optional.empty());

        // Quando / Então
        assertThatThrownBy(() -> service.obterSaldoPorConselheiro(idPessoa))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Conselheiro não encontrado ou sem jetons");
    }

    // ========== TESTES DE LISTAGEM DE SALDOS AGRUPADOS ==========

    @Test
    @DisplayName("deve listar saldos agrupados com sucesso")
    void deveListarSaldosAgrupados() {
        // Dado
        PontosRemanescentesDTO dto1 = new PontosRemanescentesDTO(1, "Dr. A", 10L, BigDecimal.valueOf(200));
        PontosRemanescentesDTO dto2 = new PontosRemanescentesDTO(2, "Dr. B", 5L, BigDecimal.valueOf(100));

        when(pontosSaldoRepositoryMock.findSaldosAgrupadosByConselheiroWithJeton())
                .thenReturn(List.of(dto1, dto2));

        when(atividadeRepositoryMock.sumPontosAtividadesValidadasNaoComputadas(1))
                .thenReturn(3);
        when(atividadeRepositoryMock.sumPontosAtividadesValidadasNaoComputadas(2))
                .thenReturn(0);

        // Quando
        List<PontosRemanescentesDTO> resultado = service.listarSaldosAgrupados();

        // Então
        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getPontosAtividadesValidadas()).isEqualTo(3L);
        assertThat(resultado.get(0).getSaldoPontos()).isEqualTo(13L); // 10 + 3
        assertThat(resultado.get(1).getSaldoPontos()).isEqualTo(5L); // 5 + 0
    }

    // ========== TESTES DE HISTÓRICO PAGINADO ==========

    @Test
    @DisplayName("deve pesquisar histórico paginado com sucesso")
    void devePesquisarHistoricoPaginado() {
        // Dado
        Integer idGestao = 1;
        Integer mes = 6;
        Integer ano = 2026;
        String termo = "Dr.";
        Pageable pageable = Pageable.unpaged();

        Jeton jeton = criarJeton(1, null, null, mes, ano, 5, BigDecimal.valueOf(500), Jeton.SITUACAO_EXCLUIDO);
        Page<Jeton> pagina = new PageImpl<>(List.of(jeton));

        when(jetonRepositoryMock.findAllByFiltersPageable(eq(idGestao), eq(mes), eq(ano), eq(termo), eq(pageable)))
                .thenReturn(pagina);

        JetonDTO dto = new JetonDTO(1, 100, "Dr. Teste", 1, "Gestão", mes, ano, 5, BigDecimal.valueOf(500), "E");
        when(jetonMapperMock.toDto(any(Jeton.class))).thenReturn(dto);

        // Quando
        Page<JetonDTO> resultado = service.pesquisarHistoricoPaginado(idGestao, mes, ano, termo, pageable);

        // Então
        assertThat(resultado).isNotNull();
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).id()).isEqualTo(1);
        verify(jetonRepositoryMock).findAllByFiltersPageable(eq(idGestao), eq(mes), eq(ano), eq(termo), eq(pageable));
    }

    // ========== TESTES DE LISTAGEM DE JETONS AGRUPADOS ==========

    @Test
    @DisplayName("deve listar jetons agrupados por gestão e mês")
    void deveListarJetonsAgrupadosPorGestaoEMes() {
        // Dado
        Integer idGestao = 1;
        Integer mes = 6;
        Integer ano = 2026;
        Gestao gestao = criarGestao(idGestao, "Gestão Teste", LocalDate.now().minusMonths(1),
                LocalDate.now().plusMonths(1));
        Conselheiro cons1 = criarConselheiro(100, "Dr. A");
        Conselheiro cons2 = criarConselheiro(101, "Dr. B");

        Jeton j1 = criarJeton(1, gestao, cons1, mes, ano, 5, BigDecimal.valueOf(500), Jeton.SITUACAO_EXCLUIDO);
        Jeton j2 = criarJeton(2, gestao, cons1, mes, ano, 3, BigDecimal.valueOf(300), Jeton.SITUACAO_EXCLUIDO);
        Jeton j3 = criarJeton(3, gestao, cons2, mes, ano, 2, BigDecimal.valueOf(200), Jeton.SITUACAO_EXCLUIDO);

        when(jetonRepositoryMock.findByGestaoIdGestaoAndMesAndAno(idGestao, mes, ano))
                .thenReturn(List.of(j1, j2, j3));

        // Quando
        List<JetonAgrupadoDTO> resultado = service.listarJetonsAgrupadosPorGestaoEMes(idGestao, mes, ano);

        // Então
        assertThat(resultado).hasSize(2); // dois conselheiros
        JetonAgrupadoDTO dto1 = resultado.stream().filter(d -> d.getIdConselheiro().equals(100)).findFirst()
                .orElseThrow();
        JetonAgrupadoDTO dto2 = resultado.stream().filter(d -> d.getIdConselheiro().equals(101)).findFirst()
                .orElseThrow();

        assertThat(dto1.getTotalJeton()).isEqualTo(8); // 5 + 3
        assertThat(dto1.getValor()).isEqualTo(BigDecimal.valueOf(800));
        assertThat(dto2.getTotalJeton()).isEqualTo(2);
        assertThat(dto2.getValor()).isEqualTo(BigDecimal.valueOf(200));
    }

    // ========== TESTES DE RELATÓRIO INDIVIDUAL ==========

    @Test
    @DisplayName("deve gerar relatório individual do conselheiro com sucesso")
    void deveGerarRelatorioIndividualConselheiro() {
        // Dado
        Integer idPessoa = 100;
        Integer idGestao = 1;
        Integer mes = 6;
        Integer ano = 2026;
        Conselheiro conselheiro = criarConselheiro(idPessoa, "Dr. Teste");
        Gestao gestao = criarGestao(idGestao, "Gestão Teste", LocalDate.now().minusMonths(1),
                LocalDate.now().plusMonths(1));

        lenient().when(conselheiroServiceMock.buscarPorId(idPessoa)).thenReturn(Optional.of(conselheiro));

        // Criar uma atividade e associá-la ao jeton
        AtividadeConselhal atividade = mock(AtividadeConselhal.class);
        lenient().when(atividade.getDataHoraAtividade()).thenReturn(LocalDateTime.of(2026, 6, 15, 10, 0));
        lenient().when(atividade.getQtdAtividade()).thenReturn(2);

        Jeton jeton = criarJeton(1, gestao, conselheiro, mes, ano, 5, BigDecimal.valueOf(500), Jeton.SITUACAO_EXCLUIDO);
        jeton.getAtividades().add(atividade);

        List<Jeton> jetons = List.of(jeton);
        lenient().when(jetonRepositoryMock.findByGestaoIdGestaoAndMesAndAno(idGestao, mes, ano))
                .thenReturn(jetons);

        // Configurar o mapper para a atividade
        AtividadeVinculadaDTO atvDTO = new AtividadeVinculadaDTO("Reunião", "2026-06-15", 2);
        lenient().when(atividadeMapperMock.toAtividadeVinculadaDto(atividade)).thenReturn(atvDTO);

        // Pontos saldo para calcular saldoExistente e pontosUtilizadosAtividades
        PontosSaldo saldoAntigo = criarSaldo(1, 0, criarResolucao(10, 3, BigDecimal.valueOf(100)), null, conselheiro,
                gestao);
        saldoAntigo.setPontosUtilizados(5);
        saldoAntigo.setPontosSobrando(0);

        PontosSaldo saldoMes = criarSaldo(2, 0, criarResolucao(10, 3, BigDecimal.valueOf(100)), atividade, conselheiro,
                gestao);
        saldoMes.setPontosUtilizados(3);
        saldoMes.setPontosSobrando(0);

        lenient().when(pontosSaldoRepositoryMock.findByJetonIdJeton(1)).thenReturn(List.of(saldoAntigo, saldoMes));

        // Soma de pontos do mês
        lenient().when(atividadeRepositoryMock.sumPontosAtividadesValidadasDoMes(idPessoa, mes, ano))
                .thenReturn(10);

        // Saldo futuro
        lenient().when(pontosSaldoRepositoryMock.sumPontosSobrandoAtivos(idPessoa, idGestao))
                .thenReturn(2);

        lenient().when(atividadeRepositoryMock.findComputadasPorConselheiroEMes(anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of());

        // Quando
        RelatorioConselheiroDTO resultado = service.gerarRelatorioIndividualConselheiro(idPessoa, idGestao, mes, ano);

        // Então
        assertThat(resultado).isNotNull();
        assertThat(resultado.nomeConselheiro()).isEqualTo("Dr. Teste");
        assertThat(resultado.saldoExistente()).isEqualTo(5);
        assertThat(resultado.saldoAtividades()).isEqualTo(10);
        assertThat(resultado.saldoUtilizado()).isEqualTo(8);
        assertThat(resultado.saldoFuturo()).isEqualTo(2);
        assertThat(resultado.atividades()).hasSize(1);
        assertThat(resultado.atividades().get(0).regra()).isEqualTo("Reunião");
        assertThat(resultado.atividades().get(0).qtd()).isEqualTo(2);
    }

    // ========== TESTES DE SOMA DE VALORES RECEBIDOS ==========

    @Test
    @DisplayName("deve somar valor recebido por conselheiro com sucesso")
    void deveSomarValorRecebidoPorConselheiro() {
        // Dado
        Integer idPessoa = 100;
        BigDecimal valorEsperado = BigDecimal.valueOf(1500.50);

        when(jetonRepositoryMock.sumValorRecebidoPorConselheiro(idPessoa))
                .thenReturn(valorEsperado);

        // Quando
        BigDecimal resultado = service.sumValorRecebidoPorConselheiro(idPessoa);

        // Então
        assertThat(resultado).isEqualTo(valorEsperado);
        verify(jetonRepositoryMock).sumValorRecebidoPorConselheiro(idPessoa);
    }

    @Test
    @DisplayName("deve retornar zero quando não há valor recebido para conselheiro")
    void deveRetornarZeroQuandoNaoHaValorRecebido() {
        // Dado
        Integer idPessoa = 100;

        when(jetonRepositoryMock.sumValorRecebidoPorConselheiro(idPessoa))
                .thenReturn(null);

        // Quando
        BigDecimal resultado = service.sumValorRecebidoPorConselheiro(idPessoa);

        // Então
        assertThat(resultado).isZero();
    }

    // ========== TESTES DE BUSCA POR ID ==========

    @Test
    @DisplayName("deve buscar jeton por ID com sucesso")
    void deveBuscarJetonPorId() {
        // Dado
        Integer id = 10;
        Jeton jeton = criarJeton(id, null, null, 6, 2026, 5, BigDecimal.valueOf(500), Jeton.SITUACAO_ATIVO);
        when(jetonRepositoryMock.findById(id)).thenReturn(Optional.of(jeton));

        // Quando
        Optional<Jeton> resultado = service.buscarPorId(id);

        // Então
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getIdJeton()).isEqualTo(id);
    }

    @Test
    @DisplayName("deve retornar Optional vazio ao buscar jeton inexistente")
    void deveRetornarOptionalVazioAoBuscarJetonInexistente() {
        // Dado
        Integer id = 999;
        when(jetonRepositoryMock.findById(id)).thenReturn(Optional.empty());

        // Quando
        Optional<Jeton> resultado = service.buscarPorId(id);

        // Então
        assertThat(resultado).isEmpty();
    }

    // ========== TESTES DE LISTAGEM TODOS ==========

    @Test
    @DisplayName("deve listar todos os jetons")
    void deveListarTodosJetons() {
        // Dado
        Jeton j1 = criarJeton(1, null, null, 6, 2026, 5, BigDecimal.valueOf(500), Jeton.SITUACAO_ATIVO);
        Jeton j2 = criarJeton(2, null, null, 6, 2026, 3, BigDecimal.valueOf(300), Jeton.SITUACAO_ATIVO);
        when(jetonRepositoryMock.findAll()).thenReturn(List.of(j1, j2));

        JetonDTO dto1 = new JetonDTO(1, 100, "Dr. A", 1, "Gestão", 6, 2026, 5, BigDecimal.valueOf(500), "A");
        JetonDTO dto2 = new JetonDTO(2, 101, "Dr. B", 1, "Gestão", 6, 2026, 3, BigDecimal.valueOf(300), "A");
        when(jetonMapperMock.toDto(any(Jeton.class))).thenReturn(dto1, dto2);

        // Quando
        List<JetonDTO> resultado = service.listarTodos();

        // Então
        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).id()).isEqualTo(1);
        assertThat(resultado.get(1).id()).isEqualTo(2);
    }

    // ========== TESTES DE LISTAGEM POR CONSELHEIRO ==========

    @Test
    @DisplayName("deve listar jetons por conselheiro com limite")
    void deveListarJetonsPorConselheiroComLimite() {
        // Dado
        Integer idPessoa = 100;
        int limit = 5;
        Jeton j1 = criarJeton(1, null, null, 6, 2026, 5, BigDecimal.valueOf(500), Jeton.SITUACAO_ATIVO);
        Page<Jeton> pagina = new PageImpl<>(List.of(j1));

        when(jetonRepositoryMock.findByConselheiroIdPessoa(eq(idPessoa), any(Pageable.class)))
                .thenReturn(pagina);

        JetonDTO dto = new JetonDTO(1, 100, "Dr. A", 1, "Gestão", 6, 2026, 5, BigDecimal.valueOf(500), "A");
        when(jetonMapperMock.toDto(any(Jeton.class))).thenReturn(dto);

        // Quando
        List<JetonDTO> resultado = service.listarPorConselheiro(idPessoa, limit);

        // Então
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).id()).isEqualTo(1);
    }

    @Test
    @DisplayName("deve listar todos os jetons por conselheiro sem limite")
    void deveListarTodosJetonsPorConselheiro() {
        // Dado
        Integer idPessoa = 100;
        Jeton j1 = criarJeton(1, null, null, 6, 2026, 5, BigDecimal.valueOf(500), Jeton.SITUACAO_ATIVO);
        when(jetonRepositoryMock.findByConselheiroIdPessoaOrderByAnoDescMesDesc(idPessoa))
                .thenReturn(List.of(j1));

        JetonDTO dto = new JetonDTO(1, 100, "Dr. A", 1, "Gestão", 6, 2026, 5, BigDecimal.valueOf(500), "A");
        when(jetonMapperMock.toDto(any(Jeton.class))).thenReturn(dto);

        // Quando
        List<JetonDTO> resultado = service.listarPorConselheiro(idPessoa);

        // Então
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).id()).isEqualTo(1);
    }

    // ========== TESTES DE LISTAGEM PAGINADA POR CONSELHEIRO ==========

    @Test
    @DisplayName("deve listar pagamentos do conselheiro com paginação")
    void deveListarPagamentosConselheiroPaginado() {
        // Dado
        Integer idConselheiro = 100;
        Integer idGestao = 1;
        Integer mes = 6;
        Integer ano = 2026;
        Pageable pageable = Pageable.unpaged();

        Jeton jeton = criarJeton(1, null, null, mes, ano, 5, BigDecimal.valueOf(500), Jeton.SITUACAO_EXCLUIDO);
        Page<Jeton> pagina = new PageImpl<>(List.of(jeton));

        when(jetonRepositoryMock.findByConselheiroIdPessoaAndFilters(eq(idConselheiro), eq(idGestao), eq(mes), eq(ano),
                eq(pageable)))
                .thenReturn(pagina);

        JetonDTO dto = new JetonDTO(1, idConselheiro, "Dr. A", idGestao, "Gestão", mes, ano, 5, BigDecimal.valueOf(500),
                "E");
        when(jetonMapperMock.toDto(any(Jeton.class))).thenReturn(dto);

        // Quando
        Page<JetonDTO> resultado = service.listarPorConselheiroPaginado(idConselheiro, idGestao, mes, ano, pageable);

        // Então
        assertThat(resultado).isNotNull();
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).id()).isEqualTo(1);
    }

    // ========== NOVOS TESTES DE EXCLUSÃO DE JETON ==========

    @Test
    @DisplayName("deve excluir jeton com sucesso")
    void deveExcluirJetonComSucesso() {
        // Dado
        Integer idJeton = 10;
        Gestao gestao = criarGestao(1, "Gestão", LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1));
        Conselheiro conselheiro = criarConselheiro(100, "Dr. A");
        Jeton jeton = criarJeton(idJeton, gestao, conselheiro, 6, 2026, 5, BigDecimal.valueOf(500),
                Jeton.SITUACAO_ATIVO);

        when(jetonRepositoryMock.findById(idJeton)).thenReturn(Optional.of(jeton));

        // Quando
        service.excluirJeton(idJeton);

        // Então
        verify(jetonRepositoryMock).deleteById(idJeton);
        verify(logJetonServiceMock).logJetonExcluido(eq(idJeton), anyString(), anyString(), eq(6), eq(2026));
    }

    // ========== NOVOS TESTES DE VERIFICAÇÃO DE FOLHA HOMOLOGADA ==========

    @Test
    @DisplayName("deve retornar true para folha homologada quando há jetons com situação EXCLUIDO")
    void deveRetornarTrueQuandoHaJetonsExcluidos() {
        // Dado
        Integer idGestao = 1;
        Integer mes = 6;
        Integer ano = 2026;
        Gestao gestao = criarGestao(idGestao, "Gestão", LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1));
        Conselheiro conselheiro = criarConselheiro(100, "Dr. A");
        Jeton jetonHomologado = criarJeton(1, gestao, conselheiro, mes, ano, 5, BigDecimal.valueOf(500),
                Jeton.SITUACAO_EXCLUIDO);

        // ✅ Todos os stubs com lenient()
        lenient().when(jetonRepositoryMock.findByGestaoIdGestaoAndMesAndAno(idGestao, mes, ano))
                .thenReturn(List.of(jetonHomologado));

        // Para a validação de pré-processamento, precisamos garantir que
        // countAtividadesFechadasNoPeriodo seja 0
        // para que a verificação de homologação seja feita pelos jetons EXCLUIDO
        lenient().when(atividadeRepositoryMock.countAtividadesFechadasNoPeriodo(idGestao, mes, ano)).thenReturn(0L);
        lenient().when(atividadeRepositoryMock.countAtividadesPendentesNoMes(idGestao, mes, ano)).thenReturn(0L);
        lenient()
                .when(atividadeRepositoryMock.countAtividadesAnterioresNaoFechadas(eq(idGestao),
                        any(LocalDateTime.class)))
                .thenReturn(0L);

        // Quando / Então - o processamento deve lançar exceção de homologação
        assertThatThrownBy(() -> service.processarFechamentoMensal(gestao, mes, ano))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("já foi homologada");
    }

    // ========== NOVOS TESTES DE HISTÓRICO SEM PAGINAÇÃO ==========

    @Test
    @DisplayName("deve pesquisar histórico sem paginação")
    void devePesquisarHistoricoSemPaginacao() {
        // Dado
        Integer idGestao = 1;
        Integer mes = 6;
        Integer ano = 2026;
        String termo = "Dr.";
        Jeton jeton = criarJeton(1, null, null, mes, ano, 5, BigDecimal.valueOf(500), Jeton.SITUACAO_EXCLUIDO);

        when(jetonRepositoryMock.findAllByFilters(eq(idGestao), eq(mes), eq(ano), eq(termo)))
                .thenReturn(List.of(jeton));

        JetonDTO dto = new JetonDTO(1, 100, "Dr. Teste", 1, "Gestão", mes, ano, 5, BigDecimal.valueOf(500), "E");
        when(jetonMapperMock.toDto(any(Jeton.class))).thenReturn(dto);

        // Quando
        List<JetonDTO> resultado = service.pesquisarHistorico(idGestao, mes, ano, termo);

        // Então
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).id()).isEqualTo(1);
        verify(jetonRepositoryMock).findAllByFilters(eq(idGestao), eq(mes), eq(ano), eq(termo));
    }

    // ========== NOVOS TESTES DE LISTAGEM DE ATIVIDADES AGRUPADAS ==========

    @Test
    @DisplayName("deve listar atividades agrupadas por conselheiro")
    void deveListarAtividadesAgrupadasPorConselheiro() {
        // Dado
        Integer idPessoa = 100;
        Integer idGestao = 1;
        Integer mes = 6;
        Integer ano = 2026;
        Gestao gestao = criarGestao(idGestao, "Gestão", LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1));
        Conselheiro conselheiro = criarConselheiro(idPessoa, "Dr. A");
        Jeton jeton = criarJeton(1, gestao, conselheiro, mes, ano, 5, BigDecimal.valueOf(500),
                Jeton.SITUACAO_EXCLUIDO);

        // Associar uma atividade ao jeton
        AtividadeConselhal atividade = mock(AtividadeConselhal.class);
        jeton.getAtividades().add(atividade);

        when(jetonRepositoryMock.findByGestaoIdGestaoAndMesAndAno(idGestao, mes, ano))
                .thenReturn(List.of(jeton));

        AtividadeVinculadaDTO dto = new AtividadeVinculadaDTO("Regra", "2026-06-10", 2);
        when(atividadeMapperMock.toAtividadeVinculadaDto(atividade)).thenReturn(dto);

        // Quando
        List<AtividadeVinculadaDTO> resultado = service.listarAtividadesAgrupadasPorConselheiro(
                idPessoa, idGestao, mes, ano);

        // Então
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).regra()).isEqualTo("Regra");
    }

    // ========== NOVOS TESTES DE RELATÓRIO GERAL COM CONSELHEIRO SEM ATIVIDADES
    // ==========

    @Test
    @DisplayName("deve gerar relatório geral com conselheiro sem atividades")
    void deveGerarRelatorioGeralComConselheiroSemAtividades() {
        // Dado
        Integer idGestao = 1;
        Integer mes = 6;
        Integer ano = 2026;
        Gestao gestao = criarGestao(idGestao, "Gestão Teste", LocalDate.now().minusMonths(1),
                LocalDate.now().plusMonths(1));
        Conselheiro cons1 = criarConselheiro(100, "Dr. A");
        Conselheiro cons2 = criarConselheiro(101, "Dr. B");

        Jeton j1 = criarJeton(1, gestao, cons1, mes, ano, 5, BigDecimal.valueOf(500), Jeton.SITUACAO_EXCLUIDO);
        Jeton j2 = criarJeton(2, gestao, cons2, mes, ano, 0, BigDecimal.ZERO, Jeton.SITUACAO_EXCLUIDO);

        when(gestaoRepositoryMock.findById(idGestao)).thenReturn(Optional.of(gestao));
        when(jetonRepositoryMock.findByGestaoIdGestaoAndMesAndAno(idGestao, mes, ano))
                .thenReturn(List.of(j1, j2));

        when(pontosSaldoRepositoryMock.findByJetonIdJeton(1)).thenReturn(List.of());
        when(pontosSaldoRepositoryMock.findByJetonIdJeton(2)).thenReturn(List.of());

        when(pontosSaldoRepositoryMock.sumPontosSobrandoAtivos(100, idGestao)).thenReturn(10);
        when(pontosSaldoRepositoryMock.sumPontosSobrandoAtivos(101, idGestao)).thenReturn(0);

        when(atividadeRepositoryMock.findComputadasPorConselheiroEMes(100, mes, ano))
                .thenReturn(List.of());
        when(atividadeRepositoryMock.findComputadasPorConselheiroEMes(101, mes, ano))
                .thenReturn(List.of());

        // Quando
        RelatorioGeralDTO resultado = service.gerarRelatorioGeral(idGestao, mes, ano);

        // Então
        assertThat(resultado.getConselheiros()).hasSize(2);
        ConselheiroRelatorioDTO dto2 = resultado.getConselheiros().stream()
                .filter(c -> c.getIdPessoa().equals(101))
                .findFirst().orElseThrow();
        assertThat(dto2.getTotalJetons()).isZero();
        assertThat(dto2.getValor()).isZero();
    }

    // ========== NOVOS TESTES DE RELATÓRIO INDIVIDUAL COM ATIVIDADES VAZIAS
    // ==========

    @Test
    @DisplayName("deve gerar relatório individual com lista vazia de atividades")
    void deveGerarRelatorioIndividualComAtividadesVazias() throws Exception {
        // Dado
        Integer idPessoa = 100;
        Integer idGestao = 1;
        Integer mes = 6;
        Integer ano = 2026;
        Conselheiro conselheiro = criarConselheiro(idPessoa, "Dr. A");

        when(conselheiroServiceMock.buscarPorId(idPessoa)).thenReturn(Optional.of(conselheiro));

        // Nenhum jeton retornado (lista vazia)
        when(jetonRepositoryMock.findByGestaoIdGestaoAndMesAndAno(idGestao, mes, ano))
                .thenReturn(List.of());

        // Soma de pontos do mês = 0
        when(atividadeRepositoryMock.sumPontosAtividadesValidadasDoMes(idPessoa, mes, ano))
                .thenReturn(0);

        // Saldo futuro = 0
        when(pontosSaldoRepositoryMock.sumPontosSobrandoAtivos(idPessoa, idGestao))
                .thenReturn(0);

        // Quando
        RelatorioConselheiroDTO resultado = service.gerarRelatorioIndividualConselheiro(idPessoa, idGestao, mes, ano);

        // Então
        assertThat(resultado).isNotNull();
        assertThat(resultado.atividades()).isEmpty();
        assertThat(resultado.saldoExistente()).isZero();
        assertThat(resultado.saldoAtividades()).isZero();
        assertThat(resultado.saldoUtilizado()).isZero();
        assertThat(resultado.saldoFuturo()).isZero();
    }

}