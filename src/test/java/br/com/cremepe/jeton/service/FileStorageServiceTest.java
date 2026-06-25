package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.util.ArquivoValidator;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do serviço de armazenamento de arquivos (FileStorageService)")
class FileStorageServiceTest {

    @Mock
    private LogJetonService logJetonServiceMock;

    @Mock
    private ArquivoValidator arquivoValidatorMock;

    @Spy
    @InjectMocks
    private FileStorageService service;

    private JSch jschMock;
    private Session sessionMock;
    private ChannelSftp channelSftpMock;

    @BeforeEach
    void setUp() throws JSchException {
        ReflectionTestUtils.setField(service, "ftpHost", "localhost");
        ReflectionTestUtils.setField(service, "ftpPort", 22);
        ReflectionTestUtils.setField(service, "ftpUser", "user");
        ReflectionTestUtils.setField(service, "ftpPass", "pass");

        jschMock = mock(JSch.class);
        sessionMock = mock(Session.class);
        channelSftpMock = mock(ChannelSftp.class);

        // ✅ Todos os stubs com lenient()
        lenient().when(jschMock.getSession(anyString(), anyString(), anyInt())).thenReturn(sessionMock);
        lenient().when(sessionMock.openChannel("sftp")).thenReturn(channelSftpMock);
        lenient().doNothing().when(sessionMock).connect(anyInt());
        lenient().doNothing().when(channelSftpMock).connect(anyInt());
    }

    // ========== SALVAR ARQUIVO ==========

    @Test
    @DisplayName("deve salvar arquivo no FTP com sucesso")
    void deveSalvarArquivoNoFtpComSucesso() throws Exception {
        MultipartFile fileMock = mock(MultipartFile.class);
        String originalName = "documento.pdf";
        String contentType = "application/pdf";
        long fileSize = 1024L;
        InputStream inputStream = new ByteArrayInputStream("conteudo".getBytes());

        when(fileMock.getOriginalFilename()).thenReturn(originalName);
        when(fileMock.getContentType()).thenReturn(contentType);
        when(fileMock.getSize()).thenReturn(fileSize);
        when(fileMock.getInputStream()).thenReturn(inputStream);

        doNothing().when(arquivoValidatorMock).validarNomeArquivo(anyString());
        doReturn(jschMock).when(service).criarJSch();

        // ✅ Stub com SftpException para o cd
        doThrow(new SftpException(2, "No such file")).doNothing().when(channelSftpMock).cd(anyString());
        doNothing().when(channelSftpMock).mkdir(anyString());
        doNothing().when(channelSftpMock).put(any(InputStream.class), anyString());

        String nomeGerado = service.salvarArquivoNoFtp(fileMock, 2026, 6);

        assertThat(nomeGerado).matches("^[a-f0-9-]+\\.pdf$");
        verify(channelSftpMock, atLeastOnce()).cd(anyString());
        verify(channelSftpMock, atLeastOnce()).mkdir(anyString());
        verify(channelSftpMock).put(any(InputStream.class), eq(nomeGerado));

        verify(logJetonServiceMock).logUploadArquivo(
                eq(originalName), eq(nomeGerado), eq(fileSize),
                eq(2026), eq(6), eq(contentType));
    }

    @Test
    @DisplayName("deve lançar exceção ao salvar arquivo com nome inválido")
    void deveLancarExcecaoAoSavarArquivoComNomeInvalido() throws Exception {
        MultipartFile fileMock = mock(MultipartFile.class);
        when(fileMock.getOriginalFilename()).thenReturn("documento.pdf");

        doThrow(new RuntimeException("Nome inválido"))
                .when(arquivoValidatorMock).validarNomeArquivo(anyString());

        assertThatThrownBy(() -> service.salvarArquivoNoFtp(fileMock, 2026, 6))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Nome inválido");

        verify(logJetonServiceMock, never()).logUploadArquivo(any(), any(), anyLong(), anyInt(), anyInt(), anyString());
        verify(channelSftpMock, never()).put(any(InputStream.class), anyString());
    }

    // ========== CARREGAR ARQUIVO ==========

    @Test
    @DisplayName("deve carregar arquivo do FTP com sucesso")
    void deveCarregarArquivoComSucesso() throws Exception {
        String fileName = "abc-123.pdf";
        byte[] conteudo = "conteudo do arquivo".getBytes();

        doReturn(jschMock).when(service).criarJSch();

        doAnswer(inv -> {
            ByteArrayOutputStream out = inv.getArgument(1);
            out.write(conteudo);
            return null;
        }).when(channelSftpMock).get(anyString(), any(ByteArrayOutputStream.class));

        Resource resource = service.carregarArquivo(fileName, 2026, 6);

        assertThat(resource).isInstanceOf(ByteArrayResource.class);
        assertThat(((ByteArrayResource) resource).getByteArray()).isEqualTo(conteudo);
        verify(channelSftpMock).get(eq("jetonFiles/2026/6/" + fileName), any(ByteArrayOutputStream.class));
    }

    @Test
    @DisplayName("deve lançar exceção ao carregar arquivo inexistente")
    void deveLancarExcecaoAoCarregarArquivoInexistente() throws Exception {
        doReturn(jschMock).when(service).criarJSch();
        // ✅ SftpException em vez de JSchException
        doThrow(new SftpException(2, "No such file"))
                .when(channelSftpMock).get(anyString(), any(ByteArrayOutputStream.class));

        assertThatThrownBy(() -> service.carregarArquivo("inexistente.pdf", 2026, 6))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Falha na operação FTP");
    }

    // ========== EXCLUIR ARQUIVO ==========

    @Test
    @DisplayName("deve excluir arquivo do FTP com sucesso")
    void deveExcluirArquivoComSucesso() throws Exception {
        doReturn(jschMock).when(service).criarJSch();
        doNothing().when(channelSftpMock).rm(anyString());

        service.excluirArquivo("file.pdf", 2026, 6);

        verify(channelSftpMock).rm("jetonFiles/2026/6/file.pdf");
        verify(logJetonServiceMock).logExcluirArquivo("file.pdf", 2026, 6);
    }

    @Test
    @DisplayName("deve registrar apenas warn ao excluir arquivo inexistente")
    void deveRegistrarApenasWarnAoExcluirArquivoInexistente() throws Exception {
        doReturn(jschMock).when(service).criarJSch();
        // ✅ SftpException em vez de RuntimeException
        doThrow(new SftpException(2, "No such file"))
                .when(channelSftpMock).rm(anyString());

        service.excluirArquivo("inexistente.pdf", 2026, 6);

        verify(channelSftpMock).rm(anyString());
        verify(logJetonServiceMock, never()).logExcluirArquivo(anyString(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("deve lançar exceção ao excluir arquivo com erro FTP genérico")
    void deveLancarExcecaoAoExcluirArquivoComErroFtp() throws Exception {
        doReturn(jschMock).when(service).criarJSch();
        // ✅ RuntimeException ou SftpException (mas a mensagem não contém "No such
        // file")
        doThrow(new RuntimeException("Permission denied"))
                .when(channelSftpMock).rm(anyString());

        assertThatThrownBy(() -> service.excluirArquivo("erro.pdf", 2026, 6))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Erro ao remover arquivo do FTP");
    }

    // ========== FALHA DE CONEXÃO ==========

    @Test
    @DisplayName("deve lançar exceção ao falhar conexão SFTP")
    void deveLancarExcecaoAoFalharConexaoSftp() throws Exception {
        MultipartFile fileMock = mock(MultipartFile.class);
        when(fileMock.getOriginalFilename()).thenReturn("teste.pdf");

        doReturn(jschMock).when(service).criarJSch();
        // ✅ JSchException no connect
        doThrow(new JSchException("Connection refused"))
                .when(sessionMock).connect(anyInt());

        // ✅ A exceção lançada é um RuntimeException com a mensagem do JSchException
        assertThatThrownBy(() -> service.salvarArquivoNoFtp(fileMock, 2026, 6))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Connection refused")
                .hasCauseInstanceOf(JSchException.class);
    }
}