package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.domain.*;
import br.com.cremepe.jeton.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AtividadeConselhalServiceTest {

    @Mock
    private AtividadeConselhalRepository atividadeRepository;

    @Mock
    private ComprovanteRepository comprovanteRepository;

    @Mock
    private GestaoRepository gestaoRepository;

    @Mock
    private GestaoConselheiroRepository gestaoConselheiroRepository;

    @Mock
    private ComprovanteService comprovanteService;

    @InjectMocks
    private AtividadeConselhalService service;

    private Gestao gestao;
    private Conselheiro conselheiro;
    private Regras regra;
    private AtividadeConselhal atividade;
    private MultipartFile mockFile;

    @BeforeEach
    void setUp() {
        gestao = new Gestao();
        gestao.setIdGestao(1);
        gestao.setDtInicio(LocalDate.of(2025, 1, 1));
        gestao.setDtFim(LocalDate.of(2025, 12, 31));

        conselheiro = new Conselheiro();
        conselheiro.setIdPessoa(10);
        Pessoa pessoa = new Pessoa();
        pessoa.setNome("Dr. Teste");
        conselheiro.setPessoa(pessoa);

        regra = new Regras();
        regra.setIdRegra(100);

        atividade = new AtividadeConselhal();
        atividade.setIdAtividade(null);
        atividade.setGestao(gestao);
        atividade.setConselheiro(conselheiro);
        atividade.setRegra(regra);
        atividade.setDataHoraAtividade(LocalDateTime.of(2025, 6, 15, 14, 30));
        atividade.setQtdAtividade(2);
        atividade.setInSituacao(AtividadeConselhal.SITUACAO_PENDENTE);
        atividade.setInTurno(AtividadeConselhal.TURNO_TARDE);

        mockFile = mock(MultipartFile.class);
    }

    // =========================================================================
    // Testes do método criar()
    // =========================================================================

    @Test
    void criar_quandoSucessoSemComprovante_deveSalvarAtividade() {
        when(gestaoRepository.findById(1)).thenReturn(Optional.of(gestao));
        when(gestaoConselheiroRepository.findByIdIdGestao(1))
                .thenReturn(List.of(createVinculo(gestao, conselheiro)));
        when(atividadeRepository.save(any(AtividadeConselhal.class))).thenAnswer(inv -> {
            AtividadeConselhal a = inv.getArgument(0);
            a.setIdAtividade(1);
            return a;
        });

        AtividadeConselhal resultado = service.criar(atividade, null, null, null);

        assertNotNull(resultado);
        assertEquals(1, resultado.getIdAtividade());
        assertNotNull(resultado.getDataHoraRegistro());
        assertEquals(AtividadeConselhal.SITUACAO_PENDENTE, resultado.getInSituacao());
        assertEquals(2, resultado.getQtdAtividade());
        verify(atividadeRepository, times(1)).save(any());
        verify(comprovanteService, never()).criarComprovante(any(), any(), any());
    }

    @Test
    void criar_quandoComprovanteEnviado_deveCriarComprovante() {
        Comprovante comprovante = new Comprovante();
        comprovante.setIdComprovante(5);
        when(comprovanteService.criarComprovante(eq(mockFile), eq(1), eq("Desc"))).thenReturn(comprovante);
        when(gestaoRepository.findById(1)).thenReturn(Optional.of(gestao));
        when(gestaoConselheiroRepository.findByIdIdGestao(1))
                .thenReturn(List.of(createVinculo(gestao, conselheiro)));
        when(atividadeRepository.save(any(AtividadeConselhal.class))).thenAnswer(inv -> inv.getArgument(0));

        AtividadeConselhal resultado = service.criar(atividade, mockFile, 1, "Desc");

        assertNotNull(resultado.getComprovante());
        assertEquals(5, resultado.getComprovante().getIdComprovante());
        verify(comprovanteService, times(1)).criarComprovante(mockFile, 1, "Desc");
        verify(atividadeRepository, times(1)).save(any());
    }

    @Test
    void criar_quandoQtdAtividadeNullOuZero_deveSetarUm() {
        atividade.setQtdAtividade(null);
        when(gestaoRepository.findById(1)).thenReturn(Optional.of(gestao));
        when(gestaoConselheiroRepository.findByIdIdGestao(1))
                .thenReturn(List.of(createVinculo(gestao, conselheiro)));
        when(atividadeRepository.save(any(AtividadeConselhal.class))).thenAnswer(inv -> inv.getArgument(0));

        AtividadeConselhal resultado = service.criar(atividade, null, null, null);

        assertEquals(1, resultado.getQtdAtividade());
    }

    @Test
    void criar_quandoGestaoNaoEncontrada_lancaExcecao() {
        when(gestaoRepository.findById(1)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.criar(atividade, null, null, null));
        assertEquals("A gestão informada não foi encontrada no sistema.", ex.getMessage());
        verify(atividadeRepository, never()).save(any());
    }

    @Test
    void criar_quandoConselheiroNaoVinculado_lancaExcecao() {
        when(gestaoRepository.findById(1)).thenReturn(Optional.of(gestao));
        when(gestaoConselheiroRepository.findByIdIdGestao(1)).thenReturn(Collections.emptyList());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.criar(atividade, null, null, null));
        assertEquals("O médico selecionado não possui vínculo ativo com a Gestão informada.", ex.getMessage());
    }

    @Test
    void criar_quandoDataForaDoMandato_lancaExcecao() {
        atividade.setDataHoraAtividade(LocalDateTime.of(2024, 12, 31, 10, 0));
        when(gestaoRepository.findById(1)).thenReturn(Optional.of(gestao));
        when(gestaoConselheiroRepository.findByIdIdGestao(1))
                .thenReturn(List.of(createVinculo(gestao, conselheiro)));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.criar(atividade, null, null, null));
        assertTrue(ex.getMessage().contains("A data da atividade"));
    }

    // =========================================================================
    // Testes do método atualizar()
    // =========================================================================

    @Test
    void atualizar_quandoSucessoSemAlterarComprovante_deveAtualizar() {
        atividade.setIdAtividade(1);
        AtividadeConselhal existente = new AtividadeConselhal();
        existente.setIdAtividade(1);
        existente.setDataHoraRegistro(LocalDateTime.now().minusDays(1));
        existente.setInSituacao(AtividadeConselhal.SITUACAO_PENDENTE);
        existente.setComprovante(null);

        when(atividadeRepository.findById(1)).thenReturn(Optional.of(existente));
        when(gestaoRepository.findById(1)).thenReturn(Optional.of(gestao));
        when(gestaoConselheiroRepository.findByIdIdGestao(1))
                .thenReturn(List.of(createVinculo(gestao, conselheiro)));
        when(atividadeRepository.save(any(AtividadeConselhal.class))).thenAnswer(inv -> inv.getArgument(0));

        AtividadeConselhal resultado = service.atualizar(atividade, null, null, null, null);

        assertEquals(existente.getDataHoraRegistro(), resultado.getDataHoraRegistro());
        assertEquals(2, resultado.getQtdAtividade());
        verify(atividadeRepository, times(1)).save(any());
    }

    @Test
    void atualizar_quandoAtividadeFechada_lancaExcecao() {
        atividade.setIdAtividade(1);
        AtividadeConselhal existente = new AtividadeConselhal();
        existente.setInSituacao(AtividadeConselhal.SITUACAO_FECHADA);
        when(atividadeRepository.findById(1)).thenReturn(Optional.of(existente));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.atualizar(atividade, null, null, null, null));
        assertEquals("Operação negada: Esta atividade está fechada em folha.", ex.getMessage());
    }

    @Test
    void atualizar_quandoNovoComprovanteEnviado_deveSubstituir() {
        atividade.setIdAtividade(1);
        Comprovante novoComp = new Comprovante();
        novoComp.setIdComprovante(99);
        AtividadeConselhal existente = new AtividadeConselhal();
        existente.setInSituacao(AtividadeConselhal.SITUACAO_PENDENTE);
        existente.setComprovante(null);
        when(atividadeRepository.findById(1)).thenReturn(Optional.of(existente));
        when(comprovanteService.criarComprovante(mockFile, 2, "NovoNome")).thenReturn(novoComp);
        when(gestaoRepository.findById(1)).thenReturn(Optional.of(gestao));
        when(gestaoConselheiroRepository.findByIdIdGestao(1))
                .thenReturn(List.of(createVinculo(gestao, conselheiro)));
        when(atividadeRepository.save(any(AtividadeConselhal.class))).thenAnswer(inv -> inv.getArgument(0));

        AtividadeConselhal resultado = service.atualizar(atividade, mockFile, 2, "NovoNome", null);

        assertNotNull(resultado.getComprovante());
        assertEquals(99, resultado.getComprovante().getIdComprovante());
    }

    @Test
    void atualizar_quandoIdComprovanteAntigoExistenteESemOutrasAtividades_deveExcluirComprovanteAntigo() {
        atividade.setIdAtividade(1);
        Integer idComprovanteAntigo = 5;
        Comprovante antigoComp = new Comprovante();
        antigoComp.setIdComprovante(5);
        Comprovante novoComp = new Comprovante();
        novoComp.setIdComprovante(99);

        AtividadeConselhal existente = new AtividadeConselhal();
        existente.setInSituacao(AtividadeConselhal.SITUACAO_PENDENTE);
        existente.setComprovante(antigoComp);

        when(atividadeRepository.findById(1)).thenReturn(Optional.of(existente));
        when(comprovanteService.criarComprovante(mockFile, 2, "NovoNome")).thenReturn(novoComp);
        when(gestaoRepository.findById(1)).thenReturn(Optional.of(gestao));
        when(gestaoConselheiroRepository.findByIdIdGestao(1))
                .thenReturn(List.of(createVinculo(gestao, conselheiro)));
        when(atividadeRepository.save(any(AtividadeConselhal.class))).thenAnswer(inv -> inv.getArgument(0));
        when(atividadeRepository.countByComprovanteIdComprovante(idComprovanteAntigo)).thenReturn(0L);
        when(comprovanteRepository.findById(idComprovanteAntigo)).thenReturn(Optional.of(antigoComp));

        service.atualizar(atividade, mockFile, 2, "NovoNome", idComprovanteAntigo);

        verify(comprovanteService, times(1)).excluirComprovante(idComprovanteAntigo);
    }

    @Test
    void atualizar_quandoApenasNomeComprovanteAlterado_deveAtualizarNome() {
        atividade.setIdAtividade(1);
        Integer idComprovanteAntigo = 5;
        Comprovante antigoComp = new Comprovante();
        antigoComp.setIdComprovante(5);
        antigoComp.setNomeComprovante("Nome Antigo");

        AtividadeConselhal existente = new AtividadeConselhal();
        existente.setInSituacao(AtividadeConselhal.SITUACAO_PENDENTE);
        existente.setComprovante(antigoComp);
        existente.setDataHoraRegistro(LocalDateTime.now());

        when(atividadeRepository.findById(1)).thenReturn(Optional.of(existente));
        when(gestaoRepository.findById(1)).thenReturn(Optional.of(gestao));
        when(gestaoConselheiroRepository.findByIdIdGestao(1))
                .thenReturn(List.of(createVinculo(gestao, conselheiro)));
        when(atividadeRepository.save(any(AtividadeConselhal.class))).thenAnswer(inv -> inv.getArgument(0));
        when(comprovanteRepository.findById(idComprovanteAntigo)).thenReturn(Optional.of(antigoComp));

        // Nenhum arquivo enviado, mas novo nome informado
        service.atualizar(atividade, null, null, "Novo Nome", idComprovanteAntigo);

        verify(comprovanteRepository, times(1)).save(antigoComp);
        assertEquals("Novo Nome", antigoComp.getNomeComprovante());
        verify(comprovanteService, never()).excluirComprovante(any());
    }

    @Test
    void atualizar_quandoNovoComprovanteMasAntigoAindaEmUso_naoDeveExcluirAntigo() {
        atividade.setIdAtividade(1);
        Integer idComprovanteAntigo = 5;
        Comprovante antigoComp = new Comprovante();
        antigoComp.setIdComprovante(5);
        Comprovante novoComp = new Comprovante();
        novoComp.setIdComprovante(99);

        AtividadeConselhal existente = new AtividadeConselhal();
        existente.setInSituacao(AtividadeConselhal.SITUACAO_PENDENTE);
        existente.setComprovante(antigoComp);

        when(atividadeRepository.findById(1)).thenReturn(Optional.of(existente));
        when(comprovanteService.criarComprovante(mockFile, 2, "NovoNome")).thenReturn(novoComp);
        when(gestaoRepository.findById(1)).thenReturn(Optional.of(gestao));
        when(gestaoConselheiroRepository.findByIdIdGestao(1))
                .thenReturn(List.of(createVinculo(gestao, conselheiro)));
        when(atividadeRepository.save(any(AtividadeConselhal.class))).thenAnswer(inv -> inv.getArgument(0));
        // Outras atividades ainda usam o comprovante antigo
        when(atividadeRepository.countByComprovanteIdComprovante(idComprovanteAntigo)).thenReturn(2L);

        service.atualizar(atividade, mockFile, 2, "NovoNome", idComprovanteAntigo);

        verify(comprovanteService, never()).excluirComprovante(idComprovanteAntigo);
    }

    // =========================================================================
    // Testes do método validar()
    // =========================================================================

    @Test
    void validar_quandoSucesso_deveMudarSituacaoParaValidada() {
        AtividadeConselhal pendente = new AtividadeConselhal();
        pendente.setInSituacao(AtividadeConselhal.SITUACAO_PENDENTE);
        when(atividadeRepository.findById(1)).thenReturn(Optional.of(pendente));
        when(atividadeRepository.save(any())).thenReturn(pendente);

        service.validar(1);

        assertEquals(AtividadeConselhal.SITUACAO_VALIDADA, pendente.getInSituacao());
        verify(atividadeRepository, times(1)).save(pendente);
    }

    @Test
    void validar_quandoAtividadeFechada_lancaExcecao() {
        AtividadeConselhal fechada = new AtividadeConselhal();
        fechada.setInSituacao(AtividadeConselhal.SITUACAO_FECHADA);
        when(atividadeRepository.findById(1)).thenReturn(Optional.of(fechada));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.validar(1));
        assertEquals("Operação negada: Esta atividade está fechada em folha.", ex.getMessage());
    }

    @Test
    void validar_quandoAtividadeNaoEncontrada_lancaExcecao() {
        when(atividadeRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.validar(99));
    }

    // =========================================================================
    // Testes do método desvalidar()
    // =========================================================================

    @Test
    void desvalidar_quandoSucesso_deveMudarSituacaoParaPendente() {
        AtividadeConselhal validada = new AtividadeConselhal();
        validada.setInSituacao(AtividadeConselhal.SITUACAO_VALIDADA);
        validada.setInComputada(AtividadeConselhal.COMPUTADA_NAO);
        when(atividadeRepository.findById(1)).thenReturn(Optional.of(validada));
        when(atividadeRepository.save(any())).thenReturn(validada);

        service.desvalidar(1);

        assertEquals(AtividadeConselhal.SITUACAO_PENDENTE, validada.getInSituacao());
        verify(atividadeRepository, times(1)).save(validada);
    }

    @Test
    void desvalidar_quandoAtividadeComputada_lancaExcecao() {
        AtividadeConselhal computada = new AtividadeConselhal();
        computada.setInComputada(AtividadeConselhal.COMPUTADA_SIM);
        when(atividadeRepository.findById(1)).thenReturn(Optional.of(computada));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.desvalidar(1));
        assertEquals("Operação negada: Esta atividade já foi computada em um processamento financeiro.",
                ex.getMessage());
    }

    // =========================================================================
    // Testes do método excluir()
    // =========================================================================

    @Test
    void excluir_quandoSucessoSemComprovante_deveRemoverAtividade() {
        AtividadeConselhal atividadeParaExcluir = new AtividadeConselhal();
        atividadeParaExcluir.setInSituacao(AtividadeConselhal.SITUACAO_PENDENTE);
        atividadeParaExcluir.setInComputada(AtividadeConselhal.COMPUTADA_NAO);
        atividadeParaExcluir.setComprovante(null);
        when(atividadeRepository.findById(1)).thenReturn(Optional.of(atividadeParaExcluir));
        doNothing().when(atividadeRepository).deleteById(1);

        service.excluir(1);

        verify(atividadeRepository, times(1)).deleteById(1);
        verify(comprovanteService, never()).excluirComprovante(any());
    }

    @Test
    void excluir_quandoComprovanteAssociadoSemOutrasAtividades_deveExcluirComprovante() {
        Comprovante comp = new Comprovante();
        comp.setIdComprovante(5);
        AtividadeConselhal atividadeComComp = new AtividadeConselhal();
        atividadeComComp.setInSituacao(AtividadeConselhal.SITUACAO_PENDENTE);
        atividadeComComp.setInComputada(AtividadeConselhal.COMPUTADA_NAO);
        atividadeComComp.setComprovante(comp);
        when(atividadeRepository.findById(1)).thenReturn(Optional.of(atividadeComComp));
        when(atividadeRepository.countByComprovanteIdComprovante(5)).thenReturn(0L);
        doNothing().when(atividadeRepository).deleteById(1);
        doNothing().when(comprovanteService).excluirComprovante(5);

        service.excluir(1);

        verify(comprovanteService, times(1)).excluirComprovante(5);
    }

    @Test
    void excluir_quandoAtividadeFechada_lancaExcecao() {
        AtividadeConselhal fechada = new AtividadeConselhal();
        fechada.setInSituacao(AtividadeConselhal.SITUACAO_FECHADA);
        when(atividadeRepository.findById(1)).thenReturn(Optional.of(fechada));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.excluir(1));
        assertTrue(ex.getMessage().contains("não pode ser excluída"));
    }

    @Test
    void excluir_quandoDataIntegrityViolation_lancaExcecao() {
        AtividadeConselhal pendente = new AtividadeConselhal();
        pendente.setInSituacao(AtividadeConselhal.SITUACAO_PENDENTE);
        pendente.setInComputada(AtividadeConselhal.COMPUTADA_NAO);
        when(atividadeRepository.findById(1)).thenReturn(Optional.of(pendente));
        doThrow(DataIntegrityViolationException.class).when(atividadeRepository).deleteById(1);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.excluir(1));
        assertEquals(
                "Não é possível remover esta atividade pois ela já possui vínculos com o histórico financeiro (Pagamentos de Jeton).",
                ex.getMessage());
    }

    @Test
    void excluir_quandoComprovantePossuiOutrasAtividades_naoDeveExcluirComprovante() {
        Comprovante comp = new Comprovante();
        comp.setIdComprovante(5);
        AtividadeConselhal atividadeComComp = new AtividadeConselhal();
        atividadeComComp.setInSituacao(AtividadeConselhal.SITUACAO_PENDENTE);
        atividadeComComp.setInComputada(AtividadeConselhal.COMPUTADA_NAO);
        atividadeComComp.setComprovante(comp);
        when(atividadeRepository.findById(1)).thenReturn(Optional.of(atividadeComComp));
        when(atividadeRepository.countByComprovanteIdComprovante(5)).thenReturn(2L); // outras atividades
        doNothing().when(atividadeRepository).deleteById(1);

        service.excluir(1);

        verify(comprovanteService, never()).excluirComprovante(5);
    }

    @Test
    void excluir_quandoFalhaAoExcluirComprovante_deveApenasLogarEProsseguir() {
        Comprovante comp = new Comprovante();
        comp.setIdComprovante(5);
        AtividadeConselhal atividadeComComp = new AtividadeConselhal();
        atividadeComComp.setInSituacao(AtividadeConselhal.SITUACAO_PENDENTE);
        atividadeComComp.setInComputada(AtividadeConselhal.COMPUTADA_NAO);
        atividadeComComp.setComprovante(comp);
        when(atividadeRepository.findById(1)).thenReturn(Optional.of(atividadeComComp));
        when(atividadeRepository.countByComprovanteIdComprovante(5)).thenReturn(0L);
        doNothing().when(atividadeRepository).deleteById(1);
        doThrow(new RuntimeException("FTP error")).when(comprovanteService).excluirComprovante(5);

        // Não deve lançar exceção
        assertDoesNotThrow(() -> service.excluir(1));

        verify(atividadeRepository, times(1)).deleteById(1);
        verify(comprovanteService, times(1)).excluirComprovante(5);
    }

    // =========================================================================
    // Testes de consulta (listar, buscar, contagens)
    // =========================================================================

    @Test
    void listarComPaginacaoEPesquisa_deveChamarRepositoryComParametrosCorretos() {
        Page<AtividadeConselhal> paginaEsperada = new PageImpl<>(Collections.emptyList());
        when(atividadeRepository.pesquisarPaginado(anyString(), anyString(), anyString(), anyString(),
                any(), any(), any(Pageable.class))).thenReturn(paginaEsperada);

        Page<AtividadeConselhal> resultado = service.listarComPaginacaoEPesquisa(
                "termo", "P", "M", "S", LocalDate.now(), LocalDate.now(), 0, 10, "dataHoraAtividade", "desc");

        assertNotNull(resultado);
        verify(atividadeRepository, times(1)).pesquisarPaginado(eq("termo"), eq("P"), eq("M"), eq("S"),
                any(), any(), any(Pageable.class));
    }

    @Test
    void listarComPaginacaoEPesquisa_quandoSizeZero_deveUsarMaxInteger() {
        Pageable expectedPageable = PageRequest.of(0, Integer.MAX_VALUE,
                Sort.by(Sort.Direction.DESC, "dataHoraAtividade"));
        Page<AtividadeConselhal> pagina = new PageImpl<>(Collections.emptyList());
        when(atividadeRepository.pesquisarPaginado(anyString(), anyString(), anyString(), anyString(),
                any(), any(), eq(expectedPageable))).thenReturn(pagina);

        service.listarComPaginacaoEPesquisa("", "", "", "", null, null, 0, 0, "dataHoraAtividade", "desc");

        verify(atividadeRepository, times(1)).pesquisarPaginado(anyString(), anyString(), anyString(), anyString(),
                any(), any(), eq(expectedPageable));
    }

    @Test
    void buscarPorId_deveRetornarOptional() {
        when(atividadeRepository.findById(1)).thenReturn(Optional.of(atividade));
        Optional<AtividadeConselhal> result = service.buscarPorId(1);
        assertTrue(result.isPresent());
        assertEquals(atividade, result.get());
    }

    @Test
    void listarPorConselheiroComFiltros_deveChamarRepository() {
        Pageable pageable = PageRequest.of(0, 5);
        Page<AtividadeConselhal> pagina = new PageImpl<>(Collections.emptyList());
        when(atividadeRepository.findByConselheiroAndFiltros(eq(10), any(), any(), eq("P"), eq(pageable)))
                .thenReturn(pagina);

        Page<AtividadeConselhal> resultado = service.listarPorConselheiroComFiltros(10, LocalDate.now(),
                LocalDate.now(), "P", pageable);
        assertNotNull(resultado);
        verify(atividadeRepository, times(1)).findByConselheiroAndFiltros(
                eq(10), any(), any(), eq("P"), eq(pageable));
    }

    @Test
    void listarPorConselheiro_deveChamarRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<AtividadeConselhal> pagina = new PageImpl<>(Collections.emptyList());
        when(atividadeRepository.findByConselheiroIdPessoa(10, pageable)).thenReturn(pagina);

        Page<AtividadeConselhal> resultado = service.listarPorConselheiro(10, pageable);
        assertNotNull(resultado);
        verify(atividadeRepository, times(1)).findByConselheiroIdPessoa(10, pageable);
    }

    @Test
    void sumPontosValidadasNaoComputadas_quandoSomaNull_deveRetornarZero() {
        when(atividadeRepository.sumPontosAtividadesValidadasNaoComputadas(10)).thenReturn(null);
        int soma = service.sumPontosValidadasNaoComputadas(10);
        assertEquals(0, soma);
    }

    @Test
    void sumPontosValidadasNaoComputadas_quandoSomaNaoNull_deveRetornarValor() {
        when(atividadeRepository.sumPontosAtividadesValidadasNaoComputadas(10)).thenReturn(42);
        int soma = service.sumPontosValidadasNaoComputadas(10);
        assertEquals(42, soma);
    }

    @Test
    void countPendentesPorConselheiro_deveChamarRepository() {
        when(atividadeRepository.countByConselheiroIdPessoaAndInSituacao(10, "P")).thenReturn(5L);
        long count = service.countPendentesPorConselheiro(10);
        assertEquals(5L, count);
    }

    @Test
    void countTotalPorConselheiro_deveChamarRepository() {
        when(atividadeRepository.countByConselheiroIdPessoa(10)).thenReturn(20L);
        long count = service.countTotalPorConselheiro(10);
        assertEquals(20L, count);
    }

    @Test
    void contarAtividadesPorComprovante_deveChamarRepository() {
        when(atividadeRepository.countByComprovanteIdComprovante(5)).thenReturn(3L);
        long count = service.contarAtividadesPorComprovante(5);
        assertEquals(3L, count);
    }

    // Helper
    private GestaoConselheiro createVinculo(Gestao gestao, Conselheiro conselheiro) {
        GestaoConselheiro vinculo = new GestaoConselheiro();
        vinculo.setGestao(gestao);
        vinculo.setConselheiro(conselheiro);
        vinculo.setInSituacao(GestaoConselheiro.SITUACAO_ATIVO);
        return vinculo;
    }
}