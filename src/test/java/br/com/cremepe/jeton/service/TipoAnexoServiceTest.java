package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.domain.TipoAnexo;
import br.com.cremepe.jeton.repository.TipoAnexoRepository;
import br.com.cremepe.jeton.util.ArquivoValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do serviço de Tipos de Anexo (TipoAnexoService)")
class TipoAnexoServiceTest {

    @Mock
    private TipoAnexoRepository repositoryMock;

    @Mock
    private LogJetonService logJetonServiceMock;

    @Mock
    private ArquivoValidator arquivoValidatorMock;

    @InjectMocks
    private TipoAnexoService service;

    // ========== HELPERS ==========

    private TipoAnexo criarTipoAnexo(Integer id, String nome, String exigePublicacao) {
        TipoAnexo tipo = new TipoAnexo();
        tipo.setIdTipo(id);
        tipo.setNome(nome);
        tipo.setExigePublicacao(exigePublicacao);
        return tipo;
    }

    private TipoAnexo criarTipoAnexoValido(Integer id) {
        return criarTipoAnexo(id, "Tipo Teste " + id, TipoAnexo.EXIGE_PUBLICACAO_NAO);
    }

    // ========== TESTES DE CRIAÇÃO ==========

    @Test
    @DisplayName("deve criar tipo de anexo com sucesso")
    void deveCriarTipoAnexoComSucesso() {
        TipoAnexo tipo = criarTipoAnexo(null, "Novo Tipo", TipoAnexo.EXIGE_PUBLICACAO_SIM);

        doNothing().when(arquivoValidatorMock).validarNomeUnicoTipoAnexo(tipo, true);

        when(repositoryMock.save(any(TipoAnexo.class)))
                .thenAnswer(inv -> {
                    TipoAnexo t = inv.getArgument(0);
                    t.setIdTipo(999);
                    return t;
                });

        TipoAnexo salvo = service.criar(tipo);

        assertThat(salvo).isNotNull();
        assertThat(salvo.getIdTipo()).isEqualTo(999);
        assertThat(salvo.getNome()).isEqualTo("Novo Tipo");
        assertThat(salvo.getExigePublicacao()).isEqualTo(TipoAnexo.EXIGE_PUBLICACAO_SIM);

        verify(arquivoValidatorMock).validarNomeUnicoTipoAnexo(tipo, true);
        verify(repositoryMock).save(tipo);
        verify(logJetonServiceMock).logTipoAnexoCriado(salvo);
    }

    @Test
    @DisplayName("deve normalizar exigePublicacao para N quando não informado")
    void deveNormalizarExigePublicacaoParaN() {
        TipoAnexo tipo = new TipoAnexo();
        tipo.setNome("Tipo sem flag");
        tipo.setExigePublicacao(null);

        doNothing().when(arquivoValidatorMock).validarNomeUnicoTipoAnexo(tipo, true);

        when(repositoryMock.save(any(TipoAnexo.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TipoAnexo salvo = service.criar(tipo);

        assertThat(salvo.getExigePublicacao()).isEqualTo(TipoAnexo.EXIGE_PUBLICACAO_NAO);
    }

    @Test
    @DisplayName("deve lançar exceção ao criar tipo com nome duplicado")
    void deveLancarExcecaoCriarNomeDuplicado() {
        TipoAnexo tipo = criarTipoAnexo(null, "Duplicado", TipoAnexo.EXIGE_PUBLICACAO_NAO);

        doThrow(new RuntimeException("Já existe um tipo de anexo cadastrado com o nome 'Duplicado'."))
                .when(arquivoValidatorMock).validarNomeUnicoTipoAnexo(tipo, true);

        assertThatThrownBy(() -> service.criar(tipo))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Já existe um tipo de anexo cadastrado com o nome 'Duplicado'.");

        verify(repositoryMock, never()).save(any());
    }

    // ========== TESTES DE ATUALIZAÇÃO ==========

    @Test
    @DisplayName("deve atualizar tipo de anexo com sucesso")
    void deveAtualizarTipoAnexoComSucesso() {
        Integer id = 50;
        TipoAnexo existente = criarTipoAnexoValido(id);
        existente.setNome("Nome Antigo");

        TipoAnexo atualizado = criarTipoAnexo(id, "Nome Novo", TipoAnexo.EXIGE_PUBLICACAO_SIM);

        when(repositoryMock.existsById(id)).thenReturn(true);
        when(repositoryMock.findById(id)).thenReturn(Optional.of(existente));

        doNothing().when(arquivoValidatorMock).validarNomeUnicoTipoAnexo(atualizado, false);

        when(repositoryMock.save(any(TipoAnexo.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TipoAnexo salvo = service.atualizar(atualizado);

        assertThat(salvo.getNome()).isEqualTo("Nome Novo");
        assertThat(salvo.getExigePublicacao()).isEqualTo(TipoAnexo.EXIGE_PUBLICACAO_SIM);

        verify(repositoryMock).save(existente);
        verify(arquivoValidatorMock).validarNomeUnicoTipoAnexo(atualizado, false);
        verify(logJetonServiceMock).logTipoAnexoAtualizado(any(TipoAnexo.class), eq(salvo));
    }

    @Test
    @DisplayName("deve lançar exceção ao atualizar tipo com ID nulo")
    void deveLancarExcecaoAtualizarComIdNulo() {
        TipoAnexo tipo = criarTipoAnexoValido(null);

        assertThatThrownBy(() -> service.atualizar(tipo))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("ID do tipo de anexo não informado para atualização.");

        verify(repositoryMock, never()).findById(any());
        verify(repositoryMock, never()).save(any());
    }

    @Test
    @DisplayName("deve lançar exceção ao atualizar tipo inexistente")
    void deveLancarExcecaoAtualizarInexistente() {
        Integer id = 999;
        TipoAnexo tipo = criarTipoAnexoValido(id);

        when(repositoryMock.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service.atualizar(tipo))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Tipo de anexo não encontrado para atualização.");

        verify(repositoryMock, never()).save(any());
        verify(arquivoValidatorMock, never()).validarNomeUnicoTipoAnexo(any(), anyBoolean());
    }

    @Test
    @DisplayName("deve lançar exceção ao atualizar tipo com nome duplicado em outro registro")
    void deveLancarExcecaoAtualizarNomeDuplicado() {
        Integer id = 50;
        TipoAnexo existente = criarTipoAnexoValido(id);
        TipoAnexo atualizado = criarTipoAnexo(id, "Novo Nome", TipoAnexo.EXIGE_PUBLICACAO_NAO);

        // ✅ Mock do existsById e findById
        when(repositoryMock.existsById(id)).thenReturn(true);
        when(repositoryMock.findById(id)).thenReturn(Optional.of(existente));

        // ✅ Configura o validador para lançar exceção
        doThrow(new RuntimeException("Já existe um tipo de anexo cadastrado com o nome 'Novo Nome'."))
                .when(arquivoValidatorMock).validarNomeUnicoTipoAnexo(atualizado, false);

        assertThatThrownBy(() -> service.atualizar(atualizado))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Já existe um tipo de anexo cadastrado com o nome 'Novo Nome'.");

        verify(repositoryMock, never()).save(any());
    }

    // ========== TESTES DE EXCLUSÃO ==========

    @Test
    @DisplayName("deve excluir tipo de anexo com sucesso quando sem comprovantes vinculados")
    void deveExcluirTipoAnexoComSucesso() {
        Integer id = 50;
        TipoAnexo tipo = criarTipoAnexoValido(id);

        when(repositoryMock.findById(id)).thenReturn(Optional.of(tipo));
        doNothing().when(arquivoValidatorMock).validarExclusaoTipoAnexo(id);

        service.excluir(id);

        verify(repositoryMock).deleteById(id);
        verify(arquivoValidatorMock).validarExclusaoTipoAnexo(id);
        verify(logJetonServiceMock).logTipoAnexoExcluido(tipo);
    }

    @Test
    @DisplayName("deve lançar exceção ao excluir tipo inexistente")
    void deveLancarExcecaoExcluirInexistente() {
        Integer id = 999;

        when(repositoryMock.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.excluir(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Tipo de anexo não encontrado para exclusão.");

        verify(repositoryMock, never()).deleteById(any());
        verify(arquivoValidatorMock, never()).validarExclusaoTipoAnexo(anyInt());
    }

    @Test
    @DisplayName("deve lançar exceção ao excluir tipo com comprovantes vinculados")
    void deveLancarExcecaoExcluirComComprovantes() {
        Integer id = 50;
        TipoAnexo tipo = criarTipoAnexoValido(id);

        when(repositoryMock.findById(id)).thenReturn(Optional.of(tipo));
        doThrow(new RuntimeException(
                "Não é possível excluir este tipo de anexo pois existem 3 comprovante(s) vinculado(s) a ele."))
                .when(arquivoValidatorMock).validarExclusaoTipoAnexo(id);

        assertThatThrownBy(() -> service.excluir(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessage(
                        "Não é possível excluir este tipo de anexo pois existem 3 comprovante(s) vinculado(s) a ele.");

        verify(repositoryMock, never()).deleteById(any());
    }

    // ========== TESTES DE CONSULTA ==========

    @Test
    @DisplayName("deve listar todos os tipos de anexo")
    void deveListarTodosTipos() {
        List<TipoAnexo> lista = List.of(
                criarTipoAnexoValido(1),
                criarTipoAnexoValido(2));
        when(repositoryMock.findAll()).thenReturn(lista);

        List<TipoAnexo> resultado = service.listarTodos();

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getIdTipo()).isEqualTo(1);
        assertThat(resultado.get(1).getIdTipo()).isEqualTo(2);
        verify(repositoryMock).findAll();
    }

    @Test
    @DisplayName("deve buscar tipo por ID com sucesso")
    void deveBuscarPorIdComSucesso() {
        Integer id = 10;
        TipoAnexo tipo = criarTipoAnexoValido(id);
        when(repositoryMock.findById(id)).thenReturn(Optional.of(tipo));

        Optional<TipoAnexo> resultado = service.buscarPorId(id);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getIdTipo()).isEqualTo(id);
        verify(repositoryMock).findById(id);
    }

    @Test
    @DisplayName("deve retornar Optional vazio ao buscar tipo inexistente")
    void deveRetornarOptionalVazioBuscarInexistente() {
        Integer id = 999;
        when(repositoryMock.findById(id)).thenReturn(Optional.empty());

        Optional<TipoAnexo> resultado = service.buscarPorId(id);

        assertThat(resultado).isEmpty();
        verify(repositoryMock).findById(id);
    }

    @Test
    @DisplayName("deve buscar tipo ou falhar com sucesso quando existe")
    void deveBuscarOuFalharComSucesso() {
        Integer id = 10;
        TipoAnexo tipo = criarTipoAnexoValido(id);
        when(repositoryMock.findById(id)).thenReturn(Optional.of(tipo));

        TipoAnexo resultado = service.buscarOuFalhar(id);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getIdTipo()).isEqualTo(id);
        verify(repositoryMock).findById(id);
    }

    @Test
    @DisplayName("deve lançar exceção ao buscar ou falhar com ID inexistente")
    void deveLancarExcecaoBuscarOuFalharInexistente() {
        Integer id = 999;
        when(repositoryMock.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarOuFalhar(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Tipo de anexo não encontrado com ID: " + id);
    }
}