package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.domain.PontosSaldo;
import br.com.cremepe.jeton.domain.Resolucao;
import br.com.cremepe.jeton.service.JetonCalculator.ResultadoAbsorcao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Testes do calculador de Jetons (JetonCalculator)")
class JetonCalculatorTest {

    private JetonCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new JetonCalculator();
    }

    // ========== HELPERS ==========

    private Resolucao criarResolucao(Integer id, Integer pontosPorJeton) {
        Resolucao r = new Resolucao();
        r.setIdResolucao(id);
        r.setNumero(1);
        r.setAno(2026);
        r.setPontosPorJeton(pontosPorJeton != null ? pontosPorJeton : 3);
        r.setValorJeton(BigDecimal.valueOf(100.00));
        return r;
    }

    private PontosSaldo criarSaldo(Integer id, Integer pontosSobrando, Resolucao resolucao, LocalDateTime dataHora) {
        PontosSaldo saldo = new PontosSaldo();
        saldo.setIdPontosSaldo(id);
        saldo.setPontosSobrando(pontosSobrando);
        saldo.setResolucao(resolucao);
        saldo.setDataHora(dataHora != null ? dataHora : LocalDateTime.now());
        saldo.setPontosTrabalhados(pontosSobrando);
        saldo.setPontosUtilizados(0);
        saldo.setInSituacao(PontosSaldo.SITUACAO_ATIVO);
        return saldo;
    }

    // ========== TESTES ==========

    @Test
    @DisplayName("deve calcular jetons a partir de saldos com uma única resolução")
    void deveCalcularJetonsComUmaResolucao() {
        // Dado
        Resolucao resolucao = criarResolucao(1, 3);
        List<PontosSaldo> fila = List.of(
                criarSaldo(1, 10, resolucao, null), // 3 jetons (sobra 1)
                criarSaldo(2, 5, resolucao, null) // buffer=1+5=6 -> 2 jetons
        );
        int maxJetons = 10;

        // Quando
        ResultadoAbsorcao resultado = calculator.calcularJetons(fila, maxJetons);

        // Então
        assertThat(resultado.totalJetons()).isEqualTo(5); // ✅ corrigido: 3 + 2
        assertThat(resultado.totalPontosConsumidos()).isEqualTo(15); // 5 * 3
        assertThat(resultado.demonstrativo().get(resolucao)).isEqualTo(5);
    }

    @Test
    @DisplayName("deve calcular jetons respeitando o limite máximo de jetons permitidos")
    void deveRespeitarLimiteMaximoJetons() {
        // Dado
        Resolucao resolucao = criarResolucao(1, 3);
        // Saldo com 100 pontos -> 33 jetons (sobra 1)
        List<PontosSaldo> fila = List.of(criarSaldo(1, 100, resolucao, null));
        int maxJetons = 5; // limite de 5 jetons

        // Quando
        ResultadoAbsorcao resultado = calculator.calcularJetons(fila, maxJetons);

        // Então
        assertThat(resultado.totalJetons()).isEqualTo(5);
        assertThat(resultado.totalPontosConsumidos()).isEqualTo(15); // 5 * 3 = 15
        // Os pontos restantes (85) ficam no buffer, mas não são consumidos
    }

    @Test
    @DisplayName("deve distribuir corretamente os jetons entre diferentes resoluções (FIFO)")
    void deveDistribuirJetonsEntreMultiplasResolucoes() {
        // Dado
        Resolucao res1 = criarResolucao(1, 3);
        Resolucao res2 = criarResolucao(2, 4);
        Resolucao res3 = criarResolucao(3, 2);

        List<PontosSaldo> fila = List.of(
                criarSaldo(1, 10, res1, LocalDateTime.now().minusMinutes(10)), // 3 jetons, sobra 1
                criarSaldo(2, 10, res2, LocalDateTime.now().minusMinutes(5)), // buffer=11 -> 2 jetons, sobra 3
                criarSaldo(3, 10, res3, LocalDateTime.now()) // buffer=13 -> 6 jetons, sobra 1
        );
        int maxJetons = 20;

        // Quando
        ResultadoAbsorcao resultado = calculator.calcularJetons(fila, maxJetons);

        // Então
        assertThat(resultado.totalJetons()).isEqualTo(11); // ✅ corrigido: 3 + 2 + 6
        assertThat(resultado.totalPontosConsumidos())
                .isEqualTo(3 * 3 + 2 * 4 + 6 * 2); // 9 + 8 + 12 = 29

        Map<Resolucao, Integer> demonstrativo = resultado.demonstrativo();
        assertThat(demonstrativo).hasSize(3);
        assertThat(demonstrativo.get(res1)).isEqualTo(3);
        assertThat(demonstrativo.get(res2)).isEqualTo(2);
        assertThat(demonstrativo.get(res3)).isEqualTo(6);
    }

    @Test
    @DisplayName("deve usar valor padrão 3 pontos por jeton quando a resolução não define")
    void deveUsarValorPadraoQuandoResolucaoNaoDefinePontosPorJeton() {
        // Dado
        Resolucao resolucaoSemPontos = criarResolucao(1, null); // pontosPorJeton = null
        List<PontosSaldo> fila = List.of(criarSaldo(1, 10, resolucaoSemPontos, null));
        int maxJetons = 10;

        // Quando
        ResultadoAbsorcao resultado = calculator.calcularJetons(fila, maxJetons);

        // Então
        assertThat(resultado.totalJetons()).isEqualTo(3); // 10 / 3 = 3 (padrão)
        assertThat(resultado.totalPontosConsumidos()).isEqualTo(9);
        Map<Resolucao, Integer> demonstrativo = resultado.demonstrativo();
        assertThat(demonstrativo.get(resolucaoSemPontos)).isEqualTo(3);
    }

    @Test
    @DisplayName("deve usar valor padrão 3 quando pontosPorJeton for zero ou negativo")
    void deveUsarValorPadraoQuandoPontosPorJetonZeroOuNegativo() {
        // Dado
        Resolucao resZero = criarResolucao(1, 0);
        Resolucao resNegativo = criarResolucao(2, -5);
        List<PontosSaldo> fila = List.of(
                criarSaldo(1, 10, resZero, null),
                criarSaldo(2, 10, resNegativo, null));
        int maxJetons = 10;

        // Quando
        ResultadoAbsorcao resultado = calculator.calcularJetons(fila, maxJetons);

        // Então
        // Ambos devem usar o padrão 3
        assertThat(resultado.totalJetons()).isEqualTo(3 + 3); // 6
        assertThat(resultado.totalPontosConsumidos()).isEqualTo(9 + 9); // 18
    }

    @Test
    @DisplayName("não deve gerar jetons quando o buffer de pontos é insuficiente")
    void naoDeveGerarJetonsQuandoPontosInsuficientes() {
        // Dado
        Resolucao resolucao = criarResolucao(1, 3);
        List<PontosSaldo> fila = List.of(criarSaldo(1, 2, resolucao, null)); // 2 pontos < 3
        int maxJetons = 10;

        // Quando
        ResultadoAbsorcao resultado = calculator.calcularJetons(fila, maxJetons);

        // Então
        assertThat(resultado.totalJetons()).isZero();
        assertThat(resultado.totalPontosConsumidos()).isZero();
        assertThat(resultado.demonstrativo()).isEmpty();
    }

    @Test
    @DisplayName("deve ignorar saldos com pontosSobrando igual a zero")
    void deveIgnorarSaldosComPontosSobrandoZero() {
        // Dado
        Resolucao resolucao = criarResolucao(1, 3);
        List<PontosSaldo> fila = List.of(
                criarSaldo(1, 0, resolucao, null),
                criarSaldo(2, 6, resolucao, null));
        int maxJetons = 10;

        // Quando
        ResultadoAbsorcao resultado = calculator.calcularJetons(fila, maxJetons);

        // Então
        assertThat(resultado.totalJetons()).isEqualTo(2); // apenas 6/3 = 2
        assertThat(resultado.totalPontosConsumidos()).isEqualTo(6);
        assertThat(resultado.demonstrativo().get(resolucao)).isEqualTo(2);
    }

    @Test
    @DisplayName("deve retornar demonstrativo vazio e totais zero quando a fila está vazia")
    void deveRetornarVazioQuandoFilaVazia() {
        // Dado
        List<PontosSaldo> fila = List.of();
        int maxJetons = 10;

        // Quando
        ResultadoAbsorcao resultado = calculator.calcularJetons(fila, maxJetons);

        // Então
        assertThat(resultado.totalJetons()).isZero();
        assertThat(resultado.totalPontosConsumidos()).isZero();
        assertThat(resultado.demonstrativo()).isEmpty();
    }

    @Test
    @DisplayName("deve acumular pontos no buffer e gerar jetons de forma FIFO")
    void deveAcumularPontosNoBufferEProcessarFIFO() {
        // Dado
        Resolucao resolucao = criarResolucao(1, 3);
        // Saldo 1: 2 pontos (não gera, acumula no buffer)
        // Saldo 2: 2 pontos (buffer total 4, gera 1 jeton, sobra 1)
        // Saldo 3: 2 pontos (buffer total 3, gera 1 jeton, sobra 0)
        List<PontosSaldo> fila = List.of(
                criarSaldo(1, 2, resolucao, LocalDateTime.now().minusMinutes(5)),
                criarSaldo(2, 2, resolucao, LocalDateTime.now().minusMinutes(3)),
                criarSaldo(3, 2, resolucao, LocalDateTime.now()));
        int maxJetons = 10;

        // Quando
        ResultadoAbsorcao resultado = calculator.calcularJetons(fila, maxJetons);

        // Então
        // Total jetons: (2+2+2)/3 = 2 jetons (sobra 0)
        assertThat(resultado.totalJetons()).isEqualTo(2);
        assertThat(resultado.totalPontosConsumidos()).isEqualTo(6);
        assertThat(resultado.demonstrativo().get(resolucao)).isEqualTo(2);
    }

    @Test
    @DisplayName("deve parar de consumir quando o limite máximo de jetons é atingido no meio da fila")
    void devePararDeConsumirQuandoLimiteAtingidoNoMeioDaFila() {
        // Dado
        Resolucao resolucao = criarResolucao(1, 3);
        // Saldo 1: 10 pontos -> 3 jetons (sobra 1)
        // Saldo 2: 10 pontos -> mais 3 jetons, mas limite é 4, então só consome 1 jeton
        // do segundo saldo
        List<PontosSaldo> fila = List.of(
                criarSaldo(1, 10, resolucao, LocalDateTime.now().minusMinutes(10)),
                criarSaldo(2, 10, resolucao, LocalDateTime.now()));
        int maxJetons = 4;

        // Quando
        ResultadoAbsorcao resultado = calculator.calcularJetons(fila, maxJetons);

        // Então
        assertThat(resultado.totalJetons()).isEqualTo(4);
        assertThat(resultado.totalPontosConsumidos()).isEqualTo(12); // 4 * 3 = 12

        // O demonstrativo deve ter 4 jetons, todos atribuídos à resolução
        assertThat(resultado.demonstrativo().get(resolucao)).isEqualTo(4);
    }

    @Test
    @DisplayName("deve manter a ordem das resoluções no demonstrativo (LinkedHashMap)")
    void deveManterOrdemDasResolucoesNoDemonstrativo() {
        // Dado
        Resolucao res1 = criarResolucao(1, 3);
        Resolucao res2 = criarResolucao(2, 4);
        Resolucao res3 = criarResolucao(3, 2);

        // A ordem na fila é res1, res2, res3
        List<PontosSaldo> fila = List.of(
                criarSaldo(1, 10, res1, LocalDateTime.now().minusMinutes(10)),
                criarSaldo(2, 10, res2, LocalDateTime.now().minusMinutes(5)),
                criarSaldo(3, 10, res3, LocalDateTime.now()));
        int maxJetons = 20;

        // Quando
        ResultadoAbsorcao resultado = calculator.calcularJetons(fila, maxJetons);

        // Então
        Map<Resolucao, Integer> demonstrativo = resultado.demonstrativo();
        List<Resolucao> chaves = new ArrayList<>(demonstrativo.keySet());

        // Verifica que a ordem é a mesma da fila (res1, res2, res3)
        assertThat(chaves).containsExactly(res1, res2, res3);
    }

    @Test
    @DisplayName("deve funcionar corretamente com limite máximo igual a zero")
    void deveFuncionarComLimiteMaximoZero() {
        // Dado
        Resolucao resolucao = criarResolucao(1, 3);
        List<PontosSaldo> fila = List.of(criarSaldo(1, 100, resolucao, null));
        int maxJetons = 0;

        // Quando
        ResultadoAbsorcao resultado = calculator.calcularJetons(fila, maxJetons);

        // Então
        assertThat(resultado.totalJetons()).isZero();
        assertThat(resultado.totalPontosConsumidos()).isZero();
        assertThat(resultado.demonstrativo()).isEmpty();
    }
}