package br.com.cremepe.jeton.service;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import br.com.cremepe.jeton.annotation.Auditar;

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

    // =========================================================================
    // UPLOAD
    // =========================================================================
    @Auditar(tabela = "file_storage", acao = "UPLOAD", descricao = "Upload de arquivo para o servidor FTP", capturarEstadoAnterior = false, dadosParametros = "{ 'nomeOriginal': #file.originalFilename, 'tamanho': #file.size, 'ano': #ano, 'mes': #mes }", auditarExcecao = true)
    public String salvarArquivoNoFtp(MultipartFile file, Integer ano, Integer mes) {
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = "";
        int i = originalFileName.lastIndexOf('.');
        if (i > 0) {
            extension = originalFileName.substring(i);
        }
        String fileName = UUID.randomUUID().toString() + extension;

        Session session = null;
        ChannelSftp channelSftp = null;
        try {
            if (fileName.contains("..")) {
                throw new RuntimeException("Caminho inválido: " + fileName);
            }

            JSch jsch = new JSch();
            session = jsch.getSession(ftpUser, ftpHost, ftpPort);
            session.setPassword(ftpPass);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(15000);
            log.debug("Conectado ao FTP para upload");

            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect(15000);

            String remoteDir = BASE_FTP_PATH + "/" + ano + "/" + mes;
            criarECarregarPasta(channelSftp, remoteDir);

            try (InputStream is = file.getInputStream()) {
                channelSftp.put(is, fileName);
            }

            log.info("Upload concluído: {} -> {}/{}", fileName, ano, mes);
            return fileName;

        } catch (Exception ex) {
            log.error("Erro no upload do arquivo {}: {}", fileName, ex.getMessage());
            throw new RuntimeException("Erro ao enviar o ficheiro para o FTP: " + ex.getMessage(), ex);
        } finally {
            fecharConexao(channelSftp, session);
        }
    }

    private void criarECarregarPasta(ChannelSftp channelSftp, String remoteDir) {
        String[] folders = remoteDir.split("/");
        for (String folder : folders) {
            if (folder.isEmpty())
                continue;
            try {
                channelSftp.cd(folder);
            } catch (Exception e) {
                try {
                    channelSftp.mkdir(folder);
                    channelSftp.cd(folder);
                    log.debug("Diretório criado: {}", folder);
                } catch (Exception ex) {
                    throw new RuntimeException("Falha ao criar diretório no FTP: " + folder, ex);
                }
            }
        }
    }

    // =========================================================================
    // DOWNLOAD
    // =========================================================================
    public Resource carregarArquivo(String fileName, Integer ano, Integer mes) {
        Session session = null;
        ChannelSftp channelSftp = null;
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(ftpUser, ftpHost, ftpPort);
            session.setPassword(ftpPass);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(10000);
            log.debug("Conectado ao FTP para download");

            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect(10000);

            String remoteFilePath = BASE_FTP_PATH + "/" + ano + "/" + mes + "/" + fileName;
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            channelSftp.get(remoteFilePath, outputStream);

            log.info("Download concluído: {}/{}", ano, mes, fileName);
            return new ByteArrayResource(outputStream.toByteArray());

        } catch (Exception e) {
            log.error("Erro no download do arquivo {}: {}", fileName, e.getMessage());
            throw new RuntimeException("Ficheiro indisponível no FTP: " + e.getMessage(), e);
        } finally {
            fecharConexao(channelSftp, session);
        }
    }

    // =========================================================================
    // EXCLUIR
    // =========================================================================
    @Auditar(tabela = "file_storage", acao = "EXCLUIR", descricao = "Remoção de arquivo do servidor FTP", capturarEstadoAnterior = false, dadosParametros = "{ 'fileName': #fileName, 'ano': #ano, 'mes': #mes }", auditarExcecao = true)
    public void excluirArquivo(String fileName, Integer ano, Integer mes) {
        Session session = null;
        ChannelSftp channelSftp = null;
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(ftpUser, ftpHost, ftpPort);
            session.setPassword(ftpPass);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(10000);

            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect(10000);

            String remoteFilePath = BASE_FTP_PATH + "/" + ano + "/" + mes + "/" + fileName;
            channelSftp.rm(remoteFilePath);
            log.info("Arquivo removido do FTP: {}", remoteFilePath);

        } catch (Exception e) {
            log.warn("Falha ao remover arquivo do FTP (pode já ter sido excluído): {}", e.getMessage());
            // Não lança exceção, apenas log
        } finally {
            fecharConexao(channelSftp, session);
        }
    }

    // =========================================================================
    // MÉTODOS AUXILIARES
    // =========================================================================
    private void fecharConexao(ChannelSftp channelSftp, Session session) {
        try {
            if (channelSftp != null && channelSftp.isConnected()) {
                channelSftp.disconnect();
            }
        } catch (Exception ignored) {
        }
        try {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        } catch (Exception ignored) {
        }
    }
}