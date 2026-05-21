// package br.com.cremepe.jeton.config;

// import com.jcraft.jsch.*;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.boot.CommandLineRunner;
// import org.springframework.stereotype.Component;

// import java.util.Vector;

// /**
// * Utilitário transacional de desenvolvimento para expurgar e limpar
// * completamente
// * todos os ficheiros físicos de teste armazenados na pasta 'jetonFiles' da
// * Locaweb.
// */
// @Component
// public class LimpezaFilesLocaweb implements CommandLineRunner {

// @Value("${ftp.locaweb.host}")
// private String ftpHost;

// @Value("${ftp.locaweb.port}")
// private int ftpPort;

// @Value("${ftp.locaweb.user}")
// private String ftpUser;

// @Value("${ftp.locaweb.pass}")
// private String ftpPass;

// @Override
// public void run(String... args) throws Exception {
// // COMENTE OU REMOVA A LINHA ABAIXO SE NÃO QUISER QUE RODE AUTOMATICAMENTE NA
// // INICIALIZAÇÃO
// // if (true) {
// // System.out.println("[INFO] Classe de limpeza carregada. Mantenha
// desativada
// // em produção.");
// // return;
// // }

// System.out.println("\n=========================================================================");
// System.out.println(" ALERTA DE OPERAÇÃO: LIMPANDO TODOS OS ARQUIVOS DE
// 'jetonFiles'");
// System.out.println("=========================================================================\n");

// Session session = null;
// ChannelSftp channelSftp = null;

// try {
// JSch jsch = new JSch();
// session = jsch.getSession(ftpUser, ftpHost, ftpPort);
// session.setPassword(ftpPass);
// session.setConfig("StrictHostKeyChecking", "no");

// session.connect(15000);
// channelSftp = (ChannelSftp) session.openChannel("sftp");
// channelSftp.connect(15000);

// String pastaRaizAlvo = "jetonFiles";

// System.out.println("[SFTP] Conexão estabelecida com a Locaweb.");
// System.out.println("[SFTP] Iniciando purgação recursiva a partir de: " +
// pastaRaizAlvo + "/");
// System.out.println("-------------------------------------------------------------------------");

// // Executa a deleção em cascata por dentro da pasta jetonFiles
// limparDiretorioSftpRecursivo(channelSftp, pastaRaizAlvo);

// System.out.println("-------------------------------------------------------------------------");
// System.out.println("[SUCESSO] Todos os ficheiros e subpastas de teste foram
// eliminados.");

// } catch (JSchException e) {
// System.err.println("[ERRO SSH] Falha de comunicação com o servidor: " +
// e.getMessage());
// } catch (SftpException e) {
// System.err.println("[AVISO SFTP] A pasta alvo não foi localizada ou já está
// limpa: " + e.getMessage());
// } finally {
// if (channelSftp != null && channelSftp.isConnected()) {
// channelSftp.disconnect();
// }
// if (session != null && session.isConnected()) {
// session.disconnect();
// System.out.println("[SFTP] Sessão de limpeza finalizada.");
// }
// }
// System.out.println("\n=========================================================================\n");
// }

// /**
// * Navega recursivamente limpando primeiro os ficheiros (folhas) e depois os
// * diretórios (nós).
// */
// @SuppressWarnings("unchecked")
// private void limparDiretorioSftpRecursivo(ChannelSftp channelSftp, String
// caminhoAtual) throws SftpException {
// Vector<ChannelSftp.LsEntry> itens = channelSftp.ls(caminhoAtual);

// if (itens == null || itens.isEmpty()) {
// return;
// }

// for (ChannelSftp.LsEntry item : itens) {
// String nomeItem = item.getFilename();

// // Despreza os seletores de navegação do Linux
// if (".".equals(nomeItem) || "..".equals(nomeItem)) {
// continue;
// }

// String caminhoCompleto = caminhoAtual.endsWith("/") ? caminhoAtual + nomeItem
// : caminhoAtual + "/" + nomeItem;
// SftpATTRS atributos = item.getAttrs();

// if (atributos.isDir()) {
// // 1. Se encontrar uma pasta (Ex: ano ou mês), entra nela primeiro para
// limpar o
// // conteúdo
// limparDiretorioSftpRecursivo(channelSftp, caminhoCompleto);

// // 2. Após esvaziar o conteúdo interno da pasta, remove o diretório vazio
// System.out.println(" 📁 Removendo pasta vazia: " + caminhoCompleto);
// channelSftp.rmdir(caminhoCompleto);
// } else {
// // 3. Se for um arquivo físico (.pdf), remove imediatamente
// System.out.println(
// " 📄 Deletando arquivo: " + caminhoCompleto + " (" + (atributos.getSize() /
// 1024) + " KB)");
// channelSftp.rm(caminhoCompleto);
// }
// }
// }
// }