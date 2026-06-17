package br.com.cremepe.jeton.service;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import br.com.cremepe.jeton.util.ArquivoValidator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    private static final String BASE_FTP_PATH = "jetonFiles";

    @Value("${ftp.locaweb.host}")
    private String ftpHost;

    @Value("${ftp.locaweb.port}")
    private int ftpPort;

    @Value("${ftp.locaweb.user}")
    private String ftpUser;

    // @Value("${ftp.locaweb.pass}")
    private String ftpPass;

    private final LogJetonService logJetonService;
    private final ArquivoValidator arquivoValidator;

    public FileStorageService(LogJetonService logJetonService, ArquivoValidator arquivoValidator) {
        this.logJetonService = logJetonService;
        this.arquivoValidator = arquivoValidator;
    }

    @Retryable(value = { JSchException.class,
            RuntimeException.class }, maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public String salvarArquivoNoFtp(MultipartFile file, Integer ano, Integer mes) {
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = "";
        int i = originalFileName.lastIndexOf('.');
        if (i > 0) {
            extension = originalFileName.substring(i);
        }
        String fileName = UUID.randomUUID().toString() + extension;

        arquivoValidator.validarNomeArquivo(fileName);

        log.info("Iniciando upload do arquivo {} para FTP (ano={}, mes={})", fileName, ano, mes);

        String resultado = executarOperacaoFtp((channelSftp) -> {
            String remoteDir = BASE_FTP_PATH + "/" + ano + "/" + mes;
            criarECarregarPasta(channelSftp, remoteDir);
            try (InputStream is = file.getInputStream()) {
                channelSftp.put(is, fileName);
            }
            log.info("Upload concluído: {} -> {}/{}", fileName, ano, mes);
            return fileName;
        });

        logJetonService.logUploadArquivo(originalFileName, resultado, file.getSize(), ano, mes, file.getContentType());
        return resultado;
    }

    @Retryable(value = { JSchException.class,
            RuntimeException.class }, maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public Resource carregarArquivo(String fileName, Integer ano, Integer mes) {
        log.info("Iniciando download do arquivo {} do FTP (ano={}, mes={})", fileName, ano, mes);

        return executarOperacaoFtp((channelSftp) -> {
            String remoteFilePath = BASE_FTP_PATH + "/" + ano + "/" + mes + "/" + fileName;
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            channelSftp.get(remoteFilePath, outputStream);
            log.info("Download concluído: {}/{} - {}", ano, mes, fileName);
            return new ByteArrayResource(outputStream.toByteArray());
        });
    }

    @Retryable(value = { JSchException.class,
            RuntimeException.class }, maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void excluirArquivo(String fileName, Integer ano, Integer mes) {
        log.info("Iniciando exclusão do arquivo {} do FTP (ano={}, mes={})", fileName, ano, mes);

        executarOperacaoFtp((channelSftp) -> {
            String remoteFilePath = BASE_FTP_PATH + "/" + ano + "/" + mes + "/" + fileName;
            try {
                channelSftp.rm(remoteFilePath);
                log.info("Arquivo removido do FTP: {}", remoteFilePath);
                logJetonService.logExcluirArquivo(fileName, ano, mes);
            } catch (Exception e) {
                if (e.getMessage().contains("No such file")) {
                    log.warn("Arquivo já foi removido anteriormente: {}", remoteFilePath);
                } else {
                    log.error("Falha ao remover arquivo {}: {}", remoteFilePath, e.getMessage());
                    throw new RuntimeException("Erro ao remover arquivo do FTP: " + e.getMessage(), e);
                }
            }
            return null;
        });
    }

    @FunctionalInterface
    private interface OperacaoFtp<T> {
        T execute(ChannelSftp channel) throws Exception;
    }

    private <T> T executarOperacaoFtp(OperacaoFtp<T> operation) {
        Session session = null;
        ChannelSftp channelSftp = null;
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(ftpUser, ftpHost, ftpPort);
            session.setPassword(ftpPass);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(15000);

            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect(15000);

            return operation.execute(channelSftp);
        } catch (JSchException e) {
            log.error("Erro de conexão SFTP (tentativa será repetida): {}", e.getMessage());
            throw new RuntimeException(e);
        } catch (Exception e) {
            log.error("Erro na operação SFTP", e);
            throw new RuntimeException("Falha na operação FTP: " + e.getMessage(), e);
        } finally {
            fecharConexao(channelSftp, session);
        }
    }

    private void criarECarregarPasta(ChannelSftp channelSftp, String remoteDir) throws Exception {
        String[] folders = remoteDir.split("/");
        for (String folder : folders) {
            if (folder.isEmpty())
                continue;
            try {
                channelSftp.cd(folder);
            } catch (Exception e) {
                channelSftp.mkdir(folder);
                channelSftp.cd(folder);
                log.debug("Diretório criado: {}", folder);
            }
        }
    }

    private void fecharConexao(ChannelSftp channelSftp, Session session) {
        if (channelSftp != null && channelSftp.isConnected()) {
            try {
                channelSftp.disconnect();
            } catch (Exception ignored) {
                log.debug("Erro ao desconectar SFTP (ignorado)");
            }
        }
        if (session != null && session.isConnected()) {
            try {
                session.disconnect();
            } catch (Exception ignored) {
                log.debug("Erro ao desconectar sessão (ignorado)");
            }
        }
    }
}