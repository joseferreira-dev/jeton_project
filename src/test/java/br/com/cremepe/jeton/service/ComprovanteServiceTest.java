package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.domain.Comprovante;
import br.com.cremepe.jeton.domain.TipoAnexo;
import br.com.cremepe.jeton.repository.ComprovanteRepository;
import br.com.cremepe.jeton.repository.TipoAnexoRepository;
import br.com.cremepe.jeton.util.ArquivoValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do serviço de Comprovantes")
class ComprovanteServiceTest {

    @Mock
    private ComprovanteRepository comprovanteRepositoryMock;

    @Mock
    private TipoAnexoRepository tipoAnexoRepositoryMock;

    @Mock
    private FileStorageService fileStorageServiceMock;

    @Mock
    private LogJetonService logJetonServiceMock;

    @Mock
    private ArquivoValidator arquivoValidatorMock;

    @InjectMocks
    private ComprovanteService service;

    // ========== HELPERS ==========

    private TipoAnexo criarTipoAnexo(Integer id, String nome) {
        TipoAnexo tipo = new TipoAnexo();
        tipo.setIdTipo(id);
        tipo.setNome(nome);
        tipo.setExigePublicacao(TipoAnexo.EXIGE_PUBLICACAO_NAO);
        return tipo;
    }

    private Comprovante criarComprovante(Integer id, String nome, String nomeArquivo, Integer mes, Integer ano) {
        Comprovante comp = new Comprovante();
        comp.setIdComprovante(id);
        comp.setNomeComprovante(nome);
        comp.setNomeArquivo(nomeArquivo);
        comp.setContentType("application/pdf");
        comp.setMes(mes);
        comp.setAno(ano);
        return comp;
    }

    // ========== TESTES DE CRIAÇÃO ==========

    @Test
    @DisplayName("deve criar comprovante com sucesso")
    void deveCriarComprovanteComSucesso() throws Exception {
        // Dado
        Integer idTipoAnexo = 10;
        String descricaoUsuario = "Ata da Reunião de Diretoria";
        String nomeArquivoGerado = "abc-123-def-456.pdf";
        MultipartFile fileMock = mock(MultipartFile.class);
        when(fileMock.getContentType()).thenReturn("application/pdf");

        TipoAnexo tipoAnexo = criarTipoAnexo(idTipoAnexo, "Atas");
        YearMonth dataAtual = YearMonth.now();
        int mesEsperado = dataAtual.getMonthValue();
        int anoEsperado = dataAtual.getYear();

        // Comportamento dos mocks
        doNothing().when(arquivoValidatorMock).validarArquivo(fileMock);
        when(arquivoValidatorMock.obterContentTypeValido("application/pdf")).thenReturn("application/pdf");
        when(fileStorageServiceMock.salvarArquivoNoFtp(fileMock, anoEsperado, mesEsperado))
                .thenReturn(nomeArquivoGerado);
        when(tipoAnexoRepositoryMock.findById(idTipoAnexo)).thenReturn(Optional.of(tipoAnexo));

        // Captura do objeto salvo
        when(comprovanteRepositoryMock.save(any(Comprovante.class)))
                .thenAnswer(inv -> {
                    Comprovante c = inv.getArgument(0);
                    c.setIdComprovante(999);
                    return c;
                });

        // Quando
        Comprovante comprovanteSalvo = service.criar(fileMock, idTipoAnexo, descricaoUsuario);

        // Então
        assertThat(comprovanteSalvo).isNotNull();
        assertThat(comprovanteSalvo.getIdComprovante()).isEqualTo(999);

        // Verifica se o validator foi chamado
        verify(arquivoValidatorMock).validarArquivo(fileMock);
        verify(arquivoValidatorMock).obterContentTypeValido("application/pdf");

        // Verifica se o arquivo foi salvo no FTP
        verify(fileStorageServiceMock).salvarArquivoNoFtp(fileMock, anoEsperado, mesEsperado);

        // Verifica se o tipo de anexo foi buscado
        verify(tipoAnexoRepositoryMock).findById(idTipoAnexo);

        // Verifica os dados gravados no comprovante
        ArgumentCaptor<Comprovante> captor = ArgumentCaptor.forClass(Comprovante.class);
        verify(comprovanteRepositoryMock).save(captor.capture());

        Comprovante comprovanteCapturado = captor.getValue();
        assertThat(comprovanteCapturado.getTipoAnexo()).isEqualTo(tipoAnexo);
        assertThat(comprovanteCapturado.getNomeComprovante()).isEqualTo(descricaoUsuario);
        assertThat(comprovanteCapturado.getNomeArquivo()).isEqualTo(nomeArquivoGerado);
        assertThat(comprovanteCapturado.getContentType()).isEqualTo("application/pdf");
        assertThat(comprovanteCapturado.getMes()).isEqualTo(mesEsperado);
        assertThat(comprovanteCapturado.getAno()).isEqualTo(anoEsperado);

        // Verifica se o log foi registrado
        verify(logJetonServiceMock).logComprovanteCriado(comprovanteSalvo);
    }

    @Test
    @DisplayName("deve lançar exceção ao validar arquivo inválido")
    void deveLancarExcecaoAoValidarArquivoInvalido() throws Exception {
        // Dado
        MultipartFile fileMock = mock(MultipartFile.class);
        Integer idTipoAnexo = 1;
        String descricao = "Descrição";

        doThrow(new RuntimeException("Arquivo vazio ou nulo."))
                .when(arquivoValidatorMock).validarArquivo(fileMock);

        // Quando / Então
        assertThatThrownBy(() -> service.criar(fileMock, idTipoAnexo, descricao))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Arquivo vazio ou nulo.");

        // Verifica que nenhuma outra operação foi executada
        verify(fileStorageServiceMock, never()).salvarArquivoNoFtp(any(), anyInt(), anyInt());
        verify(tipoAnexoRepositoryMock, never()).findById(anyInt());
        verify(comprovanteRepositoryMock, never()).save(any());
    }

    @Test
    @DisplayName("deve lançar exceção quando o tipo de anexo não existe")
    void deveLancarExcecaoQuandoTipoAnexoNaoExiste() throws Exception {
        // Dado
        Integer idTipoAnexo = 999;
        String descricao = "Comprovante";
        MultipartFile fileMock = mock(MultipartFile.class);

        doNothing().when(arquivoValidatorMock).validarArquivo(fileMock);
        when(fileStorageServiceMock.salvarArquivoNoFtp(any(), anyInt(), anyInt()))
                .thenReturn("arquivo.pdf");
        when(tipoAnexoRepositoryMock.findById(idTipoAnexo)).thenReturn(Optional.empty());

        // Quando / Então
        assertThatThrownBy(() -> service.criar(fileMock, idTipoAnexo, descricao))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Tipo de anexo inválido. ID: 999");

        // Verifica que o comprovante não foi salvo
        verify(comprovanteRepositoryMock, never()).save(any());
    }

    // ========== TESTES DE EXCLUSÃO ==========

    @Test
    @DisplayName("deve excluir comprovante com sucesso")
    void deveExcluirComprovanteComSucesso() {
        // Dado
        Integer idComprovante = 50;
        Integer mes = 6;
        Integer ano = 2026;
        String nomeArquivo = "abc-123.pdf";
        Comprovante comprovante = criarComprovante(idComprovante, "Comprovante Teste", nomeArquivo, mes, ano);

        when(comprovanteRepositoryMock.findById(idComprovante)).thenReturn(Optional.of(comprovante));

        // Quando
        service.excluir(idComprovante);

        // Então
        // Verifica exclusão do arquivo físico
        verify(fileStorageServiceMock).excluirArquivo(nomeArquivo, ano, mes);

        // Verifica exclusão do registro
        verify(comprovanteRepositoryMock).delete(comprovante);

        // Verifica log de exclusão
        ArgumentCaptor<Comprovante> captor = ArgumentCaptor.forClass(Comprovante.class);
        verify(logJetonServiceMock).logComprovanteExcluido(captor.capture());

        Comprovante comprovanteLogado = captor.getValue();
        assertThat(comprovanteLogado.getIdComprovante()).isEqualTo(idComprovante);
        assertThat(comprovanteLogado.getNomeArquivo()).isEqualTo(nomeArquivo);
    }

    @Test
    @DisplayName("não deve fazer nada ao excluir comprovante inexistente")
    void naoDeveFazerNadaAoExcluirComprovanteInexistente() {
        // Dado
        Integer idComprovante = 999;

        when(comprovanteRepositoryMock.findById(idComprovante)).thenReturn(Optional.empty());

        // Quando
        service.excluir(idComprovante);

        // Então
        verify(fileStorageServiceMock, never()).excluirArquivo(anyString(), anyInt(), anyInt());
        verify(comprovanteRepositoryMock, never()).delete(any());
        verify(logJetonServiceMock, never()).logComprovanteExcluido(any());
    }

    // ========== TESTES DE CONSULTA ==========

    @Test
    @DisplayName("deve listar todos os comprovantes com sucesso")
    void deveListarTodosOsComprovantes() {
        // Dado
        Comprovante comp1 = criarComprovante(1, "Comp A", "a.pdf", 5, 2026);
        Comprovante comp2 = criarComprovante(2, "Comp B", "b.pdf", 5, 2026);
        List<Comprovante> listaEsperada = List.of(comp1, comp2);

        when(comprovanteRepositoryMock.findAll()).thenReturn(listaEsperada);

        // Quando
        List<Comprovante> resultado = service.listarTodos();

        // Então
        assertThat(resultado).hasSize(2);
        assertThat(resultado).containsExactly(comp1, comp2);
        verify(comprovanteRepositoryMock).findAll();
    }

    @Test
    @DisplayName("deve buscar comprovante por ID com sucesso quando existe")
    void deveBuscarComprovantePorIdComSucesso() {
        // Dado
        Integer idComprovante = 10;
        Comprovante comprovante = criarComprovante(idComprovante, "Comp X", "x.pdf", 7, 2026);
        when(comprovanteRepositoryMock.findById(idComprovante)).thenReturn(Optional.of(comprovante));

        // Quando
        Optional<Comprovante> resultado = service.buscarPorId(idComprovante);

        // Então
        assertThat(resultado).isPresent();
        assertThat(resultado.get()).isEqualTo(comprovante);
        verify(comprovanteRepositoryMock).findById(idComprovante);
    }

    @Test
    @DisplayName("deve retornar Optional vazio ao buscar comprovante por ID inexistente")
    void deveRetornarOptionalVazioAoBuscarComprovanteInexistente() {
        // Dado
        Integer idComprovante = 999;
        when(comprovanteRepositoryMock.findById(idComprovante)).thenReturn(Optional.empty());

        // Quando
        Optional<Comprovante> resultado = service.buscarPorId(idComprovante);

        // Então
        assertThat(resultado).isEmpty();
        verify(comprovanteRepositoryMock).findById(idComprovante);
    }
}