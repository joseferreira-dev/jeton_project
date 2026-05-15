package br.com.cremepe.jeton.servico;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
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

    @Value("${ftp.locaweb.host}") private String ftpHost;
    @Value("${ftp.locaweb.port}") private int ftpPort;
    @Value("${ftp.locaweb.user}") private String ftpUser;
    @Value("${ftp.locaweb.pass}") private String ftpPass;

    // =========================================================================
    // UPLOAD STATELESS (Direto para Locaweb, sem tocar no disco)
    // =========================================================================
    public String storeFileToFtp(MultipartFile file, Integer ano, Integer mes) {
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = "";
        int i = originalFileName.lastIndexOf('.');
        if (i > 0) {
            extension = originalFileName.substring(i);
        }
        
        // Nome padronizado: UUID curto + extensão
        String fileName = UUID.randomUUID().toString() + extension;

        Session session = null;
        ChannelSftp channelSftp = null;
        try {
            if(fileName.contains("..")) throw new RuntimeException("Caminho inválido: " + fileName);

            JSch jsch = new JSch();
            session = jsch.getSession(ftpUser, ftpHost, ftpPort);
            session.setPassword(ftpPass);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(15000);

            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect(15000);

            String remoteDir = "jetonFiles/" + ano + "/" + mes;
            criarECarregarPasta(channelSftp, remoteDir);

            // Grava DIRETAMENTE da memória para a Locaweb
            try (InputStream is = file.getInputStream()) {
                channelSftp.put(is, fileName);
            }
            
            System.out.println("Upload Stateless concluído: " + fileName);
            return fileName;
            
        } catch (Exception ex) {
            throw new RuntimeException("Erro ao enviar o ficheiro " + fileName + " para o FTP.", ex);
        } finally {
            try { if (channelSftp != null && channelSftp.isConnected()) channelSftp.disconnect(); } catch (Exception ignored) {}
            try { if (session != null && session.isConnected()) session.disconnect(); } catch (Exception ignored) {}
        }
    }

    private void criarECarregarPasta(ChannelSftp channelSftp, String remoteDir) {
        String[] folders = remoteDir.split("/");
        for (String folder : folders) {
            if (folder.isEmpty()) continue;
            try {
                channelSftp.cd(folder);
            } catch (Exception e) { 
                try {
                    channelSftp.mkdir(folder);
                    channelSftp.cd(folder);
                } catch (Exception ex) {
                    throw new RuntimeException("Falha ao criar diretório no FTP: " + folder, ex);
                }
            }
        }
    }

    // =========================================================================
    // DOWNLOAD STATELESS (Lê da Locaweb para a Memória, sem salvar no disco)
    // =========================================================================
    public Resource loadFileAsResource(String fileName, Integer ano, Integer mes) {
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

            String remoteFilePath = "jetonFiles/" + ano + "/" + mes + "/" + fileName;

            // Faz o download da Locaweb para um Array de Bytes na RAM
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            channelSftp.get(remoteFilePath, outputStream);

            // Converte os Bytes para um Recurso que o Spring consegue devolver ao navegador
            System.out.println("Download Stateless concluído: " + fileName);
            return new ByteArrayResource(outputStream.toByteArray());

        } catch (Exception e) {
            throw new RuntimeException("Ficheiro indisponível no FTP da Locaweb: " + e.getMessage());
        } finally {
            try { if (channelSftp != null && channelSftp.isConnected()) channelSftp.disconnect(); } catch (Exception ignored) {}
            try { if (session != null && session.isConnected()) session.disconnect(); } catch (Exception ignored) {}
        }
    }

    // =========================================================================
    // DELETE STATELESS (Remove direto da Locaweb)
    // =========================================================================
    public void deleteFile(String fileName, Integer ano, Integer mes) {
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

            String remoteFilePath = "jetonFiles/" + ano + "/" + mes + "/" + fileName;
            channelSftp.rm(remoteFilePath);
            System.out.println("Exclusão Stateless concluída: " + fileName);
            
        } catch (Exception e) {
            System.err.println("Aviso: Falha ao remover arquivo do FTP (pode já ter sido excluído): " + e.getMessage());
        } finally {
            try { if (channelSftp != null && channelSftp.isConnected()) channelSftp.disconnect(); } catch (Exception ignored) {}
            try { if (session != null && session.isConnected()) session.disconnect(); } catch (Exception ignored) {}
        }
    }
}