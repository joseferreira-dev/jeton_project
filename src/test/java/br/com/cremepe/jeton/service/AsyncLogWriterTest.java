package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.domain.LogJeton;
import br.com.cremepe.jeton.domain.Usuario;
import br.com.cremepe.jeton.repository.LogJetonRepository;
import br.com.cremepe.jeton.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do serviço AsyncLogWriter")
class AsyncLogWriterTest {

    @Mock
    private LogJetonRepository logRepositoryMock;

    @Mock
    private UsuarioRepository usuarioRepositoryMock;

    @InjectMocks
    private AsyncLogWriter asyncLogWriter;

    @Test
    @DisplayName("deve registrar um log com sucesso quando o usuário existe")
    void deveRegistrarLogComSucesso() {
        // Dado
        Integer idUsuario = 42;
        String nomeTabela = "atividade_conselhal";
        String textoLog = "{\"acao\":\"CRIAR\"}";

        Usuario usuarioMock = mock(Usuario.class);
        when(usuarioRepositoryMock.getReferenceById(idUsuario)).thenReturn(usuarioMock);

        // Quando
        asyncLogWriter.writeLog(nomeTabela, idUsuario, textoLog);

        // Então
        verify(usuarioRepositoryMock).getReferenceById(idUsuario);
        verify(logRepositoryMock).save(any(LogJeton.class));

        ArgumentCaptor<LogJeton> captor = ArgumentCaptor.forClass(LogJeton.class);
        verify(logRepositoryMock).save(captor.capture());

        LogJeton logSalvo = captor.getValue();
        assertThat(logSalvo.getNomeTabela()).isEqualTo(nomeTabela);
        assertThat(logSalvo.getUsuario()).isSameAs(usuarioMock);
        assertThat(logSalvo.getTextoLog()).isEqualTo(textoLog);
        assertThat(logSalvo.getDataHoraLog()).isNotNull();
    }

    @Test
    @DisplayName("deve tratar exceção ao buscar usuário e não propagá-la")
    void deveTratarExcecaoAoBuscarUsuario() {
        // Dado
        Integer idUsuario = 999;
        String nomeTabela = "login";
        String textoLog = "{\"acao\":\"LOGOUT\"}";

        when(usuarioRepositoryMock.getReferenceById(idUsuario))
                .thenThrow(new RuntimeException("Usuário não encontrado"));

        // Quando / Então - não deve lançar exceção
        asyncLogWriter.writeLog(nomeTabela, idUsuario, textoLog);

        verify(usuarioRepositoryMock).getReferenceById(idUsuario);
        verify(logRepositoryMock, never()).save(any(LogJeton.class));
    }

    @Test
    @DisplayName("deve registrar log mesmo quando o texto é muito longo")
    void deveRegistrarLogComTextoLongo() {
        // Dado
        Integer idUsuario = 1;
        String nomeTabela = "regras";
        String textoLongo = "a".repeat(10_000); // texto de 10 mil caracteres

        Usuario usuarioMock = mock(Usuario.class);
        when(usuarioRepositoryMock.getReferenceById(idUsuario)).thenReturn(usuarioMock);

        // Quando
        asyncLogWriter.writeLog(nomeTabela, idUsuario, textoLongo);

        // Então
        ArgumentCaptor<LogJeton> captor = ArgumentCaptor.forClass(LogJeton.class);
        verify(logRepositoryMock).save(captor.capture());
        assertThat(captor.getValue().getTextoLog()).isEqualTo(textoLongo);
    }
}