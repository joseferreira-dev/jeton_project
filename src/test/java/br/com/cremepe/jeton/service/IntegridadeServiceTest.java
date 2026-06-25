package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.domain.AtividadeConselhal;
import br.com.cremepe.jeton.domain.Comprovante;
import br.com.cremepe.jeton.domain.Gestao;
import br.com.cremepe.jeton.repository.AtividadeConselhalRepository;
import br.com.cremepe.jeton.repository.ComprovanteRepository;
import br.com.cremepe.jeton.repository.GestaoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do serviço de Integridade de Dados")
class IntegridadeServiceTest {

    @Mock
    private ComprovanteRepository comprovanteRepositoryMock;

    @Mock
    private AtividadeConselhalRepository atividadeRepositoryMock;

    @Mock
    private GestaoRepository gestaoRepositoryMock;

    @Mock
    private FileStorageService fileStorageServiceMock;

    @InjectMocks
    private IntegridadeService service;

    // ========== HELPERS ==========

    private Comprovante criarComprovante(Integer id, String nomeArquivo, Integer mes, Integer ano) {
        Comprovante c = new Comprovante();
        c.setIdComprovante(id);
        c.setNomeComprovante("Comp " + id);
        c.setNomeArquivo(nomeArquivo);
        c.setMes(mes);
        c.setAno(ano);
        return c;
    }

    private Gestao criarGestao(Integer id, LocalDate inicio, LocalDate fim) {
        Gestao g = new Gestao();
        g.setIdGestao(id);
        g.setNomeGestao("Gestão " + id);
        g.setDtInicio(inicio);
        g.setDtFim(fim);
        return g;
    }

    private AtividadeConselhal criarAtividade(Integer id, LocalDateTime dataHora, Gestao gestao) {
        AtividadeConselhal a = new AtividadeConselhal();
        a.setIdAtividade(id);
        a.setDataHoraAtividade(dataHora);
        a.setGestao(gestao);
        return a;
    }

    // ========== TESTES DE VERIFICAÇÃO DE INTEGRIDADE ==========

    @Test
    @DisplayName("deve verificar integridade e encontrar comprovantes órfãos e atividades fora do mandato")
    void deveVerificarIntegridadeEncontrandoProblemas() {
        // Dado
        Comprovante comp1 = criarComprovante(1, "arq1.pdf", 5, 2026);
        Comprovante comp2 = criarComprovante(2, "arq2.pdf", 5, 2026);
        when(comprovanteRepositoryMock.findAll()).thenReturn(List.of(comp1, comp2));

        // comp1 não tem atividades -> órfão
        when(atividadeRepositoryMock.countByComprovanteIdComprovante(1)).thenReturn(0L);
        // comp2 tem atividade -> não órfão
        when(atividadeRepositoryMock.countByComprovanteIdComprovante(2)).thenReturn(1L);

        // Atividades fora do mandato
        Gestao gestao = criarGestao(10, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 28));
        AtividadeConselhal atv1 = criarAtividade(100, LocalDateTime.of(2025, 12, 15, 10, 0), gestao); // antes
        AtividadeConselhal atv2 = criarAtividade(101, LocalDateTime.of(2026, 3, 15, 10, 0), gestao); // depois
        AtividadeConselhal atv3 = criarAtividade(102, LocalDateTime.of(2026, 1, 15, 10, 0), gestao); // dentro

        when(atividadeRepositoryMock.findAll()).thenReturn(List.of(atv1, atv2, atv3));

        // Quando
        service.verificarIntegridade();

        // Então - não há exceção, apenas logs
        verify(comprovanteRepositoryMock).findAll();
        verify(atividadeRepositoryMock).findAll();
        verify(atividadeRepositoryMock, times(2)).countByComprovanteIdComprovante(anyInt());
        // Não há interações com repositório de gestão (já que gestao está carregada)
    }

    @Test
    @DisplayName("deve verificar integridade e não encontrar problemas")
    void deveVerificarIntegridadeSemProblemas() {
        // Dado
        Comprovante comp = criarComprovante(1, "ok.pdf", 5, 2026);
        when(comprovanteRepositoryMock.findAll()).thenReturn(List.of(comp));
        when(atividadeRepositoryMock.countByComprovanteIdComprovante(1)).thenReturn(1L); // tem atividade

        Gestao gestao = criarGestao(10, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        AtividadeConselhal atv = criarAtividade(100, LocalDateTime.of(2026, 6, 15, 10, 0), gestao);
        when(atividadeRepositoryMock.findAll()).thenReturn(List.of(atv));

        // Quando
        service.verificarIntegridade();

        // Então - sem logs de problema
        verify(comprovanteRepositoryMock).findAll();
        verify(atividadeRepositoryMock).findAll();
    }

    @Test
    @DisplayName("deve executar verificação manual com sucesso")
    void deveExecutarVerificacaoManual() {
        // Dado - apenas verifica se chama verificarIntegridade
        // Como verificarIntegridade é void, apenas verificamos que ela foi chamada
        // Usamos spy para confirmar chamada
        IntegridadeService spyService = spy(service);
        doNothing().when(spyService).verificarIntegridade();

        // Quando
        spyService.executarVerificacaoManual();

        // Então
        verify(spyService).verificarIntegridade();
    }

    // ========== TESTES DE LISTAGEM DE COMPROVANTES ÓRFÃOS ==========

    @Test
    @DisplayName("deve listar comprovantes órfãos com sucesso")
    void deveListarComprovantesOrfaos() {
        // Dado
        Comprovante comp1 = criarComprovante(1, "a.pdf", 5, 2026);
        Comprovante comp2 = criarComprovante(2, "b.pdf", 5, 2026);
        Comprovante comp3 = criarComprovante(3, "c.pdf", 5, 2026);

        when(comprovanteRepositoryMock.findAll()).thenReturn(List.of(comp1, comp2, comp3));
        when(atividadeRepositoryMock.countByComprovanteIdComprovante(1)).thenReturn(0L); // órfão
        when(atividadeRepositoryMock.countByComprovanteIdComprovante(2)).thenReturn(2L); // não órfão
        when(atividadeRepositoryMock.countByComprovanteIdComprovante(3)).thenReturn(0L); // órfão

        // Quando
        List<Comprovante> orfaos = service.listarComprovantesOrfaos();

        // Então
        assertThat(orfaos).hasSize(2);
        assertThat(orfaos).extracting(Comprovante::getIdComprovante).containsExactlyInAnyOrder(1, 3);

        verify(comprovanteRepositoryMock).findAll();
        verify(atividadeRepositoryMock, times(3)).countByComprovanteIdComprovante(anyInt());
    }

    @Test
    @DisplayName("deve retornar lista vazia quando não houver comprovantes órfãos")
    void deveRetornarListaVaziaQuandoNaoHaOrfaos() {
        // Dado
        Comprovante comp1 = criarComprovante(1, "a.pdf", 5, 2026);
        when(comprovanteRepositoryMock.findAll()).thenReturn(List.of(comp1));
        when(atividadeRepositoryMock.countByComprovanteIdComprovante(1)).thenReturn(1L);

        // Quando
        List<Comprovante> orfaos = service.listarComprovantesOrfaos();

        // Então
        assertThat(orfaos).isEmpty();
    }

    // ========== TESTES DE DOWNLOAD DOS COMPROVANTES ÓRFÃOS ==========

    @Test
    @DisplayName("deve gerar ZIP com comprovantes órfãos com sucesso")
    void deveGerarZipComComprovantesOrfaos() throws Exception {
        // Dado
        Comprovante comp1 = criarComprovante(1, "file1.pdf", 5, 2026);
        Comprovante comp2 = criarComprovante(2, "file2.pdf", 5, 2026);

        when(comprovanteRepositoryMock.findAll()).thenReturn(List.of(comp1, comp2));
        when(atividadeRepositoryMock.countByComprovanteIdComprovante(1)).thenReturn(0L);
        when(atividadeRepositoryMock.countByComprovanteIdComprovante(2)).thenReturn(0L);

        byte[] conteudo1 = "conteudo1".getBytes();
        byte[] conteudo2 = "conteudo2".getBytes();
        Resource resource1 = new ByteArrayResource(conteudo1);
        Resource resource2 = new ByteArrayResource(conteudo2);

        // ✅ Ordem corrigida: (nome, ano, mes)
        when(fileStorageServiceMock.carregarArquivo("file1.pdf", 2026, 5)).thenReturn(resource1);
        when(fileStorageServiceMock.carregarArquivo("file2.pdf", 2026, 5)).thenReturn(resource2);

        // Quando
        byte[] zipBytes = service.downloadComprovantesOrfaos();

        // Então
        assertThat(zipBytes).isNotEmpty();

        try (ZipInputStream zis = new ZipInputStream(new java.io.ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            int count = 0;
            while ((entry = zis.getNextEntry()) != null) {
                count++;
                // ✅ Padrão esperado: ano/mes/nome_arquivo
                assertThat(entry.getName()).matches("2026/5/file\\d+\\.pdf");
                zis.closeEntry();
            }
            assertThat(count).isEqualTo(2);
        }

        verify(fileStorageServiceMock, times(2)).carregarArquivo(anyString(), eq(2026), eq(5));
    }

    @Test
    @DisplayName("deve retornar array vazio quando não houver comprovantes órfãos para download")
    void deveRetornarArrayVazioQuandoNaoHaOrfaosParaDownload() {
        // Dado
        when(comprovanteRepositoryMock.findAll()).thenReturn(List.of());

        // Quando
        byte[] resultado = service.downloadComprovantesOrfaos();

        // Então
        assertThat(resultado).isEmpty();
        verify(fileStorageServiceMock, never()).carregarArquivo(anyString(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("deve tratar erro ao carregar arquivo do FTP durante download e incluir arquivo de erro no ZIP")
    void deveTratarErroAoCarregarArquivoDuranteDownload() throws Exception {
        // Dado
        Comprovante comp = criarComprovante(1, "erro.pdf", 5, 2026);
        when(comprovanteRepositoryMock.findAll()).thenReturn(List.of(comp));
        when(atividadeRepositoryMock.countByComprovanteIdComprovante(1)).thenReturn(0L);

        // ✅ Ordem corrigida: (nome, ano, mes)
        when(fileStorageServiceMock.carregarArquivo("erro.pdf", 2026, 5))
                .thenThrow(new RuntimeException("FTP indisponível"));

        // Quando
        byte[] zipBytes = service.downloadComprovantesOrfaos();

        // Então
        assertThat(zipBytes).isNotEmpty();

        try (ZipInputStream zis = new ZipInputStream(new java.io.ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            boolean encontrouErro = false;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().contains("erro_1.txt")) {
                    encontrouErro = true;
                    byte[] content = zis.readAllBytes();
                    assertThat(content).isNotEmpty();
                    break;
                }
                zis.closeEntry();
            }
            assertThat(encontrouErro).isTrue();
        }

        verify(fileStorageServiceMock).carregarArquivo("erro.pdf", 2026, 5);
    }

    // ========== TESTES DE EXCLUSÃO DE COMPROVANTES ÓRFÃOS ==========

    @Test
    @DisplayName("deve excluir comprovantes órfãos com sucesso")
    void deveExcluirComprovantesOrfaosComSucesso() {
        // Dado
        Comprovante comp1 = criarComprovante(1, "del1.pdf", 5, 2026);
        Comprovante comp2 = criarComprovante(2, "del2.pdf", 5, 2026);

        when(comprovanteRepositoryMock.findAll()).thenReturn(List.of(comp1, comp2));
        when(atividadeRepositoryMock.countByComprovanteIdComprovante(1)).thenReturn(0L);
        when(atividadeRepositoryMock.countByComprovanteIdComprovante(2)).thenReturn(0L);

        // ✅ Ordem corrigida: (nome, ano, mes)
        doNothing().when(fileStorageServiceMock).excluirArquivo("del1.pdf", 2026, 5);
        doNothing().when(fileStorageServiceMock).excluirArquivo("del2.pdf", 2026, 5);

        // Quando
        String resultado = service.excluirComprovantesOrfaos();

        // Então
        assertThat(resultado).contains("2 comprovantes excluídos, 0 falhas");
        verify(fileStorageServiceMock, times(2)).excluirArquivo(anyString(), eq(2026), eq(5));
        verify(comprovanteRepositoryMock, times(2)).delete(any(Comprovante.class));
    }

    @Test
    @DisplayName("deve retornar mensagem informando que não há órfãos para excluir")
    void deveRetornarMensagemQuandoNaoHaOrfaosParaExcluir() {
        // Dado
        when(comprovanteRepositoryMock.findAll()).thenReturn(List.of());

        // Quando
        String resultado = service.excluirComprovantesOrfaos();

        // Então
        assertThat(resultado).isEqualTo("Nenhum comprovante órfão encontrado para exclusão.");
        verify(fileStorageServiceMock, never()).excluirArquivo(anyString(), anyInt(), anyInt());
        verify(comprovanteRepositoryMock, never()).delete(any());
    }

    @Test
    @DisplayName("deve continuar excluindo outros comprovantes mesmo se um falhar")
    void deveContinuarExcluindoMesmoComFalha() {
        // Dado
        Comprovante comp1 = criarComprovante(1, "ok.pdf", 5, 2026);
        Comprovante comp2 = criarComprovante(2, "falha.pdf", 5, 2026);

        when(comprovanteRepositoryMock.findAll()).thenReturn(List.of(comp1, comp2));
        when(atividadeRepositoryMock.countByComprovanteIdComprovante(1)).thenReturn(0L);
        when(atividadeRepositoryMock.countByComprovanteIdComprovante(2)).thenReturn(0L);

        // ✅ Ordem corrigida: (nome, ano, mes)
        doNothing().when(fileStorageServiceMock).excluirArquivo("ok.pdf", 2026, 5);
        doThrow(new RuntimeException("Erro FTP")).when(fileStorageServiceMock)
                .excluirArquivo("falha.pdf", 2026, 5);

        // Quando
        String resultado = service.excluirComprovantesOrfaos();

        // Então
        assertThat(resultado).contains("1 comprovantes excluídos, 1 falhas");
        verify(fileStorageServiceMock, times(2)).excluirArquivo(anyString(), eq(2026), eq(5));
        verify(comprovanteRepositoryMock).delete(comp1);
        verify(comprovanteRepositoryMock, never()).delete(comp2);
    }
}