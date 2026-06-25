package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.domain.Resolucao;
import br.com.cremepe.jeton.repository.RegrasRepository;
import br.com.cremepe.jeton.repository.ResolucaoRepository;
import br.com.cremepe.jeton.util.NormativaValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do serviço de Resoluções (ResolucaoService)")
class ResolucaoServiceTest {

    @Mock
    private ResolucaoRepository repositoryMock;

    @Mock
    private RegrasRepository regrasRepositoryMock;

    @Mock
    private LogJetonService logJetonServiceMock;

    @Mock
    private NormativaValidator normativaValidatorMock;

    @InjectMocks
    private ResolucaoService service;

    // ========== HELPERS ==========

    private Resolucao criarResolucao(Integer id, Integer numero, Integer ano, LocalDate inicio, LocalDate fim,
            String revogado, BigDecimal valorJeton) {
        Resolucao r = new Resolucao();
        r.setIdResolucao(id);
        r.setNumero(numero);
        r.setAno(ano);
        r.setDtInicioVigencia(inicio);
        r.setDtFimVigencia(fim);
        r.setInRevogado(revogado);
        r.setValorJeton(valorJeton != null ? valorJeton : BigDecimal.valueOf(100));
        r.setPontosPorJeton(3);
        r.setMaxJetonsDia(3);
        r.setMaxJetonsPeriodo(1);
        r.setMaxJetonsMes(22);
        r.setEmenta("Ementa padrão");
        return r;
    }

    private Resolucao criarResolucaoValida(Integer id) {
        return criarResolucao(id, 10, 2026,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                Resolucao.REVOGADO_NAO,
                BigDecimal.valueOf(100));
    }

    // ========== TESTES DE CRIAÇÃO ==========

    @Test
    @DisplayName("deve criar resolução com sucesso")
    void deveCriarResolucaoComSucesso() {
        // Dado
        Integer numero = 10;
        Integer ano = 2026;
        LocalDate inicio = LocalDate.of(2026, 1, 1);
        LocalDate fim = LocalDate.of(2026, 12, 31);
        BigDecimal valorJeton = BigDecimal.valueOf(150);

        Resolucao resolucao = criarResolucao(null, numero, ano, inicio, fim, Resolucao.REVOGADO_NAO, valorJeton);

        doNothing().when(normativaValidatorMock)
                .validarResolucao(numero, ano, inicio, fim, null);

        when(repositoryMock.save(any(Resolucao.class)))
                .thenAnswer(inv -> {
                    Resolucao r = inv.getArgument(0);
                    r.setIdResolucao(999);
                    return r;
                });

        // Quando
        Resolucao salva = service.criar(resolucao);

        // Então
        assertThat(salva).isNotNull();
        assertThat(salva.getIdResolucao()).isEqualTo(999);
        assertThat(salva.getNumero()).isEqualTo(numero);
        assertThat(salva.getAno()).isEqualTo(ano);
        assertThat(salva.getValorJeton()).isEqualByComparingTo(valorJeton);
        assertThat(salva.getInRevogado()).isEqualTo(Resolucao.REVOGADO_NAO);

        verify(normativaValidatorMock).validarResolucao(numero, ano, inicio, fim, null);
        verify(repositoryMock).save(resolucao);
        verify(logJetonServiceMock).logResolucaoCriada(salva);
    }

    @Test
    @DisplayName("deve normalizar inRevogado para N ao criar resolução sem valor")
    void deveNormalizarInRevogadoParaNAoCriar() {
        // Dado
        Resolucao resolucao = criarResolucao(null, 10, 2026,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                null,
                BigDecimal.valueOf(100));

        doNothing().when(normativaValidatorMock).validarResolucao(anyInt(), anyInt(), any(), any(), isNull());

        when(repositoryMock.save(any(Resolucao.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Quando
        Resolucao salva = service.criar(resolucao);

        // Então
        assertThat(salva.getInRevogado()).isEqualTo(Resolucao.REVOGADO_NAO);
    }

    @Test
    @DisplayName("deve definir valores padrão para parâmetros ausentes ao criar resolução")
    void deveDefinirValoresPadraoParaParametrosAusentes() {
        // Dado - Resolução sem valores para parâmetros que devem ser padronizados
        Resolucao resolucao = new Resolucao();
        resolucao.setNumero(1);
        resolucao.setAno(2026);
        resolucao.setEmenta("Ementa teste");
        resolucao.setValorJeton(null); // ✅ deve ser definido como zero
        resolucao.setPontosPorJeton(null);
        resolucao.setMaxJetonsDia(null);
        resolucao.setMaxJetonsPeriodo(null);
        resolucao.setMaxJetonsMes(null);
        resolucao.setInRevogado(null);

        doNothing().when(normativaValidatorMock)
                .validarResolucao(anyInt(), anyInt(), any(), any(), isNull());

        when(repositoryMock.save(any(Resolucao.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Quando
        Resolucao salva = service.criar(resolucao);

        // Então
        assertThat(salva.getValorJeton()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(salva.getPontosPorJeton()).isEqualTo(3);
        assertThat(salva.getMaxJetonsDia()).isEqualTo(3);
        assertThat(salva.getMaxJetonsPeriodo()).isEqualTo(1);
        assertThat(salva.getMaxJetonsMes()).isEqualTo(22);
        assertThat(salva.getInRevogado()).isEqualTo(Resolucao.REVOGADO_NAO);
    }

    @Test
    @DisplayName("deve lançar exceção ao criar resolução com número duplicado")
    void deveLancarExcecaoCriarResolucaoNumeroDuplicado() {
        // Dado
        Resolucao resolucao = criarResolucaoValida(null);

        doThrow(new RuntimeException("Já existe uma resolução cadastrada com o número 10/2026"))
                .when(normativaValidatorMock).validarResolucao(anyInt(), anyInt(), any(), any(), isNull());

        // Quando / Então
        assertThatThrownBy(() -> service.criar(resolucao))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Já existe uma resolução cadastrada com o número 10/2026");

        verify(repositoryMock, never()).save(any());
    }

    // ========== TESTES DE ATUALIZAÇÃO ==========

    @Test
    @DisplayName("deve atualizar resolução com sucesso")
    void deveAtualizarResolucaoComSucesso() {
        // Dado
        Integer id = 50;
        Integer novoNumero = 20;
        Integer novoAno = 2027;
        LocalDate novoInicio = LocalDate.of(2027, 1, 1);
        LocalDate novoFim = LocalDate.of(2027, 12, 31);
        BigDecimal novoValor = BigDecimal.valueOf(200);

        Resolucao existente = criarResolucaoValida(id);
        Resolucao atualizada = criarResolucao(id, novoNumero, novoAno, novoInicio, novoFim,
                Resolucao.REVOGADO_NAO, novoValor);

        when(repositoryMock.existsById(id)).thenReturn(true);
        when(repositoryMock.findById(id)).thenReturn(Optional.of(existente));
        doNothing().when(normativaValidatorMock)
                .validarResolucao(novoNumero, novoAno, novoInicio, novoFim, id);

        when(repositoryMock.save(any(Resolucao.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Quando
        Resolucao salva = service.atualizar(atualizada);

        // Então
        assertThat(salva.getNumero()).isEqualTo(novoNumero);
        assertThat(salva.getAno()).isEqualTo(novoAno);
        assertThat(salva.getValorJeton()).isEqualByComparingTo(novoValor);
        assertThat(salva.getDtInicioVigencia()).isEqualTo(novoInicio);
        assertThat(salva.getDtFimVigencia()).isEqualTo(novoFim);

        verify(repositoryMock).save(existente);
        verify(logJetonServiceMock).logResolucaoAtualizada(any(Resolucao.class), eq(salva));
    }

    @Test
    @DisplayName("deve lançar exceção ao atualizar resolução inexistente")
    void deveLancarExcecaoAtualizarResolucaoInexistente() {
        // Dado
        Integer id = 999;
        Resolucao resolucao = criarResolucaoValida(id);

        when(repositoryMock.existsById(id)).thenReturn(false);

        // Quando / Então
        assertThatThrownBy(() -> service.atualizar(resolucao))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Resolução não encontrada para atualização.");

        verify(repositoryMock, never()).save(any());
    }

    @Test
    @DisplayName("deve lançar exceção ao atualizar resolução sem ID")
    void deveLancarExcecaoAtualizarResolucaoSemId() {
        // Dado
        Resolucao resolucao = criarResolucaoValida(null);

        // Quando / Então
        assertThatThrownBy(() -> service.atualizar(resolucao))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("ID da resolução não informado para atualização.");

        verify(repositoryMock, never()).existsById(any());
        verify(repositoryMock, never()).save(any());
    }

    // ========== TESTES DE REVOGAÇÃO ==========

    @Test
    @DisplayName("deve revogar resolução com sucesso e revogar regras vinculadas")
    void deveRevogarResolucaoComSucesso() {
        // Dado
        Integer id = 50;
        Resolucao resolucao = criarResolucaoValida(id);

        when(repositoryMock.findById(id)).thenReturn(Optional.of(resolucao));

        when(repositoryMock.save(any(Resolucao.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Quando
        service.revogar(id);

        // Então
        assertThat(resolucao.getInRevogado()).isEqualTo(Resolucao.REVOGADO_SIM);
        verify(repositoryMock).save(resolucao);
        verify(regrasRepositoryMock).revogarRegrasPorResolucao(id);
        verify(logJetonServiceMock).logResolucaoRevogada(resolucao);
    }

    @Test
    @DisplayName("deve lançar exceção ao revogar resolução já revogada")
    void deveLancarExcecaoRevogarResolucaoJaRevogada() {
        // Dado
        Integer id = 50;
        Resolucao resolucao = criarResolucao(id, 10, 2026,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                Resolucao.REVOGADO_SIM,
                BigDecimal.valueOf(100));

        when(repositoryMock.findById(id)).thenReturn(Optional.of(resolucao));

        // Quando / Então
        assertThatThrownBy(() -> service.revogar(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("A resolução já está revogada.");

        verify(repositoryMock, never()).save(any());
        verify(regrasRepositoryMock, never()).revogarRegrasPorResolucao(anyInt());
    }

    // ========== TESTES DE RESTAURAÇÃO ==========

    @Test
    @DisplayName("deve restaurar resolução com sucesso e restaurar regras vinculadas")
    void deveRestaurarResolucaoComSucesso() {
        // Dado
        Integer id = 50;
        Resolucao resolucao = criarResolucao(id, 10, 2026,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                Resolucao.REVOGADO_SIM,
                BigDecimal.valueOf(100));

        when(repositoryMock.findById(id)).thenReturn(Optional.of(resolucao));

        when(repositoryMock.save(any(Resolucao.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Quando
        service.restaurar(id);

        // Então
        assertThat(resolucao.getInRevogado()).isEqualTo(Resolucao.REVOGADO_NAO);
        verify(repositoryMock).save(resolucao);
        verify(regrasRepositoryMock).restaurarRegrasPorResolucao(id);
        verify(logJetonServiceMock).logResolucaoRestaurada(resolucao);
    }

    @Test
    @DisplayName("deve lançar exceção ao restaurar resolução já em vigor")
    void deveLancarExcecaoRestaurarResolucaoJaEmVigor() {
        // Dado
        Integer id = 50;
        Resolucao resolucao = criarResolucaoValida(id);

        when(repositoryMock.findById(id)).thenReturn(Optional.of(resolucao));

        // Quando / Então
        assertThatThrownBy(() -> service.restaurar(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("A resolução já está em vigor.");

        verify(repositoryMock, never()).save(any());
        verify(regrasRepositoryMock, never()).restaurarRegrasPorResolucao(anyInt());
    }

    // ========== TESTES DE EXCLUSÃO ==========

    @Test
    @DisplayName("deve excluir resolução com sucesso quando revogada e sem regras vinculadas")
    void deveExcluirResolucaoComSucesso() {
        // Dado
        Integer id = 50;
        Resolucao resolucao = criarResolucao(id, 10, 2026,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                Resolucao.REVOGADO_SIM,
                BigDecimal.valueOf(100));

        when(repositoryMock.findById(id)).thenReturn(Optional.of(resolucao));
        when(regrasRepositoryMock.countByResolucaoIdResolucao(id)).thenReturn(0L);

        // Quando
        service.excluir(id);

        // Então
        verify(repositoryMock).deleteById(id);
        verify(logJetonServiceMock).logResolucaoExcluida(resolucao);
    }

    @Test
    @DisplayName("deve lançar exceção ao excluir resolução em vigor (não revogada)")
    void deveLancarExcecaoExcluirResolucaoEmVigor() {
        // Dado
        Integer id = 50;
        Resolucao resolucao = criarResolucaoValida(id);

        when(repositoryMock.findById(id)).thenReturn(Optional.of(resolucao));

        // Quando / Então
        assertThatThrownBy(() -> service.excluir(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Para excluir, a resolução deve estar revogada primeiro.");

        verify(repositoryMock, never()).deleteById(any());
    }

    @Test
    @DisplayName("deve lançar exceção ao excluir resolução com regras vinculadas")
    void deveLancarExcecaoExcluirResolucaoComRegras() {
        // Dado
        Integer id = 50;
        Resolucao resolucao = criarResolucao(id, 10, 2026,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                Resolucao.REVOGADO_SIM,
                BigDecimal.valueOf(100));

        when(repositoryMock.findById(id)).thenReturn(Optional.of(resolucao));
        when(regrasRepositoryMock.countByResolucaoIdResolucao(id)).thenReturn(3L);

        // Quando / Então
        assertThatThrownBy(() -> service.excluir(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessage(
                        "Não é possível excluir a resolução pois existem 3 regra(s) vinculada(s). Revogue-as ou exclua-as antes.");

        verify(repositoryMock, never()).deleteById(any());
    }

    // ========== TESTES DE CONSULTA ==========

    @Test
    @DisplayName("deve listar todas as resoluções")
    void deveListarTodasResolucoes() {
        // Dado
        List<Resolucao> lista = List.of(
                criarResolucaoValida(1),
                criarResolucaoValida(2));
        when(repositoryMock.findAll()).thenReturn(lista);

        // Quando
        List<Resolucao> resultado = service.listarTodos();

        // Então
        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getIdResolucao()).isEqualTo(1);
        assertThat(resultado.get(1).getIdResolucao()).isEqualTo(2);
        verify(repositoryMock).findAll();
    }

    @Test
    @DisplayName("deve buscar resolução por ID com sucesso")
    void deveBuscarResolucaoPorIdComSucesso() {
        // Dado
        Integer id = 10;
        Resolucao resolucao = criarResolucaoValida(id);
        when(repositoryMock.findById(id)).thenReturn(Optional.of(resolucao));

        // Quando
        Optional<Resolucao> resultado = service.buscarPorId(id);

        // Então
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getIdResolucao()).isEqualTo(id);
        verify(repositoryMock).findById(id);
    }

    @Test
    @DisplayName("deve retornar Optional vazio ao buscar resolução inexistente")
    void deveRetornarOptionalVazioBuscarInexistente() {
        // Dado
        Integer id = 999;
        when(repositoryMock.findById(id)).thenReturn(Optional.empty());

        // Quando
        Optional<Resolucao> resultado = service.buscarPorId(id);

        // Então
        assertThat(resultado).isEmpty();
        verify(repositoryMock).findById(id);
    }

    @Test
    @DisplayName("deve buscar resolução ou falhar com sucesso quando existe")
    void deveBuscarOuFalharComSucesso() {
        // Dado
        Integer id = 10;
        Resolucao resolucao = criarResolucaoValida(id);
        when(repositoryMock.findById(id)).thenReturn(Optional.of(resolucao));

        // Quando
        Resolucao resultado = service.buscarOuFalhar(id);

        // Então
        assertThat(resultado).isNotNull();
        assertThat(resultado.getIdResolucao()).isEqualTo(id);
    }

    @Test
    @DisplayName("deve lançar exceção ao buscar ou falhar com ID inexistente")
    void deveLancarExcecaoBuscarOuFalharInexistente() {
        // Dado
        Integer id = 999;
        when(repositoryMock.findById(id)).thenReturn(Optional.empty());

        // Quando / Então
        assertThatThrownBy(() -> service.buscarOuFalhar(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Resolução não encontrada com ID: " + id);
    }

    @Test
    @DisplayName("deve listar resoluções paginadas com filtros")
    void deveListarResolucoesPaginadasComFiltros() {
        // Dado
        String termo = "10";
        String situacao = "N";
        int page = 0, size = 10;
        String sortField = "ano", sortDir = "desc";

        Page<Resolucao> paginaEsperada = new PageImpl<>(List.of(criarResolucaoValida(1)));

        when(repositoryMock.findAllByFilters(eq(termo), eq(situacao), any(Pageable.class)))
                .thenReturn(paginaEsperada);

        // Quando
        Page<Resolucao> resultado = service.listarComPaginacaoEPesquisa(termo, situacao, page, size, sortField,
                sortDir);

        // Então
        assertThat(resultado).isNotNull();
        assertThat(resultado.getContent()).hasSize(1);
        verify(repositoryMock).findAllByFilters(eq(termo), eq(situacao), any(Pageable.class));
    }

    @Test
    @DisplayName("deve listar resoluções paginadas sem ordenação (size = 0)")
    void deveListarResolucoesPaginadasSemOrdenacao() {
        // Dado
        String termo = "";
        String situacao = "";
        int page = 0, size = 0;

        Page<Resolucao> paginaEsperada = new PageImpl<>(List.of(criarResolucaoValida(1)));

        when(repositoryMock.findAllByFilters(eq(termo), eq(situacao), any(Pageable.class)))
                .thenReturn(paginaEsperada);

        // Quando
        Page<Resolucao> resultado = service.listarComPaginacaoEPesquisa(termo, situacao, page, size, "ano", "asc");

        // Então
        assertThat(resultado).isNotNull();
        verify(repositoryMock).findAllByFilters(eq(termo), eq(situacao), any(Pageable.class));
    }
}