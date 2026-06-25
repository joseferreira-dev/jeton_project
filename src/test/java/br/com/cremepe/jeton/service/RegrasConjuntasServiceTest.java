package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.domain.Regras;
import br.com.cremepe.jeton.domain.RegrasConjuntas;
import br.com.cremepe.jeton.repository.RegrasConjuntasRepository;
import br.com.cremepe.jeton.util.RegraValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do serviço de Regras Conjuntas (RegrasConjuntasService)")
class RegrasConjuntasServiceTest {

    @Mock
    private RegrasConjuntasRepository repositoryMock;

    @Mock
    private LogJetonService logJetonServiceMock;

    @Mock
    private RegrasService regrasServiceMock;

    @Mock
    private RegraValidator regraValidatorMock;

    @InjectMocks
    private RegrasConjuntasService service;

    // ========== HELPERS ==========

    private Regras criarRegra(Integer id, String nome) {
        Regras r = new Regras();
        r.setIdRegra(id);
        r.setNomeRegra(nome);
        r.setPontos(3);
        return r;
    }

    private RegrasConjuntas criarRegraConjunta(Integer id, String nome, String tipoLimite, Integer pontosLimite,
            List<Regras> regras) {
        RegrasConjuntas rc = new RegrasConjuntas();
        rc.setIdRegraConjunta(id);
        rc.setNomeRegra(nome);
        rc.setInTipoLimite(tipoLimite);
        rc.setPontosLimite(pontosLimite);
        rc.setRegrasAgrupadas(regras != null ? new ArrayList<>(regras) : new ArrayList<>());
        return rc;
    }

    private RegrasConjuntas criarRegraConjuntaValida(Integer id) {
        List<Regras> regras = List.of(criarRegra(1, "Regra A"), criarRegra(2, "Regra B"));
        return criarRegraConjunta(id, "Agrupamento Teste", RegrasConjuntas.TIPO_LIMITE_DIARIO, 10, regras);
    }

    // ========== TESTES DE CRIAÇÃO ==========

    @Test
    @DisplayName("deve criar regra conjunta com sucesso")
    void deveCriarRegraConjuntaComSucesso() {
        // Dado
        List<Regras> regras = new ArrayList<>(List.of(criarRegra(1, "Regra A"), criarRegra(2, "Regra B")));
        RegrasConjuntas regraConjunta = criarRegraConjunta(null, "Novo Agrupamento",
                RegrasConjuntas.TIPO_LIMITE_MENSAL, 15, regras);

        // Mocks do validator
        doNothing().when(regraValidatorMock).validarNomeRegraConjuntaUnico(anyString(), isNull());
        doNothing().when(regraValidatorMock).validarPontosLimitePositivo(anyInt());
        doNothing().when(regraValidatorMock).validarRegrasAgrupadasNaoVazias(regraConjunta);

        // Mock do save
        when(repositoryMock.save(any(RegrasConjuntas.class)))
                .thenAnswer(inv -> {
                    RegrasConjuntas rc = inv.getArgument(0);
                    rc.setIdRegraConjunta(999);
                    return rc;
                });

        // Quando
        RegrasConjuntas salva = service.criar(regraConjunta);

        // Então
        assertThat(salva).isNotNull();
        assertThat(salva.getIdRegraConjunta()).isEqualTo(999);
        assertThat(salva.getNomeRegra()).isEqualTo("Novo Agrupamento");
        assertThat(salva.getInTipoLimite()).isEqualTo(RegrasConjuntas.TIPO_LIMITE_MENSAL);
        assertThat(salva.getPontosLimite()).isEqualTo(15);
        assertThat(salva.getRegrasAgrupadas()).hasSize(2);

        verify(regraValidatorMock).validarNomeRegraConjuntaUnico("Novo Agrupamento", null);
        verify(regraValidatorMock).validarPontosLimitePositivo(15);
        verify(regraValidatorMock).validarRegrasAgrupadasNaoVazias(regraConjunta);
        verify(repositoryMock).save(regraConjunta);
        verify(logJetonServiceMock).logRegraConjuntaCriada(salva);
    }

    @Test
    @DisplayName("deve normalizar tipo de limite para D quando não informado")
    void deveNormalizarTipoLimiteParaDPadrao() {
        // Dado
        List<Regras> regras = List.of(criarRegra(1, "Regra A"));
        RegrasConjuntas regraConjunta = new RegrasConjuntas();
        regraConjunta.setNomeRegra("Agrupamento");
        regraConjunta.setInTipoLimite(null);
        regraConjunta.setPontosLimite(10);
        regraConjunta.setRegrasAgrupadas(regras);

        doNothing().when(regraValidatorMock).validarNomeRegraConjuntaUnico(anyString(), isNull());
        doNothing().when(regraValidatorMock).validarPontosLimitePositivo(anyInt());
        doNothing().when(regraValidatorMock).validarRegrasAgrupadasNaoVazias(regraConjunta);

        when(repositoryMock.save(any(RegrasConjuntas.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Quando
        RegrasConjuntas salva = service.criar(regraConjunta);

        // Então
        assertThat(salva.getInTipoLimite()).isEqualTo(RegrasConjuntas.TIPO_LIMITE_DIARIO);
    }

    @Test
    @DisplayName("deve lançar exceção ao criar regra conjunta com nome duplicado")
    void deveLancarExcecaoCriarNomeDuplicado() {
        // Dado
        RegrasConjuntas regraConjunta = criarRegraConjuntaValida(null);

        doThrow(new RuntimeException("Já existe uma regra conjunta cadastrada com o nome 'Agrupamento Teste'."))
                .when(regraValidatorMock).validarNomeRegraConjuntaUnico(anyString(), isNull());

        // Quando / Então
        assertThatThrownBy(() -> service.criar(regraConjunta))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Já existe uma regra conjunta cadastrada com o nome 'Agrupamento Teste'.");

        verify(repositoryMock, never()).save(any());
    }

    @Test
    @DisplayName("deve lançar exceção ao criar regra conjunta com pontos limite inválido")
    void deveLancarExcecaoCriarPontosLimiteInvalido() {
        // Dado
        RegrasConjuntas regraConjunta = criarRegraConjuntaValida(null);
        regraConjunta.setPontosLimite(0);

        doNothing().when(regraValidatorMock).validarNomeRegraConjuntaUnico(anyString(), isNull());
        doThrow(new RuntimeException("O limite de pontos deve ser maior que zero."))
                .when(regraValidatorMock).validarPontosLimitePositivo(0);

        // Quando / Então
        assertThatThrownBy(() -> service.criar(regraConjunta))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("O limite de pontos deve ser maior que zero.");

        verify(repositoryMock, never()).save(any());
    }

    @Test
    @DisplayName("deve lançar exceção ao criar regra conjunta sem regras agrupadas")
    void deveLancarExcecaoCriarSemRegrasAgrupadas() {
        // Dado
        RegrasConjuntas regraConjunta = criarRegraConjunta(null, "Agrupamento Vazio",
                RegrasConjuntas.TIPO_LIMITE_DIARIO, 10, new ArrayList<>());

        doNothing().when(regraValidatorMock).validarNomeRegraConjuntaUnico(anyString(), isNull());
        doNothing().when(regraValidatorMock).validarPontosLimitePositivo(anyInt());
        doThrow(new RuntimeException("A regra conjunta deve ter pelo menos uma regra associada."))
                .when(regraValidatorMock).validarRegrasAgrupadasNaoVazias(regraConjunta);

        // Quando / Então
        assertThatThrownBy(() -> service.criar(regraConjunta))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("A regra conjunta deve ter pelo menos uma regra associada.");

        verify(repositoryMock, never()).save(any());
    }

    // ========== TESTES DE ATUALIZAÇÃO ==========

    @Test
    @DisplayName("deve atualizar regra conjunta com sucesso")
    void deveAtualizarRegraConjuntaComSucesso() {
        // Dado
        Integer id = 50;
        List<Regras> regrasAntigas = List.of(criarRegra(1, "Regra A"), criarRegra(2, "Regra B"));
        List<Regras> regrasNovas = List.of(criarRegra(1, "Regra A"), criarRegra(3, "Regra C"));

        RegrasConjuntas existente = criarRegraConjunta(id, "Antigo", RegrasConjuntas.TIPO_LIMITE_DIARIO, 10,
                regrasAntigas);
        RegrasConjuntas atualizada = criarRegraConjunta(id, "Atualizado", RegrasConjuntas.TIPO_LIMITE_MENSAL, 20,
                regrasNovas);

        // ✅ Mock do existsById e findById
        when(repositoryMock.existsById(id)).thenReturn(true);
        when(repositoryMock.findById(id)).thenReturn(Optional.of(existente));

        doNothing().when(regraValidatorMock).validarNomeRegraConjuntaUnico("Atualizado", id);
        doNothing().when(regraValidatorMock).validarPontosLimitePositivo(20);
        doNothing().when(regraValidatorMock).validarRegrasAgrupadasNaoVazias(atualizada);

        when(repositoryMock.save(any(RegrasConjuntas.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Quando
        RegrasConjuntas salva = service.atualizar(atualizada);

        // Então
        assertThat(salva.getNomeRegra()).isEqualTo("Atualizado");
        assertThat(salva.getInTipoLimite()).isEqualTo(RegrasConjuntas.TIPO_LIMITE_MENSAL);
        assertThat(salva.getPontosLimite()).isEqualTo(20);
        assertThat(salva.getRegrasAgrupadas()).hasSize(2);
        assertThat(salva.getRegrasAgrupadas()).extracting(Regras::getIdRegra).containsExactlyInAnyOrder(1, 3);

        verify(repositoryMock).save(existente);
        verify(logJetonServiceMock).logRegraConjuntaAtualizada(any(RegrasConjuntas.class), eq(salva));
    }

    @Test
    @DisplayName("deve lançar exceção ao atualizar regra conjunta inexistente")
    void deveLancarExcecaoAtualizarInexistente() {
        // Dado
        Integer id = 999;
        RegrasConjuntas regraConjunta = criarRegraConjuntaValida(id);

        when(repositoryMock.existsById(id)).thenReturn(false);

        // Quando / Então
        assertThatThrownBy(() -> service.atualizar(regraConjunta))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Regra conjunta não encontrada para atualização.");

        verify(repositoryMock, never()).save(any());
    }

    @Test
    @DisplayName("deve lançar exceção ao atualizar regra conjunta sem ID")
    void deveLancarExcecaoAtualizarSemId() {
        // Dado
        RegrasConjuntas regraConjunta = criarRegraConjuntaValida(null);

        // Quando / Então
        assertThatThrownBy(() -> service.atualizar(regraConjunta))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("ID da regra conjunta não informado para atualização.");

        verify(repositoryMock, never()).existsById(any());
        verify(repositoryMock, never()).save(any());
    }

    // ========== TESTES DE EXCLUSÃO ==========

    @Test
    @DisplayName("deve excluir regra conjunta com sucesso e remover associações")
    void deveExcluirRegraConjuntaComSucesso() {
        // Dado
        Integer id = 50;
        List<Regras> regras = List.of(criarRegra(1, "Regra A"), criarRegra(2, "Regra B"));
        // ✅ Criação com lista mutável
        RegrasConjuntas regraConjunta = criarRegraConjunta(id, "Agrupamento", RegrasConjuntas.TIPO_LIMITE_DIARIO, 10,
                regras);

        when(repositoryMock.findById(id)).thenReturn(Optional.of(regraConjunta));

        // Quando
        service.excluir(id);

        // Então
        // Verifica que as regras agrupadas foram limpas
        ArgumentCaptor<RegrasConjuntas> captor = ArgumentCaptor.forClass(RegrasConjuntas.class);
        verify(repositoryMock).save(captor.capture());
        assertThat(captor.getValue().getRegrasAgrupadas()).isEmpty();

        verify(repositoryMock).deleteById(id);
        verify(logJetonServiceMock).logRegraConjuntaExcluida(eq(regraConjunta), anyString());
    }

    @Test
    @DisplayName("deve lançar exceção ao excluir regra conjunta inexistente")
    void deveLancarExcecaoExcluirInexistente() {
        // Dado
        Integer id = 999;

        when(repositoryMock.findById(id)).thenReturn(Optional.empty());

        // Quando / Então
        assertThatThrownBy(() -> service.excluir(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Regra Conjunta não encontrada com ID: " + id);

        verify(repositoryMock, never()).deleteById(any());
    }

    // ========== TESTES DE CONSULTA ==========

    @Test
    @DisplayName("deve listar todas as regras conjuntas")
    void deveListarTodasRegrasConjuntas() {
        // Dado
        List<RegrasConjuntas> lista = List.of(
                criarRegraConjuntaValida(1),
                criarRegraConjuntaValida(2));
        when(repositoryMock.findAll()).thenReturn(lista);

        // Quando
        List<RegrasConjuntas> resultado = service.listarTodos();

        // Então
        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getIdRegraConjunta()).isEqualTo(1);
        assertThat(resultado.get(1).getIdRegraConjunta()).isEqualTo(2);
        verify(repositoryMock).findAll();
    }

    @Test
    @DisplayName("deve buscar regra conjunta por ID com sucesso")
    void deveBuscarPorIdComSucesso() {
        // Dado
        Integer id = 10;
        RegrasConjuntas regraConjunta = criarRegraConjuntaValida(id);
        when(repositoryMock.findById(id)).thenReturn(Optional.of(regraConjunta));

        // Quando
        Optional<RegrasConjuntas> resultado = service.buscarPorId(id);

        // Então
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getIdRegraConjunta()).isEqualTo(id);
        verify(repositoryMock).findById(id);
    }

    @Test
    @DisplayName("deve retornar Optional vazio ao buscar regra conjunta inexistente")
    void deveRetornarOptionalVazioBuscarInexistente() {
        // Dado
        Integer id = 999;
        when(repositoryMock.findById(id)).thenReturn(Optional.empty());

        // Quando
        Optional<RegrasConjuntas> resultado = service.buscarPorId(id);

        // Então
        assertThat(resultado).isEmpty();
        verify(repositoryMock).findById(id);
    }

    @Test
    @DisplayName("deve buscar regra conjunta ou falhar com sucesso quando existe")
    void deveBuscarOuFalharComSucesso() {
        // Dado
        Integer id = 10;
        RegrasConjuntas regraConjunta = criarRegraConjuntaValida(id);
        when(repositoryMock.findById(id)).thenReturn(Optional.of(regraConjunta));

        // Quando
        RegrasConjuntas resultado = service.buscarOuFalhar(id);

        // Então
        assertThat(resultado).isNotNull();
        assertThat(resultado.getIdRegraConjunta()).isEqualTo(id);
        verify(repositoryMock).findById(id);
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
                .hasMessage("Regra Conjunta não encontrada com ID: " + id);
    }

    @Test
    @DisplayName("deve listar regras conjuntas paginadas com filtros")
    void deveListarPaginadasComFiltros() {
        // Dado
        String termo = "teste";
        String tipoLimite = "D";
        int page = 0, size = 10;
        String sortField = "nomeRegra", sortDir = "asc";

        Page<RegrasConjuntas> paginaEsperada = new PageImpl<>(List.of(criarRegraConjuntaValida(1)));

        when(repositoryMock.findAllByFilters(eq(termo), eq(tipoLimite), any(Pageable.class)))
                .thenReturn(paginaEsperada);

        // Quando
        Page<RegrasConjuntas> resultado = service.listarComPaginacaoEPesquisa(termo, tipoLimite,
                page, size, sortField, sortDir);

        // Então
        assertThat(resultado).isNotNull();
        assertThat(resultado.getContent()).hasSize(1);
        verify(repositoryMock).findAllByFilters(eq(termo), eq(tipoLimite), any(Pageable.class));
    }

    @Test
    @DisplayName("deve listar regras conjuntas paginadas sem ordenação (size = 0)")
    void deveListarPaginadasSemOrdenacao() {
        // Dado
        String termo = "";
        String tipoLimite = "";
        int page = 0, size = 0;

        Page<RegrasConjuntas> paginaEsperada = new PageImpl<>(List.of(criarRegraConjuntaValida(1)));

        when(repositoryMock.findAllByFilters(eq(termo), eq(tipoLimite), any(Pageable.class)))
                .thenReturn(paginaEsperada);

        // Quando
        Page<RegrasConjuntas> resultado = service.listarComPaginacaoEPesquisa(termo, tipoLimite,
                page, size, "nomeRegra", "asc");

        // Então
        assertThat(resultado).isNotNull();
        verify(repositoryMock).findAllByFilters(eq(termo), eq(tipoLimite), any(Pageable.class));
    }
}