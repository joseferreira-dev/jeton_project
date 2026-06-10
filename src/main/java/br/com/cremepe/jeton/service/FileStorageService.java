package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.annotation.Auditar;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
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

    @Value("${ftp.locaweb.pass}")
    private String ftpPass;

    @Auditar(tabela = "file_storage", acao = "UPLOAD", descricao = "Upload de arquivo para o servidor FTP", capturarEstadoAnterior = false, dadosParametros = "{ 'nomeOriginal': #file.originalFilename, 'tamanho': #file.size, 'ano': #ano, 'mes': #mes }", auditarExcecao = true)
    public String salvarArquivoNoFtp(MultipartFile file, Integer ano, Integer mes) {
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = "";
        int i = originalFileName.lastIndexOf('.');
        if (i > 0) {
            extension = originalFileName.substring(i);
        }
        String fileName = UUID.randomUUID().toString() + extension;

        if (fileName.contains("..")) {
            throw new RuntimeException("Caminho inválido: " + fileName);
        }

        return executarOperacaoFtp((channelSftp) -> {
            String remoteDir = BASE_FTP_PATH + "/" + ano + "/" + mes;
            criarECarregarPasta(channelSftp, remoteDir);
            try (InputStream is = file.getInputStream()) {
                channelSftp.put(is, fileName);
            }
            log.info("Upload concluído: {} -> {}/{}", fileName, ano, mes);
            return fileName;
        });
    }

    public Resource carregarArquivo(String fileName, Integer ano, Integer mes) {
        return executarOperacaoFtp((channelSftp) -> {
            String remoteFilePath = BASE_FTP_PATH + "/" + ano + "/" + mes + "/" + fileName;
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            channelSftp.get(remoteFilePath, outputStream);
            log.info("Download concluído: {}/{} - {}", ano, mes, fileName);
            return new ByteArrayResource(outputStream.toByteArray());
        });
    }

    @Auditar(tabela = "file_storage", acao = "EXCLUIR", descricao = "Remoção de arquivo do servidor FTP", capturarEstadoAnterior = false, dadosParametros = "{ 'fileName': #fileName, 'ano': #ano, 'mes': #mes }", auditarExcecao = true)
    public void excluirArquivo(String fileName, Integer ano, Integer mes) {
        executarOperacaoFtp((channelSftp) -> {
            String remoteFilePath = BASE_FTP_PATH + "/" + ano + "/" + mes + "/" + fileName;
            try {
                channelSftp.rm(remoteFilePath);
                log.info("Arquivo removido do FTP: {}", remoteFilePath);
            } catch (Exception e) {
                log.warn("Falha ao remover arquivo (pode já ter sido excluído): {}", e.getMessage());
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
            }
        }
        if (session != null && session.isConnected()) {
            try {
                session.disconnect();
            } catch (Exception ignored) {
            }
        }
    }
}