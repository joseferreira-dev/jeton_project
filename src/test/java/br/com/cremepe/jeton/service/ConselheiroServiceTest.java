package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.domain.Conselheiro;
import br.com.cremepe.jeton.domain.Pessoa;
import br.com.cremepe.jeton.domain.Usuario;
import br.com.cremepe.jeton.repository.ConselheiroRepository;
import br.com.cremepe.jeton.repository.GestaoConselheiroRepository;
import br.com.cremepe.jeton.repository.PessoaRepository;
import br.com.cremepe.jeton.repository.UsuarioRepository;
import br.com.cremepe.jeton.util.PessoaValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do serviço de Conselheiros")
class ConselheiroServiceTest {

    @Mock
    private ConselheiroRepository conselheiroRepositoryMock;

    @Mock
    private GestaoConselheiroRepository gestaoConselheiroRepositoryMock;

    @Mock
    private PessoaRepository pessoaRepositoryMock;

    @Mock
    private UsuarioRepository usuarioRepositoryMock;

    @Mock
    private PasswordEncoder passwordEncoderMock;

    @Mock
    private PermissaoService permissaoServiceMock;

    @Mock
    private UsuarioService usuarioServiceMock;

    @Mock
    private LogJetonService logJetonServiceMock;

    @Mock
    private PessoaValidator pessoaValidatorMock;

    @InjectMocks
    private ConselheiroService service;

    // ========== HELPERS ==========

    private Pessoa criarPessoa(Integer id, String nome, String cpf, String email) {
        Pessoa p = new Pessoa();
        p.setIdPessoa(id);
        p.setNome(nome);
        p.setCpf(cpf);
        p.setEmail(email);
        p.setInTipoPessoa(Pessoa.TIPO_CONSELHEIRO);
        return p;
    }

    private Conselheiro criarConselheiro(Integer id, String nome, String cpf, String email, Integer crm, String senha) {
        Pessoa p = criarPessoa(id, nome, cpf, email);
        Conselheiro c = new Conselheiro();
        c.setIdPessoa(id);
        c.setPessoa(p);
        c.setCrm(crm);
        c.setInSituacao(Conselheiro.SITUACAO_ATIVO);
        c.setSenhaAcesso(senha);
        return c;
    }

    private Usuario mockUsuarioComId(Integer id) {
        Usuario usuario = mock(Usuario.class);
        lenient().when(usuario.getIdUsuarioPessoa()).thenReturn(id);
        lenient().when(usuario.getInSituacao()).thenReturn(Usuario.SITUACAO_ATIVO);
        return usuario;
    }

    // ========== TESTES DE CRIAÇÃO ==========

    @Test
    @DisplayName("deve criar conselheiro com sucesso")
    void deveCriarConselheiroComSucesso() {
        // Dado
        String cpf = "12345678900";
        Integer crm = 12345;
        String senha = "senha123";
        Integer idEsperado = 999;
        Conselheiro conselheiro = criarConselheiro(null, "Dr. Teste", cpf, "teste@email.com", crm, senha);

        // Mocks de validação
        doNothing().when(pessoaValidatorMock).validarCpf(anyString());
        doNothing().when(pessoaValidatorMock).validarCpfUnico(anyString(), isNull());
        doNothing().when(pessoaValidatorMock).validarCrmUnico(anyInt(), isNull());

        // Mock do save do conselheiro
        when(conselheiroRepositoryMock.save(any(Conselheiro.class)))
                .thenAnswer(inv -> {
                    Conselheiro c = inv.getArgument(0);
                    c.setIdPessoa(idEsperado);
                    return c;
                });

        // Mock do save do usuário retornando um Usuario com ID
        Usuario usuarioMock = mockUsuarioComId(idEsperado);
        when(usuarioRepositoryMock.save(any(Usuario.class))).thenReturn(usuarioMock);

        // Mock da permissão
        when(permissaoServiceMock.concederPermissao(eq(idEsperado), eq("C")))
                .thenReturn(mock(br.com.cremepe.jeton.domain.UsuarioAcesso.class));

        // Quando
        Conselheiro salvo = service.criar(conselheiro);

        // Então
        assertThat(salvo).isNotNull();
        assertThat(salvo.getIdPessoa()).isEqualTo(idEsperado);
        assertThat(salvo.getPessoa().getCpf()).isEqualTo(cpf);
        assertThat(salvo.getCrm()).isEqualTo(crm);

        verify(pessoaValidatorMock).validarCpf(cpf);
        verify(pessoaValidatorMock).validarCpfUnico(cpf, null);
        verify(pessoaValidatorMock).validarCrmUnico(crm, null);
        verify(conselheiroRepositoryMock).save(any(Conselheiro.class));
        verify(usuarioRepositoryMock).save(any(Usuario.class));
        verify(permissaoServiceMock).concederPermissao(eq(idEsperado), eq("C"));
        verify(logJetonServiceMock).logConselheiroCriado(salvo);
    }

    @Test
    @DisplayName("deve lançar exceção ao criar conselheiro com CPF inválido")
    void deveLancarExcecaoCriarConselheiroCpfInvalido() {
        String cpfInvalido = "123";
        Conselheiro conselheiro = criarConselheiro(null, "Dr. Teste", cpfInvalido, "teste@email.com", 12345, "senha");

        doThrow(new RuntimeException("CPF inválido."))
                .when(pessoaValidatorMock).validarCpf(cpfInvalido);

        assertThatThrownBy(() -> service.criar(conselheiro))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("CPF inválido.");

        verify(conselheiroRepositoryMock, never()).save(any());
    }

    @Test
    @DisplayName("deve lançar exceção ao criar conselheiro com CPF já cadastrado")
    void deveLancarExcecaoCriarConselheiroCpfDuplicado() {
        String cpf = "12345678900";
        Conselheiro conselheiro = criarConselheiro(null, "Dr. Teste", cpf, "teste@email.com", 12345, "senha");

        doNothing().when(pessoaValidatorMock).validarCpf(cpf);
        doThrow(new RuntimeException("Já existe um cadastro com este CPF."))
                .when(pessoaValidatorMock).validarCpfUnico(cpf, null);

        assertThatThrownBy(() -> service.criar(conselheiro))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Já existe um cadastro com este CPF.");

        verify(conselheiroRepositoryMock, never()).save(any());
    }

    @Test
    @DisplayName("deve lançar exceção ao criar conselheiro com CRM já cadastrado")
    void deveLancarExcecaoCriarConselheiroCrmDuplicado() {
        Integer crm = 12345;
        Conselheiro conselheiro = criarConselheiro(null, "Dr. Teste", "12345678900", "teste@email.com", crm, "senha");

        doNothing().when(pessoaValidatorMock).validarCpf(anyString());
        doNothing().when(pessoaValidatorMock).validarCpfUnico(anyString(), isNull());
        doThrow(new RuntimeException("Já existe um conselheiro com o CRM " + crm))
                .when(pessoaValidatorMock).validarCrmUnico(crm, null);

        assertThatThrownBy(() -> service.criar(conselheiro))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Já existe um conselheiro com o CRM " + crm);

        verify(conselheiroRepositoryMock, never()).save(any());
    }

    @Test
    @DisplayName("deve lançar exceção ao criar conselheiro com senha em branco")
    void deveLancarExcecaoCriarConselheiroSemSenha() {
        Conselheiro conselheiro = criarConselheiro(null, "Dr. Teste", "12345678900", "teste@email.com", 12345, null);
        conselheiro.setSenhaAcesso(null);

        doNothing().when(pessoaValidatorMock).validarCpf(anyString());
        doNothing().when(pessoaValidatorMock).validarCpfUnico(anyString(), isNull());
        doNothing().when(pessoaValidatorMock).validarCrmUnico(anyInt(), isNull());

        when(conselheiroRepositoryMock.save(any(Conselheiro.class)))
                .thenAnswer(inv -> {
                    Conselheiro c = inv.getArgument(0);
                    c.setIdPessoa(999);
                    return c;
                });

        assertThatThrownBy(() -> service.criar(conselheiro))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("A senha é obrigatória para criar o acesso no sistema.");

        verify(conselheiroRepositoryMock).save(any(Conselheiro.class));
        verify(usuarioRepositoryMock, never()).save(any());
        verify(permissaoServiceMock, never()).concederPermissao(anyInt(), anyString());
    }

    // ========== TESTES DE ATUALIZAÇÃO ==========

    @Test
    @DisplayName("deve atualizar conselheiro com sucesso mantendo a mesma senha")
    void deveAtualizarConselheiroComSucesso() {
        Integer id = 100;
        String cpf = "11122233344";
        Integer crm = 54321;
        Conselheiro existente = criarConselheiro(id, "Dr. Antigo", cpf, "antigo@email.com", crm, "senhaAntiga");
        Conselheiro atualizado = criarConselheiro(id, "Dr. Novo", cpf, "novo@email.com", crm, null);

        when(conselheiroRepositoryMock.findById(id)).thenReturn(Optional.of(existente));
        when(conselheiroRepositoryMock.save(any(Conselheiro.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario usuarioMock = mockUsuarioComId(id);
        when(usuarioRepositoryMock.save(any(Usuario.class))).thenReturn(usuarioMock);

        when(permissaoServiceMock.hasPermissao(id, "C")).thenReturn(true);

        Conselheiro salvo = service.atualizar(atualizado);

        assertThat(salvo).isNotNull();
        assertThat(salvo.getPessoa().getNome()).isEqualTo("Dr. Novo");
        assertThat(salvo.getPessoa().getEmail()).isEqualTo("novo@email.com");

        verify(conselheiroRepositoryMock).save(existente);
        verify(usuarioRepositoryMock).save(any(Usuario.class));
        verify(logJetonServiceMock).logConselheiroAtualizado(any(Conselheiro.class), any(Conselheiro.class));
    }

    @Test
    @DisplayName("deve atualizar conselheiro com sucesso e conceder permissão se não existir")
    void deveAtualizarConselheiroEConcederPermissao() {
        Integer id = 200;
        String cpf = "22233344455";
        Integer crm = 98765;
        Conselheiro existente = criarConselheiro(id, "Dr. Teste", cpf, "teste@email.com", crm, "senha");
        Conselheiro atualizado = criarConselheiro(id, "Dr. Teste Atualizado", cpf, "novo@email.com", crm, null);

        when(conselheiroRepositoryMock.findById(id)).thenReturn(Optional.of(existente));
        when(conselheiroRepositoryMock.save(any(Conselheiro.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario usuarioMock = mockUsuarioComId(id);
        when(usuarioRepositoryMock.save(any(Usuario.class))).thenReturn(usuarioMock);

        when(permissaoServiceMock.hasPermissao(id, "C")).thenReturn(false);
        when(permissaoServiceMock.concederPermissao(id, "C"))
                .thenReturn(mock(br.com.cremepe.jeton.domain.UsuarioAcesso.class));

        Conselheiro salvo = service.atualizar(atualizado);

        assertThat(salvo).isNotNull();
        verify(permissaoServiceMock).concederPermissao(id, "C");
        verify(usuarioRepositoryMock).save(any(Usuario.class));
        verify(logJetonServiceMock).logConselheiroAtualizado(any(), any());
    }

    @Test
    @DisplayName("deve lançar exceção ao atualizar conselheiro inexistente")
    void deveLancarExcecaoAtualizarConselheiroInexistente() {
        Integer idInexistente = 999;
        Conselheiro conselheiro = criarConselheiro(idInexistente, "Dr. X", "123", "x@x.com", 123, "senha");

        when(conselheiroRepositoryMock.findById(idInexistente)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(conselheiro))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Conselheiro não encontrado para edição.");

        verify(conselheiroRepositoryMock, never()).save(any());
    }

    // ========== TESTES DE EXCLUSÃO ==========

    @Test
    @DisplayName("deve excluir conselheiro com sucesso")
    void deveExcluirConselheiroComSucesso() {
        Integer id = 300;
        Conselheiro conselheiro = criarConselheiro(id, "Dr. Delete", "33344455566", "delete@email.com", 11111, "senha");
        when(conselheiroRepositoryMock.findById(id)).thenReturn(Optional.of(conselheiro));

        service.excluir(id);

        verify(usuarioServiceMock).excluir(id);
        verify(logJetonServiceMock).logConselheiroExcluido(any(Conselheiro.class));
    }

    @Test
    @DisplayName("deve lançar exceção ao excluir conselheiro inexistente")
    void deveLancarExcecaoExcluirConselheiroInexistente() {
        Integer idInexistente = 999;
        when(conselheiroRepositoryMock.findById(idInexistente)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.excluir(idInexistente))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Conselheiro não encontrado para exclusão.");

        verify(usuarioServiceMock, never()).excluir(anyInt());
    }

    // ========== TESTES DE CONSULTA ==========

    @Test
    @DisplayName("deve listar todos os conselheiros")
    void deveListarTodosConselheiros() {
        List<Conselheiro> lista = List.of(
                criarConselheiro(1, "Dr. A", "111", "a@a.com", 111, null),
                criarConselheiro(2, "Dr. B", "222", "b@b.com", 222, null));
        when(conselheiroRepositoryMock.findAll()).thenReturn(lista);

        List<Conselheiro> resultado = service.listarTodos();

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getPessoa().getNome()).isEqualTo("Dr. A");
        verify(conselheiroRepositoryMock).findAll();
    }

    @Test
    @DisplayName("deve buscar conselheiro por ID com sucesso")
    void deveBuscarConselheiroPorIdComSucesso() {
        Integer id = 10;
        Conselheiro conselheiro = criarConselheiro(id, "Dr. Busca", "444", "busca@email.com", 444, null);
        when(conselheiroRepositoryMock.findById(id)).thenReturn(Optional.of(conselheiro));

        Optional<Conselheiro> resultado = service.buscarPorId(id);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getIdPessoa()).isEqualTo(id);
    }

    @Test
    @DisplayName("deve listar conselheiros paginados com filtros")
    void deveListarConselheirosPaginadosComFiltros() {
        String termo = "Dr.";
        String situacao = "A";
        int page = 0, size = 10;
        String sortField = "pessoa.nome", sortDir = "asc";
        Page<Conselheiro> paginaEsperada = new PageImpl<>(List.of());

        when(conselheiroRepositoryMock.findAllByFilters(anyString(), anyString(), any(Pageable.class)))
                .thenReturn(paginaEsperada);

        Page<Conselheiro> resultado = service.listarComPaginacaoEPesquisa(termo, situacao, page, size, sortField,
                sortDir);

        assertThat(resultado).isNotNull();
        verify(conselheiroRepositoryMock).findAllByFilters(eq(termo), eq(situacao), any(Pageable.class));
    }

    @Test
    @DisplayName("deve listar conselheiros não vinculados a uma gestão")
    void deveListarConselheirosNaoVinculados() {
        Integer idGestao = 5;
        String termo = "Dr.";
        Page<Conselheiro> pagina = new PageImpl<>(List.of(
                criarConselheiro(10, "Dr. Não Vinculado", "555", "nao@email.com", 555, null)));

        when(gestaoConselheiroRepositoryMock.findByGestaoIdGestao(idGestao))
                .thenReturn(List.of());
        when(conselheiroRepositoryMock.findNaoVinculados(anyString(), anyList(), any(Pageable.class)))
                .thenReturn(pagina);

        List<Conselheiro> resultado = service.listarNaoVinculados(idGestao, termo);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getPessoa().getNome()).isEqualTo("Dr. Não Vinculado");
    }

    @Test
    @DisplayName("deve listar conselheiros não vinculados tratando termo nulo")
    void deveListarConselheirosNaoVinculadosComTermoNulo() {
        Integer idGestao = 5;
        Page<Conselheiro> pagina = new PageImpl<>(List.of());

        when(gestaoConselheiroRepositoryMock.findByGestaoIdGestao(idGestao))
                .thenReturn(List.of());
        when(conselheiroRepositoryMock.findNaoVinculados(eq(""), anyList(), any(Pageable.class)))
                .thenReturn(pagina);

        List<Conselheiro> resultado = service.listarNaoVinculados(idGestao, null);

        assertThat(resultado).isEmpty();
        verify(conselheiroRepositoryMock).findNaoVinculados(eq(""), anyList(), any(Pageable.class));
    }
}