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
    // Testes do endpoint POST /atividades/salvar (criação)
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
}