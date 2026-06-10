package br.com.cremepe.jeton.controller;

import br.com.cremepe.jeton.domain.*;
import br.com.cremepe.jeton.dto.LoteAtividadeDTO;
import br.com.cremepe.jeton.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AtividadeConselhalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AtividadeConselhalService atividadeService;

    @MockitoBean
    private AtividadeLoteService atividadeLoteService;

    @MockitoBean
    private ConselheiroService conselheiroService;

    @MockitoBean
    private GestaoService gestaoService;

    @MockitoBean
    private RegrasService regrasService;

    @MockitoBean
    private TipoAnexoService tipoAnexoService;

    private MockHttpSession session;
    private ViewUserLogin usuarioLogado;

    @BeforeEach
    void setUp() {
        usuarioLogado = new ViewUserLogin();
        ReflectionTestUtils.setField(usuarioLogado, "idPessoa", 1);
        ReflectionTestUtils.setField(usuarioLogado, "nome", "Usuário Teste");
        ReflectionTestUtils.setField(usuarioLogado, "inTipoPessoa", "F");
        ReflectionTestUtils.setField(usuarioLogado, "permissoes", "A");

        session = new MockHttpSession();
        session.setAttribute("usuarioLogado", usuarioLogado);
    }

    // =========================================================================
    // Testes do endpoint GET /atividades
    // =========================================================================

    @Test
    void listar_quandoUsuarioNaoAutenticado_deveRedirecionarParaLogin() throws Exception {
        mockMvc.perform(get("/atividades"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void listar_quandoAutenticado_deveRetornarPaginaComAtividades() throws Exception {
        Page<AtividadeConselhal> pagina = new PageImpl<>(Collections.emptyList());
        when(atividadeService.listarComPaginacaoEPesquisa(anyString(), anyString(), anyString(), anyString(),
                any(), any(), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(pagina);

        mockMvc.perform(get("/atividades").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("atividadeconselhal/lista"))
                .andExpect(model().attributeExists("paginaAtividades"));
    }

    // =========================================================================
    // Testes do endpoint GET /atividades/novo
    // =========================================================================

    @Test
    void prepararNovo_quandoNaoAutenticado_deveRedirecionar() throws Exception {
        mockMvc.perform(get("/atividades/novo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void prepararNovo_quandoAutenticado_deveRetornarFormulario() throws Exception {
        when(conselheiroService.listarTodos()).thenReturn(Collections.emptyList());
        when(gestaoService.listarTodos()).thenReturn(Collections.emptyList());
        when(regrasService.listarResolucoesComRegras()).thenReturn(Collections.emptyList());
        when(regrasService.listarPortariasComRegras()).thenReturn(Collections.emptyList());
        when(tipoAnexoService.listarTodos()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/atividades/novo").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("atividadeconselhal/formulario"))
                .andExpect(model().attributeExists("atividade", "listaConselheiros", "listaGestoes"));
    }

    // =========================================================================
    // Testes do endpoint POST /atividades/salvar
    // =========================================================================

    @Test
    void salvar_quandoCriacaoComSucesso_deveRedirecionarComMensagem() throws Exception {
        AtividadeConselhal atividade = new AtividadeConselhal();
        atividade.setIdAtividade(null);
        when(atividadeService.criar(any(AtividadeConselhal.class), any(), any(), any()))
                .thenReturn(new AtividadeConselhal());

        mockMvc.perform(post("/atividades/salvar")
                .session(session)
                .param("dataAtividadePura", "2025-03-15T14:30")
                .flashAttr("atividade", atividade))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/atividades"))
                .andExpect(flash().attributeExists("sucesso"));
    }

    @Test
    void salvar_quandoAtualizacaoComSucesso_deveRedirecionarComMensagem() throws Exception {
        AtividadeConselhal atividade = new AtividadeConselhal();
        atividade.setIdAtividade(1);
        when(atividadeService.buscarPorId(1)).thenReturn(java.util.Optional.of(atividade));
        when(atividadeService.atualizar(any(AtividadeConselhal.class), any(), any(), any(), any()))
                .thenReturn(atividade);

        mockMvc.perform(post("/atividades/salvar")
                .session(session)
                .param("dataAtividadePura", "2025-03-15T14:30")
                .flashAttr("atividade", atividade))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/atividades"))
                .andExpect(flash().attributeExists("sucesso"));
    }

    @Test
    void salvar_quandoDataInvalida_deveRedirecionarComErro() throws Exception {
        AtividadeConselhal atividade = new AtividadeConselhal();
        atividade.setIdAtividade(null);

        mockMvc.perform(post("/atividades/salvar")
                .session(session)
                .param("dataAtividadePura", "data-invalida")
                .flashAttr("atividade", atividade))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/atividades/novo"))
                .andExpect(flash().attributeExists("erro"));
    }

    @Test
    void salvar_quandoServicoLancaExcecao_deveRedirecionarComErro() throws Exception {
        AtividadeConselhal atividade = new AtividadeConselhal();
        atividade.setIdAtividade(null);
        when(atividadeService.criar(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Erro de negócio"));

        mockMvc.perform(post("/atividades/salvar")
                .session(session)
                .param("dataAtividadePura", "2025-03-15T14:30")
                .flashAttr("atividade", atividade))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/atividades"))
                .andExpect(flash().attributeExists("erro"));
    }

    // =========================================================================
    // Testes do endpoint GET /atividades/editar/{id}
    // =========================================================================

    @Test
    void prepararEditar_quandoAtividadeEncontradaSemCompartilhamento_deveRetornarFormulario() throws Exception {
        Comprovante comprovante = new Comprovante();
        comprovante.setIdComprovante(10);
        AtividadeConselhal atividade = new AtividadeConselhal();
        atividade.setIdAtividade(1);
        atividade.setComprovante(comprovante);

        when(atividadeService.buscarPorId(1)).thenReturn(java.util.Optional.of(atividade));
        when(atividadeService.contarAtividadesPorComprovante(10)).thenReturn(1L);
        when(conselheiroService.listarTodos()).thenReturn(Collections.emptyList());
        when(gestaoService.listarTodos()).thenReturn(Collections.emptyList());
        when(regrasService.listarResolucoesComRegras()).thenReturn(Collections.emptyList());
        when(regrasService.listarPortariasComRegras()).thenReturn(Collections.emptyList());
        when(tipoAnexoService.listarTodos()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/atividades/editar/1").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("atividadeconselhal/formulario"))
                .andExpect(model().attributeExists("atividade"));
    }

    @Test
    void prepararEditar_quandoComprovanteCompartilhado_deveRedirecionarParaEdicaoLote() throws Exception {
        Comprovante comprovante = new Comprovante();
        comprovante.setIdComprovante(10);
        AtividadeConselhal atividade = new AtividadeConselhal();
        atividade.setIdAtividade(1);
        atividade.setComprovante(comprovante);

        when(atividadeService.buscarPorId(1)).thenReturn(java.util.Optional.of(atividade));
        when(atividadeService.contarAtividadesPorComprovante(10)).thenReturn(2L);

        mockMvc.perform(get("/atividades/editar/1").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/atividades/lote/editar/10"))
                .andExpect(flash().attributeExists("info"));
    }

    @Test
    void prepararEditar_quandoAtividadeNaoEncontrada_deveRedirecionarComErro() throws Exception {
        when(atividadeService.buscarPorId(99)).thenReturn(Optional.empty());

        mockMvc.perform(get("/atividades/editar/99").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/atividades"))
                .andExpect(flash().attributeExists("erro"));
    }

    // =========================================================================
    // Testes do endpoint GET /atividades/validar/{id}
    // =========================================================================

    @Test
    void validar_quandoSucesso_deveRedirecionarComMensagem() throws Exception {
        doNothing().when(atividadeService).validar(1);

        mockMvc.perform(get("/atividades/validar/1").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/atividades"))
                .andExpect(flash().attributeExists("sucesso"));
    }

    @Test
    void validar_quandoErro_deveRedirecionarComMensagemDeErro() throws Exception {
        doThrow(new RuntimeException("Atividade já fechada")).when(atividadeService).validar(1);

        mockMvc.perform(get("/atividades/validar/1").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/atividades"))
                .andExpect(flash().attributeExists("erro"));
    }

    @Test
    void validar_quandoNaoAutenticado_deveRedirecionarParaLogin() throws Exception {
        mockMvc.perform(get("/atividades/validar/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // =========================================================================
    // Testes do endpoint GET /atividades/desvalidar/{id}
    // =========================================================================

    @Test
    void desvalidar_quandoSucesso_deveRedirecionarComMensagem() throws Exception {
        doNothing().when(atividadeService).desvalidar(1);

        mockMvc.perform(get("/atividades/desvalidar/1").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/atividades"))
                .andExpect(flash().attributeExists("sucesso"));
    }

    @Test
    void desvalidar_quandoErro_deveRedirecionarComMensagemErro() throws Exception {
        doThrow(new RuntimeException("Atividade já computada")).when(atividadeService).desvalidar(1);

        mockMvc.perform(get("/atividades/desvalidar/1").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/atividades"))
                .andExpect(flash().attributeExists("erro"));
    }

    @Test
    void desvalidar_quandoNaoAutenticado_deveRedirecionarParaLogin() throws Exception {
        mockMvc.perform(get("/atividades/desvalidar/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // =========================================================================
    // Testes do endpoint GET /atividades/excluir/{id}
    // =========================================================================

    @Test
    void excluir_quandoSucesso_deveRedirecionarComMensagem() throws Exception {
        doNothing().when(atividadeService).excluir(1);

        mockMvc.perform(get("/atividades/excluir/1").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/atividades"))
                .andExpect(flash().attributeExists("sucesso"));
    }

    @Test
    void excluir_quandoErro_deveRedirecionarComMensagemErro() throws Exception {
        doThrow(new RuntimeException("Erro ao excluir")).when(atividadeService).excluir(1);

        mockMvc.perform(get("/atividades/excluir/1").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/atividades"))
                .andExpect(flash().attributeExists("erro"));
    }

    @Test
    void excluir_quandoNaoAutenticado_deveRedirecionarParaLogin() throws Exception {
        mockMvc.perform(get("/atividades/excluir/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // =========================================================================
    // Testes do endpoint GET /atividades/lote/novo
    // =========================================================================

    @Test
    void prepararLote_quandoAutenticado_deveRetornarFormulario() throws Exception {
        when(gestaoService.listarTodos()).thenReturn(Collections.emptyList());
        when(tipoAnexoService.listarTodos()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/atividades/lote/novo").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("atividadeconselhal/lote_formulario"))
                .andExpect(model().attributeExists("listaGestoes", "listaTiposAnexo"));
    }

    @Test
    void prepararLote_quandoNaoAutenticado_deveRedirecionar() throws Exception {
        mockMvc.perform(get("/atividades/lote/novo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // =========================================================================
    // Testes do endpoint POST /atividades/lote/salvar
    // =========================================================================

    @Test
    void salvarLote_quandoSucesso_deveRedirecionarComMensagem() throws Exception {
        LoteAtividadeDTO dto = new LoteAtividadeDTO();
        dto.setIdsConselheiros(List.of(1, 2));
        when(atividadeLoteService.criarLote(any(LoteAtividadeDTO.class)))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(post("/atividades/lote/salvar")
                .session(session)
                .flashAttr("loteAtividadeDTO", dto))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/atividades"))
                .andExpect(flash().attributeExists("sucesso"));
    }

    @Test
    void salvarLote_quandoNaoAutenticado_deveRedirecionar() throws Exception {
        LoteAtividadeDTO dto = new LoteAtividadeDTO();
        mockMvc.perform(post("/atividades/lote/salvar")
                .flashAttr("loteAtividadeDTO", dto))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void salvarLote_quandoErroNoServico_deveRedirecionarComMensagemErro() throws Exception {
        LoteAtividadeDTO dto = new LoteAtividadeDTO();
        dto.setIdsConselheiros(List.of(1));
        when(atividadeLoteService.criarLote(any())).thenThrow(new RuntimeException("Erro ao criar lote"));

        mockMvc.perform(post("/atividades/lote/salvar")
                .session(session)
                .flashAttr("loteAtividadeDTO", dto))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/atividades"))
                .andExpect(flash().attributeExists("erro"));
    }

    @Test
    void salvarLote_quandoTurnoVazio_deveChamarServicoComTurnoNull() throws Exception {
        LoteAtividadeDTO dto = new LoteAtividadeDTO();
        dto.setIdsConselheiros(List.of(1));
        dto.setInTurno("");
        when(atividadeLoteService.criarLote(any(LoteAtividadeDTO.class)))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(post("/atividades/lote/salvar")
                .session(session)
                .flashAttr("loteAtividadeDTO", dto))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/atividades"));

        verify(atividadeLoteService).criarLote(argThat(dtoCapturado -> dtoCapturado.getInTurno() == null));
    }

    // =========================================================================
    // Testes do endpoint GET /atividades/lote/editar/{idComprovante}
    // =========================================================================

    @Test
    void prepararEdicaoLote_quandoComprovanteExistente_deveRetornarFormulario() throws Exception {
        Comprovante comprovante = new Comprovante();
        comprovante.setIdComprovante(10);

        Conselheiro conselheiro = new Conselheiro();
        conselheiro.setIdPessoa(1);
        Pessoa pessoa = new Pessoa();
        pessoa.setNome("Dr. Teste");
        conselheiro.setPessoa(pessoa);

        Regras regra = new Regras();
        regra.setIdRegra(100);
        regra.setNomeRegra("Regra Teste");

        AtividadeConselhal referencia = new AtividadeConselhal();
        referencia.setComprovante(comprovante);
        referencia.setConselheiro(conselheiro);
        referencia.setRegra(regra);

        when(atividadeLoteService.listarPorComprovante(10)).thenReturn(List.of(referencia));
        when(gestaoService.listarTodos()).thenReturn(Collections.emptyList());
        when(tipoAnexoService.listarTodos()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/atividades/lote/editar/10").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("atividadeconselhal/lote_edicao"))
                .andExpect(model().attributeExists("atividadeReferencia", "quantidade", "idComprovante"));
    }

    @Test
    void prepararEdicaoLote_quandoComprovanteNaoEncontrado_deveRedirecionarComErro() throws Exception {
        when(atividadeLoteService.listarPorComprovante(99)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/atividades/lote/editar/99").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/atividades"))
                .andExpect(flash().attributeExists("erro"));
    }

    @Test
    void prepararEdicaoLote_quandoNaoAutenticado_deveRedirecionar() throws Exception {
        mockMvc.perform(get("/atividades/lote/editar/10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // =========================================================================
    // Testes do endpoint POST /atividades/lote/atualizar/{idComprovante}
    // =========================================================================

    @Test
    void atualizarLote_quandoSucesso_deveRedirecionarComMensagem() throws Exception {
        LoteAtividadeDTO dto = new LoteAtividadeDTO();
        dto.setIdsConselheiros(List.of(1, 2));
        doNothing().when(atividadeLoteService).atualizarLote(eq(10), any(LoteAtividadeDTO.class));

        mockMvc.perform(post("/atividades/lote/atualizar/10")
                .session(session)
                .flashAttr("loteAtividadeDTO", dto))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/atividades"))
                .andExpect(flash().attributeExists("sucesso"));
    }

    @Test
    void atualizarLote_quandoNaoAutenticado_deveRedirecionar() throws Exception {
        LoteAtividadeDTO dto = new LoteAtividadeDTO();
        mockMvc.perform(post("/atividades/lote/atualizar/10")
                .flashAttr("loteAtividadeDTO", dto))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void atualizarLote_quandoErroNoServico_deveRedirecionarComMensagemErro() throws Exception {
        LoteAtividadeDTO dto = new LoteAtividadeDTO();
        dto.setIdsConselheiros(List.of(1));
        doThrow(new RuntimeException("Erro ao atualizar lote")).when(atividadeLoteService)
                .atualizarLote(eq(10), any(LoteAtividadeDTO.class));

        mockMvc.perform(post("/atividades/lote/atualizar/10")
                .session(session)
                .flashAttr("loteAtividadeDTO", dto))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/atividades"))
                .andExpect(flash().attributeExists("erro"));
    }

    @Test
    void atualizarLote_quandoTurnoVazio_deveChamarServicoComTurnoNull() throws Exception {
        LoteAtividadeDTO dto = new LoteAtividadeDTO();
        dto.setIdsConselheiros(List.of(1));
        dto.setInTurno("");
        doNothing().when(atividadeLoteService).atualizarLote(eq(10), any(LoteAtividadeDTO.class));

        mockMvc.perform(post("/atividades/lote/atualizar/10")
                .session(session)
                .flashAttr("loteAtividadeDTO", dto))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/atividades"));

        verify(atividadeLoteService).atualizarLote(eq(10), argThat(dtoCapturado -> dtoCapturado.getInTurno() == null));
    }
}