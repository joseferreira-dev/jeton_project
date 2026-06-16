package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.domain.AtividadeConselhal;
import br.com.cremepe.jeton.domain.Comprovante;
import br.com.cremepe.jeton.domain.Gestao;
import br.com.cremepe.jeton.repository.AtividadeConselhalRepository;
import br.com.cremepe.jeton.repository.ComprovanteRepository;
import br.com.cremepe.jeton.repository.GestaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class IntegridadeService {

    private static final Logger log = LoggerFactory.getLogger(IntegridadeService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ComprovanteRepository comprovanteRepository;
    private final AtividadeConselhalRepository atividadeRepository;
    private final GestaoRepository gestaoRepository;
    private final FileStorageService fileStorageService; // injetado

    public IntegridadeService(ComprovanteRepository comprovanteRepository,
            AtividadeConselhalRepository atividadeRepository,
            GestaoRepository gestaoRepository,
            FileStorageService fileStorageService) {
        this.comprovanteRepository = comprovanteRepository;
        this.atividadeRepository = atividadeRepository;
        this.gestaoRepository = gestaoRepository;
        this.fileStorageService = fileStorageService;
    }

    @Scheduled(cron = "0 0 2 * * ?") // todos os dias às 02:00
    @Transactional(readOnly = true)
    public void verificarIntegridade() {
        log.info("Iniciando verificação periódica de integridade dos dados.");
        long inicio = System.currentTimeMillis();

        verificarComprovantesOrfaos();
        verificarAtividadesForaDoMandato();

        long duracao = System.currentTimeMillis() - inicio;
        log.info("Verificação de integridade concluída em {} ms.", duracao);
    }

    private void verificarComprovantesOrfaos() {
        List<Comprovante> todosComprovantes = comprovanteRepository.findAll();
        long orfaos = 0;

        for (Comprovante comprovante : todosComprovantes) {
            long count = atividadeRepository.countByComprovanteIdComprovante(comprovante.getIdComprovante());
            if (count == 0) {
                orfaos++;
                log.warn("Comprovante órfão encontrado: ID={}, nome='{}', arquivo='{}', mês/ano={}/{}",
                        comprovante.getIdComprovante(),
                        comprovante.getNomeComprovante(),
                        comprovante.getNomeArquivo(),
                        comprovante.getMes(),
                        comprovante.getAno());
            }
        }

        if (orfaos == 0) {
            log.info("Nenhum comprovante órfão encontrado.");
        } else {
            log.info("Total de comprovantes órfãos: {}", orfaos);
        }
    }

    private void verificarAtividadesForaDoMandato() {
        List<AtividadeConselhal> todasAtividades = atividadeRepository.findAll();
        long foraDoMandato = 0;

        for (AtividadeConselhal atividade : todasAtividades) {
            Gestao gestao = atividade.getGestao();
            if (gestao == null) {
                // Atividade sem gestão (não deveria ocorrer, mas registramos)
                log.error("Atividade sem gestão associada: ID={}, conselheiro={}, data={}",
                        atividade.getIdAtividade(),
                        atividade.getConselheiro() != null ? atividade.getConselheiro().getPessoa().getNome() : "N/A",
                        atividade.getDataHoraAtividade());
                continue;
            }

            LocalDate dataAtividade = atividade.getDataHoraAtividade().toLocalDate();
            LocalDate inicio = gestao.getDtInicio();
            LocalDate fim = gestao.getDtFim();

            if (dataAtividade.isBefore(inicio) || dataAtividade.isAfter(fim)) {
                foraDoMandato++;
                log.warn(
                        "Atividade fora do mandato: ID={}, conselheiro='{}', gestão='{}', dataAtividade={}, período da gestão={} a {}",
                        atividade.getIdAtividade(),
                        atividade.getConselheiro() != null ? atividade.getConselheiro().getPessoa().getNome() : "N/A",
                        gestao.getNomeGestao(),
                        dataAtividade.format(DATE_FORMAT),
                        inicio.format(DATE_FORMAT),
                        fim.format(DATE_FORMAT));
            }
        }

        if (foraDoMandato == 0) {
            log.info("Nenhuma atividade com data fora do mandato encontrada.");
        } else {
            log.info("Total de atividades fora do mandato: {}", foraDoMandato);
        }
    }

    public void executarVerificacaoManual() {
        verificarIntegridade();
    }

    @Transactional(readOnly = true)
    public List<Comprovante> listarComprovantesOrfaos() {
        List<Comprovante> todos = comprovanteRepository.findAll();
        return todos.stream()
                .filter(c -> atividadeRepository.countByComprovanteIdComprovante(c.getIdComprovante()) == 0)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public byte[] downloadComprovantesOrfaos() {
        List<Comprovante> orfaos = listarComprovantesOrfaos();
        if (orfaos.isEmpty()) {
            log.info("Nenhum comprovante órfão para baixar.");
            return new byte[0];
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ZipOutputStream zos = new ZipOutputStream(baos)) {

            for (Comprovante comp : orfaos) {
                try {
                    // Carrega o recurso do FTP
                    Resource resource = fileStorageService.carregarArquivo(
                            comp.getNomeArquivo(),
                            comp.getAno(),
                            comp.getMes());

                    // Cria uma entrada no ZIP com caminho: ano/mes/nome_arquivo
                    String entryName = String.format("%d/%d/%s",
                            comp.getAno(),
                            comp.getMes(),
                            comp.getNomeArquivo());
                    ZipEntry entry = new ZipEntry(entryName);
                    zos.putNextEntry(entry);

                    // Copia o conteúdo
                    try (InputStream is = resource.getInputStream()) {
                        is.transferTo(zos);
                    }
                    zos.closeEntry();

                    log.debug("Arquivo adicionado ao ZIP: {}", entryName);

                } catch (Exception e) {
                    log.error("Erro ao processar comprovante ID {} para download: {}",
                            comp.getIdComprovante(), e.getMessage());

                    // Adiciona um arquivo de erro no ZIP para não perder o registro do problema
                    String errorEntry = String.format("erros/erro_%d.txt", comp.getIdComprovante());
                    zos.putNextEntry(new ZipEntry(errorEntry));
                    zos.write(("Erro ao baixar: " + e.getMessage()).getBytes());
                    zos.closeEntry();
                }
            }
            zos.finish();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Falha ao criar ZIP de comprovantes órfãos", e);
            throw new RuntimeException("Erro ao gerar arquivo ZIP dos comprovantes órfãos.", e);
        }
    }

    @Transactional
    public String excluirComprovantesOrfaos() {
        List<Comprovante> orfaos = listarComprovantesOrfaos();
        if (orfaos.isEmpty()) {
            return "Nenhum comprovante órfão encontrado para exclusão.";
        }

        int sucessos = 0;
        int falhas = 0;

        for (Comprovante comp : orfaos) {
            try {
                // 1. Remove o arquivo físico do FTP
                fileStorageService.excluirArquivo(comp.getNomeArquivo(), comp.getAno(), comp.getMes());

                // 2. Remove o registro do banco
                comprovanteRepository.delete(comp);

                sucessos++;
                log.info("Comprovante órfão excluído: ID={}, arquivo={}", comp.getIdComprovante(),
                        comp.getNomeArquivo());

            } catch (Exception e) {
                falhas++;
                log.error("Falha ao excluir comprovante órfão ID {}: {}",
                        comp.getIdComprovante(), e.getMessage(), e);
            }
        }

        String resultado = String.format("Exclusão concluída: %d comprovantes excluídos, %d falhas.",
                sucessos, falhas);
        log.info(resultado);
        return resultado;
    }
}