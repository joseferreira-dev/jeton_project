package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.Usuario;
import br.com.cremepe.jeton.dominio.ViewUserLogin;
import br.com.cremepe.jeton.repositorio.UsuarioRepository;
import br.com.cremepe.jeton.repositorio.ViewUserLoginRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ViewUserLoginRepository viewUserLoginRepository;

    @Transactional(readOnly = true)
    public Optional<ViewUserLogin> autenticar(String cpf, String senha) {
        String cpfLimpo = cpf.replaceAll("[^0-9]", "");
        Optional<ViewUserLogin> user = viewUserLoginRepository.findByCpf(cpfLimpo);
        if (user.isPresent()) {
            if (user.get().getSenha().equals(gerarSHA256(senha))) return user;
        }
        return Optional.empty();
    }

    @Transactional
    public Usuario salvar(Usuario usuario) {
        if (usuario.getIdUsuarioPessoa() != null) {
            // EDIÇÃO
            Usuario existente = usuarioRepository.findById(usuario.getIdUsuarioPessoa())
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

            // 1. Tratamento da Senha
            if (usuario.getSenha() == null || usuario.getSenha().trim().isEmpty()) {
                usuario.setSenha(existente.getSenha()); // Mantém a senha atual
            } else {
                usuario.setSenha(gerarSHA256(usuario.getSenha())); // Nova senha
            }

            // 2. Sincronização obrigatória de IDs para @MapsId
            if (usuario.getPessoa() != null) {
                usuario.getPessoa().setIdPessoa(usuario.getIdUsuarioPessoa());
                usuario.getPessoa().setInTipoPessoa(existente.getPessoa().getInTipoPessoa());
            }
        } else {
            // NOVO CADASTRO
            usuario.setSenha(gerarSHA256(usuario.getSenha()));
            if (usuario.getPessoa() != null) {
                usuario.getPessoa().setInTipoPessoa("U");
            }
        }

        return usuarioRepository.save(usuario);
    }

    private String gerarSHA256(String senha) {
        try {
            MessageDigest algoritmo = MessageDigest.getInstance("SHA-256");
            byte[] messageDigest = algoritmo.digest(senha.getBytes(StandardCharsets.UTF_8));
            StringBuilder stringHexa = new StringBuilder();
            for (byte b : messageDigest) stringHexa.append(String.format("%02X", 0xFF & b));
            return stringHexa.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Erro na criptografia", e);
        }
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarTodos() { return usuarioRepository.findAll(); }

    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorId(Integer id) { return usuarioRepository.findById(id); }

    @Transactional
    public void excluir(Integer id) { usuarioRepository.deleteById(id); }
}