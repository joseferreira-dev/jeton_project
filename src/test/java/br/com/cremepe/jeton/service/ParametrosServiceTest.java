package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.domain.Parametros;
import br.com.cremepe.jeton.repository.ParametrosRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do serviço de Parâmetros (ParametrosService)")
class ParametrosServiceTest {

    @Mock
    private ParametrosRepository repositoryMock;

    @Mock
    private LogJetonService logJetonServiceMock;

    @InjectMocks
    private ParametrosService service;

    // ========== TESTES ==========

    @Test
    @DisplayName("deve retornar false quando o sistema não está bloqueado")
    void deveRetornarFalseQuandoSistemaNaoBloqueado() {
        // Dado
        Parametros parametros = new Parametros();
        parametros.setBloqueaSistema("N");
        when(repositoryMock.findById(1)).thenReturn(Optional.of(parametros));

        // Quando
        boolean bloqueado = service.isSistemaBloqueado();

        // Então
        assertThat(bloqueado).isFalse();
        verify(repositoryMock).findById(1);
    }

    @Test
    @DisplayName("deve retornar true quando o sistema está bloqueado")
    void deveRetornarTrueQuandoSistemaBloqueado() {
        // Dado
        Parametros parametros = new Parametros();
        parametros.setBloqueaSistema("S");
        when(repositoryMock.findById(1)).thenReturn(Optional.of(parametros));

        // Quando
        boolean bloqueado = service.isSistemaBloqueado();

        // Então
        assertThat(bloqueado).isTrue();
        verify(repositoryMock).findById(1);
    }

    @Test
    @DisplayName("deve retornar o status atual 'N'")
    void deveRetornarStatusAtual() {
        // Dado
        Parametros parametros = new Parametros();
        parametros.setBloqueaSistema("N");
        when(repositoryMock.findById(1)).thenReturn(Optional.of(parametros));

        // Quando
        String status = service.obterStatus();

        // Então
        assertThat(status).isEqualTo("N");
        verify(repositoryMock).findById(1);
    }

    @Test
    @DisplayName("deve criar novo registro com bloqueio N se não existir parâmetros")
    void deveCriarNovoRegistroQuandoNaoExistir() {
        // Dado
        when(repositoryMock.findById(1)).thenReturn(Optional.empty());

        // Mock do save
        when(repositoryMock.save(any(Parametros.class)))
                .thenAnswer(inv -> {
                    Parametros p = inv.getArgument(0);
                    p.setId(1);
                    return p;
                });

        // Quando
        boolean bloqueado = service.isSistemaBloqueado();

        // Então
        assertThat(bloqueado).isFalse();

        ArgumentCaptor<Parametros> captor = ArgumentCaptor.forClass(Parametros.class);
        verify(repositoryMock).save(captor.capture());

        Parametros salvo = captor.getValue();
        assertThat(salvo.getBloqueaSistema()).isEqualTo("N");
        assertThat(salvo.getId()).isEqualTo(1);
    }

    @Test
    @DisplayName("deve alternar bloqueio de N para S e registrar log")
    void deveAlternarBloqueioDeNParaS() {
        // Dado
        Parametros parametros = new Parametros();
        parametros.setId(1);
        parametros.setBloqueaSistema("N");

        when(repositoryMock.findById(1)).thenReturn(Optional.of(parametros));

        // Mock do save
        when(repositoryMock.save(any(Parametros.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Quando
        service.alternarBloqueio();

        // Então
        assertThat(parametros.getBloqueaSistema()).isEqualTo("S");

        verify(repositoryMock).save(parametros);
        verify(logJetonServiceMock).logBloqueioAlternado("N", "S");
    }

    @Test
    @DisplayName("deve alternar bloqueio de S para N e registrar log")
    void deveAlternarBloqueioDeSParaN() {
        // Dado
        Parametros parametros = new Parametros();
        parametros.setId(1);
        parametros.setBloqueaSistema("S");

        when(repositoryMock.findById(1)).thenReturn(Optional.of(parametros));

        when(repositoryMock.save(any(Parametros.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Quando
        service.alternarBloqueio();

        // Então
        assertThat(parametros.getBloqueaSistema()).isEqualTo("N");

        verify(repositoryMock).save(parametros);
        verify(logJetonServiceMock).logBloqueioAlternado("S", "N");
    }

    @Test
    @DisplayName("deve criar novo registro ao alternar bloqueio se não existir parâmetros")
    void deveCriarNovoRegistroAoAlternarSeNaoExistir() {
        // Dado: nenhum registro existe
        when(repositoryMock.findById(1)).thenReturn(Optional.empty());

        // Mock do save: retorna o próprio objeto com ID 1
        when(repositoryMock.save(any(Parametros.class)))
                .thenAnswer(inv -> {
                    Parametros p = inv.getArgument(0);
                    p.setId(1);
                    return p;
                });

        when(repositoryMock.findById(1))
                .thenReturn(Optional.empty()) // primeira chamada
                .thenAnswer(inv -> {
                    // segunda chamada: retorna o registro salvo com "S"
                    Parametros p = new Parametros();
                    p.setId(1);
                    p.setBloqueaSistema("S");
                    return Optional.of(p);
                });

        // Quando
        String status = service.alternarBloqueio();

        // Então
        assertThat(status).isEqualTo("BLOQUEADO");

        // Verifica que o save foi chamado
        verify(repositoryMock, atLeastOnce()).save(any(Parametros.class));
        verify(logJetonServiceMock).logBloqueioAlternado("N", "S");

        // Verifica que o status agora é "S"
        assertThat(service.isSistemaBloqueado()).isTrue();
        assertThat(service.obterStatus()).isEqualTo("S");
    }
}