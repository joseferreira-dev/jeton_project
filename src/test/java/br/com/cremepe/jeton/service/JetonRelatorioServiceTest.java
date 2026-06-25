package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.dto.AtividadeRelatorioDTO;
import br.com.cremepe.jeton.dto.ConselheiroRelatorioDTO;
import br.com.cremepe.jeton.dto.RelatorioGeralDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("Testes do serviço de geração de relatórios de Jetons (JetonRelatorioService)")
class JetonRelatorioServiceTest {

    private JetonRelatorioService service;

    private RelatorioGeralDTO relatorioDTO;

    @BeforeEach
    void setUp() {
        service = new JetonRelatorioService();

        // Cria um DTO de relatório com dados mínimos para os testes
        relatorioDTO = new RelatorioGeralDTO();
        relatorioDTO.setIdGestao(1);
        relatorioDTO.setNomeGestao("Gestão Teste");
        relatorioDTO.setMes(6);
        relatorioDTO.setAno(2026);
        relatorioDTO.setTotalGeralJetons(8);
        relatorioDTO.setTotalGeralValor(BigDecimal.valueOf(800.00));

        // Conselheiro 1
        ConselheiroRelatorioDTO cons1 = new ConselheiroRelatorioDTO();
        cons1.setIdPessoa(100);
        cons1.setNome("Dr. A");
        cons1.setTotalJetons(5);
        cons1.setValor(BigDecimal.valueOf(500.00));
        cons1.setSaldoAnterior(10);
        cons1.setPontosAcumuladosMes(6);
        cons1.setSaldoFuturo(4);

        AtividadeRelatorioDTO atv1 = new AtividadeRelatorioDTO();
        atv1.setRegra("Reunião");
        atv1.setData(LocalDate.of(2026, 6, 10));
        atv1.setQuantidade(2);

        AtividadeRelatorioDTO atv2 = new AtividadeRelatorioDTO();
        atv2.setRegra("Sindicância");
        atv2.setData(LocalDate.of(2026, 6, 15));
        atv2.setQuantidade(1);

        cons1.setAtividades(List.of(atv1, atv2));

        // Conselheiro 2
        ConselheiroRelatorioDTO cons2 = new ConselheiroRelatorioDTO();
        cons2.setIdPessoa(101);
        cons2.setNome("Dr. B");
        cons2.setTotalJetons(3);
        cons2.setValor(BigDecimal.valueOf(300.00));
        cons2.setSaldoAnterior(5);
        cons2.setPontosAcumuladosMes(3);
        cons2.setSaldoFuturo(2);

        AtividadeRelatorioDTO atv3 = new AtividadeRelatorioDTO();
        atv3.setRegra("Reunião");
        atv3.setData(LocalDate.of(2026, 6, 20));
        atv3.setQuantidade(3);

        cons2.setAtividades(List.of(atv3));

        relatorioDTO.setConselheiros(List.of(cons1, cons2));
    }

    // ========== TESTES DO EXCEL ==========

    @Test
    @DisplayName("deve gerar relatório Excel com sucesso")
    void deveGerarRelatorioExcelComSucesso() throws Exception {
        // Quando
        byte[] excelBytes = service.gerarExcelRelatorio(relatorioDTO);

        // Então
        assertThat(excelBytes).isNotNull();
        assertThat(excelBytes).isNotEmpty();
        // Tamanho mínimo esperado para um arquivo Excel válido
        assertThat(excelBytes.length).isGreaterThan(1000);
    }

    @Test
    @DisplayName("deve gerar relatório Excel mesmo com lista vazia de conselheiros")
    void deveGerarRelatorioExcelComListaVazia() throws Exception {
        // Dado
        RelatorioGeralDTO dtoVazio = new RelatorioGeralDTO();
        dtoVazio.setIdGestao(2);
        dtoVazio.setNomeGestao("Gestão Vazia");
        dtoVazio.setMes(7);
        dtoVazio.setAno(2026);
        dtoVazio.setConselheiros(List.of());
        dtoVazio.setTotalGeralJetons(0);
        dtoVazio.setTotalGeralValor(BigDecimal.ZERO);

        // Quando
        byte[] excelBytes = service.gerarExcelRelatorio(dtoVazio);

        // Então
        assertThat(excelBytes).isNotNull();
        assertThat(excelBytes).isNotEmpty();
        // Mesmo sem dados, o Excel contém cabeçalhos e totais
        assertThat(excelBytes.length).isGreaterThan(500);
    }

    // ========== TESTES DO PDF ==========

    @Test
    @DisplayName("deve gerar relatório PDF com sucesso")
    void deveGerarRelatorioPdfComSucesso() throws Exception {
        // Quando
        byte[] pdfBytes = service.gerarPdfRelatorio(relatorioDTO);

        // Então
        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes).isNotEmpty();
        // Tamanho mínimo esperado para um PDF válido
        assertThat(pdfBytes.length).isGreaterThan(1000);
    }

    @Test
    @DisplayName("deve gerar relatório PDF mesmo com lista vazia de conselheiros")
    void deveGerarRelatorioPdfComListaVazia() throws Exception {
        // Dado
        RelatorioGeralDTO dtoVazio = new RelatorioGeralDTO();
        dtoVazio.setIdGestao(2);
        dtoVazio.setNomeGestao("Gestão Vazia");
        dtoVazio.setMes(7);
        dtoVazio.setAno(2026);
        dtoVazio.setConselheiros(List.of());
        dtoVazio.setTotalGeralJetons(0);
        dtoVazio.setTotalGeralValor(BigDecimal.ZERO);

        // Quando
        byte[] pdfBytes = service.gerarPdfRelatorio(dtoVazio);

        // Então
        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes).isNotEmpty();
        assertThat(pdfBytes.length).isGreaterThan(500);
    }

    // ========== TESTES DE INTEGRIDADE ==========

    @Test
    @DisplayName("não deve lançar exceção ao gerar Excel com dados nulos")
    void naoDeveLancarExcecaoAoGerarExcelComDadosNulos() {
        // Dado
        RelatorioGeralDTO dtoComNulos = new RelatorioGeralDTO();
        dtoComNulos.setConselheiros(List.of());
        dtoComNulos.setTotalGeralJetons(0);
        dtoComNulos.setTotalGeralValor(BigDecimal.ZERO);
        dtoComNulos.setIdGestao(1);
        dtoComNulos.setNomeGestao("Gestão");
        dtoComNulos.setMes(1);
        dtoComNulos.setAno(2026);

        // Quando / Então
        assertThatCode(() -> service.gerarExcelRelatorio(dtoComNulos))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("não deve lançar exceção ao gerar PDF com dados nulos")
    void naoDeveLancarExcecaoAoGerarPdfComDadosNulos() {
        // Dado
        RelatorioGeralDTO dtoComNulos = new RelatorioGeralDTO();
        dtoComNulos.setConselheiros(List.of());
        dtoComNulos.setTotalGeralJetons(0);
        dtoComNulos.setTotalGeralValor(BigDecimal.ZERO);
        dtoComNulos.setIdGestao(1);
        dtoComNulos.setNomeGestao("Gestão");
        dtoComNulos.setMes(1);
        dtoComNulos.setAno(2026);

        // Quando / Então
        assertThatCode(() -> service.gerarPdfRelatorio(dtoComNulos))
                .doesNotThrowAnyException();
    }
}