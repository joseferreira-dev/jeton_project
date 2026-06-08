package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.annotation.Auditar;
import br.com.cremepe.jeton.annotation.AuditoriaContext;
import br.com.cremepe.jeton.annotation.AuditoriaUser;
import br.com.cremepe.jeton.domain.ViewUserLogin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LoginService {

    private static final Logger log = LoggerFactory.getLogger(LoginService.class);

    @Autowired
    private UsuarioService usuarioService;

    @Auditar(tabela = "login", acao = "LOGIN", descricao = "Tentativa de login no sistema", dadosParametros = "{ 'cpf': #cpf }", capturarEstadoAnterior = false, auditarExcecao = true, incluirRetorno = true)
    public ViewUserLogin login(String cpf, String senha) {
        Optional<ViewUserLogin> usuarioOpt = usuarioService.autenticar(cpf, senha);
        if (usuarioOpt.isEmpty()) {
            throw new RuntimeException("CPF ou senha inválidos");
        }
        ViewUserLogin usuario = usuarioOpt.get();
        AuditoriaContext.setUsuario(new AuditoriaUser(usuario.getIdPessoa(), usuario.getNome()));

        log.info("Login bem-sucedido: usuário {} (ID {})", usuario.getNome(), usuario.getIdPessoa());
        return usuario;
    }

    @Auditar(tabela = "login", acao = "LOGOUT", descricao = "Logout do sistema", dadosParametros = "{ 'usuarioId': #idUsuario, 'usuarioNome': #nomeUsuario }", capturarEstadoAnterior = false, auditarExcecao = true, incluirRetorno = false)
    public void logout(Integer idUsuario, String nomeUsuario) {
        log.info("Logout do usuário: {} (ID {})", nomeUsuario, idUsuario);
    }
}