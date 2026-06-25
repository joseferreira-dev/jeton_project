package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.domain.ViewAtividadeConselhal;
import br.com.cremepe.jeton.dto.AtividadeAgrupadaRelatorioDTO;
import br.com.cremepe.jeton.repository.ViewAtividadeConselhalRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do serviço de Relatórios (RelatorioService)")
class RelatorioServiceTest {

    @Mock
    private ViewAtividadeConselhalRepository viewRepositoryMock;

    @Mock
    private LogJetonService logJetonServiceMock;

    @InjectMocks
    private RelatorioService service;

    // ========== HELPER ==========

    private ViewAtividadeConselhal criarViewAtividade(
            Integer idAtividade,
            Integer idGestao,
            Integer idPessoa,
            Integer idRegra,
            String nomeGestao,
            String nomeConselheiro,
            String nomeRegra,
            Integer qtd,
            Integer pontos,
            LocalDateTime dataHora) {

        // Usa lenient() para evitar UnnecessaryStubbingException
        ViewAtividadeConselhal viewMock = mock(ViewAtividadeConselhal.class, withSettings().lenient());
        lenient().when(viewMock.getIdAtividade()).thenReturn(idAtividade);
        lenient().when(viewMock.getIdGestao()).thenReturn(idGestao);
        lenient().when(viewMock.getIdPessoa()).thenReturn(idPessoa);
        lenient().when(viewMock.getIdRegra()).thenReturn(idRegra);
        lenient().when(viewMock.getNomeGestao()).thenReturn(nomeGestao);
        lenient().when(viewMock.getNome()).thenReturn(nomeConselheiro);
        lenient().when(viewMock.getNomeRegra()).thenReturn(nomeRegra);
        lenient().when(viewMock.getQtdAtividade()).thenReturn(qtd);
        lenient().when(viewMock.getPontos()).thenReturn(pontos);
        lenient().when(viewMock.getDataHoraAtividade()).thenReturn(dataHora);
        return viewMock;
    }

    // ========== TESTES ==========

    @Test
    @DisplayName("deve gerar relatório agrupado com sucesso")
    void deveGerarRelatorioAgrupadoComSucesso() {
        // Dado
        Integer idGestao = 1;
        LocalDate dataInicio = LocalDate.of(2026, 1, 1);
        LocalDate dataFim = LocalDate.of(2026, 1, 31);

        LocalDateTime inicio = dataInicio.atStartOfDay();
        LocalDateTime fim = dataFim.atTime(LocalTime.MAX);

        ViewAtividadeConselhal atv1 = criarViewAtividade(1, idGestao, 100, 10, "Gestão 2026", "Dr. A",
                "Reunião", 2, 3, LocalDateTime.of(2026, 1, 10, 10, 0));
        ViewAtividadeConselhal atv2 = criarViewAtividade(2, idGestao, 100, 20, "Gestão 2026", "Dr. A",
                "Sindicância", 1, 5, LocalDateTime.of(2026, 1, 15, 14, 0));
        ViewAtividadeConselhal atv3 = criarViewAtividade(3, idGestao, 101, 10, "Gestão 2026", "Dr. B",
                "Reunião", 3, 3, LocalDateTime.of(2026, 1, 20, 9, 0));

        when(viewRepositoryMock.findForReport(eq(idGestao), isNull(), eq(inicio), eq(fim)))
                .thenReturn(List.of(atv1, atv2, atv3));

        // Quando
        List<AtividadeAgrupadaRelatorioDTO> resultado = service.gerarRelatorioAgrupado(
                idGestao, null, null, dataInicio, dataFim);

        // Então
        assertThat(resultado).hasSize(2); // Dr. A e Dr. B

        // Verifica Dr. A
        AtividadeAgrupadaRelatorioDTO dtoA = resultado.stream()
                .filter(d -> d.getConselheiro().equals("Dr. A"))
                .findFirst()
                .orElseThrow();
        assertThat(dtoA.getGestao()).isEqualTo("Gestão 2026");
        assertThat(dtoA.getRegras()).containsEntry("Reunião", 2);
        assertThat(dtoA.getRegras()).containsEntry("Sindicância", 1);
        assertThat(dtoA.getTotalPontos()).isEqualTo(2 * 3 + 1 * 5); // 11

        // Verifica Dr. B
        AtividadeAgrupadaRelatorioDTO dtoB = resultado.stream()
                .filter(d -> d.getConselheiro().equals("Dr. B"))
                .findFirst()
                .orElseThrow();
        assertThat(dtoB.getGestao()).isEqualTo("Gestão 2026");
        assertThat(dtoB.getRegras()).containsEntry("Reunião", 3);
        assertThat(dtoB.getTotalPontos()).isEqualTo(3 * 3); // 9

        // ✅ totalRegistros = número de grupos = 2
        verify(logJetonServiceMock).logRelatorioGerado(eq(idGestao), isNull(), isNull(),
                eq(dataInicio), eq(dataFim), eq(2));
    }

    @Test
    @DisplayName("deve filtrar relatório por conselheiro específico")
    void deveFiltrarRelatorioPorConselheiro() {
        // Dado
        Integer idGestao = 1;
        Integer idConselheiro = 100;
        LocalDate dataInicio = LocalDate.of(2026, 1, 1);
        LocalDate dataFim = LocalDate.of(2026, 1, 31);

        LocalDateTime inicio = dataInicio.atStartOfDay();
        LocalDateTime fim = dataFim.atTime(LocalTime.MAX);

        ViewAtividadeConselhal atv1 = criarViewAtividade(1, idGestao, 100, 10, "Gestão", "Dr. A",
                "Reunião", 2, 3, LocalDateTime.now());
        ViewAtividadeConselhal atv2 = criarViewAtividade(2, idGestao, 100, 20, "Gestão", "Dr. A",
                "Sindicância", 1, 5, LocalDateTime.now());

        when(viewRepositoryMock.findForReport(eq(idGestao), eq(idConselheiro), eq(inicio), eq(fim)))
                .thenReturn(List.of(atv1, atv2));

        // Quando
        List<AtividadeAgrupadaRelatorioDTO> resultado = service.gerarRelatorioAgrupado(
                idGestao, idConselheiro, null, dataInicio, dataFim);

        // Então
        assertThat(resultado).hasSize(1); // apenas Dr. A
        assertThat(resultado.get(0).getConselheiro()).isEqualTo("Dr. A");
        assertThat(resultado.get(0).getRegras()).hasSize(2);

        verify(viewRepositoryMock).findForReport(eq(idGestao), eq(idConselheiro), eq(inicio), eq(fim));
        // ✅ totalRegistros = número de grupos = 1
        verify(logJetonServiceMock).logRelatorioGerado(eq(idGestao), eq(idConselheiro), isNull(),
                eq(dataInicio), eq(dataFim), eq(1));
    }

    // Os demais testes permanecem inalterados, mas com a correção do helper
    // (usando lenient) e as expectativas de log ajustadas conforme necessário.
    // Seguem os testes restantes com as correções aplicadas.

    @Test
    @DisplayName("deve filtrar relatório por regra específica")
    void deveFiltrarRelatorioPorRegra() {
        // Dado
        Integer idGestao = 1;
        Integer idRegra = 10;
        LocalDate dataInicio = LocalDate.of(2026, 1, 1);
        LocalDate dataFim = LocalDate.of(2026, 1, 31);

        LocalDateTime inicio = dataInicio.atStartOfDay();
        LocalDateTime fim = dataFim.atTime(LocalTime.MAX);

        ViewAtividadeConselhal atv1 = criarViewAtividade(1, idGestao, 100, 10, "Gestão", "Dr. A",
                "Reunião", 2, 3, LocalDateTime.now());
        ViewAtividadeConselhal atv2 = criarViewAtividade(2, idGestao, 100, 20, "Gestão", "Dr. A",
                "Sindicância", 1, 5, LocalDateTime.now());
        ViewAtividadeConselhal atv3 = criarViewAtividade(3, idGestao, 101, 10, "Gestão", "Dr. B",
                "Reunião", 3, 3, LocalDateTime.now());

        when(viewRepositoryMock.findForReport(eq(idGestao), isNull(), eq(inicio), eq(fim)))
                .thenReturn(List.of(atv1, atv2, atv3));

        // Quando
        List<AtividadeAgrupadaRelatorioDTO> resultado = service.gerarRelatorioAgrupado(
                idGestao, null, idRegra, dataInicio, dataFim);

        // Então
        assertThat(resultado).hasSize(2); // Dr. A e Dr. B
        for (AtividadeAgrupadaRelatorioDTO dto : resultado) {
            assertThat(dto.getRegras().keySet()).containsOnly("Reunião");
        }

        AtividadeAgrupadaRelatorioDTO dtoA = resultado.stream()
                .filter(d -> d.getConselheiro().equals("Dr. A"))
                .findFirst()
                .orElseThrow();
        assertThat(dtoA.getRegras().get("Reunião")).isEqualTo(2);

        AtividadeAgrupadaRelatorioDTO dtoB = resultado.stream()
                .filter(d -> d.getConselheiro().equals("Dr. B"))
                .findFirst()
                .orElseThrow();
        assertThat(dtoB.getRegras().get("Reunião")).isEqualTo(3);

        // totalRegistros = 2
        verify(logJetonServiceMock).logRelatorioGerado(eq(idGestao), isNull(), eq(idRegra),
                eq(dataInicio), eq(dataFim), eq(2));
    }

    @Test
    @DisplayName("deve converter datas corretamente para LocalDateTime")
    void deveConverterDatasCorretamente() {
        // Dado
        Integer idGestao = 1;
        LocalDate dataInicio = LocalDate.of(2026, 1, 15);
        LocalDate dataFim = LocalDate.of(2026, 1, 20);

        LocalDateTime expectedInicio = dataInicio.atStartOfDay();
        LocalDateTime expectedFim = dataFim.atTime(LocalTime.MAX);

        when(viewRepositoryMock.findForReport(eq(idGestao), isNull(), eq(expectedInicio), eq(expectedFim)))
                .thenReturn(List.of());

        // Quando
        service.gerarRelatorioAgrupado(idGestao, null, null, dataInicio, dataFim);

        // Então
        verify(viewRepositoryMock).findForReport(eq(idGestao), isNull(), eq(expectedInicio), eq(expectedFim));
        verify(logJetonServiceMock).logRelatorioGerado(eq(idGestao), isNull(), isNull(),
                eq(dataInicio), eq(dataFim), eq(0));
    }

    @Test
    @DisplayName("deve retornar lista vazia quando não há dados")
    void deveRetornarListaVaziaQuandoNaoHaDados() {
        // Dado
        Integer idGestao = 1;
        LocalDate dataInicio = LocalDate.of(2026, 1, 1);
        LocalDate dataFim = LocalDate.of(2026, 1, 31);

        when(viewRepositoryMock.findForReport(anyInt(), isNull(), any(), any()))
                .thenReturn(List.of());

        // Quando
        List<AtividadeAgrupadaRelatorioDTO> resultado = service.gerarRelatorioAgrupado(
                idGestao, null, null, dataInicio, dataFim);

        // Então
        assertThat(resultado).isEmpty();
        verify(logJetonServiceMock).logRelatorioGerado(eq(idGestao), isNull(), isNull(),
                eq(dataInicio), eq(dataFim), eq(0));
    }

    @Test
    @DisplayName("deve agrupar atividades por conselheiro corretamente com múltiplas regras")
    void deveAgruparPorConselheiroComMultiplasRegras() {
        // Dado
        Integer idGestao = 1;
        LocalDateTime dataHoraBase = LocalDateTime.now();

        ViewAtividadeConselhal atv1 = criarViewAtividade(1, idGestao, 100, 10, "Gestão X", "Dr. A",
                "Regra 1", 1, 2, dataHoraBase);
        ViewAtividadeConselhal atv2 = criarViewAtividade(2, idGestao, 100, 20, "Gestão X", "Dr. A",
                "Regra 2", 3, 4, dataHoraBase);
        ViewAtividadeConselhal atv3 = criarViewAtividade(3, idGestao, 100, 10, "Gestão X", "Dr. A",
                "Regra 1", 2, 2, dataHoraBase);

        when(viewRepositoryMock.findForReport(anyInt(), isNull(), any(), any()))
                .thenReturn(List.of(atv1, atv2, atv3));

        // Quando
        List<AtividadeAgrupadaRelatorioDTO> resultado = service.gerarRelatorioAgrupado(
                idGestao, null, null, null, null);

        // Então
        assertThat(resultado).hasSize(1);
        AtividadeAgrupadaRelatorioDTO dto = resultado.get(0);
        assertThat(dto.getConselheiro()).isEqualTo("Dr. A");
        assertThat(dto.getRegras().get("Regra 1")).isEqualTo(3);
        assertThat(dto.getRegras().get("Regra 2")).isEqualTo(3);
        assertThat(dto.getTotalPontos()).isEqualTo(2 + 12 + 4); // 18
    }

    @Test
    @DisplayName("deve calcular total de pontos corretamente quando qtd ou pontos são nulos")
    void deveCalcularTotalPontosComNulos() {
        // Dado
        Integer idGestao = 1;
        LocalDateTime dataHoraBase = LocalDateTime.now();

        ViewAtividadeConselhal atv1 = criarViewAtividade(1, idGestao, 100, 10, "Gestão", "Dr. A",
                "Regra 1", null, 3, dataHoraBase);
        ViewAtividadeConselhal atv2 = criarViewAtividade(2, idGestao, 100, 20, "Gestão", "Dr. A",
                "Regra 2", 2, null, dataHoraBase);
        ViewAtividadeConselhal atv3 = criarViewAtividade(3, idGestao, 100, 10, "Gestão", "Dr. A",
                "Regra 1", 2, 3, dataHoraBase);

        when(viewRepositoryMock.findForReport(anyInt(), isNull(), any(), any()))
                .thenReturn(List.of(atv1, atv2, atv3));

        // Quando
        List<AtividadeAgrupadaRelatorioDTO> resultado = service.gerarRelatorioAgrupado(
                idGestao, null, null, null, null);

        // Então
        assertThat(resultado).hasSize(1);
        AtividadeAgrupadaRelatorioDTO dto = resultado.get(0);
        assertThat(dto.getTotalPontos()).isEqualTo(6);
        assertThat(dto.getRegras().get("Regra 1")).isEqualTo(2);
        assertThat(dto.getRegras().get("Regra 2")).isEqualTo(2);
    }

    @Test
    @DisplayName("deve ordenar resultado por nome do conselheiro")
    void deveOrdenarResultadoPorNome() {
        // Dado
        Integer idGestao = 1;
        LocalDateTime dataHoraBase = LocalDateTime.now();

        ViewAtividadeConselhal atvC = criarViewAtividade(1, idGestao, 102, 10, "Gestão", "Carlos",
                "Regra", 1, 1, dataHoraBase);
        ViewAtividadeConselhal atvA = criarViewAtividade(2, idGestao, 100, 10, "Gestão", "Ana",
                "Regra", 1, 1, dataHoraBase);
        ViewAtividadeConselhal atvB = criarViewAtividade(3, idGestao, 101, 10, "Gestão", "Bruno",
                "Regra", 1, 1, dataHoraBase);

        when(viewRepositoryMock.findForReport(anyInt(), isNull(), any(), any()))
                .thenReturn(List.of(atvC, atvA, atvB));

        // Quando
        List<AtividadeAgrupadaRelatorioDTO> resultado = service.gerarRelatorioAgrupado(
                idGestao, null, null, null, null);

        // Então
        assertThat(resultado).extracting(AtividadeAgrupadaRelatorioDTO::getConselheiro)
                .containsExactly("Ana", "Bruno", "Carlos");
    }
}