package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.domain.Conselheiro;
import br.com.cremepe.jeton.domain.Pessoa;
import br.com.cremepe.jeton.domain.Usuario;
import br.com.cremepe.jeton.repository.ConselheiroRepository;
import br.com.cremepe.jeton.repository.PessoaRepository;
import br.com.cremepe.jeton.repository.UsuarioRepository;
import br.com.cremepe.jeton.util.PessoaValidator;
import jakarta.persistence.EntityManager;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do serviço de Usuários (UsuarioService)")
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepositoryMock;

    @Mock
    private PessoaRepository pessoaRepositoryMock;

    @Mock
    private ConselheiroRepository conselheiroRepositoryMock;

    @Mock
    private PasswordEncoder passwordEncoderMock;

    @Mock
    private LogJetonService logJetonServiceMock;

    @Mock
    private PessoaValidator pessoaValidatorMock;

    @Mock
    private EntityManager entityManagerMock; // ✅ CORREÇÃO: mock do EntityManager

    @InjectMocks
    private UsuarioService service;

    // ========== HELPERS ==========

    private Pessoa criarPessoa(Integer id, String nome, String cpf, String email, String tipo) {
        Pessoa p = new Pessoa();
        p.setIdPessoa(id);
        p.setNome(nome);
        p.setCpf(cpf);
        p.setEmail(email);
        p.setInTipoPessoa(tipo);
        return p;
    }

    private Usuario criarUsuario(Integer id, String nome, String cpf, String email, String tipo, String senha,
            String situacao, boolean isConselheiro) {
        Pessoa p = criarPessoa(id, nome, cpf, email, tipo);
        Usuario u = new Usuario();
        u.setIdUsuarioPessoa(id);
        u.setPessoa(p);
        u.setSenha(senha);
        u.setInSituacao(situacao);
        u.seteConselheiro(isConselheiro);
        if (isConselheiro) {
            u.setCrm(12345);
        }
        return u;
    }

    private Usuario criarUsuarioValido(Integer id, boolean isConselheiro) {
        String tipo = isConselheiro ? Pessoa.TIPO_CONSELHEIRO : Pessoa.TIPO_FUNCIONARIO;
        String senha = isConselheiro ? "senhaConselheiro" : "senhaFuncionario";
        // ✅ CORREÇÃO: CPF com 11 dígitos consistentes
        String cpf = String.format("%011d", id != null ? id : 999);
        return criarUsuario(id, "Usuário " + (id != null ? id : 999), cpf,
                "usuario" + (id != null ? id : 999) + "@email.com",
                tipo, senha, Usuario.SITUACAO_ATIVO, isConselheiro);
    }

    // ========== TESTES DE CRIAÇÃO ==========

    @Test
    @DisplayName("deve criar usuário funcionário com sucesso")
    void deveCriarUsuarioFuncionarioComSucesso() {
        // Dado
        Integer idEsperado = 999;
        Usuario usuario = criarUsuarioValido(null, false);
        usuario.setSenha("senha123");

        doNothing().when(pessoaValidatorMock).validarCpf(anyString());
        doNothing().when(pessoaValidatorMock).validarCpfUnico(anyString(), isNull());
        doNothing().when(pessoaValidatorMock).validarEmailUnico(anyString(), isNull());

        when(passwordEncoderMock.encode("senha123")).thenReturn("hashSenha");

        when(usuarioRepositoryMock.save(any(Usuario.class)))
                .thenAnswer(inv -> {
                    Usuario u = inv.getArgument(0);
                    u.setIdUsuarioPessoa(idEsperado);
                    u.getPessoa().setIdPessoa(idEsperado);
                    return u;
                });

        // Quando
        Usuario salvo = service.criar(usuario);

        // Então
        assertThat(salvo).isNotNull();
        assertThat(salvo.getIdUsuarioPessoa()).isEqualTo(idEsperado);
        assertThat(salvo.getPessoa().getInTipoPessoa()).isEqualTo(Pessoa.TIPO_FUNCIONARIO);
        assertThat(salvo.getSenha()).isEqualTo("hashSenha");

        verify(pessoaValidatorMock).validarCpf(usuario.getPessoa().getCpf());
        verify(pessoaValidatorMock).validarCpfUnico(usuario.getPessoa().getCpf(), null);
        verify(pessoaValidatorMock).validarEmailUnico(usuario.getPessoa().getEmail(), null);
        verify(passwordEncoderMock).encode("senha123");
        verify(usuarioRepositoryMock).save(usuario);
        verify(conselheiroRepositoryMock, never()).save(any());
        verify(logJetonServiceMock).logUsuarioCriado(salvo);
    }

    @Test
    @DisplayName("deve criar usuário conselheiro com sucesso e sincronizar tabela conselheiro")
    void deveCriarUsuarioConselheiroComSucesso() {
        // Dado
        Integer idEsperado = 888;
        Integer crm = 54321;
        Usuario usuario = criarUsuarioValido(null, true);
        usuario.setCrm(crm);

        doNothing().when(pessoaValidatorMock).validarCpf(anyString());
        doNothing().when(pessoaValidatorMock).validarCpfUnico(anyString(), isNull());
        doNothing().when(pessoaValidatorMock).validarEmailUnico(anyString(), isNull());
        doNothing().when(pessoaValidatorMock).validarCrmUnico(crm, null);

        when(passwordEncoderMock.encode(anyString())).thenReturn("hashSenha");

        when(usuarioRepositoryMock.save(any(Usuario.class)))
                .thenAnswer(inv -> {
                    Usuario u = inv.getArgument(0);
                    u.setIdUsuarioPessoa(idEsperado);
                    u.getPessoa().setIdPessoa(idEsperado);
                    return u;
                });

        // ✅ Captura o Conselheiro salvo
        ArgumentCaptor<Conselheiro> conselheiroCaptor = ArgumentCaptor.forClass(Conselheiro.class);
        when(conselheiroRepositoryMock.save(conselheiroCaptor.capture()))
                .thenAnswer(inv -> inv.getArgument(0));

        // Quando
        Usuario salvo = service.criar(usuario);

        // Então
        assertThat(salvo).isNotNull();
        assertThat(salvo.getIdUsuarioPessoa()).isEqualTo(idEsperado);

        Conselheiro conselheiroSalvo = conselheiroCaptor.getValue();
        assertThat(conselheiroSalvo).isNotNull();
        // ✅ Verifica que a pessoa associada tem o ID esperado, em vez de verificar
        // diretamente o idPessoa
        assertThat(conselheiroSalvo.getPessoa().getIdPessoa()).isEqualTo(idEsperado);
        assertThat(conselheiroSalvo.getCrm()).isEqualTo(crm);
        assertThat(conselheiroSalvo.getInSituacao()).isEqualTo(Usuario.SITUACAO_ATIVO);

        verify(pessoaValidatorMock).validarCrmUnico(crm, null);
        verify(conselheiroRepositoryMock).save(any(Conselheiro.class));
        verify(logJetonServiceMock).logUsuarioCriado(salvo);
    }

    @Test
    @DisplayName("deve lançar exceção ao criar usuário com CPF inválido")
    void deveLancarExcecaoCriarUsuarioCpfInvalido() {
        // Dado
        Usuario usuario = criarUsuarioValido(null, false);
        String cpf = usuario.getPessoa().getCpf();

        // ✅ CORREÇÃO: o stub usa o CPF real que o serviço passará
        doThrow(new RuntimeException("CPF inválido."))
                .when(pessoaValidatorMock).validarCpf(cpf);

        // Quando / Então
        assertThatThrownBy(() -> service.criar(usuario))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("CPF inválido.");

        verify(usuarioRepositoryMock, never()).save(any());
    }

    @Test
    @DisplayName("deve lançar exceção ao criar usuário com CPF duplicado")
    void deveLancarExcecaoCriarUsuarioCpfDuplicado() {
        // Dado
        Usuario usuario = criarUsuarioValido(null, false);
        String cpf = usuario.getPessoa().getCpf();

        doNothing().when(pessoaValidatorMock).validarCpf(cpf);
        // ✅ CORREÇÃO: usa o CPF real
        doThrow(new RuntimeException("Já existe um cadastro com este CPF."))
                .when(pessoaValidatorMock).validarCpfUnico(cpf, null);

        // Quando / Então
        assertThatThrownBy(() -> service.criar(usuario))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Já existe um cadastro com este CPF.");

        verify(usuarioRepositoryMock, never()).save(any());
    }

    @Test
    @DisplayName("deve lançar exceção ao criar usuário com email duplicado")
    void deveLancarExcecaoCriarUsuarioEmailDuplicado() {
        // Dado
        Usuario usuario = criarUsuarioValido(null, false);
        String email = usuario.getPessoa().getEmail();

        doNothing().when(pessoaValidatorMock).validarCpf(anyString());
        doNothing().when(pessoaValidatorMock).validarCpfUnico(anyString(), isNull());
        doThrow(new RuntimeException("Já existe um cadastro com este e-mail."))
                .when(pessoaValidatorMock).validarEmailUnico(email, null);

        // Quando / Então
        assertThatThrownBy(() -> service.criar(usuario))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Já existe um cadastro com este e-mail.");

        verify(usuarioRepositoryMock, never()).save(any());
    }

    @Test
    @DisplayName("deve lançar exceção ao criar usuário conselheiro com CRM duplicado")
    void deveLancarExcecaoCriarUsuarioConselheiroCrmDuplicado() {
        // Dado
        Integer crm = 12345;
        Usuario usuario = criarUsuarioValido(null, true);
        usuario.setCrm(crm);

        doNothing().when(pessoaValidatorMock).validarCpf(anyString());
        doNothing().when(pessoaValidatorMock).validarCpfUnico(anyString(), isNull());
        doNothing().when(pessoaValidatorMock).validarEmailUnico(anyString(), isNull());
        doThrow(new RuntimeException("Já existe um conselheiro com o CRM " + crm))
                .when(pessoaValidatorMock).validarCrmUnico(crm, null);

        // Quando / Então
        assertThatThrownBy(() -> service.criar(usuario))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Já existe um conselheiro com o CRM " + crm);

        verify(usuarioRepositoryMock, never()).save(any());
    }

    @Test
    @DisplayName("deve lançar exceção ao criar usuário conselheiro sem CRM")
    void deveLancarExcecaoCriarUsuarioConselheiroSemCrm() {
        // Dado
        Usuario usuario = criarUsuarioValido(null, true);
        usuario.setCrm(null);

        doNothing().when(pessoaValidatorMock).validarCpf(anyString());
        doNothing().when(pessoaValidatorMock).validarCpfUnico(anyString(), isNull());
        doNothing().when(pessoaValidatorMock).validarEmailUnico(anyString(), isNull());

        // Quando / Então
        assertThatThrownBy(() -> service.criar(usuario))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("O número do CRM é obrigatório para médicos conselheiros.");

        verify(usuarioRepositoryMock, never()).save(any());
    }

    @Test
    @DisplayName("deve lançar exceção ao criar usuário sem senha")
    void deveLancarExcecaoCriarUsuarioSemSenha() {
        // Dado
        Usuario usuario = criarUsuarioValido(null, false);
        usuario.setSenha(null);

        // Quando / Então
        assertThatThrownBy(() -> service.criar(usuario))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("A senha é obrigatória para novos usuários.");

        verify(usuarioRepositoryMock, never()).save(any());
    }

    // ========== TESTES DE ATUALIZAÇÃO ==========

    @Test
    @DisplayName("deve atualizar usuário com sucesso")
    void deveAtualizarUsuarioComSucesso() {
        // Dado
        Integer id = 100;
        Usuario existente = criarUsuarioValido(id, false);
        existente.setSenha("hashAntiga");

        Usuario atualizado = criarUsuarioValido(id, false);
        atualizado.setSenha("novaSenha");

        when(usuarioRepositoryMock.findById(id)).thenReturn(Optional.of(existente));
        doNothing().when(pessoaValidatorMock).validarCpf(anyString());
        doNothing().when(pessoaValidatorMock).validarCpfUnico(anyString(), eq(id));
        doNothing().when(pessoaValidatorMock).validarEmailUnico(anyString(), eq(id));

        when(passwordEncoderMock.encode("novaSenha")).thenReturn("hashNova");

        when(usuarioRepositoryMock.save(any(Usuario.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Quando
        Usuario salvo = service.atualizar(atualizado);

        // Então
        assertThat(salvo).isNotNull();
        assertThat(salvo.getSenha()).isEqualTo("hashNova");

        verify(usuarioRepositoryMock).save(existente);
        verify(logJetonServiceMock).logUsuarioAtualizado(any(Usuario.class), eq(salvo));
    }

    @Test
    @DisplayName("deve manter a senha existente ao atualizar sem informar nova senha")
    void deveManterSenhaExistenteAoAtualizarSemNovaSenha() {
        // Dado
        Integer id = 100;
        Usuario existente = criarUsuarioValido(id, false);
        existente.setSenha("hashExistente");

        Usuario atualizado = criarUsuarioValido(id, false);
        atualizado.setSenha(null);

        when(usuarioRepositoryMock.findById(id)).thenReturn(Optional.of(existente));
        doNothing().when(pessoaValidatorMock).validarCpf(anyString());
        doNothing().when(pessoaValidatorMock).validarCpfUnico(anyString(), eq(id));
        doNothing().when(pessoaValidatorMock).validarEmailUnico(anyString(), eq(id));

        when(usuarioRepositoryMock.save(any(Usuario.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Quando
        Usuario salvo = service.atualizar(atualizado);

        // Então
        assertThat(salvo).isNotNull();
        assertThat(salvo.getSenha()).isEqualTo("hashExistente");
        verify(passwordEncoderMock, never()).encode(anyString());
    }

    @Test
    @DisplayName("deve lançar exceção ao atualizar usuário inexistente")
    void deveLancarExcecaoAtualizarUsuarioInexistente() {
        // Dado
        Integer id = 999;
        Usuario usuario = criarUsuarioValido(id, false);

        when(usuarioRepositoryMock.findById(id)).thenReturn(Optional.empty());

        // Quando / Então
        assertThatThrownBy(() -> service.atualizar(usuario))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Usuário não encontrado para atualização.");

        verify(usuarioRepositoryMock, never()).save(any());
    }

    @Test
    @DisplayName("deve lançar exceção ao atualizar usuário com ID nulo")
    void deveLancarExcecaoAtualizarUsuarioIdNulo() {
        // Dado
        Usuario usuario = criarUsuarioValido(null, false);

        // Quando / Então
        assertThatThrownBy(() -> service.atualizar(usuario))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("ID do usuário não informado para atualização.");

        verify(usuarioRepositoryMock, never()).findById(any());
        verify(usuarioRepositoryMock, never()).save(any());
    }

    // ========== TESTES DE ATUALIZAÇÃO DE PERFIL ==========

    @Test
    @DisplayName("deve atualizar perfil do usuário com sucesso")
    void deveAtualizarPerfilComSucesso() {
        // Dado
        Integer id = 50;
        Usuario existente = criarUsuarioValido(id, false);
        existente.getPessoa().setNome("Nome Antigo");
        existente.getPessoa().setEmail("antigo@email.com");
        existente.setSenha("hashAntiga");

        Usuario perfilAtualizado = new Usuario();
        perfilAtualizado.setIdUsuarioPessoa(id);
        Pessoa pessoaAtualizada = criarPessoa(id, "Nome Novo", "12345678900", "novo@email.com",
                Pessoa.TIPO_FUNCIONARIO);
        perfilAtualizado.setPessoa(pessoaAtualizada);
        perfilAtualizado.setSenha("novaSenha");

        when(usuarioRepositoryMock.findById(id)).thenReturn(Optional.of(existente));
        doNothing().when(pessoaValidatorMock).validarEmailUnico("novo@email.com", id);

        when(passwordEncoderMock.encode("novaSenha")).thenReturn("hashNova");

        when(usuarioRepositoryMock.save(any(Usuario.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Quando
        service.atualizarPerfil(perfilAtualizado);

        // Então
        assertThat(existente.getPessoa().getNome()).isEqualTo("Nome Novo");
        assertThat(existente.getPessoa().getEmail()).isEqualTo("novo@email.com");
        assertThat(existente.getSenha()).isEqualTo("hashNova");

        verify(pessoaValidatorMock).validarEmailUnico("novo@email.com", id);
        verify(usuarioRepositoryMock).save(existente);
        verify(logJetonServiceMock).logUsuarioAtualizado(any(Usuario.class), eq(existente));
    }

    @Test
    @DisplayName("deve manter senha ao atualizar perfil sem alterar senha")
    void deveManterSenhaAoAtualizarPerfilSemAlterarSenha() {
        // Dado
        Integer id = 50;
        Usuario existente = criarUsuarioValido(id, false);
        existente.getPessoa().setNome("Nome Antigo");
        existente.setSenha("hashExistente");

        Usuario perfilAtualizado = new Usuario();
        perfilAtualizado.setIdUsuarioPessoa(id);
        Pessoa pessoaAtualizada = criarPessoa(id, "Nome Novo", "12345678900", "novo@email.com",
                Pessoa.TIPO_FUNCIONARIO);
        perfilAtualizado.setPessoa(pessoaAtualizada);
        perfilAtualizado.setSenha(null);

        when(usuarioRepositoryMock.findById(id)).thenReturn(Optional.of(existente));
        doNothing().when(pessoaValidatorMock).validarEmailUnico("novo@email.com", id);

        when(usuarioRepositoryMock.save(any(Usuario.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Quando
        service.atualizarPerfil(perfilAtualizado);

        // Então
        assertThat(existente.getPessoa().getNome()).isEqualTo("Nome Novo");
        assertThat(existente.getSenha()).isEqualTo("hashExistente");
        verify(passwordEncoderMock, never()).encode(anyString());
    }

    @Test
    @DisplayName("deve lançar exceção ao atualizar perfil com email duplicado")
    void deveLancarExcecaoAtualizarPerfilEmailDuplicado() {
        // Dado
        Integer id = 50;
        Usuario existente = criarUsuarioValido(id, false);

        Usuario perfilAtualizado = new Usuario();
        perfilAtualizado.setIdUsuarioPessoa(id);
        Pessoa pessoaAtualizada = criarPessoa(id, "Nome", "123", "duplicado@email.com", Pessoa.TIPO_FUNCIONARIO);
        perfilAtualizado.setPessoa(pessoaAtualizada);

        when(usuarioRepositoryMock.findById(id)).thenReturn(Optional.of(existente));
        doThrow(new RuntimeException("Já existe um cadastro com este e-mail."))
                .when(pessoaValidatorMock).validarEmailUnico("duplicado@email.com", id);

        // Quando / Então
        assertThatThrownBy(() -> service.atualizarPerfil(perfilAtualizado))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Já existe um cadastro com este e-mail.");

        verify(usuarioRepositoryMock, never()).save(any());
    }

    // ========== TESTES DE EXCLUSÃO ==========

    @Test
    @DisplayName("deve excluir usuário com sucesso e remover registros relacionados")
    void deveExcluirUsuarioComSucesso() {
        // Dado
        Integer id = 200;
        Usuario usuario = criarUsuarioValido(id, false);

        when(usuarioRepositoryMock.findById(id)).thenReturn(Optional.of(usuario));

        // ✅ Injeção manual do EntityManager mockado
        ReflectionTestUtils.setField(service, "entityManager", entityManagerMock);
        doNothing().when(entityManagerMock).detach(any());

        // Quando
        service.excluir(id);

        // Então
        verify(usuarioRepositoryMock).deletePermissoesNative(id);
        verify(usuarioRepositoryMock).deleteConselheiroNative(id);
        verify(usuarioRepositoryMock).deleteUsuarioNative(id);
        verify(usuarioRepositoryMock).deletePessoaNative(id);
        verify(logJetonServiceMock).logUsuarioExcluido(usuario);
    }

    @Test
    @DisplayName("deve lançar exceção ao excluir usuário inexistente")
    void deveLancarExcecaoExcluirUsuarioInexistente() {
        // Dado
        Integer id = 999;

        when(usuarioRepositoryMock.findById(id)).thenReturn(Optional.empty());

        // Quando / Então
        assertThatThrownBy(() -> service.excluir(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Usuário não encontrado para exclusão.");

        verify(usuarioRepositoryMock, never()).deletePermissoesNative(anyInt());
        verify(usuarioRepositoryMock, never()).deleteConselheiroNative(anyInt());
        verify(usuarioRepositoryMock, never()).deleteUsuarioNative(anyInt());
        verify(usuarioRepositoryMock, never()).deletePessoaNative(anyInt());
    }

    // ========== TESTES DE CONSULTA ==========

    @Test
    @DisplayName("deve listar todos os usuários")
    void deveListarTodosUsuarios() {
        // Dado
        List<Usuario> lista = List.of(
                criarUsuarioValido(1, false),
                criarUsuarioValido(2, true));
        when(usuarioRepositoryMock.findAll()).thenReturn(lista);

        // Quando
        List<Usuario> resultado = service.listarTodos();

        // Então
        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getIdUsuarioPessoa()).isEqualTo(1);
        assertThat(resultado.get(1).getIdUsuarioPessoa()).isEqualTo(2);
        verify(usuarioRepositoryMock).findAll();
    }

    @Test
    @DisplayName("deve buscar usuário por ID com sucesso")
    void deveBuscarUsuarioPorIdComSucesso() {
        // Dado
        Integer id = 10;
        Usuario usuario = criarUsuarioValido(id, false);
        when(usuarioRepositoryMock.findById(id)).thenReturn(Optional.of(usuario));

        // Quando
        Optional<Usuario> resultado = service.buscarPorId(id);

        // Então
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getIdUsuarioPessoa()).isEqualTo(id);
        verify(usuarioRepositoryMock).findById(id);
    }

    @Test
    @DisplayName("deve retornar Optional vazio ao buscar usuário inexistente")
    void deveRetornarOptionalVazioBuscarInexistente() {
        // Dado
        Integer id = 999;
        when(usuarioRepositoryMock.findById(id)).thenReturn(Optional.empty());

        // Quando
        Optional<Usuario> resultado = service.buscarPorId(id);

        // Então
        assertThat(resultado).isEmpty();
        verify(usuarioRepositoryMock).findById(id);
    }

    @Test
    @DisplayName("deve listar usuários paginados com filtros")
    void deveListarUsuariosPaginadosComFiltros() {
        // Dado
        String termo = "João";
        String situacao = "A";
        int page = 0, size = 10;
        String sortField = "pessoa.nome", sortDir = "asc";

        Page<Usuario> paginaEsperada = new PageImpl<>(List.of(criarUsuarioValido(1, false)));

        when(usuarioRepositoryMock.findAllByFilters(eq(termo), anyString(), eq(situacao), any(Pageable.class)))
                .thenReturn(paginaEsperada);

        // Quando
        Page<Usuario> resultado = service.listarComPaginacaoEPesquisa(termo, situacao, page, size, sortField, sortDir);

        // Então
        assertThat(resultado).isNotNull();
        assertThat(resultado.getContent()).hasSize(1);
        verify(usuarioRepositoryMock).findAllByFilters(eq(termo), anyString(), eq(situacao), any(Pageable.class));
    }

    @Test
    @DisplayName("deve buscar por CPF quando termo contém números")
    void deveBuscarPorCpfQuandoTermoContemNumeros() {
        // Dado
        String termo = "12345678900";
        String situacao = "";
        int page = 0, size = 10;
        String sortField = "pessoa.nome", sortDir = "asc";

        Page<Usuario> paginaEsperada = new PageImpl<>(List.of(criarUsuarioValido(1, false)));

        when(usuarioRepositoryMock.findAllByFilters(eq(termo), eq("12345678900"), eq(situacao), any(Pageable.class)))
                .thenReturn(paginaEsperada);

        // Quando
        Page<Usuario> resultado = service.listarComPaginacaoEPesquisa(termo, situacao, page, size, sortField, sortDir);

        // Então
        assertThat(resultado).isNotNull();
        verify(usuarioRepositoryMock).findAllByFilters(eq(termo), eq("12345678900"), eq(situacao), any(Pageable.class));
    }
}