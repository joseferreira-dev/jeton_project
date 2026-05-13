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

@Transactional
    public Usuario salvar(Usuario usuario) {
        // 1. Limpa o CPF
        if (usuario.getPessoa() != null && usuario.getPessoa().getCpf() != null) {
            usuario.getPessoa().setCpf(usuario.getPessoa().getCpf().replaceAll("[^0-9]", ""));
        }

        if (usuario.getIdUsuarioPessoa() != null && usuario.getIdUsuarioPessoa() > 0) {
            // EDIÇÃO
            Usuario existente = usuarioRepository.findById(usuario.getIdUsuarioPessoa())
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

            if (usuario.getSenha() == null || usuario.getSenha().trim().isEmpty()) {
                usuario.setSenha(existente.getSenha());
            } else {
                usuario.setSenha(gerarSHA256(usuario.getSenha()));
            }

            usuario.getPessoa().setIdPessoa(usuario.getIdUsuarioPessoa());
            
            // Mantém o tipo que já estava no banco para não quebrar a regra
            usuario.getPessoa().setInTipoPessoa(existente.getPessoa().getInTipoPessoa());
        } 
        else {
            // NOVO CADASTRO
            usuario.setIdUsuarioPessoa(null);
            if (usuario.getPessoa() != null) {
                usuario.getPessoa().setIdPessoa(null);
                
                // CORREÇÃO AQUI: Mudamos de 'U' para 'F' para aceitar a regra do banco [F/C]
                usuario.getPessoa().setInTipoPessoa("F"); 
            }

            if (usuario.getSenha() == null || usuario.getSenha().trim().isEmpty()) {
                throw new RuntimeException("A senha é obrigatória para novos usuários.");
            }
            usuario.setSenha(gerarSHA256(usuario.getSenha()));
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
    public Optional<ViewUserLogin> autenticar(String cpf, String senha) {
        String cpfLimpo = cpf.replaceAll("[^0-9]", "");
        return viewUserLoginRepository.findByCpf(cpfLimpo)
                .filter(u -> u.getSenha().equals(gerarSHA256(senha)));
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarTodos() { return usuarioRepository.findAll(); }

    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorId(Integer id) { return usuarioRepository.findById(id); }

    @Transactional
    public void excluir(Integer id) { usuarioRepository.deleteById(id); }
}