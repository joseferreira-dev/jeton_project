package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.domain.AtividadeConselhal;
import br.com.cremepe.jeton.repository.AtividadeConselhalRepository;
import br.com.cremepe.jeton.repository.ComprovanteRepository;
import br.com.cremepe.jeton.repository.ConselheiroRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do serviço de Dashboard")
class DashboardServiceTest {

    @Mock
    private AtividadeConselhalRepository atividadeRepositoryMock;

    @Mock
    private ConselheiroRepository conselheiroRepositoryMock;

    @Mock
    private ComprovanteRepository comprovanteRepositoryMock;

    @InjectMocks
    private DashboardService service;

    // ========== TESTES ==========

    @Test
    @DisplayName("deve retornar o total de atividades pendentes")
    void deveRetornarTotalAtividadesPendentes() {
        // Dado
        long totalEsperado = 15L;
        when(atividadeRepositoryMock.countByInSituacao("P")).thenReturn(totalEsperado);

        // Quando
        long resultado = service.getTotalAtividadesPendentes();

        // Então
        assertThat(resultado).isEqualTo(totalEsperado);
        verify(atividadeRepositoryMock).countByInSituacao("P");
    }

    @Test
    @DisplayName("deve retornar o total de conselheiros ativos")
    void deveRetornarTotalConselheirosAtivos() {
        // Dado
        long totalEsperado = 42L;
        when(conselheiroRepositoryMock.countByInSituacao("A")).thenReturn(totalEsperado);

        // Quando
        long resultado = service.getTotalConselheirosAtivos();

        // Então
        assertThat(resultado).isEqualTo(totalEsperado);
        verify(conselheiroRepositoryMock).countByInSituacao("A");
    }

    @Test
    @DisplayName("deve retornar o total de atividades do mês atual")
    void deveRetornarTotalAtividadesDoMes() {
        // Dado
        LocalDate hoje = LocalDate.now();
        int mesAtual = hoje.getMonthValue();
        int anoAtual = hoje.getYear();
        long totalEsperado = 8L;

        when(atividadeRepositoryMock.countAtividadesDoMes(mesAtual, anoAtual))
                .thenReturn(totalEsperado);

        // Quando
        long resultado = service.getTotalAtividadesDoMes();

        // Então
        assertThat(resultado).isEqualTo(totalEsperado);
        verify(atividadeRepositoryMock).countAtividadesDoMes(mesAtual, anoAtual);
    }

    @Test
    @DisplayName("deve retornar zero quando não houver atividades no mês")
    void deveRetornarZeroQuandoNaoHouverAtividadesNoMes() {
        // Dado
        LocalDate hoje = LocalDate.now();
        int mesAtual = hoje.getMonthValue();
        int anoAtual = hoje.getYear();

        when(atividadeRepositoryMock.countAtividadesDoMes(mesAtual, anoAtual))
                .thenReturn(0L);

        // Quando
        long resultado = service.getTotalAtividadesDoMes();

        // Então
        assertThat(resultado).isZero();
        verify(atividadeRepositoryMock).countAtividadesDoMes(mesAtual, anoAtual);
    }

    @Test
    @DisplayName("deve retornar o total de comprovantes salvos")
    void deveRetornarTotalComprovantes() {
        // Dado
        long totalEsperado = 120L;
        when(comprovanteRepositoryMock.count()).thenReturn(totalEsperado);

        // Quando
        long resultado = service.getTotalComprovantes();

        // Então
        assertThat(resultado).isEqualTo(totalEsperado);
        verify(comprovanteRepositoryMock).count();
    }

    @Test
    @DisplayName("deve retornar a lista das 5 últimas atividades registradas")
    void deveRetornarUltimasAtividades() {
        // Dado
        int limit = 5;
        List<AtividadeConselhal> listaEsperada = List.of(
                mock(AtividadeConselhal.class),
                mock(AtividadeConselhal.class));

        when(atividadeRepositoryMock.findTop5ByOrderByDataHoraRegistroDesc())
                .thenReturn(listaEsperada);

        // Quando
        List<AtividadeConselhal> resultado = service.getUltimasAtividades(limit);

        // Então
        assertThat(resultado).hasSize(2);
        assertThat(resultado).isEqualTo(listaEsperada);

        // Verifica que o método do repositório foi chamado (ignora o parâmetro limit,
        // pois o repositório usa hardcoded Top5)
        verify(atividadeRepositoryMock).findTop5ByOrderByDataHoraRegistroDesc();
    }

    @Test
    @DisplayName("deve retornar lista vazia quando não houver atividades recentes")
    void deveRetornarListaVaziaQuandoNaoHouveAtividadesRecentes() {
        // Dado
        int limit = 5;
        when(atividadeRepositoryMock.findTop5ByOrderByDataHoraRegistroDesc())
                .thenReturn(List.of());

        // Quando
        List<AtividadeConselhal> resultado = service.getUltimasAtividades(limit);

        // Então
        assertThat(resultado).isEmpty();
        verify(atividadeRepositoryMock).findTop5ByOrderByDataHoraRegistroDesc();
    }
}