package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.Usuario;
import br.com.cremepe.jeton.dominio.ViewUserLogin;
import br.com.cremepe.jeton.repositorio.UsuarioRepository;
import br.com.cremepe.jeton.repositorio.ViewUserLoginRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Serviço central de gestão de utilizadores e autenticação.
 * Substitui as antigas FachadaUsuario e FachadaAutenticar.
 */
@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ViewUserLoginRepository viewUserLoginRepository;

    /**
     * Valida as credenciais de um utilizador.
     * Esta lógica substitui a funcionalidade da antiga FachadaAutenticar.
     */
    @Transactional(readOnly = true)
    public Optional<ViewUserLogin> autenticar(String cpf, String senha) {
        Optional<ViewUserLogin> user = viewUserLoginRepository.findByCpf(cpf);
        
        // No Passo 7, utilizaremos o Spring Security para uma comparação de Hash segura.
        // Por agora, mantemos a lógica de validação simples baseada no legado.
        if (user.isPresent() && user.get().getSenha().equals(senha)) {
            return user;
        }
        return Optional.empty();
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorCpf(String cpf) {
        return usuarioRepository.findByPessoaCpf(cpf);
    }

    /**
     * Salva ou atualiza um utilizador e a sua respetiva Pessoa.
     * @Transactional garante que a Pessoa e o Utilizador sejam gravados juntos ou nenhum deles.
     */
    @Transactional
    public Usuario salvar(Usuario usuario) {
        // Exemplo de reaproveitamento de validação legada:
        // if (!ValidarCPF.isCPF(usuario.getPessoa().getCpf())) { ... }
        
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public void alterarSituacao(Integer id, String novaSituacao) {
        usuarioRepository.findById(id).ifPresent(u -> {
            u.setInSituacao(novaSituacao);
            usuarioRepository.save(u);
        });
    }
}