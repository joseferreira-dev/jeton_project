package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.domain.Gestao;
import br.com.cremepe.jeton.repository.GestaoRepository;
import br.com.cremepe.jeton.util.GestaoValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do serviço de Gestão")
class GestaoServiceTest {

    @Mock
    private GestaoRepository gestaoRepositoryMock;

    @Mock
    private LogJetonService logJetonServiceMock;

    @Mock
    private GestaoValidator gestaoValidatorMock;

    @InjectMocks
    private GestaoService service;

    // ========== HELPERS ==========

    private Gestao criarGestao(Integer id, String nome, LocalDate inicio, LocalDate fim) {
        Gestao g = new Gestao();
        g.setIdGestao(id);
        g.setNomeGestao(nome);
        g.setDtInicio(inicio);
        g.setDtFim(fim);
        return g;
    }

    private Gestao criarGestaoValida(Integer id) {
        LocalDate inicio = LocalDate.now().minusMonths(1);
        LocalDate fim = LocalDate.now().plusMonths(1);
        return criarGestao(id, "Gestão Teste " + id, inicio, fim);
    }

    // ========== TESTES DE CRIAÇÃO ==========

    @Test
    @DisplayName("deve criar gestão com sucesso")
    void deveCriarGestaoComSucesso() {
        // Dado
        Gestao gestao = criarGestaoValida(null);
        Integer idEsperado = 10;

        // O validator não lança exceção
        doNothing().when(gestaoValidatorMock).validarGestao(gestao);

        // Mock do save
        when(gestaoRepositoryMock.save(any(Gestao.class)))
                .thenAnswer(inv -> {
                    Gestao g = inv.getArgument(0);
                    g.setIdGestao(idEsperado);
                    return g;
                });

        // Quando
        Gestao salva = service.criar(gestao);

        // Então
        assertThat(salva).isNotNull();
        assertThat(salva.getIdGestao()).isEqualTo(idEsperado);
        assertThat(salva.getNomeGestao()).isEqualTo(gestao.getNomeGestao());

        // Verifica se o validator foi chamado
        verify(gestaoValidatorMock).validarGestao(gestao);

        // Verifica se o repositório salvou
        verify(gestaoRepositoryMock).save(gestao);

        // Verifica se o log foi registrado
        verify(logJetonServiceMock).logGestaoCriada(salva);
    }

    @Test
    @DisplayName("deve lançar exceção ao criar gestão com nome duplicado")
    void deveLancarExcecaoCriarGestaoNomeDuplicado() {
        // Dado
        Gestao gestao = criarGestaoValida(null);

        doThrow(new RuntimeException("Já existe uma gestão cadastrada com o nome 'Gestão Teste'"))
                .when(gestaoValidatorMock).validarGestao(gestao);

        // Quando / Então
        assertThatThrownBy(() -> service.criar(gestao))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Já existe uma gestão cadastrada com o nome 'Gestão Teste'");

        // Verifica que o save não foi chamado
        verify(gestaoRepositoryMock, never()).save(any());
        verify(logJetonServiceMock, never()).logGestaoCriada(any());
    }

    @Test
    @DisplayName("deve lançar exceção ao criar gestão com período sobreposto")
    void deveLancarExcecaoCriarGestaoPeriodoSobreposto() {
        // Dado
        Gestao gestao = criarGestaoValida(null);

        doThrow(new RuntimeException("O período selecionado coincide com uma gestão já cadastrada."))
                .when(gestaoValidatorMock).validarGestao(gestao);

        // Quando / Então
        assertThatThrownBy(() -> service.criar(gestao))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("O período selecionado coincide com uma gestão já cadastrada.");

        verify(gestaoRepositoryMock, never()).save(any());
    }

    // ========== TESTES DE ATUALIZAÇÃO ==========

    @Test
    @DisplayName("deve atualizar gestão com sucesso")
    void deveAtualizarGestaoComSucesso() {
        // Dado
        Integer id = 100;
        Gestao existente = criarGestaoValida(id);
        existente.setNomeGestao("Nome Antigo");

        Gestao atualizada = criarGestaoValida(id);
        atualizada.setNomeGestao("Nome Novo");

        when(gestaoRepositoryMock.findById(id)).thenReturn(Optional.of(existente));
        doNothing().when(gestaoValidatorMock).validarGestao(atualizada);

        // Mock do save
        when(gestaoRepositoryMock.save(any(Gestao.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Quando
        Gestao resultado = service.atualizar(atualizada);

        // Então
        assertThat(resultado).isNotNull();
        assertThat(resultado.getNomeGestao()).isEqualTo("Nome Novo");

        // Verifica que o repositório salvou a entidade existente (atualizada)
        verify(gestaoRepositoryMock).save(existente);

        // Verifica que o validator foi chamado com os novos dados
        verify(gestaoValidatorMock).validarGestao(atualizada);

        // Verifica o log (com cópia do antigo e novo)
        verify(logJetonServiceMock).logGestaoAtualizada(any(Gestao.class), eq(resultado));
    }

    @Test
    @DisplayName("deve lançar exceção ao atualizar gestão inexistente")
    void deveLancarExcecaoAtualizarGestaoInexistente() {
        // Dado
        Integer id = 999;
        Gestao gestao = criarGestaoValida(id);

        when(gestaoRepositoryMock.findById(id)).thenReturn(Optional.empty());

        // Quando / Então
        assertThatThrownBy(() -> service.atualizar(gestao))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Gestão não encontrada com ID: " + id);

        verify(gestaoRepositoryMock, never()).save(any());
        verify(logJetonServiceMock, never()).logGestaoAtualizada(any(), any());
    }

    @Test
    @DisplayName("deve lançar exceção ao atualizar gestão sem ID informado")
    void deveLancarExcecaoAtualizarGestaoSemId() {
        // Dado
        Gestao gestao = criarGestaoValida(null);

        // Quando / Então
        assertThatThrownBy(() -> service.atualizar(gestao))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("ID da gestão não informado para atualização.");

        verify(gestaoRepositoryMock, never()).findById(any());
        verify(gestaoRepositoryMock, never()).save(any());
    }

    // ========== TESTES DE EXCLUSÃO ==========

    @Test
    @DisplayName("deve excluir gestão com sucesso")
    void deveExcluirGestaoComSucesso() {
        // Dado
        Integer id = 50;
        Gestao existente = criarGestaoValida(id);

        when(gestaoRepositoryMock.findById(id)).thenReturn(Optional.of(existente));
        doNothing().when(gestaoRepositoryMock).deleteById(id);

        // Quando
        service.excluir(id);

        // Então
        verify(gestaoRepositoryMock).deleteById(id);
        verify(logJetonServiceMock).logGestaoExcluida(any(Gestao.class));
    }

    @Test
    @DisplayName("deve lançar exceção ao excluir gestão inexistente")
    void deveLancarExcecaoExcluirGestaoInexistente() {
        // Dado
        Integer id = 999;

        when(gestaoRepositoryMock.findById(id)).thenReturn(Optional.empty());

        // Quando / Então
        assertThatThrownBy(() -> service.excluir(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Gestão não encontrada com ID: " + id);

        verify(gestaoRepositoryMock, never()).deleteById(any());
        verify(logJetonServiceMock, never()).logGestaoExcluida(any());
    }

    // ========== TESTES DE CONSULTA ==========

    @Test
    @DisplayName("deve listar todas as gestões com sucesso")
    void deveListarTodasAsGestoes() {
        // Dado
        List<Gestao> lista = List.of(
                criarGestaoValida(1),
                criarGestaoValida(2));

        when(gestaoRepositoryMock.findAll()).thenReturn(lista);

        // Quando
        List<Gestao> resultado = service.listarTodos();

        // Então
        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getIdGestao()).isEqualTo(1);
        assertThat(resultado.get(1).getIdGestao()).isEqualTo(2);
        verify(gestaoRepositoryMock).findAll();
    }

    @Test
    @DisplayName("deve retornar lista vazia quando não houver gestões")
    void deveRetornarListaVaziaQuandoNaoHouverGestoes() {
        // Dado
        when(gestaoRepositoryMock.findAll()).thenReturn(List.of());

        // Quando
        List<Gestao> resultado = service.listarTodos();

        // Então
        assertThat(resultado).isEmpty();
        verify(gestaoRepositoryMock).findAll();
    }

    @Test
    @DisplayName("deve buscar gestão por ID com sucesso")
    void deveBuscarGestaoPorIdComSucesso() {
        // Dado
        Integer id = 10;
        Gestao gestao = criarGestaoValida(id);

        when(gestaoRepositoryMock.findById(id)).thenReturn(Optional.of(gestao));

        // Quando
        Optional<Gestao> resultado = service.buscarPorId(id);

        // Então
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getIdGestao()).isEqualTo(id);
        verify(gestaoRepositoryMock).findById(id);
    }

    @Test
    @DisplayName("deve retornar Optional vazio ao buscar gestão por ID inexistente")
    void deveRetornarOptionalVazioAoBuscarGestaoInexistente() {
        // Dado
        Integer id = 999;

        when(gestaoRepositoryMock.findById(id)).thenReturn(Optional.empty());

        // Quando
        Optional<Gestao> resultado = service.buscarPorId(id);

        // Então
        assertThat(resultado).isEmpty();
        verify(gestaoRepositoryMock).findById(id);
    }

    @Test
    @DisplayName("deve buscar gestão ou falhar com sucesso quando existe")
    void deveBuscarGestaoOuFalharComSucesso() {
        // Dado
        Integer id = 5;
        Gestao gestao = criarGestaoValida(id);

        when(gestaoRepositoryMock.findById(id)).thenReturn(Optional.of(gestao));

        // Quando
        Gestao resultado = service.buscarGestaoOuFalhar(id);

        // Então
        assertThat(resultado).isNotNull();
        assertThat(resultado.getIdGestao()).isEqualTo(id);
        verify(gestaoRepositoryMock).findById(id);
    }

    @Test
    @DisplayName("deve lançar exceção ao buscar gestão ou falhar com ID inexistente")
    void deveLancarExcecaoBuscarGestaoOuFalharInexistente() {
        // Dado
        Integer id = 999;

        when(gestaoRepositoryMock.findById(id)).thenReturn(Optional.empty());

        // Quando / Então
        assertThatThrownBy(() -> service.buscarGestaoOuFalhar(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Gestão não encontrada com ID: " + id);

        verify(gestaoRepositoryMock).findById(id);
    }

    @Test
    @DisplayName("deve listar gestões paginadas com filtro de nome")
    void deveListarGestoesPaginadasComFiltros() {
        // Dado
        String termo = "Teste";
        int page = 0;
        int size = 10;
        String sortField = "nomeGestao";
        String sortDir = "asc";

        Page<Gestao> paginaEsperada = new PageImpl<>(List.of(criarGestaoValida(1), criarGestaoValida(2)));

        when(gestaoRepositoryMock.findByNomeGestaoContainingIgnoreCase(eq(termo), any(Pageable.class)))
                .thenReturn(paginaEsperada);

        // Quando
        Page<Gestao> resultado = service.listarComPaginacaoEPesquisa(termo, page, size, sortField, sortDir);

        // Então
        assertThat(resultado).isNotNull();
        assertThat(resultado.getContent()).hasSize(2);

        // Verifica se o repositório foi chamado com os parâmetros corretos
        verify(gestaoRepositoryMock).findByNomeGestaoContainingIgnoreCase(eq(termo), any(Pageable.class));
    }

    @Test
    @DisplayName("deve listar gestões paginadas com ordenação decrescente")
    void deveListarGestoesPaginadasComOrdenacaoDesc() {
        // Dado
        String termo = "Gestão";
        int page = 0;
        int size = 20;
        String sortField = "dtInicio";
        String sortDir = "desc";

        Page<Gestao> paginaEsperada = new PageImpl<>(List.of(criarGestaoValida(5), criarGestaoValida(3)));

        when(gestaoRepositoryMock.findByNomeGestaoContainingIgnoreCase(eq(termo), any(Pageable.class)))
                .thenReturn(paginaEsperada);

        // Quando
        Page<Gestao> resultado = service.listarComPaginacaoEPesquisa(termo, page, size, sortField, sortDir);

        // Então
        assertThat(resultado).isNotNull();
        assertThat(resultado.getContent()).hasSize(2);

        // O Pageable é criado com Sort.Direction.DESC
        verify(gestaoRepositoryMock).findByNomeGestaoContainingIgnoreCase(eq(termo), any(Pageable.class));
    }

    @Test
    @DisplayName("deve listar gestões paginadas sem paginação (size = 0)")
    void deveListarGestoesPaginadasSemPaginacao() {
        // Dado
        String termo = "";
        int page = 0;
        int size = 0; // tamanho zero = sem paginação
        String sortField = "nomeGestao";
        String sortDir = "asc";

        Page<Gestao> paginaEsperada = new PageImpl<>(List.of(criarGestaoValida(1)));

        when(gestaoRepositoryMock.findByNomeGestaoContainingIgnoreCase(eq(termo), any(Pageable.class)))
                .thenReturn(paginaEsperada);

        // Quando
        Page<Gestao> resultado = service.listarComPaginacaoEPesquisa(termo, page, size, sortField, sortDir);

        // Então
        assertThat(resultado).isNotNull();
        // O Pageable é criado com Pageable.unpaged(sort)
        verify(gestaoRepositoryMock).findByNomeGestaoContainingIgnoreCase(eq(termo), any(Pageable.class));
    }
}