package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.domain.*;
import br.com.cremepe.jeton.repository.PontosSaldoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do serviço de Pontos Saldo (PontosSaldoService)")
class PontosSaldoServiceTest {

    @Mock
    private PontosSaldoRepository repositoryMock;

    @Mock
    private LogJetonService logJetonServiceMock;

    @InjectMocks
    private PontosSaldoService service;

    // ========== HELPERS ==========

    private PontosSaldo criarPontosSaldo(Integer id, Integer pontosTrabalhados, Integer pontosUtilizados,
            Integer pontosSobrando, String situacao, Conselheiro conselheiro,
            Gestao gestao, Resolucao resolucao, AtividadeConselhal atividade) {
        PontosSaldo ps = new PontosSaldo();
        ps.setIdPontosSaldo(id);
        ps.setPontosTrabalhados(pontosTrabalhados);
        ps.setPontosUtilizados(pontosUtilizados);
        ps.setPontosSobrando(pontosSobrando);
        ps.setInSituacao(situacao);
        ps.setConselheiro(conselheiro);
        ps.setGestao(gestao);
        ps.setResolucao(resolucao);
        ps.setAtividade(atividade);
        ps.setDataHora(LocalDateTime.now());
        return ps;
    }

    private Conselheiro criarConselheiro(Integer id) {
        Pessoa p = new Pessoa();
        p.setIdPessoa(id);
        p.setNome("Médico " + id);
        Conselheiro c = new Conselheiro();
        c.setIdPessoa(id);
        c.setPessoa(p);
        return c;
    }

    private Gestao criarGestao(Integer id) {
        Gestao g = new Gestao();
        g.setIdGestao(id);
        g.setNomeGestao("Gestão " + id);
        return g;
    }

    private Resolucao criarResolucao(Integer id) {
        Resolucao r = new Resolucao();
        r.setIdResolucao(id);
        return r;
    }

    // ========== TESTES DE CRIAÇÃO ==========

    @Test
    @DisplayName("deve criar saldo de pontos com sucesso")
    void deveCriarSaldoComSucesso() {
        // Dado
        Conselheiro conselheiro = criarConselheiro(1);
        Gestao gestao = criarGestao(10);
        Resolucao resolucao = criarResolucao(5);

        PontosSaldo saldo = new PontosSaldo();
        saldo.setPontosTrabalhados(10);
        saldo.setPontosUtilizados(0);
        saldo.setPontosSobrando(10);
        saldo.setConselheiro(conselheiro);
        saldo.setGestao(gestao);
        saldo.setResolucao(resolucao);
        saldo.setInSituacao(PontosSaldo.SITUACAO_ATIVO);

        // Mock do save
        when(repositoryMock.save(any(PontosSaldo.class)))
                .thenAnswer(inv -> {
                    PontosSaldo ps = inv.getArgument(0);
                    ps.setIdPontosSaldo(999);
                    return ps;
                });

        // Quando
        PontosSaldo salvo = service.criar(saldo);

        // Então
        assertThat(salvo).isNotNull();
        assertThat(salvo.getIdPontosSaldo()).isEqualTo(999);
        assertThat(salvo.getDataHora()).isNotNull();
        assertThat(salvo.getInSituacao()).isEqualTo(PontosSaldo.SITUACAO_ATIVO);

        verify(repositoryMock).save(saldo);
        verify(logJetonServiceMock).logPontosSaldoCriado(salvo);
    }

    @Test
    @DisplayName("deve normalizar valores nulos ao criar saldo")
    void deveNormalizarValoresNulosAoCriar() {
        // Dado
        PontosSaldo saldo = new PontosSaldo();
        saldo.setPontosTrabalhados(null);
        saldo.setPontosUtilizados(null);
        saldo.setPontosSobrando(null);
        saldo.setInSituacao(null);

        when(repositoryMock.save(any(PontosSaldo.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Quando
        PontosSaldo salvo = service.criar(saldo);

        // Então
        assertThat(salvo.getPontosTrabalhados()).isZero();
        assertThat(salvo.getPontosUtilizados()).isZero();
        assertThat(salvo.getPontosSobrando()).isZero();
        assertThat(salvo.getInSituacao()).isEqualTo(PontosSaldo.SITUACAO_ATIVO);
        assertThat(salvo.getDataHora()).isNotNull();
    }

    // ========== TESTES DE ATUALIZAÇÃO ==========

    @Test
    @DisplayName("deve atualizar saldo com sucesso")
    void deveAtualizarSaldoComSucesso() {
        // Dado
        Integer id = 50;
        Conselheiro conselheiro = criarConselheiro(1);
        Gestao gestao = criarGestao(10);
        Resolucao resolucao = criarResolucao(5);

        PontosSaldo existente = criarPontosSaldo(id, 10, 0, 10, PontosSaldo.SITUACAO_ATIVO,
                conselheiro, gestao, resolucao, null);

        PontosSaldo atualizado = new PontosSaldo();
        atualizado.setIdPontosSaldo(id);
        atualizado.setPontosTrabalhados(15);
        atualizado.setPontosUtilizados(5);
        atualizado.setPontosSobrando(10);
        atualizado.setConselheiro(conselheiro);
        atualizado.setGestao(gestao);
        atualizado.setResolucao(resolucao);
        atualizado.setInSituacao(PontosSaldo.SITUACAO_ATIVO);

        when(repositoryMock.existsById(id)).thenReturn(true);
        when(repositoryMock.findById(id)).thenReturn(Optional.of(existente));
        when(repositoryMock.save(any(PontosSaldo.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Quando
        PontosSaldo salvo = service.atualizar(atualizado);

        // Então
        assertThat(salvo).isNotNull();
        assertThat(salvo.getPontosTrabalhados()).isEqualTo(15);
        assertThat(salvo.getPontosUtilizados()).isEqualTo(5);

        verify(repositoryMock).save(existente);
        verify(logJetonServiceMock).logPontosSaldoAtualizado(any(PontosSaldo.class), eq(salvo));
    }

    @Test
    @DisplayName("deve lançar exceção ao atualizar saldo inexistente")
    void deveLancarExcecaoAtualizarSaldoInexistente() {
        // Dado
        Integer id = 999;
        PontosSaldo saldo = new PontosSaldo();
        saldo.setIdPontosSaldo(id);

        when(repositoryMock.existsById(id)).thenReturn(false);

        // Quando / Então
        assertThatThrownBy(() -> service.atualizar(saldo))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Saldo de pontos não encontrado para atualização.");

        verify(repositoryMock, never()).save(any());
    }

    @Test
    @DisplayName("deve lançar exceção ao atualizar saldo já utilizado")
    void deveLancarExcecaoAtualizarSaldoUtilizado() {
        // Dado
        Integer id = 50;
        PontosSaldo existente = criarPontosSaldo(id, 10, 10, 0, PontosSaldo.SITUACAO_UTILIZADO,
                null, null, null, null);

        PontosSaldo atualizado = new PontosSaldo();
        atualizado.setIdPontosSaldo(id);

        when(repositoryMock.existsById(id)).thenReturn(true);
        when(repositoryMock.findById(id)).thenReturn(Optional.of(existente));

        // Quando / Então
        assertThatThrownBy(() -> service.atualizar(atualizado))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Não é possível alterar um saldo que já foi utilizado ou excluído.");

        verify(repositoryMock, never()).save(any());
    }

    @Test
    @DisplayName("deve lançar exceção ao atualizar saldo com valores negativos")
    void deveLancarExcecaoAtualizarSaldoValoresNegativos() {
        // Dado
        Integer id = 50;
        PontosSaldo existente = criarPontosSaldo(id, 10, 0, 10, PontosSaldo.SITUACAO_ATIVO,
                null, null, null, null);

        PontosSaldo atualizado = new PontosSaldo();
        atualizado.setIdPontosSaldo(id);
        atualizado.setPontosTrabalhados(-5);

        when(repositoryMock.existsById(id)).thenReturn(true);
        when(repositoryMock.findById(id)).thenReturn(Optional.of(existente));

        // Quando / Então
        assertThatThrownBy(() -> service.atualizar(atualizado))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Os valores de pontos não podem ser negativos.");

        verify(repositoryMock, never()).save(any());
    }

    // ========== TESTES DE EXCLUSÃO ==========

    @Test
    @DisplayName("deve excluir saldo com sucesso quando inativo e sem pontos sobrando")
    void deveExcluirSaldoComSucesso() {
        // Dado
        Integer id = 100;
        Conselheiro conselheiro = criarConselheiro(1);
        Gestao gestao = criarGestao(10);
        PontosSaldo saldo = criarPontosSaldo(id, 10, 10, 0, PontosSaldo.SITUACAO_INATIVO,
                conselheiro, gestao, null, null);

        when(repositoryMock.findById(id)).thenReturn(Optional.of(saldo));

        // Quando
        service.excluir(id);

        // Então
        verify(repositoryMock).deleteById(id);
        verify(logJetonServiceMock).logPontosSaldoExcluido(saldo);
    }

    @Test
    @DisplayName("deve lançar exceção ao excluir saldo já utilizado")
    void deveLancarExcecaoExcluirSaldoUtilizado() {
        // Dado
        Integer id = 50;
        PontosSaldo saldo = criarPontosSaldo(id, 10, 10, 0, PontosSaldo.SITUACAO_UTILIZADO,
                null, null, null, null);

        when(repositoryMock.findById(id)).thenReturn(Optional.of(saldo));

        // Quando / Então
        assertThatThrownBy(() -> service.excluir(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Não é possível excluir um saldo que já foi utilizado em pagamentos.");

        verify(repositoryMock, never()).deleteById(any());
    }

    @Test
    @DisplayName("deve lançar exceção ao excluir saldo ativo com pontos remanescentes")
    void deveLancarExcecaoExcluirSaldoAtivoComPontos() {
        // Dado
        Integer id = 50;
        PontosSaldo saldo = criarPontosSaldo(id, 10, 0, 5, PontosSaldo.SITUACAO_ATIVO,
                null, null, null, null);

        when(repositoryMock.findById(id)).thenReturn(Optional.of(saldo));

        // Quando / Então
        assertThatThrownBy(() -> service.excluir(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Não é possível excluir um saldo ativo com pontos remanescentes. Considere inativá-lo.");

        verify(repositoryMock, never()).deleteById(any());
    }

    @Test
    @DisplayName("deve lançar exceção ao excluir saldo inexistente")
    void deveLancarExcecaoExcluirSaldoInexistente() {
        // Dado
        Integer id = 999;

        when(repositoryMock.findById(id)).thenReturn(Optional.empty());

        // Quando / Então
        assertThatThrownBy(() -> service.excluir(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Saldo de pontos não encontrado com ID: " + id);

        verify(repositoryMock, never()).deleteById(any());
    }

    // ========== TESTES DE CONSULTA ==========

    @Test
    @DisplayName("deve listar todos os saldos")
    void deveListarTodosSaldos() {
        // Dado
        List<PontosSaldo> lista = List.of(
                criarPontosSaldo(1, 10, 0, 10, "A", null, null, null, null),
                criarPontosSaldo(2, 5, 5, 0, "I", null, null, null, null));
        when(repositoryMock.findAll()).thenReturn(lista);

        // Quando
        List<PontosSaldo> resultado = service.listarTodos();

        // Então
        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getIdPontosSaldo()).isEqualTo(1);
        assertThat(resultado.get(1).getIdPontosSaldo()).isEqualTo(2);
        verify(repositoryMock).findAll();
    }

    @Test
    @DisplayName("deve buscar saldo por ID com sucesso")
    void deveBuscarSaldoPorIdComSucesso() {
        // Dado
        Integer id = 10;
        PontosSaldo saldo = criarPontosSaldo(id, 10, 0, 10, "A", null, null, null, null);
        when(repositoryMock.findById(id)).thenReturn(Optional.of(saldo));

        // Quando
        Optional<PontosSaldo> resultado = service.buscarPorId(id);

        // Então
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getIdPontosSaldo()).isEqualTo(id);
        verify(repositoryMock).findById(id);
    }

    @Test
    @DisplayName("deve retornar Optional vazio ao buscar saldo inexistente")
    void deveRetornarOptionalVazioBuscarInexistente() {
        // Dado
        Integer id = 999;
        when(repositoryMock.findById(id)).thenReturn(Optional.empty());

        // Quando
        Optional<PontosSaldo> resultado = service.buscarPorId(id);

        // Então
        assertThat(resultado).isEmpty();
        verify(repositoryMock).findById(id);
    }

    @Test
    @DisplayName("deve buscar saldo ou falhar com sucesso quando existe")
    void deveBuscarOuFalharComSucesso() {
        // Dado
        Integer id = 10;
        PontosSaldo saldo = criarPontosSaldo(id, 10, 0, 10, "A", null, null, null, null);
        when(repositoryMock.findById(id)).thenReturn(Optional.of(saldo));

        // Quando
        PontosSaldo resultado = service.buscarOuFalhar(id);

        // Então
        assertThat(resultado).isNotNull();
        assertThat(resultado.getIdPontosSaldo()).isEqualTo(id);
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
                .hasMessage("Saldo de pontos não encontrado com ID: " + id);
    }

    @Test
    @DisplayName("deve buscar saldos disponíveis ordenados por FIFO")
    void deveBuscarSaldosDisponiveisOrdenadosFIFO() {
        // Dado
        Integer idPessoa = 1;
        Integer idGestao = 10;
        List<PontosSaldo> saldos = List.of(
                criarPontosSaldo(1, 10, 0, 10, "A", null, null, null, null),
                criarPontosSaldo(2, 5, 0, 5, "A", null, null, null, null));

        when(repositoryMock.findSaldosDisponiveisOrderedFIFO(idPessoa, idGestao))
                .thenReturn(saldos);

        // Quando
        List<PontosSaldo> resultado = service.buscarSaldosDisponiveis(idPessoa, idGestao);

        // Então
        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getIdPontosSaldo()).isEqualTo(1);
        assertThat(resultado.get(1).getIdPontosSaldo()).isEqualTo(2);
        verify(repositoryMock).findSaldosDisponiveisOrderedFIFO(idPessoa, idGestao);
    }

    @Test
    @DisplayName("deve verificar se existe saldo ativo para conselheiro e gestão")
    void deveVerificarExistenciaSaldoAtivo() {
        // Dado
        Integer idPessoa = 1;
        Integer idGestao = 10;

        when(repositoryMock.existsByConselheiroIdPessoaAndGestaoIdGestaoAndInSituacao(
                idPessoa, idGestao, PontosSaldo.SITUACAO_ATIVO))
                .thenReturn(true);

        // Quando
        boolean existe = service.existeSaldoAtivoParaConselheiroGestao(idPessoa, idGestao);

        // Então
        assertThat(existe).isTrue();
        verify(repositoryMock).existsByConselheiroIdPessoaAndGestaoIdGestaoAndInSituacao(
                idPessoa, idGestao, PontosSaldo.SITUACAO_ATIVO);
    }

    @Test
    @DisplayName("deve somar pontos sobrando total de um conselheiro")
    void deveSomarPontosSobrandoTotal() {
        // Dado
        Integer idPessoa = 1;
        Integer somaEsperada = 15;

        when(repositoryMock.sumPontosSobrandoTotal(idPessoa)).thenReturn(somaEsperada);

        // Quando
        int resultado = service.somarPontosSobrandoTotal(idPessoa);

        // Então
        assertThat(resultado).isEqualTo(somaEsperada);
        verify(repositoryMock).sumPontosSobrandoTotal(idPessoa);
    }

    @Test
    @DisplayName("deve retornar zero ao somar pontos sobrando de conselheiro sem saldos")
    void deveRetornarZeroAoSomarPontosSobrandoDeConselheiroSemSaldos() {
        // Dado
        Integer idPessoa = 1;

        when(repositoryMock.sumPontosSobrandoTotal(idPessoa)).thenReturn(null);

        // Quando
        int resultado = service.somarPontosSobrandoTotal(idPessoa);

        // Então
        assertThat(resultado).isZero();
        verify(repositoryMock).sumPontosSobrandoTotal(idPessoa);
    }
}