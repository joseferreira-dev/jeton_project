package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.domain.NivelAcesso;
import br.com.cremepe.jeton.domain.Pessoa;
import br.com.cremepe.jeton.domain.Usuario;
import br.com.cremepe.jeton.domain.UsuarioAcesso;
import br.com.cremepe.jeton.domain.UsuarioAcessoId;
import br.com.cremepe.jeton.repository.NivelAcessoRepository;
import br.com.cremepe.jeton.repository.UsuarioAcessoRepository;
import br.com.cremepe.jeton.repository.UsuarioRepository;
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
@DisplayName("Testes do serviço de Permissões (PermissaoService)")
class PermissaoServiceTest {

    @Mock
    private UsuarioAcessoRepository usuarioAcessoRepositoryMock;

    @Mock
    private UsuarioRepository usuarioRepositoryMock;

    @Mock
    private NivelAcessoRepository nivelAcessoRepositoryMock;

    @Mock
    private LogJetonService logJetonServiceMock;

    @InjectMocks
    private PermissaoService service;

    // ========== HELPERS ==========

    private Usuario criarUsuario(Integer id, String nome) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuarioPessoa(id);
        // Cria uma pessoa mock para evitar NPE no log
        Pessoa pessoa = new Pessoa();
        pessoa.setIdPessoa(id);
        pessoa.setNome(nome);
        usuario.setPessoa(pessoa);
        return usuario;
    }

    private NivelAcesso criarNivel(String id, String nome) {
        NivelAcesso nivel = new NivelAcesso();
        nivel.setIdNivel(id);
        nivel.setNomeNivel(nome);
        return nivel;
    }

    private UsuarioAcesso criarUsuarioAcesso(Integer idUsuario, String idNivel) {
        UsuarioAcessoId id = new UsuarioAcessoId(idUsuario, idNivel);
        UsuarioAcesso ua = new UsuarioAcesso();
        ua.setId(id);
        return ua;
    }

    // ========== TESTES DE CONCESSÃO ==========

    @Test
    @DisplayName("deve conceder permissão com sucesso quando usuário e nível existem")
    void deveConcederPermissaoComSucesso() {
        // Dado
        Integer idUsuario = 1;
        String idNivel = "A";
        Usuario usuario = criarUsuario(idUsuario, "Usuário Teste");
        NivelAcesso nivel = criarNivel(idNivel, "Atividades");

        UsuarioAcessoId idComposto = new UsuarioAcessoId(idUsuario, idNivel);

        // Mock: permissão não existe
        when(usuarioAcessoRepositoryMock.findById(idComposto)).thenReturn(Optional.empty());
        when(usuarioRepositoryMock.findById(idUsuario)).thenReturn(Optional.of(usuario));
        when(nivelAcessoRepositoryMock.findById(idNivel)).thenReturn(Optional.of(nivel));

        // Mock do save
        when(usuarioAcessoRepositoryMock.save(any(UsuarioAcesso.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Quando
        UsuarioAcesso resultado = service.concederPermissao(idUsuario, idNivel);

        // Então
        assertThat(resultado).isNotNull();
        assertThat(resultado.getId().getIdUsuarioPessoa()).isEqualTo(idUsuario);
        assertThat(resultado.getId().getIdNivel()).isEqualTo(idNivel);

        verify(usuarioAcessoRepositoryMock).save(any(UsuarioAcesso.class));
        verify(logJetonServiceMock).logPermissaoConcedida(usuario, nivel);
    }

    @Test
    @DisplayName("deve retornar permissão existente ao tentar conceder permissão duplicada")
    void deveRetornarPermissaoExistenteAoConcederDuplicada() {
        // Dado
        Integer idUsuario = 1;
        String idNivel = "A";
        UsuarioAcessoId idComposto = new UsuarioAcessoId(idUsuario, idNivel);
        UsuarioAcesso permissaoExistente = criarUsuarioAcesso(idUsuario, idNivel);

        when(usuarioAcessoRepositoryMock.findById(idComposto)).thenReturn(Optional.of(permissaoExistente));

        // Quando
        UsuarioAcesso resultado = service.concederPermissao(idUsuario, idNivel);

        // Então
        assertThat(resultado).isEqualTo(permissaoExistente);

        // Não deve buscar usuário/nível nem salvar
        verify(usuarioRepositoryMock, never()).findById(anyInt());
        verify(nivelAcessoRepositoryMock, never()).findById(anyString());
        verify(usuarioAcessoRepositoryMock, never()).save(any());
        verify(logJetonServiceMock, never()).logPermissaoConcedida(any(), any());
    }

    @Test
    @DisplayName("deve lançar exceção ao conceder permissão com usuário inexistente")
    void deveLancarExcecaoAoConcederPermissaoUsuarioInexistente() {
        // Dado
        Integer idUsuario = 999;
        String idNivel = "A";
        UsuarioAcessoId idComposto = new UsuarioAcessoId(idUsuario, idNivel);

        when(usuarioAcessoRepositoryMock.findById(idComposto)).thenReturn(Optional.empty());
        when(usuarioRepositoryMock.findById(idUsuario)).thenReturn(Optional.empty());

        // Quando / Então
        assertThatThrownBy(() -> service.concederPermissao(idUsuario, idNivel))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Usuário não encontrado com ID: " + idUsuario);

        verify(nivelAcessoRepositoryMock, never()).findById(anyString());
        verify(usuarioAcessoRepositoryMock, never()).save(any());
        verify(logJetonServiceMock, never()).logPermissaoConcedida(any(), any());
    }

    @Test
    @DisplayName("deve lançar exceção ao conceder permissão com nível inexistente")
    void deveLancarExcecaoAoConcederPermissaoNivelInexistente() {
        // Dado
        Integer idUsuario = 1;
        String idNivel = "Z";
        UsuarioAcessoId idComposto = new UsuarioAcessoId(idUsuario, idNivel);
        Usuario usuario = criarUsuario(idUsuario, "Usuário Teste");

        when(usuarioAcessoRepositoryMock.findById(idComposto)).thenReturn(Optional.empty());
        when(usuarioRepositoryMock.findById(idUsuario)).thenReturn(Optional.of(usuario));
        when(nivelAcessoRepositoryMock.findById(idNivel)).thenReturn(Optional.empty());

        // Quando / Então
        assertThatThrownBy(() -> service.concederPermissao(idUsuario, idNivel))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Nível de acesso não encontrado: " + idNivel);

        verify(usuarioAcessoRepositoryMock, never()).save(any());
        verify(logJetonServiceMock, never()).logPermissaoConcedida(any(), any());
    }

    // ========== TESTES DE REVOGAÇÃO ==========

    @Test
    @DisplayName("deve revogar permissão com sucesso quando existe")
    void deveRevogarPermissaoComSucesso() {
        // Dado
        Integer idUsuario = 1;
        String idNivel = "A";
        UsuarioAcessoId idComposto = new UsuarioAcessoId(idUsuario, idNivel);
        Usuario usuario = criarUsuario(idUsuario, "Usuário Teste");
        NivelAcesso nivel = criarNivel(idNivel, "Atividades");

        when(usuarioAcessoRepositoryMock.existsById(idComposto)).thenReturn(true);
        when(usuarioRepositoryMock.findById(idUsuario)).thenReturn(Optional.of(usuario));
        when(nivelAcessoRepositoryMock.findById(idNivel)).thenReturn(Optional.of(nivel));

        // Quando
        service.revogarPermissao(idUsuario, idNivel);

        // Então
        verify(usuarioAcessoRepositoryMock).deleteById(idComposto);
        verify(logJetonServiceMock).logPermissaoRevogada(usuario, nivel);
    }

    @Test
    @DisplayName("não deve fazer nada ao revogar permissão inexistente")
    void naoDeveFazerNadaAoRevogarPermissaoInexistente() {
        // Dado
        Integer idUsuario = 1;
        String idNivel = "X";
        UsuarioAcessoId idComposto = new UsuarioAcessoId(idUsuario, idNivel);

        when(usuarioAcessoRepositoryMock.existsById(idComposto)).thenReturn(false);

        // Quando
        service.revogarPermissao(idUsuario, idNivel);

        // Então
        verify(usuarioAcessoRepositoryMock, never()).deleteById(any());
        verify(usuarioRepositoryMock, never()).findById(anyInt());
        verify(nivelAcessoRepositoryMock, never()).findById(anyString());
        verify(logJetonServiceMock, never()).logPermissaoRevogada(any(), any());
    }

    @Test
    @DisplayName("deve revogar todas as permissões de um usuário com sucesso")
    void deveRevogarTodasPermissoesComSucesso() {
        // Dado
        Integer idUsuario = 1;
        Usuario usuario = criarUsuario(idUsuario, "Usuário Teste");
        List<UsuarioAcesso> permissoes = List.of(
                criarUsuarioAcesso(idUsuario, "A"),
                criarUsuarioAcesso(idUsuario, "B"),
                criarUsuarioAcesso(idUsuario, "C"));

        when(usuarioAcessoRepositoryMock.findByIdIdUsuarioPessoa(idUsuario)).thenReturn(permissoes);
        when(usuarioRepositoryMock.findById(idUsuario)).thenReturn(Optional.of(usuario));

        // Quando
        service.revogarTodasPermissoes(idUsuario);

        // Então
        verify(usuarioAcessoRepositoryMock).deleteByUsuarioId(idUsuario);
        verify(logJetonServiceMock).logTodasPermissoesRevogadas(usuario);
    }

    @Test
    @DisplayName("não deve fazer nada ao revogar todas as permissões de usuário sem permissões")
    void naoDeveFazerNadaAoRevogarTodasPermissoesDeUsuarioSemPermissoes() {
        // Dado
        Integer idUsuario = 1;

        when(usuarioAcessoRepositoryMock.findByIdIdUsuarioPessoa(idUsuario)).thenReturn(List.of());

        // Quando
        service.revogarTodasPermissoes(idUsuario);

        // Então
        verify(usuarioAcessoRepositoryMock, never()).deleteByUsuarioId(anyInt());
        verify(usuarioRepositoryMock, never()).findById(anyInt());
        verify(logJetonServiceMock, never()).logTodasPermissoesRevogadas(any());
    }

    // ========== TESTES DE CONSULTA ==========

    @Test
    @DisplayName("deve verificar permissão retornando true quando existe")
    void deveVerificarPermissaoRetornandoTrue() {
        // Dado
        Integer idUsuario = 1;
        String idNivel = "A";
        UsuarioAcessoId idComposto = new UsuarioAcessoId(idUsuario, idNivel);

        when(usuarioAcessoRepositoryMock.existsById(idComposto)).thenReturn(true);

        // Quando
        boolean temPermissao = service.hasPermissao(idUsuario, idNivel);

        // Então
        assertThat(temPermissao).isTrue();
        verify(usuarioAcessoRepositoryMock).existsById(idComposto);
    }

    @Test
    @DisplayName("deve verificar permissão retornando false quando não existe")
    void deveVerificarPermissaoRetornandoFalse() {
        // Dado
        Integer idUsuario = 1;
        String idNivel = "Z";
        UsuarioAcessoId idComposto = new UsuarioAcessoId(idUsuario, idNivel);

        when(usuarioAcessoRepositoryMock.existsById(idComposto)).thenReturn(false);

        // Quando
        boolean temPermissao = service.hasPermissao(idUsuario, idNivel);

        // Então
        assertThat(temPermissao).isFalse();
        verify(usuarioAcessoRepositoryMock).existsById(idComposto);
    }

    @Test
    @DisplayName("deve listar os IDs das permissões de um usuário")
    void deveListarPermissoesDoUsuario() {
        // Dado
        Integer idUsuario = 1;
        List<UsuarioAcesso> permissoes = List.of(
                criarUsuarioAcesso(idUsuario, "A"),
                criarUsuarioAcesso(idUsuario, "B"));

        when(usuarioAcessoRepositoryMock.findByIdIdUsuarioPessoa(idUsuario)).thenReturn(permissoes);

        // Quando
        List<String> resultado = service.listarPermissoesDoUsuario(idUsuario);

        // Então
        assertThat(resultado).containsExactly("A", "B");
        verify(usuarioAcessoRepositoryMock).findByIdIdUsuarioPessoa(idUsuario);
    }

    @Test
    @DisplayName("deve listar as permissões completas de um usuário")
    void deveListarPermissoesCompletasDoUsuario() {
        // Dado
        Integer idUsuario = 1;
        List<UsuarioAcesso> permissoes = List.of(
                criarUsuarioAcesso(idUsuario, "A"),
                criarUsuarioAcesso(idUsuario, "B"));

        when(usuarioAcessoRepositoryMock.findByIdIdUsuarioPessoa(idUsuario)).thenReturn(permissoes);

        // Quando
        List<UsuarioAcesso> resultado = service.listarPermissoesCompletas(idUsuario);

        // Então
        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getId().getIdNivel()).isEqualTo("A");
        assertThat(resultado.get(1).getId().getIdNivel()).isEqualTo("B");
        verify(usuarioAcessoRepositoryMock).findByIdIdUsuarioPessoa(idUsuario);
    }

    @Test
    @DisplayName("deve listar todas as permissões de todos os usuários")
    void deveListarTodasAsPermissoes() {
        // Dado
        List<UsuarioAcesso> todas = List.of(
                criarUsuarioAcesso(1, "A"),
                criarUsuarioAcesso(1, "B"),
                criarUsuarioAcesso(2, "A"));

        when(usuarioAcessoRepositoryMock.findAll()).thenReturn(todas);

        // Quando
        List<UsuarioAcesso> resultado = service.listarTodos();

        // Então
        assertThat(resultado).hasSize(3);
        verify(usuarioAcessoRepositoryMock).findAll();
    }
}