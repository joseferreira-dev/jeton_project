package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.domain.NivelAcesso;
import br.com.cremepe.jeton.repository.NivelAcessoRepository;
import br.com.cremepe.jeton.repository.UsuarioAcessoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
@DisplayName("Testes do serviço de Níveis de Acesso")
class NivelAcessoServiceTest {

    @Mock
    private NivelAcessoRepository repositoryMock;

    @Mock
    private UsuarioAcessoRepository usuarioAcessoRepositoryMock;

    @Mock
    private LogJetonService logJetonServiceMock;

    @InjectMocks
    private NivelAcessoService service;

    // ========== HELPERS ==========

    private NivelAcesso criarNivel(String id, String nome) {
        NivelAcesso nivel = new NivelAcesso();
        nivel.setIdNivel(id);
        nivel.setNomeNivel(nome);
        return nivel;
    }

    // ========== TESTES DE CRIAÇÃO ==========

    @Test
    @DisplayName("deve criar nível de acesso com sucesso")
    void deveCriarNivelComSucesso() {
        // Dado
        String id = "X";
        String nome = "Novo Nível";
        NivelAcesso nivel = criarNivel(id, nome);

        // ✅ Mock das verificações de unicidade
        when(repositoryMock.existsById(id)).thenReturn(false);
        when(repositoryMock.findByNomeNivel(nome)).thenReturn(Optional.empty());

        // ✅ Mock do save
        when(repositoryMock.save(any(NivelAcesso.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Quando
        NivelAcesso salvo = service.criar(nivel);

        // Então
        assertThat(salvo).isNotNull();
        assertThat(salvo.getIdNivel()).isEqualTo(id);
        assertThat(salvo.getNomeNivel()).isEqualTo(nome);

        verify(repositoryMock).save(nivel);
        verify(logJetonServiceMock).logNivelAcessoCriado(salvo);
    }

    @Test
    @DisplayName("deve lançar exceção ao criar nível com ID já existente")
    void deveLancarExcecaoCriarNivelIdExistente() {
        // Dado
        String id = "A";
        NivelAcesso nivel = criarNivel(id, "Atividades");

        when(repositoryMock.existsById(id)).thenReturn(true);

        // Quando / Então
        assertThatThrownBy(() -> service.criar(nivel))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Já existe um nível de acesso com o ID 'A'.");

        verify(repositoryMock, never()).save(any());
        verify(logJetonServiceMock, never()).logNivelAcessoCriado(any());
    }

    @Test
    @DisplayName("deve lançar exceção ao criar nível com nome já existente")
    void deveLancarExcecaoCriarNivelNomeExistente() {
        // Dado
        String id = "X";
        String nome = "Atividades";
        NivelAcesso nivel = criarNivel(id, nome);

        when(repositoryMock.existsById(id)).thenReturn(false);
        NivelAcesso existente = criarNivel("A", nome);
        when(repositoryMock.findByNomeNivel(nome)).thenReturn(Optional.of(existente));

        // Quando / Então
        assertThatThrownBy(() -> service.criar(nivel))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Já existe um nível de acesso com o nome 'Atividades'.");

        verify(repositoryMock, never()).save(any());
    }

    @Test
    @DisplayName("deve normalizar ID para maiúsculo ao criar nível")
    void deveNormalizarIdParaMaiusculoAoCriar() {
        // Dado
        String idMinusculo = "x";
        String nome = "Novo Nível";
        NivelAcesso nivel = criarNivel(idMinusculo, nome);

        when(repositoryMock.existsById("X")).thenReturn(false);
        when(repositoryMock.findByNomeNivel(nome)).thenReturn(Optional.empty());

        ArgumentCaptor<NivelAcesso> captor = ArgumentCaptor.forClass(NivelAcesso.class);
        when(repositoryMock.save(captor.capture()))
                .thenAnswer(inv -> inv.getArgument(0));

        // Quando
        service.criar(nivel);

        // Então
        NivelAcesso salvo = captor.getValue();
        assertThat(salvo.getIdNivel()).isEqualTo("X");
        assertThat(salvo.getNomeNivel()).isEqualTo(nome);
    }

    // ========== TESTES DE ATUALIZAÇÃO ==========

    @Test
    @DisplayName("deve atualizar nível de acesso com sucesso")
    void deveAtualizarNivelComSucesso() {
        // Dado
        String id = "A";
        String nomeAntigo = "Atividades";
        String nomeNovo = "Atividades Conselhais";
        NivelAcesso existente = criarNivel(id, nomeAntigo);
        NivelAcesso atualizado = criarNivel(id, nomeNovo);

        when(repositoryMock.findById(id)).thenReturn(Optional.of(existente));
        when(repositoryMock.findByNomeNivel(nomeNovo)).thenReturn(Optional.empty());

        when(repositoryMock.save(any(NivelAcesso.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Quando
        NivelAcesso salvo = service.atualizar(atualizado);

        // Então
        assertThat(salvo).isNotNull();
        assertThat(salvo.getNomeNivel()).isEqualTo(nomeNovo);

        verify(repositoryMock).save(existente);
        verify(logJetonServiceMock).logNivelAcessoAtualizado(any(NivelAcesso.class), eq(salvo));
    }

    @Test
    @DisplayName("deve lançar exceção ao atualizar nível inexistente")
    void deveLancarExcecaoAtualizarNivelInexistente() {
        // Dado
        String id = "Z";
        NivelAcesso nivel = criarNivel(id, "Inexistente");

        when(repositoryMock.findById(id)).thenReturn(Optional.empty());

        // Quando / Então
        assertThatThrownBy(() -> service.atualizar(nivel))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Nível de acesso não encontrado para atualização.");

        verify(repositoryMock, never()).save(any());
    }

    @Test
    @DisplayName("deve lançar exceção ao atualizar nível com nome já existente em outro registro")
    void deveLancarExcecaoAtualizarNivelNomeExistente() {
        // Dado
        String id = "B";
        String nome = "Bloqueio";
        NivelAcesso existente = criarNivel(id, "Antigo");
        NivelAcesso atualizado = criarNivel(id, nome);

        when(repositoryMock.findById(id)).thenReturn(Optional.of(existente));
        NivelAcesso outro = criarNivel("C", nome);
        when(repositoryMock.findByNomeNivel(nome)).thenReturn(Optional.of(outro));

        // Quando / Então
        assertThatThrownBy(() -> service.atualizar(atualizado))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Já existe um nível de acesso com o nome 'Bloqueio'.");

        verify(repositoryMock, never()).save(any());
    }

    // ========== TESTES DE EXCLUSÃO ==========

    @Test
    @DisplayName("deve excluir nível de acesso com sucesso quando não está em uso")
    void deveExcluirNivelComSucesso() {
        // Dado
        String id = "Z";
        NivelAcesso nivel = criarNivel(id, "Nível Teste");

        when(repositoryMock.findById(id)).thenReturn(Optional.of(nivel));
        when(usuarioAcessoRepositoryMock.existsByIdIdNivel(id)).thenReturn(false);

        // Quando
        service.excluir(id);

        // Então
        verify(repositoryMock).deleteById(id);
        verify(logJetonServiceMock).logNivelAcessoExcluido(id, nivel.getNomeNivel());
    }

    @Test
    @DisplayName("deve lançar exceção ao excluir nível que está em uso por usuários")
    void deveLancarExcecaoExcluirNivelEmUso() {
        // Dado
        String id = "A";
        NivelAcesso nivel = criarNivel(id, "Atividades");

        lenient().when(repositoryMock.findById(id)).thenReturn(Optional.of(nivel));
        lenient().when(usuarioAcessoRepositoryMock.existsByIdIdNivel(id)).thenReturn(true);

        // Quando / Então
        assertThatThrownBy(() -> service.excluir(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessage(
                        "Não é possível excluir o nível de acesso 'A' pois ele está associado a um ou mais usuários.");

        verify(repositoryMock, never()).deleteById(any());
        verify(logJetonServiceMock, never()).logNivelAcessoExcluido(anyString(), anyString());
    }

    @Test
    @DisplayName("deve lançar exceção ao excluir nível inexistente")
    void deveLancarExcecaoExcluirNivelInexistente() {
        // Dado
        String id = "Z";

        when(repositoryMock.findById(id)).thenReturn(Optional.empty());

        // Quando / Então
        assertThatThrownBy(() -> service.excluir(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Nível de acesso não encontrado: Z");

        verify(repositoryMock, never()).deleteById(any());
    }

    // ========== TESTES DE CONSULTA ==========

    @Test
    @DisplayName("deve listar todos os níveis de acesso")
    void deveListarTodosNiveis() {
        // Dado
        List<NivelAcesso> lista = List.of(
                criarNivel("A", "Atividades"),
                criarNivel("B", "Bloqueio"));
        when(repositoryMock.findAll()).thenReturn(lista);

        // Quando
        List<NivelAcesso> resultado = service.listarTodos();

        // Então
        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getIdNivel()).isEqualTo("A");
        assertThat(resultado.get(1).getIdNivel()).isEqualTo("B");
        verify(repositoryMock).findAll();
    }

    @Test
    @DisplayName("deve buscar nível por ID com sucesso")
    void deveBuscarNivelPorIdComSucesso() {
        // Dado
        String id = "A";
        NivelAcesso nivel = criarNivel(id, "Atividades");
        when(repositoryMock.findById(id)).thenReturn(Optional.of(nivel));

        // Quando
        Optional<NivelAcesso> resultado = service.buscarPorId(id);

        // Então
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getIdNivel()).isEqualTo(id);
    }

    @Test
    @DisplayName("deve retornar Optional vazio ao buscar nível inexistente")
    void deveRetornarOptionalVazioBuscarInexistente() {
        // Dado
        String id = "Z";
        when(repositoryMock.findById(id)).thenReturn(Optional.empty());

        // Quando
        Optional<NivelAcesso> resultado = service.buscarPorId(id);

        // Então
        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("deve buscar nível ou falhar com sucesso quando existe")
    void deveBuscarOuFalharComSucesso() {
        // Dado
        String id = "A";
        NivelAcesso nivel = criarNivel(id, "Atividades");
        when(repositoryMock.findById(id)).thenReturn(Optional.of(nivel));

        // Quando
        NivelAcesso resultado = service.buscarOuFalhar(id);

        // Então
        assertThat(resultado).isNotNull();
        assertThat(resultado.getIdNivel()).isEqualTo(id);
    }

    @Test
    @DisplayName("deve lançar exceção ao buscar ou falhar com ID inexistente")
    void deveLancarExcecaoBuscarOuFalharInexistente() {
        // Dado
        String id = "Z";
        when(repositoryMock.findById(id)).thenReturn(Optional.empty());

        // Quando / Então
        assertThatThrownBy(() -> service.buscarOuFalhar(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Nível de acesso não encontrado: Z");
    }
}