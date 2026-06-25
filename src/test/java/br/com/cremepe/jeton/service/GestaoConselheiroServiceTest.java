package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.domain.*;
import br.com.cremepe.jeton.repository.AtividadeConselhalRepository;
import br.com.cremepe.jeton.repository.ConselheiroRepository;
import br.com.cremepe.jeton.repository.GestaoConselheiroRepository;
import br.com.cremepe.jeton.repository.GestaoRepository;
import br.com.cremepe.jeton.repository.PessoaRepository;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do serviço de Vínculos Gestão-Conselheiro")
class GestaoConselheiroServiceTest {

    @Mock
    private GestaoConselheiroRepository gestaoConselheiroRepositoryMock;

    @Mock
    private GestaoRepository gestaoRepositoryMock;

    @Mock
    private ConselheiroRepository conselheiroRepositoryMock;

    @Mock
    private AtividadeConselhalRepository atividadeRepositoryMock;

    @Mock
    private PessoaRepository pessoaRepositoryMock;

    @Mock
    private LogJetonService logJetonServiceMock;

    @InjectMocks
    private GestaoConselheiroService service;

    // ========== HELPERS ==========

    private Gestao criarGestao(Integer id, String nome) {
        Gestao g = new Gestao();
        g.setIdGestao(id);
        g.setNomeGestao(nome);
        return g;
    }

    private Conselheiro criarConselheiro(Integer id, String nome) {
        Pessoa p = new Pessoa();
        p.setIdPessoa(id);
        p.setNome(nome);
        Conselheiro c = new Conselheiro();
        c.setIdPessoa(id);
        c.setPessoa(p);
        c.setInSituacao(Conselheiro.SITUACAO_ATIVO);
        return c;
    }

    private GestaoConselheiro criarVinculo(Integer idGestao, Integer idPessoa, String situacao) {
        GestaoConselheiroId id = new GestaoConselheiroId(idGestao, idPessoa);
        GestaoConselheiro v = new GestaoConselheiro();
        v.setId(id);
        v.setGestao(criarGestao(idGestao, "Gestão " + idGestao));
        v.setConselheiro(criarConselheiro(idPessoa, "Médico " + idPessoa));
        v.setInSituacao(situacao);
        return v;
    }

    // ========== TESTES DE CRIAÇÃO ==========

    @Test
    @DisplayName("deve criar vínculo ativo com sucesso e inativar outros vínculos do conselheiro")
    void deveCriarVinculoAtivoComSucesso() {
        // Dado
        Integer idGestao = 1;
        Integer idPessoa = 100;
        GestaoConselheiro vinculo = criarVinculo(idGestao, idPessoa, GestaoConselheiro.SITUACAO_ATIVO);

        when(gestaoConselheiroRepositoryMock.existsByGestaoAndConselheiro(idGestao, idPessoa))
                .thenReturn(false);
        when(gestaoRepositoryMock.findById(idGestao))
                .thenReturn(Optional.of(criarGestao(idGestao, "Gestão Teste")));
        when(pessoaRepositoryMock.findById(idPessoa))
                .thenReturn(Optional.of(criarPessoa(idPessoa, "Médico Teste")));

        GestaoConselheiro outroVinculoAtivo = criarVinculo(2, idPessoa, GestaoConselheiro.SITUACAO_ATIVO);
        when(gestaoConselheiroRepositoryMock.findByConselheiroIdPessoaAndInSituacao(idPessoa,
                GestaoConselheiro.SITUACAO_ATIVO))
                .thenReturn(List.of(outroVinculoAtivo));

        when(gestaoConselheiroRepositoryMock.save(any(GestaoConselheiro.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Quando
        GestaoConselheiro salvo = service.criar(vinculo);

        // Então
        assertThat(salvo).isNotNull();
        assertThat(salvo.getInSituacao()).isEqualTo(GestaoConselheiro.SITUACAO_ATIVO);

        ArgumentCaptor<GestaoConselheiro> captor = ArgumentCaptor.forClass(GestaoConselheiro.class);
        verify(gestaoConselheiroRepositoryMock, times(2)).save(captor.capture());
        boolean algumInativado = captor.getAllValues().stream()
                .anyMatch(v -> GestaoConselheiro.SITUACAO_INATIVO.equals(v.getInSituacao()));
        assertThat(algumInativado).isTrue();

        verify(logJetonServiceMock).logVinculoCriado(salvo);
    }

    @Test
    @DisplayName("deve criar vínculo inativo sem inativar outros vínculos")
    void deveCriarVinculoInativoSemInativarOutros() {
        // Dado
        Integer idGestao = 1;
        Integer idPessoa = 100;
        GestaoConselheiro vinculo = criarVinculo(idGestao, idPessoa, GestaoConselheiro.SITUACAO_INATIVO);

        when(gestaoConselheiroRepositoryMock.existsByGestaoAndConselheiro(idGestao, idPessoa))
                .thenReturn(false);
        when(gestaoRepositoryMock.findById(idGestao))
                .thenReturn(Optional.of(criarGestao(idGestao, "Gestão Teste")));
        when(pessoaRepositoryMock.findById(idPessoa))
                .thenReturn(Optional.of(criarPessoa(idPessoa, "Médico Teste")));

        when(gestaoConselheiroRepositoryMock.save(any(GestaoConselheiro.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Quando
        GestaoConselheiro salvo = service.criar(vinculo);

        // Então
        assertThat(salvo).isNotNull();
        assertThat(salvo.getInSituacao()).isEqualTo(GestaoConselheiro.SITUACAO_INATIVO);

        // Verifica que NÃO inativou outros vínculos (não buscou os ativos)
        verify(gestaoConselheiroRepositoryMock, never())
                .findByConselheiroIdPessoaAndInSituacao(anyInt(), anyString());
        verify(logJetonServiceMock).logVinculoCriado(salvo);
    }

    @Test
    @DisplayName("deve lançar exceção ao criar vínculo duplicado")
    void deveLancarExcecaoCriarVinculoDuplicado() {
        // Dado
        Integer idGestao = 1;
        Integer idPessoa = 100;
        GestaoConselheiro vinculo = criarVinculo(idGestao, idPessoa, GestaoConselheiro.SITUACAO_ATIVO);

        when(gestaoConselheiroRepositoryMock.existsByGestaoAndConselheiro(idGestao, idPessoa))
                .thenReturn(true);

        // Quando / Então
        assertThatThrownBy(() -> service.criar(vinculo))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Já existe um vínculo entre a gestão informada e este conselheiro.");

        verify(gestaoConselheiroRepositoryMock, never()).save(any());
    }

    // ========== TESTES DE ATUALIZAÇÃO ==========

    @Test
    @DisplayName("deve atualizar vínculo com sucesso")
    void deveAtualizarVinculoComSucesso() {
        // Dado
        Integer idGestao = 1;
        Integer idPessoa = 100;
        GestaoConselheiro vinculoExistente = criarVinculo(idGestao, idPessoa, GestaoConselheiro.SITUACAO_INATIVO);
        GestaoConselheiro vinculoAtualizado = criarVinculo(idGestao, idPessoa, GestaoConselheiro.SITUACAO_ATIVO);

        when(gestaoConselheiroRepositoryMock.existsById(any(GestaoConselheiroId.class)))
                .thenReturn(true);
        when(gestaoConselheiroRepositoryMock.findById(any(GestaoConselheiroId.class)))
                .thenReturn(Optional.of(vinculoExistente));

        when(gestaoRepositoryMock.findById(idGestao))
                .thenReturn(Optional.of(criarGestao(idGestao, "Gestão Teste")));
        when(pessoaRepositoryMock.findById(idPessoa))
                .thenReturn(Optional.of(criarPessoa(idPessoa, "Médico Teste")));

        // Mock para inativar outros vínculos (já que agora está ativo)
        when(gestaoConselheiroRepositoryMock.findByConselheiroIdPessoaAndInSituacao(idPessoa,
                GestaoConselheiro.SITUACAO_ATIVO))
                .thenReturn(List.of());

        when(gestaoConselheiroRepositoryMock.save(any(GestaoConselheiro.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Quando
        GestaoConselheiro salvo = service.atualizar(vinculoAtualizado);

        // Então
        assertThat(salvo.getInSituacao()).isEqualTo(GestaoConselheiro.SITUACAO_ATIVO);
        verify(logJetonServiceMock).logVinculoAtualizado(any(GestaoConselheiro.class), any(GestaoConselheiro.class));
    }

    @Test
    @DisplayName("deve lançar exceção ao atualizar vínculo inexistente")
    void deveLancarExcecaoAtualizarVinculoInexistente() {
        // Dado
        Integer idGestao = 1;
        Integer idPessoa = 100;
        GestaoConselheiro vinculo = criarVinculo(idGestao, idPessoa, GestaoConselheiro.SITUACAO_ATIVO);

        when(gestaoConselheiroRepositoryMock.existsById(any(GestaoConselheiroId.class)))
                .thenReturn(false);

        // Quando / Então
        assertThatThrownBy(() -> service.atualizar(vinculo))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Vínculo não encontrado para atualização.");

        verify(gestaoConselheiroRepositoryMock, never()).save(any());
    }

    // ========== TESTES DE ATIVAÇÃO / INATIVAÇÃO ==========

    @Test
    @DisplayName("deve ativar vínculo com sucesso e inativar outros vínculos do conselheiro")
    void deveAtivarVinculoComSucesso() {
        // Dado
        Integer idGestao = 1;
        Integer idPessoa = 100;
        GestaoConselheiro vinculo = criarVinculo(idGestao, idPessoa, GestaoConselheiro.SITUACAO_INATIVO);

        when(gestaoConselheiroRepositoryMock.findById(any(GestaoConselheiroId.class)))
                .thenReturn(Optional.of(vinculo));

        // Outro vínculo ativo do mesmo conselheiro
        GestaoConselheiro outroVinculo = criarVinculo(2, idPessoa, GestaoConselheiro.SITUACAO_ATIVO);
        when(gestaoConselheiroRepositoryMock.findByConselheiroIdPessoaAndInSituacao(idPessoa,
                GestaoConselheiro.SITUACAO_ATIVO))
                .thenReturn(List.of(outroVinculo));

        when(gestaoConselheiroRepositoryMock.save(any(GestaoConselheiro.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Quando
        service.ativarVinculo(idGestao, idPessoa);

        // Então
        assertThat(vinculo.getInSituacao()).isEqualTo(GestaoConselheiro.SITUACAO_ATIVO);
        assertThat(outroVinculo.getInSituacao()).isEqualTo(GestaoConselheiro.SITUACAO_INATIVO);

        verify(logJetonServiceMock).logVinculoAtivado(idGestao, idPessoa);
    }

    @Test
    @DisplayName("não deve fazer nada ao ativar vínculo já ativo")
    void naoDeveFazerNadaAoAtivarVinculoJaAtivo() {
        // Dado
        Integer idGestao = 1;
        Integer idPessoa = 100;
        GestaoConselheiro vinculo = criarVinculo(idGestao, idPessoa, GestaoConselheiro.SITUACAO_ATIVO);

        when(gestaoConselheiroRepositoryMock.findById(any(GestaoConselheiroId.class)))
                .thenReturn(Optional.of(vinculo));

        // Quando
        service.ativarVinculo(idGestao, idPessoa);

        // Então
        // Nenhuma alteração deve ser feita
        verify(gestaoConselheiroRepositoryMock, never()).save(any());
        verify(gestaoConselheiroRepositoryMock, never())
                .findByConselheiroIdPessoaAndInSituacao(anyInt(), anyString());
        verify(logJetonServiceMock, never()).logVinculoAtivado(anyInt(), anyInt());
    }

    @Test
    @DisplayName("deve inativar vínculo com sucesso")
    void deveInativarVinculoComSucesso() {
        // Dado
        Integer idGestao = 1;
        Integer idPessoa = 100;
        GestaoConselheiro vinculo = criarVinculo(idGestao, idPessoa, GestaoConselheiro.SITUACAO_ATIVO);

        when(gestaoConselheiroRepositoryMock.findById(any(GestaoConselheiroId.class)))
                .thenReturn(Optional.of(vinculo));
        when(gestaoConselheiroRepositoryMock.save(any(GestaoConselheiro.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Quando
        service.inativarVinculo(idGestao, idPessoa);

        // Então
        assertThat(vinculo.getInSituacao()).isEqualTo(GestaoConselheiro.SITUACAO_INATIVO);
        verify(logJetonServiceMock).logVinculoInativado(idGestao, idPessoa);
    }

    @Test
    @DisplayName("não deve fazer nada ao inativar vínculo já inativo")
    void naoDeveFazerNadaAoInativarVinculoJaInativo() {
        // Dado
        Integer idGestao = 1;
        Integer idPessoa = 100;
        GestaoConselheiro vinculo = criarVinculo(idGestao, idPessoa, GestaoConselheiro.SITUACAO_INATIVO);

        when(gestaoConselheiroRepositoryMock.findById(any(GestaoConselheiroId.class)))
                .thenReturn(Optional.of(vinculo));

        // Quando
        service.inativarVinculo(idGestao, idPessoa);

        // Então
        verify(gestaoConselheiroRepositoryMock, never()).save(any());
        verify(logJetonServiceMock, never()).logVinculoInativado(anyInt(), anyInt());
    }

    // ========== TESTES DE EXCLUSÃO ==========

    @Test
    @DisplayName("deve excluir vínculo com sucesso quando não há atividades associadas")
    void deveExcluirVinculoComSucesso() {
        // Dado
        Integer idGestao = 1;
        Integer idPessoa = 100;
        GestaoConselheiro vinculo = criarVinculo(idGestao, idPessoa, GestaoConselheiro.SITUACAO_ATIVO);

        when(gestaoConselheiroRepositoryMock.findById(any(GestaoConselheiroId.class)))
                .thenReturn(Optional.of(vinculo));
        when(atividadeRepositoryMock.countByGestaoIdGestaoAndConselheiroIdPessoa(idGestao, idPessoa))
                .thenReturn(0L);

        // Quando
        service.excluir(idGestao, idPessoa);

        // Então
        verify(gestaoConselheiroRepositoryMock).deleteById(any(GestaoConselheiroId.class));
        verify(logJetonServiceMock).logVinculoExcluido(any(GestaoConselheiro.class));
    }

    @Test
    @DisplayName("deve lançar exceção ao excluir vínculo com atividades associadas")
    void deveLancarExcecaoExcluirVinculoComAtividades() {
        // Dado
        Integer idGestao = 1;
        Integer idPessoa = 100;
        GestaoConselheiro vinculo = criarVinculo(idGestao, idPessoa, GestaoConselheiro.SITUACAO_ATIVO);

        when(gestaoConselheiroRepositoryMock.findById(any(GestaoConselheiroId.class)))
                .thenReturn(Optional.of(vinculo));
        when(atividadeRepositoryMock.countByGestaoIdGestaoAndConselheiroIdPessoa(idGestao, idPessoa))
                .thenReturn(5L); // tem atividades

        // Quando / Então
        assertThatThrownBy(() -> service.excluir(idGestao, idPessoa))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Não é possível excluir o vínculo pois existem atividades associadas.");

        verify(gestaoConselheiroRepositoryMock, never()).deleteById(any());
        verify(logJetonServiceMock, never()).logVinculoExcluido(any());
    }

    // ========== TESTES DE ATUALIZAÇÃO EM MASSA ==========

    @Test
    @DisplayName("deve atualizar vínculos em massa adicionando novos e removendo não selecionados")
    void deveAtualizarVinculosEmMassaComAdicaoERemocao() {
        // Dado
        Integer idGestao = 1;
        List<Integer> idsConselheirosSelecionados = List.of(100, 102, 103);

        Gestao gestao = criarGestao(idGestao, "Gestão Teste");

        GestaoConselheiro vinculo1 = criarVinculo(idGestao, 100, GestaoConselheiro.SITUACAO_ATIVO);
        GestaoConselheiro vinculo2 = criarVinculo(idGestao, 101, GestaoConselheiro.SITUACAO_ATIVO);

        when(gestaoRepositoryMock.findById(idGestao)).thenReturn(Optional.of(gestao));
        when(gestaoConselheiroRepositoryMock.findByIdIdGestao(idGestao))
                .thenReturn(List.of(vinculo1, vinculo2));

        // ✅ Stub necessário com lenient()
        lenient().when(gestaoConselheiroRepositoryMock.findById(new GestaoConselheiroId(idGestao, 101)))
                .thenReturn(Optional.of(vinculo2));

        Conselheiro conselheiro102 = criarConselheiro(102, "Médico 102");
        Conselheiro conselheiro103 = criarConselheiro(103, "Médico 103");

        when(conselheiroRepositoryMock.findById(102)).thenReturn(Optional.of(conselheiro102));
        when(conselheiroRepositoryMock.findById(103)).thenReturn(Optional.of(conselheiro103));

        when(gestaoConselheiroRepositoryMock.findByConselheiroIdPessoaAndInSituacao(eq(102),
                eq(GestaoConselheiro.SITUACAO_ATIVO)))
                .thenReturn(List.of());
        when(gestaoConselheiroRepositoryMock.findByConselheiroIdPessoaAndInSituacao(eq(103),
                eq(GestaoConselheiro.SITUACAO_ATIVO)))
                .thenReturn(List.of());

        when(atividadeRepositoryMock.countByGestaoIdGestaoAndConselheiroIdPessoa(idGestao, 101))
                .thenReturn(0L);

        when(gestaoConselheiroRepositoryMock.save(any(GestaoConselheiro.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Quando
        Map<String, List<Integer>> resultado = service.atualizarVinculosEmMassa(idGestao, idsConselheirosSelecionados);

        // Então
        assertThat(resultado.get("removidos")).contains(101);
        assertThat(resultado.get("adicionados")).contains(102, 103);

        verify(gestaoConselheiroRepositoryMock).deleteById(new GestaoConselheiroId(idGestao, 101));
        verify(gestaoConselheiroRepositoryMock, times(2)).save(any(GestaoConselheiro.class));
        verify(logJetonServiceMock).logVinculosAtualizadosEmMassa(idGestao, List.of(101), List.of(102, 103));
    }

    @Test
    @DisplayName("deve manter vínculo com atividades ao atualizar em massa mesmo se não selecionado")
    void deveManterVinculoComAtividadesNaAtualizacaoEmMassa() {
        // Dado
        Integer idGestao = 1;
        List<Integer> idsConselheirosSelecionados = List.of(100);

        Gestao gestao = criarGestao(idGestao, "Gestão Teste");
        GestaoConselheiro vinculo1 = criarVinculo(idGestao, 100, GestaoConselheiro.SITUACAO_ATIVO);
        GestaoConselheiro vinculo2 = criarVinculo(idGestao, 101, GestaoConselheiro.SITUACAO_ATIVO);

        when(gestaoRepositoryMock.findById(idGestao)).thenReturn(Optional.of(gestao));
        when(gestaoConselheiroRepositoryMock.findByIdIdGestao(idGestao))
                .thenReturn(List.of(vinculo1, vinculo2));

        // ✅ Stub com any() para evitar problemas de igualdade de objetos
        lenient().when(gestaoConselheiroRepositoryMock.findById(any(GestaoConselheiroId.class)))
                .thenReturn(Optional.of(vinculo2));

        // Conselheiro 101 tem atividades, então não será removido
        when(atividadeRepositoryMock.countByGestaoIdGestaoAndConselheiroIdPessoa(idGestao, 101))
                .thenReturn(3L);

        // Quando
        Map<String, List<Integer>> resultado = service.atualizarVinculosEmMassa(idGestao, idsConselheirosSelecionados);

        // Então
        assertThat(resultado.get("removidos")).doesNotContain(101);
        assertThat(resultado.get("adicionados")).isEmpty();

        verify(gestaoConselheiroRepositoryMock, never()).deleteById(new GestaoConselheiroId(idGestao, 101));
    }

    @Test
    @DisplayName("deve lançar exceção ao atualizar em massa com gestão inexistente")
    void deveLancarExcecaoAtualizarEmMassaGestaoInexistente() {
        // Dado
        Integer idGestao = 999;
        when(gestaoRepositoryMock.findById(idGestao)).thenReturn(Optional.empty());

        // Quando / Então
        assertThatThrownBy(() -> service.atualizarVinculosEmMassa(idGestao, List.of(1, 2)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Gestão não encontrada: 999");
    }

    // ========== TESTES DE CONSULTA ==========

    @Test
    @DisplayName("deve listar vínculos paginados com filtros")
    void deveListarVinculosPaginadosComFiltros() {
        // Dado
        String termo = "teste";
        String situacao = "A";
        int page = 0, size = 10;
        String sortField = "gestao.nomeGestao", sortDir = "asc";
        Page<GestaoConselheiro> paginaEsperada = new PageImpl<>(List.of());

        when(gestaoConselheiroRepositoryMock.findAllByFilters(anyString(), anyString(), any(Pageable.class)))
                .thenReturn(paginaEsperada);

        // Quando
        Page<GestaoConselheiro> resultado = service.listarComPaginacaoEPesquisa(termo, situacao, page, size, sortField,
                sortDir);

        // Então
        assertThat(resultado).isNotNull();
        verify(gestaoConselheiroRepositoryMock).findAllByFilters(eq(termo), eq(situacao), any(Pageable.class));
    }

    @Test
    @DisplayName("deve listar todos os vínculos")
    void deveListarTodosVinculos() {
        // Dado
        List<GestaoConselheiro> lista = List.of(
                criarVinculo(1, 100, "A"),
                criarVinculo(1, 101, "I"));
        when(gestaoConselheiroRepositoryMock.findAll()).thenReturn(lista);

        // Quando
        List<GestaoConselheiro> resultado = service.listarTodos();

        // Então
        assertThat(resultado).hasSize(2);
        verify(gestaoConselheiroRepositoryMock).findAll();
    }

    @Test
    @DisplayName("deve buscar vínculo por ID com sucesso")
    void deveBuscarVinculoPorIdComSucesso() {
        // Dado
        Integer idGestao = 1;
        Integer idPessoa = 100;
        GestaoConselheiro vinculo = criarVinculo(idGestao, idPessoa, "A");

        when(gestaoConselheiroRepositoryMock.findById(new GestaoConselheiroId(idGestao, idPessoa)))
                .thenReturn(Optional.of(vinculo));

        // Quando
        Optional<GestaoConselheiro> resultado = service.buscarPorId(idGestao, idPessoa);

        // Então
        assertThat(resultado).isPresent();
        assertThat(resultado.get()).isEqualTo(vinculo);
    }

    @Test
    @DisplayName("deve buscar vínculo ou falhar quando não encontrado")
    void deveBuscarVinculoOuFalharQuandoNaoEncontrado() {
        // Dado
        Integer idGestao = 1;
        Integer idPessoa = 100;

        when(gestaoConselheiroRepositoryMock.findById(any(GestaoConselheiroId.class)))
                .thenReturn(Optional.empty());

        // Quando / Então
        assertThatThrownBy(() -> service.buscarOuFalhar(idGestao, idPessoa))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Vínculo não encontrado para gestão " + idGestao + " e conselheiro " + idPessoa);
    }

    @Test
    @DisplayName("deve buscar vínculo por conselheiro e status com sucesso")
    void deveBuscarVinculoPorConselheiroEStatus() {
        // Dado
        Integer idPessoa = 100;
        String situacao = "A";
        GestaoConselheiro vinculo = criarVinculo(1, idPessoa, situacao);

        when(gestaoConselheiroRepositoryMock.findByConselheiroIdPessoaAndInSituacao(idPessoa, situacao))
                .thenReturn(List.of(vinculo));

        // Quando
        Optional<GestaoConselheiro> resultado = service.buscarPorConselheiroEStatus(idPessoa, situacao);

        // Então
        assertThat(resultado).isPresent();
        assertThat(resultado.get()).isEqualTo(vinculo);
    }

    @Test
    @DisplayName("deve retornar Optional vazio quando não houver vínculo para conselheiro e status")
    void deveRetornarOptionalVazioQuandoNaoHaVinculoParaConselheiroEStatus() {
        // Dado
        Integer idPessoa = 100;
        String situacao = "A";

        when(gestaoConselheiroRepositoryMock.findByConselheiroIdPessoaAndInSituacao(idPessoa, situacao))
                .thenReturn(List.of());

        // Quando
        Optional<GestaoConselheiro> resultado = service.buscarPorConselheiroEStatus(idPessoa, situacao);

        // Então
        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("deve verificar se existe vínculo para conselheiro")
    void deveVerificarExistenciaVinculoParaConselheiro() {
        // Dado
        Integer idPessoa = 100;
        when(gestaoConselheiroRepositoryMock.existsByConselheiroIdPessoa(idPessoa))
                .thenReturn(true);

        // Quando
        boolean existe = service.existeVinculoParaConselheiro(idPessoa);

        // Então
        assertThat(existe).isTrue();
        verify(gestaoConselheiroRepositoryMock).existsByConselheiroIdPessoa(idPessoa);
    }

    @Test
    @DisplayName("deve buscar conselheiros com atividades em uma gestão")
    void deveBuscarConselheirosComAtividadesNaGestao() {
        // Dado
        Integer idGestao = 1;
        GestaoConselheiro vinculo1 = criarVinculo(idGestao, 100, "A");
        GestaoConselheiro vinculo2 = criarVinculo(idGestao, 101, "A");

        when(gestaoConselheiroRepositoryMock.findByIdIdGestao(idGestao))
                .thenReturn(List.of(vinculo1, vinculo2));

        // Conselheiro 100 tem atividades, 101 não tem
        when(atividadeRepositoryMock.countByGestaoIdGestaoAndConselheiroIdPessoa(idGestao, 100))
                .thenReturn(3L);
        when(atividadeRepositoryMock.countByGestaoIdGestaoAndConselheiroIdPessoa(idGestao, 101))
                .thenReturn(0L);

        // Quando
        List<Integer> resultado = service.findConselheirosComAtividadesNaGestao(idGestao);

        // Então
        assertThat(resultado).containsExactly(100);
        assertThat(resultado).doesNotContain(101);
    }

    @Test
    @DisplayName("deve listar vínculos ativos por gestão")
    void deveListarVinculosAtivosPorGestao() {
        // Dado
        Integer idGestao = 1;
        GestaoConselheiro vinculoAtivo = criarVinculo(idGestao, 100, GestaoConselheiro.SITUACAO_ATIVO);

        when(gestaoConselheiroRepositoryMock.findByIdIdGestaoAndInSituacao(idGestao, GestaoConselheiro.SITUACAO_ATIVO))
                .thenReturn(List.of(vinculoAtivo));

        // Quando
        List<GestaoConselheiro> resultado = service.listarPorGestaoAtivos(idGestao);

        // Então
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getId().getIdPessoa()).isEqualTo(100);
    }

    @Test
    @DisplayName("deve listar vínculos inativos por gestão")
    void deveListarVinculosInativosPorGestao() {
        // Dado
        Integer idGestao = 1;
        GestaoConselheiro vinculoInativo = criarVinculo(idGestao, 101, GestaoConselheiro.SITUACAO_INATIVO);

        when(gestaoConselheiroRepositoryMock.findByIdIdGestaoAndInSituacao(idGestao,
                GestaoConselheiro.SITUACAO_INATIVO))
                .thenReturn(List.of(vinculoInativo));

        // Quando
        List<GestaoConselheiro> resultado = service.listarPorGestaoInativos(idGestao);

        // Então
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getId().getIdPessoa()).isEqualTo(101);
    }

    @Test
    @DisplayName("deve listar vínculos por conselheiro ativos")
    void deveListarVinculosPorConselheiroAtivos() {
        // Dado
        Integer idPessoa = 100;
        GestaoConselheiro vinculo = criarVinculo(1, idPessoa, GestaoConselheiro.SITUACAO_ATIVO);

        when(gestaoConselheiroRepositoryMock.findByConselheiroIdPessoaAndInSituacao(idPessoa,
                GestaoConselheiro.SITUACAO_ATIVO))
                .thenReturn(List.of(vinculo));

        // Quando
        List<GestaoConselheiro> resultado = service.listarPorConselheiroAtivos(idPessoa);

        // Então
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getId().getIdGestao()).isEqualTo(1);
    }

    @Test
    @DisplayName("deve listar vínculos por conselheiro inativos")
    void deveListarVinculosPorConselheiroInativos() {
        // Dado
        Integer idPessoa = 100;
        GestaoConselheiro vinculo = criarVinculo(2, idPessoa, GestaoConselheiro.SITUACAO_INATIVO);

        when(gestaoConselheiroRepositoryMock.findByConselheiroIdPessoaAndInSituacao(idPessoa,
                GestaoConselheiro.SITUACAO_INATIVO))
                .thenReturn(List.of(vinculo));

        // Quando
        List<GestaoConselheiro> resultado = service.listarPorConselheiroInativos(idPessoa);

        // Então
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getId().getIdGestao()).isEqualTo(2);
    }

    @Test
    @DisplayName("deve listar todos os vínculos de um conselheiro")
    void deveListarTodosVinculosDeConselheiro() {
        // Dado
        Integer idPessoa = 100;
        GestaoConselheiro vinculo1 = criarVinculo(1, idPessoa, "A");
        GestaoConselheiro vinculo2 = criarVinculo(2, idPessoa, "I");

        when(gestaoConselheiroRepositoryMock.findByIdIdPessoa(idPessoa))
                .thenReturn(List.of(vinculo1, vinculo2));

        // Quando
        List<GestaoConselheiro> resultado = service.listarPorConselheiro(idPessoa);

        // Então
        assertThat(resultado).hasSize(2);
        assertThat(resultado).containsExactlyInAnyOrder(vinculo1, vinculo2);
    }

    @Test
    @DisplayName("deve listar todos os vínculos de uma gestão")
    void deveListarTodosVinculosDeGestao() {
        // Dado
        Integer idGestao = 1;
        GestaoConselheiro vinculo1 = criarVinculo(idGestao, 100, "A");
        GestaoConselheiro vinculo2 = criarVinculo(idGestao, 101, "I");

        when(gestaoConselheiroRepositoryMock.findByIdIdGestao(idGestao))
                .thenReturn(List.of(vinculo1, vinculo2));

        // Quando
        List<GestaoConselheiro> resultado = service.listarPorGestao(idGestao);

        // Então
        assertThat(resultado).hasSize(2);
        assertThat(resultado).containsExactlyInAnyOrder(vinculo1, vinculo2);
    }

    // ========== MÉTODO AUXILIAR PARA PESSOA (usado nos logs) ==========

    private Pessoa criarPessoa(Integer id, String nome) {
        Pessoa p = new Pessoa();
        p.setIdPessoa(id);
        p.setNome(nome);
        return p;
    }
}