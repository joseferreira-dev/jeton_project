package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.domain.Portaria;
import br.com.cremepe.jeton.domain.Regras;
import br.com.cremepe.jeton.domain.Resolucao;
import br.com.cremepe.jeton.repository.AtividadeConselhalRepository;
import br.com.cremepe.jeton.repository.PortariaRepository;
import br.com.cremepe.jeton.repository.RegrasRepository;
import br.com.cremepe.jeton.repository.ResolucaoRepository;
import br.com.cremepe.jeton.util.RegraValidator;
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
@DisplayName("Testes do serviço de Regras (RegrasService)")
class RegrasServiceTest {

    @Mock
    private RegrasRepository repositoryMock;

    @Mock
    private ResolucaoRepository resolucaoRepositoryMock;

    @Mock
    private PortariaRepository portariaRepositoryMock;

    @Mock
    private AtividadeConselhalRepository atividadeRepositoryMock;

    @Mock
    private LogJetonService logJetonServiceMock;

    @Mock
    private RegraValidator regraValidatorMock;

    @InjectMocks
    private RegrasService service;

    // ========== HELPERS ==========

    private Resolucao criarResolucao(Integer id) {
        Resolucao r = new Resolucao();
        r.setIdResolucao(id);
        r.setNumero(1);
        r.setAno(2026);
        return r;
    }

    private Portaria criarPortaria(Integer id) {
        Portaria p = new Portaria();
        p.setIdPortaria(id);
        p.setNumero(100);
        p.setAno(2026);
        p.setInRevogado(Portaria.REVOGADO_NAO);
        return p;
    }

    private Regras criarRegra(Integer id, String nome, Integer pontos, Resolucao resolucao, Portaria portaria,
            String revogado, String judicante) {
        Regras r = new Regras();
        r.setIdRegra(id);
        r.setNomeRegra(nome);
        r.setDescricao("Descrição da regra");
        r.setPontos(pontos);
        r.setInRevogado(revogado);
        r.setPontosLimitesTurno(0);
        r.setInJudicante(judicante);
        r.setResolucao(resolucao);
        r.setPortaria(portaria);
        return r;
    }

    private Regras criarRegraValida(Integer id) {
        return criarRegra(id, "Regra Teste", 3, criarResolucao(10), null,
                Regras.REVOGADO_NAO, Regras.JUDICANTE_NAO);
    }

    // ========== TESTES DE CRIAÇÃO ==========

    @Test
    @DisplayName("deve criar regra com sucesso")
    void deveCriarRegraComSucesso() {
        // Dado
        Resolucao resolucao = criarResolucao(10);
        Regras regra = criarRegra(null, "Nova Regra", 5, resolucao, null,
                Regras.REVOGADO_NAO, Regras.JUDICANTE_NAO);

        when(resolucaoRepositoryMock.findById(10)).thenReturn(Optional.of(resolucao));
        doNothing().when(regraValidatorMock).validarNomeRegraUnico(anyString(), isNull());
        doNothing().when(regraValidatorMock).validarPortariaNaoRevogada(any());

        when(repositoryMock.save(any(Regras.class)))
                .thenAnswer(inv -> {
                    Regras r = inv.getArgument(0);
                    r.setIdRegra(999);
                    return r;
                });

        // Quando
        Regras salva = service.criar(regra);

        // Então
        assertThat(salva).isNotNull();
        assertThat(salva.getIdRegra()).isEqualTo(999);
        assertThat(salva.getNomeRegra()).isEqualTo("Nova Regra");
        assertThat(salva.getPontos()).isEqualTo(5);
        assertThat(salva.getInRevogado()).isEqualTo(Regras.REVOGADO_NAO);
        assertThat(salva.getInJudicante()).isEqualTo(Regras.JUDICANTE_NAO);

        verify(resolucaoRepositoryMock).findById(10);
        verify(regraValidatorMock).validarNomeRegraUnico(anyString(), isNull());
        verify(regraValidatorMock).validarPortariaNaoRevogada(any());
        verify(repositoryMock).save(regra);
        verify(logJetonServiceMock).logRegraCriada(salva);
    }

    @Test
    @DisplayName("deve criar regra com portaria associada com sucesso")
    void deveCriarRegraComPortaria() {
        // Dado
        Resolucao resolucao = criarResolucao(10);
        Portaria portaria = criarPortaria(20);
        Regras regra = criarRegra(null, "Regra com Portaria", 3, resolucao, portaria,
                Regras.REVOGADO_NAO, Regras.JUDICANTE_NAO);

        when(resolucaoRepositoryMock.findById(10)).thenReturn(Optional.of(resolucao));
        when(portariaRepositoryMock.findById(20)).thenReturn(Optional.of(portaria));
        doNothing().when(regraValidatorMock).validarNomeRegraUnico(anyString(), isNull());
        doNothing().when(regraValidatorMock).validarPortariaNaoRevogada(portaria);

        when(repositoryMock.save(any(Regras.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Quando
        Regras salva = service.criar(regra);

        // Então
        assertThat(salva.getPortaria()).isEqualTo(portaria);
        verify(portariaRepositoryMock).findById(20);
        verify(regraValidatorMock).validarPortariaNaoRevogada(portaria);
    }

    @Test
    @DisplayName("deve lançar exceção ao criar regra com resolução nula")
    void deveLancarExcecaoCriarRegraResolucaoNula() {
        // Dado
        Regras regra = new Regras();
        regra.setResolucao(null);

        // Quando / Então
        assertThatThrownBy(() -> service.criar(regra))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("A resolução é obrigatória. Selecione uma resolução válida.");

        verify(repositoryMock, never()).save(any());
    }

    @Test
    @DisplayName("deve lançar exceção ao criar regra com resolução inexistente")
    void deveLancarExcecaoCriarRegraResolucaoInexistente() {
        // Dado
        Resolucao resolucao = criarResolucao(999);
        Regras regra = criarRegra(null, "Regra", 3, resolucao, null,
                Regras.REVOGADO_NAO, Regras.JUDICANTE_NAO);

        when(resolucaoRepositoryMock.findById(999)).thenReturn(Optional.empty());

        // Quando / Então
        assertThatThrownBy(() -> service.criar(regra))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Resolução não encontrada com ID: 999");

        verify(repositoryMock, never()).save(any());
    }

    @Test
    @DisplayName("deve lançar exceção ao criar regra com nome duplicado")
    void deveLancarExcecaoCriarRegraNomeDuplicado() {
        // Dado
        Resolucao resolucao = criarResolucao(10);
        Regras regra = criarRegra(null, "Regra Duplicada", 3, resolucao, null,
                Regras.REVOGADO_NAO, Regras.JUDICANTE_NAO);

        when(resolucaoRepositoryMock.findById(10)).thenReturn(Optional.of(resolucao));
        doThrow(new RuntimeException("Já existe uma regra cadastrada com o nome 'Regra Duplicada'."))
                .when(regraValidatorMock).validarNomeRegraUnico(eq("Regra Duplicada"), isNull());

        // Quando / Então
        assertThatThrownBy(() -> service.criar(regra))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Já existe uma regra cadastrada com o nome 'Regra Duplicada'.");

        verify(repositoryMock, never()).save(any());
    }

    @Test
    @DisplayName("deve lançar exceção ao criar regra com portaria revogada")
    void deveLancarExcecaoCriarRegraPortariaRevogada() {
        // Dado
        Resolucao resolucao = criarResolucao(10);
        Portaria portaria = criarPortaria(20);
        portaria.setInRevogado(Portaria.REVOGADO_SIM);
        Regras regra = criarRegra(null, "Regra", 3, resolucao, portaria,
                Regras.REVOGADO_NAO, Regras.JUDICANTE_NAO);

        when(resolucaoRepositoryMock.findById(10)).thenReturn(Optional.of(resolucao));
        when(portariaRepositoryMock.findById(20)).thenReturn(Optional.of(portaria));
        doNothing().when(regraValidatorMock).validarNomeRegraUnico(anyString(), isNull());
        doThrow(new RuntimeException("Não é possível vincular a regra a uma portaria revogada."))
                .when(regraValidatorMock).validarPortariaNaoRevogada(portaria);

        // Quando / Então
        assertThatThrownBy(() -> service.criar(regra))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Não é possível vincular a regra a uma portaria revogada.");

        verify(repositoryMock, never()).save(any());
    }

    @Test
    @DisplayName("deve normalizar flags ao criar regra")
    void deveNormalizarFlagsAoCriar() {
        // Dado
        Resolucao resolucao = criarResolucao(10);
        Regras regra = new Regras();
        regra.setResolucao(resolucao);
        regra.setNomeRegra("Regra");
        regra.setPontos(null);
        regra.setInRevogado(null);
        regra.setInJudicante(null);
        regra.setPontosLimitesTurno(null);

        when(resolucaoRepositoryMock.findById(10)).thenReturn(Optional.of(resolucao));
        doNothing().when(regraValidatorMock).validarNomeRegraUnico(anyString(), isNull());
        doNothing().when(regraValidatorMock).validarPortariaNaoRevogada(any());

        when(repositoryMock.save(any(Regras.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Quando
        Regras salva = service.criar(regra);

        // Então
        assertThat(salva.getInRevogado()).isEqualTo(Regras.REVOGADO_NAO);
        assertThat(salva.getInJudicante()).isEqualTo(Regras.JUDICANTE_NAO);
        assertThat(salva.getPontos()).isZero(); // ✅ agora testa normalização de null para 0
        assertThat(salva.getPontosLimitesTurno()).isZero();
        assertThat(salva.getNomeRegra()).isEqualTo("Regra");
    }

    // ========== TESTES DE ATUALIZAÇÃO ==========

    @Test
    @DisplayName("deve atualizar regra com sucesso")
    void deveAtualizarRegraComSucesso() {
        // Dado
        Integer id = 50;
        Resolucao resolucao = criarResolucao(10);
        Regras existente = criarRegraValida(id);
        Regras atualizada = criarRegra(id, "Regra Atualizada", 10, resolucao, null,
                Regras.REVOGADO_NAO, Regras.JUDICANTE_SIM);

        when(repositoryMock.existsById(id)).thenReturn(true);
        when(repositoryMock.findById(id)).thenReturn(Optional.of(existente));

        when(resolucaoRepositoryMock.findById(10)).thenReturn(Optional.of(resolucao));
        doNothing().when(regraValidatorMock).validarNomeRegraUnico(eq("Regra Atualizada"), eq(id));
        doNothing().when(regraValidatorMock).validarPortariaNaoRevogada(any());

        when(repositoryMock.save(any(Regras.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Quando
        Regras salva = service.atualizar(atualizada);

        // Então
        assertThat(salva.getNomeRegra()).isEqualTo("Regra Atualizada");
        assertThat(salva.getPontos()).isEqualTo(10);
        assertThat(salva.getInJudicante()).isEqualTo(Regras.JUDICANTE_SIM);

        verify(repositoryMock).save(existente);
        verify(logJetonServiceMock).logRegraAtualizada(any(Regras.class), eq(salva));
    }

    @Test
    @DisplayName("deve lançar exceção ao atualizar regra inexistente")
    void deveLancarExcecaoAtualizarRegraInexistente() {
        // Dado
        Integer id = 999;
        Regras regra = criarRegraValida(id);

        when(repositoryMock.existsById(id)).thenReturn(false);

        // Quando / Então
        assertThatThrownBy(() -> service.atualizar(regra))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Regra não encontrada para atualização.");

        verify(repositoryMock, never()).save(any());
    }

    @Test
    @DisplayName("deve lançar exceção ao atualizar regra sem ID")
    void deveLancarExcecaoAtualizarRegraSemId() {
        // Dado
        Regras regra = criarRegraValida(null);

        // Quando / Então
        assertThatThrownBy(() -> service.atualizar(regra))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("ID da regra não informado para atualização.");

        verify(repositoryMock, never()).findById(any());
        verify(repositoryMock, never()).save(any());
    }

    // ========== TESTES DE REVOGAÇÃO ==========

    @Test
    @DisplayName("deve revogar regra com sucesso")
    void deveRevogarRegraComSucesso() {
        // Dado
        Integer id = 50;
        Regras regra = criarRegraValida(id);

        when(repositoryMock.findById(id)).thenReturn(Optional.of(regra));
        doNothing().when(regraValidatorMock).validarRegraNaoRevogada(regra);

        when(repositoryMock.save(any(Regras.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Quando
        service.revogar(id);

        // Então
        assertThat(regra.getInRevogado()).isEqualTo(Regras.REVOGADO_SIM);
        verify(repositoryMock).save(regra);
        verify(logJetonServiceMock).logRegraRevogada(regra);
    }

    @Test
    @DisplayName("deve lançar exceção ao revogar regra já revogada")
    void deveLancarExcecaoRevogarRegraJaRevogada() {
        // Dado
        Integer id = 50;
        Regras regra = criarRegraValida(id);
        regra.setInRevogado(Regras.REVOGADO_SIM);

        when(repositoryMock.findById(id)).thenReturn(Optional.of(regra));
        doThrow(new RuntimeException("A regra já está revogada."))
                .when(regraValidatorMock).validarRegraNaoRevogada(regra);

        // Quando / Então
        assertThatThrownBy(() -> service.revogar(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("A regra já está revogada.");

        verify(repositoryMock, never()).save(any());
    }

    // ========== TESTES DE RESTAURAÇÃO ==========

    @Test
    @DisplayName("deve restaurar regra com sucesso")
    void deveRestaurarRegraComSucesso() {
        // Dado
        Integer id = 50;
        Regras regra = criarRegraValida(id);
        regra.setInRevogado(Regras.REVOGADO_SIM);

        when(repositoryMock.findById(id)).thenReturn(Optional.of(regra));
        doNothing().when(regraValidatorMock).validarRegraRevogada(regra);

        when(repositoryMock.save(any(Regras.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Quando
        service.restaurar(id);

        // Então
        assertThat(regra.getInRevogado()).isEqualTo(Regras.REVOGADO_NAO);
        verify(repositoryMock).save(regra);
        verify(logJetonServiceMock).logRegraRestaurada(regra);
    }

    @Test
    @DisplayName("deve lançar exceção ao restaurar regra em vigor")
    void deveLancarExcecaoRestaurarRegraEmVigor() {
        // Dado
        Integer id = 50;
        Regras regra = criarRegraValida(id);

        when(repositoryMock.findById(id)).thenReturn(Optional.of(regra));
        doThrow(new RuntimeException("A regra já está em vigor."))
                .when(regraValidatorMock).validarRegraRevogada(regra);

        // Quando / Então
        assertThatThrownBy(() -> service.restaurar(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("A regra já está em vigor.");

        verify(repositoryMock, never()).save(any());
    }

    // ========== TESTES DE EXCLUSÃO ==========

    @Test
    @DisplayName("deve excluir regra com sucesso quando revogada e sem atividades")
    void deveExcluirRegraComSucesso() {
        // Dado
        Integer id = 50;
        Regras regra = criarRegraValida(id);
        regra.setInRevogado(Regras.REVOGADO_SIM);

        when(repositoryMock.findById(id)).thenReturn(Optional.of(regra));
        when(atividadeRepositoryMock.countByRegraIdRegra(id)).thenReturn(0L);
        doNothing().when(regraValidatorMock).validarExclusaoRegra(regra, 0L);

        // Quando
        service.excluir(id);

        // Então
        verify(repositoryMock).deleteById(id);
        verify(logJetonServiceMock).logRegraExcluida(regra);
    }

    @Test
    @DisplayName("deve lançar exceção ao excluir regra em vigor")
    void deveLancarExcecaoExcluirRegraEmVigor() {
        // Dado
        Integer id = 50;
        Regras regra = criarRegraValida(id);

        when(repositoryMock.findById(id)).thenReturn(Optional.of(regra));
        doThrow(new RuntimeException("Para excluir, a regra deve estar revogada primeiro."))
                .when(regraValidatorMock).validarExclusaoRegra(regra, 0L);

        // Quando / Então
        assertThatThrownBy(() -> service.excluir(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Para excluir, a regra deve estar revogada primeiro.");

        verify(repositoryMock, never()).deleteById(any());
    }

    @Test
    @DisplayName("deve lançar exceção ao excluir regra com atividades vinculadas")
    void deveLancarExcecaoExcluirRegraComAtividades() {
        // Dado
        Integer id = 50;
        Regras regra = criarRegraValida(id);
        regra.setInRevogado(Regras.REVOGADO_SIM);

        when(repositoryMock.findById(id)).thenReturn(Optional.of(regra));
        when(atividadeRepositoryMock.countByRegraIdRegra(id)).thenReturn(5L);
        doThrow(new RuntimeException("Não é possível excluir a regra pois existem 5 atividade(s) vinculada(s) a ela."))
                .when(regraValidatorMock).validarExclusaoRegra(regra, 5L);

        // Quando / Então
        assertThatThrownBy(() -> service.excluir(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Não é possível excluir a regra pois existem 5 atividade(s) vinculada(s) a ela.");

        verify(repositoryMock, never()).deleteById(any());
    }

    // ========== TESTES DE CONSULTA ==========

    @Test
    @DisplayName("deve listar todas as regras")
    void deveListarTodasRegras() {
        // Dado
        List<Regras> lista = List.of(
                criarRegraValida(1),
                criarRegraValida(2));
        when(repositoryMock.findAll()).thenReturn(lista);

        // Quando
        List<Regras> resultado = service.listarTodos();

        // Então
        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getIdRegra()).isEqualTo(1);
        assertThat(resultado.get(1).getIdRegra()).isEqualTo(2);
        verify(repositoryMock).findAll();
    }

    @Test
    @DisplayName("deve buscar regra por ID com sucesso")
    void deveBuscarRegraPorIdComSucesso() {
        // Dado
        Integer id = 10;
        Regras regra = criarRegraValida(id);
        when(repositoryMock.findById(id)).thenReturn(Optional.of(regra));

        // Quando
        Optional<Regras> resultado = service.buscarPorId(id);

        // Então
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getIdRegra()).isEqualTo(id);
        verify(repositoryMock).findById(id);
    }

    @Test
    @DisplayName("deve retornar Optional vazio ao buscar regra inexistente")
    void deveRetornarOptionalVazioBuscarInexistente() {
        // Dado
        Integer id = 999;
        when(repositoryMock.findById(id)).thenReturn(Optional.empty());

        // Quando
        Optional<Regras> resultado = service.buscarPorId(id);

        // Então
        assertThat(resultado).isEmpty();
        verify(repositoryMock).findById(id);
    }

    @Test
    @DisplayName("deve buscar regra ou falhar com sucesso quando existe")
    void deveBuscarOuFalharComSucesso() {
        // Dado
        Integer id = 10;
        Regras regra = criarRegraValida(id);
        when(repositoryMock.findById(id)).thenReturn(Optional.of(regra));

        // Quando
        Regras resultado = service.buscarOuFalhar(id);

        // Então
        assertThat(resultado).isNotNull();
        assertThat(resultado.getIdRegra()).isEqualTo(id);
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
                .hasMessage("Regra não encontrada com ID: " + id);
    }

    @Test
    @DisplayName("deve listar regras paginadas com filtros")
    void deveListarRegrasPaginadasComFiltros() {
        // Dado
        String termo = "teste";
        String situacao = "N";
        String judicante = "S";
        int page = 0, size = 10;
        String sortField = "nomeRegra", sortDir = "asc";

        Page<Regras> paginaEsperada = new PageImpl<>(List.of(criarRegraValida(1)));

        when(repositoryMock.findAllByFilters(eq(termo), eq(situacao), eq(judicante), any(Pageable.class)))
                .thenReturn(paginaEsperada);

        // Quando
        Page<Regras> resultado = service.listarComPaginacaoEPesquisa(termo, situacao, judicante,
                page, size, sortField, sortDir);

        // Então
        assertThat(resultado).isNotNull();
        assertThat(resultado.getContent()).hasSize(1);
        verify(repositoryMock).findAllByFilters(eq(termo), eq(situacao), eq(judicante), any(Pageable.class));
    }

    @Test
    @DisplayName("deve listar resoluções que possuem regras")
    void deveListarResolucoesComRegras() {
        // Dado
        List<Resolucao> resolucoes = List.of(criarResolucao(1), criarResolucao(2));
        when(repositoryMock.findResolucoesComRegras()).thenReturn(resolucoes);

        // Quando
        List<Resolucao> resultado = service.listarResolucoesComRegras();

        // Então
        assertThat(resultado).hasSize(2);
        verify(repositoryMock).findResolucoesComRegras();
    }

    @Test
    @DisplayName("deve listar portarias que possuem regras")
    void deveListarPortariasComRegras() {
        // Dado
        List<Portaria> portarias = List.of(criarPortaria(1), criarPortaria(2));
        when(repositoryMock.findPortariasComRegras()).thenReturn(portarias);

        // Quando
        List<Portaria> resultado = service.listarPortariasComRegras();

        // Então
        assertThat(resultado).hasSize(2);
        verify(repositoryMock).findPortariasComRegras();
    }

    @Test
    @DisplayName("deve listar portarias compatíveis com uma resolução")
    void deveListarPortariasCompativeis() {
        // Dado
        Integer idResolucao = 10;
        List<Portaria> portarias = List.of(criarPortaria(1), criarPortaria(2));
        when(repositoryMock.findPortariasCompativeis(idResolucao)).thenReturn(portarias);

        // Quando
        List<Portaria> resultado = service.listarPortariasCompativeis(idResolucao);

        // Então
        assertThat(resultado).hasSize(2);
        verify(repositoryMock).findPortariasCompativeis(idResolucao);
    }

    @Test
    @DisplayName("deve listar resoluções compatíveis com uma portaria")
    void deveListarResolucoesCompativeis() {
        // Dado
        Integer idPortaria = 20;
        List<Resolucao> resolucoes = List.of(criarResolucao(1), criarResolucao(2));
        when(repositoryMock.findResolucoesCompativeis(idPortaria)).thenReturn(resolucoes);

        // Quando
        List<Resolucao> resultado = service.listarResolucoesCompativeis(idPortaria);

        // Então
        assertThat(resultado).hasSize(2);
        verify(repositoryMock).findResolucoesCompativeis(idPortaria);
    }

    @Test
    @DisplayName("deve listar regras exatas por resolução e portaria")
    void deveListarRegrasExatas() {
        // Dado
        Integer idResolucao = 10;
        Integer idPortaria = 20;
        List<Regras> regras = List.of(criarRegraValida(1), criarRegraValida(2));
        when(repositoryMock.findRegrasExatas(idResolucao, idPortaria)).thenReturn(regras);

        // Quando
        List<Regras> resultado = service.listarRegrasExatas(idResolucao, idPortaria);

        // Então
        assertThat(resultado).hasSize(2);
        verify(repositoryMock).findRegrasExatas(idResolucao, idPortaria);
    }

    @Test
    @DisplayName("deve listar regras por normativas incluindo revogadas")
    void deveListarRegrasPorNormativasInclusiveRevogadas() {
        // Dado
        Integer idResolucao = 10;
        Integer idPortaria = 20;
        List<Regras> regras = List.of(criarRegraValida(1), criarRegraValida(2));
        when(repositoryMock.findRegrasPorNormativasInclusiveRevogadas(idResolucao, idPortaria))
                .thenReturn(regras);

        // Quando
        List<Regras> resultado = service.listarRegrasPorNormativasInclusiveRevogadas(idResolucao, idPortaria);

        // Então
        assertThat(resultado).hasSize(2);
        verify(repositoryMock).findRegrasPorNormativasInclusiveRevogadas(idResolucao, idPortaria);
    }

    @Test
    @DisplayName("deve buscar resolução vigente por data")
    void deveBuscarResolucaoPorData() {
        // Dado
        LocalDate data = LocalDate.of(2026, 6, 15);
        Resolucao resolucao = criarResolucao(10);
        when(repositoryMock.findResolucaoPorData(data)).thenReturn(List.of(resolucao));

        // Quando
        Optional<Resolucao> resultado = service.buscarResolucaoPorData(data);

        // Então
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getIdResolucao()).isEqualTo(10);
        verify(repositoryMock).findResolucaoPorData(data);
    }

    @Test
    @DisplayName("deve retornar Optional vazio ao buscar resolução por data sem resultado")
    void deveRetornarOptionalVazioBuscarResolucaoPorData() {
        // Dado
        LocalDate data = LocalDate.of(2026, 6, 15);
        when(repositoryMock.findResolucaoPorData(data)).thenReturn(List.of());

        // Quando
        Optional<Resolucao> resultado = service.buscarResolucaoPorData(data);

        // Então
        assertThat(resultado).isEmpty();
        verify(repositoryMock).findResolucaoPorData(data);
    }

    @Test
    @DisplayName("deve buscar portaria vigente por data")
    void deveBuscarPortariaPorData() {
        // Dado
        LocalDate data = LocalDate.of(2026, 6, 15);
        Portaria portaria = criarPortaria(20);
        when(repositoryMock.findPortariaPorData(data)).thenReturn(List.of(portaria));

        // Quando
        Optional<Portaria> resultado = service.buscarPortariaPorData(data);

        // Então
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getIdPortaria()).isEqualTo(20);
        verify(repositoryMock).findPortariaPorData(data);
    }

    @Test
    @DisplayName("deve retornar Optional vazio ao buscar portaria por data sem resultado")
    void deveRetornarOptionalVazioBuscarPortariaPorData() {
        // Dado
        LocalDate data = LocalDate.of(2026, 6, 15);
        when(repositoryMock.findPortariaPorData(data)).thenReturn(List.of());

        // Quando
        Optional<Portaria> resultado = service.buscarPortariaPorData(data);

        // Então
        assertThat(resultado).isEmpty();
        verify(repositoryMock).findPortariaPorData(data);
    }

    @Test
    @DisplayName("deve listar regras por resolução")
    void deveListarRegrasPorResolucao() {
        // Dado
        Integer idResolucao = 10;
        List<Regras> regras = List.of(criarRegraValida(1), criarRegraValida(2));
        when(repositoryMock.findByResolucaoIdResolucao(idResolucao)).thenReturn(regras);

        // Quando
        List<Regras> resultado = service.listarRegrasPorResolucao(idResolucao);

        // Então
        assertThat(resultado).hasSize(2);
        verify(repositoryMock).findByResolucaoIdResolucao(idResolucao);
    }

    @Test
    @DisplayName("deve listar todas as regras quando idResolucao é nulo")
    void deveListarTodasRegrasQuandoResolucaoNula() {
        // Dado
        List<Regras> regras = List.of(criarRegraValida(1), criarRegraValida(2));
        when(repositoryMock.findAll()).thenReturn(regras);

        // Quando
        List<Regras> resultado = service.listarRegrasPorResolucao(null);

        // Então
        assertThat(resultado).hasSize(2);
        verify(repositoryMock).findAll();
    }
}