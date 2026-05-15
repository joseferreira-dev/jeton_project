package br.com.cremepe.jeton.servico;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;

    // Lendo as credenciais de forma segura do application.properties
    @Value("${ftp.locaweb.host}") private String ftpHost;
    @Value("${ftp.locaweb.port}") private int ftpPort;
    @Value("${ftp.locaweb.user}") private String ftpUser;
    @Value("${ftp.locaweb.pass}") private String ftpPass;

    public FileStorageService() {
        this.fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Não foi possível criar o diretório de cache local.", ex);
        }
    }

    // =========================================================================
    // UPLOAD DIRETAMENTE PARA O FTP (Locaweb)
    // =========================================================================
    public String storeFileToFtp(MultipartFile file, Integer ano, Integer mes) {
        // 1. Extrai a extensão original
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = "";
        int i = originalFileName.lastIndexOf('.');
        if (i > 0) {
            extension = originalFileName.substring(i);
        }

        // 2. O nome no FTP será apenas o UUID + extensão (Garante menos de 70 caracteres)
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

            // Caminho da pasta na Locaweb (ex: jetonFiles/2026/5)
            String remoteDir = "jetonFiles/" + ano + "/" + mes;

            // Navega até a pasta, criando-a se ela não existir ainda
            criarECarregarPasta(channelSftp, remoteDir);

            // Envia o ficheiro em bytes diretamente para o servidor da Locaweb
            try (InputStream is = file.getInputStream()) {
                channelSftp.put(is, fileName);
            }
            
            System.out.println("Ficheiro enviado com sucesso para a Locaweb: " + fileName);
            return fileName;
            
        } catch (Exception ex) {
            throw new RuntimeException("Erro ao enviar o ficheiro " + fileName + " diretamente para o FTP.", ex);
        } finally {
            try { if (channelSftp != null && channelSftp.isConnected()) channelSftp.disconnect(); } catch (Exception ignored) {}
            try { if (session != null && session.isConnected()) session.disconnect(); } catch (Exception ignored) {}
        }
    }

    // Método utilitário CORRIGIDO (Navegação Relativa)
    private void criarECarregarPasta(ChannelSftp channelSftp, String remoteDir) {
        String[] folders = remoteDir.split("/");
        
        for (String folder : folders) {
            if (folder.isEmpty()) continue;
            
            try {
                // Tenta entrar na pasta diretamente (Caminho relativo)
                channelSftp.cd(folder);
            } catch (Exception e) { 
                // Se der erro (a pasta não existe), criamos e depois entramos
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
    // DOWNLOAD (Cache Local com Fallback no FTP)
    // =========================================================================
    public Resource loadFileAsResource(String fileName, Integer ano, Integer mes) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            
            if(resource.exists()) {
                return resource; 
            } else {
                System.out.println("Ficheiro " + fileName + " não encontrado em cache. Buscando no FTP da Locaweb...");
                boolean sucessoFtp = descarregarDoFtpLegado(fileName, ano, mes, filePath);
                
                if (sucessoFtp) {
                    return new UrlResource(filePath.toUri());
                } else {
                    throw new RuntimeException("Ficheiro indisponível no FTP da Locaweb.");
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("Erro ao carregar o ficheiro: " + fileName, ex);
        }
    }

    private boolean descarregarDoFtpLegado(String fileName, Integer ano, Integer mes, Path targetLocalPath) {
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

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            channelSftp.get(remoteFilePath, outputStream);

            Files.write(targetLocalPath, outputStream.toByteArray());
            return true;
        } catch (Exception e) {
            System.err.println("Falha ao comunicar com FTP: " + e.getMessage());
            return false;
        } finally {
            try { if (channelSftp != null && channelSftp.isConnected()) channelSftp.disconnect(); } catch (Exception ignored) {}
            try { if (session != null && session.isConnected()) session.disconnect(); } catch (Exception ignored) {}
        }
    }

    // =========================================================================
    // EXCLUSÃO DE FICHEIRO (Local e Locaweb)
    // =========================================================================
    public void deleteFile(String fileName, Integer ano, Integer mes) {
        // 1. Tenta deletar da cache local (se existir)
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Files.deleteIfExists(filePath);
        } catch (Exception e) {
            System.err.println("Aviso: Não foi possível remover da cache local: " + e.getMessage());
        }

        // 2. Conecta no FTP da Locaweb e remove o arquivo original
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
            System.out.println("Arquivo " + fileName + " removido da Locaweb com sucesso!");
            
        } catch (Exception e) {
            System.err.println("Aviso: Falha ao remover arquivo do FTP (pode já ter sido excluído): " + e.getMessage());
        } finally {
            try { if (channelSftp != null && channelSftp.isConnected()) channelSftp.disconnect(); } catch (Exception ignored) {}
            try { if (session != null && session.isConnected()) session.disconnect(); } catch (Exception ignored) {}
        }
    }
}