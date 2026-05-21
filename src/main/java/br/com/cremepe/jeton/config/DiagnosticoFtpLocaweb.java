package br.com.cremepe.jeton.config;

import com.jcraft.jsch.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Vector;

/**
 * Utilitário de diagnóstico focado exclusivamente na listagem de arquivos
 * contidos dentro da pasta de armazenamento 'jetonFiles' no servidor SFTP.
 */
@Component
public class DiagnosticoFtpLocaweb implements CommandLineRunner {

    @Value("${ftp.locaweb.host}")
    private String ftpHost;

    @Value("${ftp.locaweb.port}")
    private int ftpPort;

    @Value("${ftp.locaweb.user}")
    private String ftpUser;

    @Value("${ftp.locaweb.pass}")
    private String ftpPass;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("\n=========================================================================");
        System.out.println(" AUDITORIA REMOTA: LISTAGEM DE ARQUIVOS NA PASTA 'jetonFiles'");
        System.out.println("=========================================================================\n");

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

            // Define o caminho alvo estrito da aplicação
            String pastaAlvo = "jetonFiles";

            System.out.println("[SFTP] Conectado ao servidor da Locaweb.");
            System.out.println("[SFTP] Varrendo o diretório remoto: " + pastaAlvo + "/");
            System.out.println("-------------------------------------------------------------------------");
            System.out.println(" COMPROVANTES LOCALIZADOS NO SERVIDOR:");
            System.out.println("-------------------------------------------------------------------------");

            // Executa a listagem focada apenas na pasta jetonFiles
            listarApenasArquivosJetonFiles(channelSftp, pastaAlvo);

        } catch (JSchException e) {
            System.err.println("[ERRO SSH] Falha na conexão segura com o host: " +
                    e.getMessage());
        } catch (SftpException e) {
            System.err.println("[AVISO SFTP] A pasta '" + ftpUser + "/jetonFiles' ainda não existe ou está vazia: "
                    + e.getMessage());
        } finally {
            if (channelSftp != null && channelSftp.isConnected()) {
                channelSftp.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
                System.out.println("\n[SFTP] Sessão remota encerrada com segurança.");
            }
        }

        System.out.println("\n=========================================================================");
        System.out.println(" FIM DA LEITURA DE ARQUIVOS DA HOSPEDAGEM");
        System.out.println("=========================================================================\n");
    }

    /**
     * Varre as subpastas de forma recursiva, exibindo no terminal apenas os
     * arquivos físicos.
     */
    @SuppressWarnings("unchecked")
    private void listarApenasArquivosJetonFiles(ChannelSftp channelSftp, String caminhoCvo) throws SftpException {
        Vector<ChannelSftp.LsEntry> itens = channelSftp.ls(caminhoCvo);

        if (itens == null || itens.isEmpty()) {
            return;
        }

        for (ChannelSftp.LsEntry item : itens) {
            String nomeItem = item.getFilename();

            // Ignora ponteiros circulares de navegação do Linux
            if (".".equals(nomeItem) || "..".equals(nomeItem)) {
                continue;
            }

            // Monta o caminho completo do item atual
            String caminhoCompleto = caminhoCvo.endsWith("/") ? caminhoCvo + nomeItem : caminhoCvo + "/" + nomeItem;
            SftpATTRS atributos = item.getAttrs();

            if (atributos.isDir()) {
                // Se for um diretório (ex: ano ou mês), desce na árvore sem imprimir a pasta na
                // tela
                listarApenasArquivosJetonFiles(channelSftp, caminhoCompleto);
            } else {
                // Se for um arquivo físico de comprovante, calcula o tamanho e exibe o caminho
                // relativo limpo
                long tamanhoBytes = atributos.getSize();
                long tamanhoKb = tamanhoBytes / 1024;
                String formatacaoTamanho = tamanhoKb > 0 ? tamanhoKb + " KB"
                        : tamanhoBytes +
                                " bytes";

                System.out.println(" 📄 " + caminhoCompleto + " (" + formatacaoTamanho +
                        ")");
            }
        }
    }
}