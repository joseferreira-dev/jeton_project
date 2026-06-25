package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.domain.*;
import br.com.cremepe.jeton.repository.AtividadeConselhalRepository;
import br.com.cremepe.jeton.repository.ComprovanteRepository;
import br.com.cremepe.jeton.repository.GestaoConselheiroRepository;
import br.com.cremepe.jeton.repository.GestaoRepository;
import br.com.cremepe.jeton.util.AtividadeValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do serviço de Atividades Conselhais")
class AtividadeConselhalServiceTest {

    @Mock
    private AtividadeConselhalRepository atividadeRepositoryMock;

    @Mock
    private ComprovanteRepository comprovanteRepositoryMock;

    @Mock
    private GestaoRepository gestaoRepositoryMock;

    @Mock
    private GestaoConselheiroRepository gestaoConselheiroRepositoryMock;

    @Mock
    private ComprovanteService comprovanteServiceMock;

    @Mock
    private LogJetonService logJetonServiceMock;

    @Mock
    private AtividadeValidator atividadeValidatorMock;

    @InjectMocks
    private AtividadeConselhalService service;

    // ========== HELPERS ==========

    private Gestao criarGestao(Integer id, LocalDate inicio, LocalDate fim) {
        Gestao g = new Gestao();
        g.setIdGestao(id);
        g.setDtInicio(inicio);
        g.setDtFim(fim);
        g.setNomeGestao("Gestão Teste");
        return g;
    }

    private Conselheiro criarConselheiro(Integer id) {
        Pessoa p = new Pessoa();
        p.setIdPessoa(id);
        p.setNome("Médico Teste");
        Conselheiro c = new Conselheiro();
        c.setIdPessoa(id);
        c.setPessoa(p);
        c.setInSituacao(Conselheiro.SITUACAO_ATIVO);
        return c;
    }

    private Regras criarRegra(Integer id) {
        Regras r = new Regras();
        r.setIdRegra(id);
        r.setNomeRegra("Regra Teste");
        r.setPontos(2);
        return r;
    }

    private Comprovante criarComprovante(Integer id) {
        Comprovante c = new Comprovante();
        c.setIdComprovante(id);
        c.setNomeComprovante("Comprovante Teste");
        c.setNomeArquivo("arquivo.pdf");
        c.setMes(6);
        c.setAno(2026);
        return c;
    }

    private AtividadeConselhal criarAtividade(Integer id, Gestao gestao, Conselheiro conselheiro, Regras regra,
            Comprovante comprovante, String situacao) {
        AtividadeConselhal a = new AtividadeConselhal();
        a.setIdAtividade(id);
        a.setGestao(gestao);
        a.setConselheiro(conselheiro);
        a.setRegra(regra);
        a.setComprovante(comprovante);
        a.setQtdAtividade(1);
        a.setDataHoraAtividade(LocalDateTime.now());
        a.setDataHoraRegistro(LocalDateTime.now());
        a.setInTurno(AtividadeConselhal.TURNO_MANHA);
        a.setInSituacao(situacao);
        a.setInComputada(AtividadeConselhal.COMPUTADA_NAO);
        return a;
    }

    // ========== TESTES DE CRIAÇÃO ==========

    @Test
    @DisplayName("deve criar atividade sem comprovante com sucesso")
    void deveCriarAtividadeSemComprovante() {
        // Dado
        Integer idGestao = 1;
        Integer idConselheiro = 10;
        Integer idRegra = 5;
        LocalDateTime dataHora = LocalDateTime.now();

        Gestao gestao = criarGestao(idGestao, LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1));
        Conselheiro conselheiro = criarConselheiro(idConselheiro);
        Regras regra = criarRegra(idRegra);

        AtividadeConselhal atividade = new AtividadeConselhal();
        atividade.setGestao(gestao);
        atividade.setConselheiro(conselheiro);
        atividade.setRegra(regra);
        atividade.setDataHoraAtividade(dataHora);
        atividade.setQtdAtividade(1);

        // Configura mocks do validator
        doNothing().when(atividadeValidatorMock).validarDataHoraObrigatoria(dataHora);
        when(atividadeValidatorMock.validarGestaoExistente(idGestao)).thenReturn(gestao);
        doNothing().when(atividadeValidatorMock).validarVinculoConselheiroGestao(idConselheiro, idGestao);
        doNothing().when(atividadeValidatorMock).validarDataDentroDoMandato(dataHora.toLocalDate(), idGestao);

        // Mock do save
        when(atividadeRepositoryMock.save(any(AtividadeConselhal.class))).thenAnswer(inv -> {
            AtividadeConselhal a = inv.getArgument(0);
            a.setIdAtividade(100);
            return a;
        });

        // Quando
        AtividadeConselhal salva = service.criar(atividade, null, null, null);

        // Então
        assertThat(salva.getIdAtividade()).isEqualTo(100);
        assertThat(salva.getDataHoraRegistro()).isNotNull();
        assertThat(salva.getInSituacao()).isEqualTo(AtividadeConselhal.SITUACAO_PENDENTE);
        assertThat(salva.getComprovante()).isNull();
        assertThat(salva.getQtdAtividade()).isEqualTo(1);
        assertThat(salva.getGestao()).isEqualTo(gestao);

        verify(atividadeRepositoryMock).save(any(AtividadeConselhal.class));
        verify(logJetonServiceMock).logAtividadeCriada(salva);
        verify(comprovanteServiceMock, never()).criar(any(), any(), any());
    }

    @Test
    @DisplayName("deve criar atividade com comprovante com sucesso")
    void deveCriarAtividadeComComprovante() throws Exception {
        // Dado
        Integer idGestao = 1;
        Integer idConselheiro = 10;
        Integer idRegra = 5;
        Integer idTipoAnexo = 2;
        String nomeComprovante = "Ata Reunião";
        MultipartFile fileMock = mock(MultipartFile.class);
        when(fileMock.isEmpty()).thenReturn(false);

        Gestao gestao = criarGestao(idGestao, LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1));
        Conselheiro conselheiro = criarConselheiro(idConselheiro);
        Regras regra = criarRegra(idRegra);
        Comprovante comprovante = criarComprovante(200);

        AtividadeConselhal atividade = new AtividadeConselhal();
        atividade.setGestao(gestao);
        atividade.setConselheiro(conselheiro);
        atividade.setRegra(regra);
        atividade.setDataHoraAtividade(LocalDateTime.now());
        atividade.setQtdAtividade(1);

        // Configura mocks do validator
        doNothing().when(atividadeValidatorMock).validarDataHoraObrigatoria(any());
        when(atividadeValidatorMock.validarGestaoExistente(idGestao)).thenReturn(gestao);
        doNothing().when(atividadeValidatorMock).validarVinculoConselheiroGestao(idConselheiro, idGestao);
        doNothing().when(atividadeValidatorMock).validarDataDentroDoMandato(any(), eq(idGestao));

        when(comprovanteServiceMock.criar(fileMock, idTipoAnexo, nomeComprovante)).thenReturn(comprovante);

        when(atividadeRepositoryMock.save(any(AtividadeConselhal.class))).thenAnswer(inv -> {
            AtividadeConselhal a = inv.getArgument(0);
            a.setIdAtividade(101);
            return a;
        });

        // Quando
        AtividadeConselhal salva = service.criar(atividade, fileMock, idTipoAnexo, nomeComprovante);

        // Então
        assertThat(salva.getIdAtividade()).isEqualTo(101);
        assertThat(salva.getComprovante()).isNotNull();
        assertThat(salva.getComprovante().getIdComprovante()).isEqualTo(200);

        verify(comprovanteServiceMock).criar(fileMock, idTipoAnexo, nomeComprovante);
        verify(logJetonServiceMock).logAtividadeCriada(salva);
    }

    @Test
    @DisplayName("deve lançar exceção ao criar atividade com data/hora nula")
    void deveLancarExcecaoQuandoDataHoraNula() {
        AtividadeConselhal atividade = new AtividadeConselhal();
        atividade.setDataHoraAtividade(null);

        doThrow(new RuntimeException("Data/hora obrigatória"))
                .when(atividadeValidatorMock).validarDataHoraObrigatoria(null);

        assertThatThrownBy(() -> service.criar(atividade, null, null, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Data/hora obrigatória");

        verify(atividadeRepositoryMock, never()).save(any());
    }

    // ========== TESTES DE ATUALIZAÇÃO ==========

    @Test
    @DisplayName("deve atualizar atividade sem alterar comprovante com sucesso")
    void deveAtualizarAtividadeSemAlterarComprovante() {
        // Dado
        Integer idAtividade = 50;
        Integer idGestao = 1;
        Integer idConselheiro = 10;

        Gestao gestao = criarGestao(idGestao, LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1));
        Conselheiro conselheiro = criarConselheiro(idConselheiro);
        Regras regra = criarRegra(5);
        Comprovante comprovante = criarComprovante(300);

        AtividadeConselhal existente = criarAtividade(idAtividade, gestao, conselheiro, regra, comprovante,
                AtividadeConselhal.SITUACAO_PENDENTE);

        AtividadeConselhal nova = new AtividadeConselhal();
        nova.setIdAtividade(idAtividade);
        nova.setGestao(gestao);
        nova.setConselheiro(conselheiro);
        nova.setRegra(regra);
        nova.setDataHoraAtividade(LocalDateTime.now().plusDays(1));
        nova.setQtdAtividade(2);
        nova.setInSituacao(AtividadeConselhal.SITUACAO_PENDENTE);

        when(atividadeRepositoryMock.findById(idAtividade)).thenReturn(Optional.of(existente));
        doNothing().when(atividadeValidatorMock).validarAtividadeNaoFechada(existente);
        doNothing().when(atividadeValidatorMock).validarDataHoraObrigatoria(any());
        when(atividadeValidatorMock.validarGestaoExistente(idGestao)).thenReturn(gestao);
        doNothing().when(atividadeValidatorMock).validarVinculoConselheiroGestao(idConselheiro, idGestao);
        doNothing().when(atividadeValidatorMock).validarDataDentroDoMandato(any(), eq(idGestao));

        when(atividadeRepositoryMock.save(any(AtividadeConselhal.class))).thenReturn(nova);

        // Quando
        AtividadeConselhal atualizada = service.atualizar(nova, null, null, null, null);

        // Então
        assertThat(atualizada.getDataHoraAtividade()).isEqualTo(nova.getDataHoraAtividade());
        assertThat(atualizada.getQtdAtividade()).isEqualTo(2);
        assertThat(atualizada.getComprovante()).isSameAs(comprovante); // mantido

        verify(atividadeRepositoryMock).save(any());
        verify(logJetonServiceMock).logAtividadeAtualizada(any(), any());
        verify(comprovanteServiceMock, never()).excluir(anyInt());
        verify(atividadeRepositoryMock, never()).desvincularComprovante(anyInt());
    }

    @Test
    @DisplayName("deve atualizar atividade substituindo o comprovante e excluir o antigo se não houver mais vínculos")
    void deveAtualizarAtividadeSubstituindoComprovante() throws Exception {
        // Dado
        Integer idAtividade = 50;
        Integer idComprovanteAntigo = 300;
        Integer idTipoAnexo = 2;
        String nomeComprovante = "Novo Comprovante";
        MultipartFile fileMock = mock(MultipartFile.class);
        when(fileMock.isEmpty()).thenReturn(false);

        Gestao gestao = criarGestao(1, LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1));
        Conselheiro conselheiro = criarConselheiro(10);
        Regras regra = criarRegra(5);
        Comprovante comprovanteAntigo = criarComprovante(idComprovanteAntigo);

        AtividadeConselhal existente = criarAtividade(idAtividade, gestao, conselheiro, regra, comprovanteAntigo,
                AtividadeConselhal.SITUACAO_PENDENTE);

        Comprovante novoComprovante = criarComprovante(400);

        AtividadeConselhal nova = new AtividadeConselhal();
        nova.setIdAtividade(idAtividade);
        nova.setGestao(gestao);
        nova.setConselheiro(conselheiro);
        nova.setRegra(regra);
        nova.setDataHoraAtividade(LocalDateTime.now().plusDays(1));
        nova.setQtdAtividade(1);

        // Mocks de comportamento
        when(atividadeRepositoryMock.findById(idAtividade)).thenReturn(Optional.of(existente));
        doNothing().when(atividadeValidatorMock).validarAtividadeNaoFechada(existente);
        doNothing().when(atividadeValidatorMock).validarDataHoraObrigatoria(any());
        when(atividadeValidatorMock.validarGestaoExistente(1)).thenReturn(gestao);
        doNothing().when(atividadeValidatorMock).validarVinculoConselheiroGestao(10, 1);
        doNothing().when(atividadeValidatorMock).validarDataDentroDoMandato(any(), eq(1));

        when(comprovanteServiceMock.criar(fileMock, idTipoAnexo, nomeComprovante)).thenReturn(novoComprovante);
        when(atividadeRepositoryMock.save(any(AtividadeConselhal.class))).thenReturn(nova);
        when(atividadeRepositoryMock.countByComprovanteIdComprovante(idComprovanteAntigo)).thenReturn(0L);

        doNothing().when(atividadeRepositoryMock).desvincularComprovante(idAtividade);
        when(comprovanteRepositoryMock.findById(idComprovanteAntigo))
                .thenReturn(Optional.of(comprovanteAntigo));

        // Quando
        AtividadeConselhal atualizada = service.atualizar(nova, fileMock, idTipoAnexo, nomeComprovante,
                idComprovanteAntigo);

        // Então
        assertThat(atualizada.getComprovante()).isEqualTo(novoComprovante);

        verify(atividadeRepositoryMock).desvincularComprovante(idAtividade);
        verify(comprovanteServiceMock).excluir(idComprovanteAntigo);
        verify(logJetonServiceMock).logAtividadeAtualizada(any(), any());
    }

    @Test
    @DisplayName("deve lançar exceção ao tentar atualizar atividade já fechada")
    void deveLancarExcecaoAoAtualizarAtividadeFechada() {
        Integer idAtividade = 50;
        AtividadeConselhal existente = criarAtividade(idAtividade, null, null, null, null,
                AtividadeConselhal.SITUACAO_FECHADA);

        when(atividadeRepositoryMock.findById(idAtividade)).thenReturn(Optional.of(existente));
        doThrow(new RuntimeException("Atividade já fechada"))
                .when(atividadeValidatorMock).validarAtividadeNaoFechada(existente);

        AtividadeConselhal nova = new AtividadeConselhal();
        nova.setIdAtividade(idAtividade);

        assertThatThrownBy(() -> service.atualizar(nova, null, null, null, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Atividade já fechada");

        verify(atividadeRepositoryMock, never()).save(any());
    }

    // ========== TESTES DE VALIDAÇÃO ==========

    @Test
    @DisplayName("deve validar atividade pendente com sucesso")
    void deveValidarAtividadePendente() {
        Integer idAtividade = 60;
        AtividadeConselhal atividade = criarAtividade(idAtividade, null, null, null, null,
                AtividadeConselhal.SITUACAO_PENDENTE);

        when(atividadeRepositoryMock.findById(idAtividade)).thenReturn(Optional.of(atividade));
        doNothing().when(atividadeValidatorMock).validarAtividadeNaoFechada(atividade);
        when(atividadeRepositoryMock.save(any(AtividadeConselhal.class))).thenReturn(atividade);

        service.validar(idAtividade);

        assertThat(atividade.getInSituacao()).isEqualTo(AtividadeConselhal.SITUACAO_VALIDADA);
        verify(logJetonServiceMock).logAtividadeValidada(idAtividade);
    }

    @Test
    @DisplayName("deve lançar exceção ao validar atividade já fechada")
    void deveLancarExcecaoAoValidarAtividadeFechada() {
        Integer idAtividade = 61;
        AtividadeConselhal atividade = criarAtividade(idAtividade, null, null, null, null,
                AtividadeConselhal.SITUACAO_FECHADA);

        when(atividadeRepositoryMock.findById(idAtividade)).thenReturn(Optional.of(atividade));
        doThrow(new RuntimeException("Atividade já fechada"))
                .when(atividadeValidatorMock).validarAtividadeNaoFechada(atividade);

        assertThatThrownBy(() -> service.validar(idAtividade))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Atividade já fechada");

        verify(atividadeRepositoryMock, never()).save(any());
    }

    // ========== TESTES DE DESVALIDAÇÃO ==========

    @Test
    @DisplayName("deve desvalidar atividade validada com sucesso")
    void deveDesvalidarAtividadeValidada() {
        Integer idAtividade = 70;
        AtividadeConselhal atividade = criarAtividade(idAtividade, null, null, null, null,
                AtividadeConselhal.SITUACAO_VALIDADA);
        atividade.setInComputada(AtividadeConselhal.COMPUTADA_NAO);

        when(atividadeRepositoryMock.findById(idAtividade)).thenReturn(Optional.of(atividade));
        doNothing().when(atividadeValidatorMock).validarAtividadeNaoFechada(atividade);
        doNothing().when(atividadeValidatorMock).validarAtividadeNaoComputada(atividade);
        when(atividadeRepositoryMock.save(any(AtividadeConselhal.class))).thenReturn(atividade);

        service.desvalidar(idAtividade);

        assertThat(atividade.getInSituacao()).isEqualTo(AtividadeConselhal.SITUACAO_PENDENTE);
        verify(logJetonServiceMock).logAtividadeDesvalidada(idAtividade);
    }

    @Test
    @DisplayName("deve lançar exceção ao desvalidar atividade já computada")
    void deveLancarExcecaoAoDesvalidarAtividadeComputada() {
        Integer idAtividade = 71;
        AtividadeConselhal atividade = criarAtividade(idAtividade, null, null, null, null,
                AtividadeConselhal.SITUACAO_VALIDADA);
        atividade.setInComputada(AtividadeConselhal.COMPUTADA_SIM);

        when(atividadeRepositoryMock.findById(idAtividade)).thenReturn(Optional.of(atividade));
        doNothing().when(atividadeValidatorMock).validarAtividadeNaoFechada(atividade);
        doThrow(new RuntimeException("Atividade já computada"))
                .when(atividadeValidatorMock).validarAtividadeNaoComputada(atividade);

        assertThatThrownBy(() -> service.desvalidar(idAtividade))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Atividade já computada");

        verify(atividadeRepositoryMock, never()).save(any());
    }

    // ========== TESTES DE EXCLUSÃO ==========

    @Test
    @DisplayName("deve excluir atividade sem comprovante com sucesso")
    void deveExcluirAtividadeSemComprovante() {
        Integer idAtividade = 80;
        AtividadeConselhal atividade = criarAtividade(idAtividade, null, null, null, null,
                AtividadeConselhal.SITUACAO_PENDENTE);
        atividade.setComprovante(null);

        when(atividadeRepositoryMock.findById(idAtividade)).thenReturn(Optional.of(atividade));
        doNothing().when(atividadeValidatorMock).validarExclusaoPermitida(atividade);

        service.excluir(idAtividade);

        verify(atividadeRepositoryMock).deleteById(idAtividade);
        verify(comprovanteServiceMock, never()).excluir(anyInt());
        verify(logJetonServiceMock).logAtividadeExcluida(any());
    }

    @Test
    @DisplayName("deve excluir atividade e também excluir comprovante se não houver outras atividades vinculadas")
    void deveExcluirAtividadeEComprovante() {
        Integer idAtividade = 81;
        Integer idComprovante = 500;
        Comprovante comprovante = criarComprovante(idComprovante);
        AtividadeConselhal atividade = criarAtividade(idAtividade, null, null, null, comprovante,
                AtividadeConselhal.SITUACAO_PENDENTE);

        when(atividadeRepositoryMock.findById(idAtividade)).thenReturn(Optional.of(atividade));
        doNothing().when(atividadeValidatorMock).validarExclusaoPermitida(atividade);
        when(atividadeRepositoryMock.countByComprovanteIdComprovante(idComprovante)).thenReturn(0L);

        service.excluir(idAtividade);

        verify(atividadeRepositoryMock).deleteById(idAtividade);
        verify(comprovanteServiceMock).excluir(idComprovante);
        verify(logJetonServiceMock).logAtividadeExcluida(any());
    }

    @Test
    @DisplayName("deve lançar exceção ao excluir atividade já fechada")
    void deveLancarExcecaoAoExcluirAtividadeFechada() {
        Integer idAtividade = 82;
        AtividadeConselhal atividade = criarAtividade(idAtividade, null, null, null, null,
                AtividadeConselhal.SITUACAO_FECHADA);

        when(atividadeRepositoryMock.findById(idAtividade)).thenReturn(Optional.of(atividade));
        doThrow(new RuntimeException("Atividade fechada não pode ser excluída"))
                .when(atividadeValidatorMock).validarExclusaoPermitida(atividade);

        assertThatThrownBy(() -> service.excluir(idAtividade))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Atividade fechada não pode ser excluída");

        verify(atividadeRepositoryMock, never()).deleteById(anyInt());
    }

    // ========== TESTES DE CONSULTA ==========

    @Test
    @DisplayName("deve buscar atividade por ID com sucesso")
    void deveBuscarAtividadePorId() {
        Integer id = 90;
        AtividadeConselhal atividade = criarAtividade(id, null, null, null, null, "P");
        when(atividadeRepositoryMock.findById(id)).thenReturn(Optional.of(atividade));

        Optional<AtividadeConselhal> resultado = service.buscarPorId(id);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getIdAtividade()).isEqualTo(id);
    }

    @Test
    @DisplayName("deve listar atividades paginadas com filtros")
    void deveListarAtividadesPaginadasComFiltros() {
        // Dado
        String termo = "teste";
        String situacao = "P";
        String turno = "M";
        String comprovanteFiltro = "S";
        LocalDate dataInicio = LocalDate.now().minusDays(10);
        LocalDate dataFim = LocalDate.now();
        int page = 0, size = 10;
        String sortField = "dataHoraAtividade";
        String sortDir = "desc";

        Page<AtividadeConselhal> paginaEsperada = new PageImpl<>(List.of());
        when(atividadeRepositoryMock.findAllByFilters(anyString(), anyString(), anyString(), anyString(),
                any(), any(), any(Pageable.class))).thenReturn(paginaEsperada);

        // Quando
        Page<AtividadeConselhal> resultado = service.listarComPaginacaoEPesquisa(
                termo, situacao, turno, comprovanteFiltro, dataInicio, dataFim, page, size, sortField, sortDir);

        // Então
        assertThat(resultado).isNotNull();
        verify(atividadeRepositoryMock).findAllByFilters(eq(termo), eq(situacao), eq(turno), eq(comprovanteFiltro),
                eq(dataInicio), eq(dataFim), any(Pageable.class));
    }
}