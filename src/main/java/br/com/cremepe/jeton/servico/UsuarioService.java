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

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ViewUserLoginRepository viewUserLoginRepository;

    /**
     * Valida as credenciais comparando o CPF limpo e a senha em SHA-256 Maiúsculo.
     */
    @Transactional(readOnly = true)
    public Optional<ViewUserLogin> autenticar(String cpf, String senha) {
        
        // 1. Limpa o CPF (deixa só números) para garantir que bata com o banco
        String cpfLimpo = cpf.replaceAll("[^0-9]", "");
        
        Optional<ViewUserLogin> user = viewUserLoginRepository.findByCpf(cpfLimpo);
        
        if (user.isPresent()) {
            // 2. Transforma a senha digitada usando o algoritmo SHA-256 (igual ao legado)
            String senhaCriptografada = gerarSHA256(senha);
            
            // 3. Compara (O legado usa maiúsculas, por isso o ignoreCase ou o método exato)
            if (user.get().getSenha().equals(senhaCriptografada)) {
                return user;
            }
        }
        return Optional.empty();
    }

    /**
     * REPLICA EXATA do método presente no seu arquivo Criptografia.java
     */
    private String gerarSHA256(String senha) {
        try {
            MessageDigest algoritmo = MessageDigest.getInstance("SHA-256");
            byte[] messageDigest = algoritmo.digest(senha.getBytes(StandardCharsets.UTF_8));

            StringBuilder stringHexa = new StringBuilder();
            for (byte b : messageDigest) {
                // Formatação %02X garante letras MAIÚSCULAS como no sistema antigo
                stringHexa.append(String.format("%02X", 0xFF & b));
            }
            return stringHexa.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Erro ao processar criptografia SHA-256", e);
        }
    }

    // ==============================================================
    // MÉTODOS DE CRUD
    // ==============================================================

    @Transactional(readOnly = true)
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorId(Integer id) {
        return usuarioRepository.findById(id);
    }

    @Transactional
    public Usuario salvar(Usuario usuario) {
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public void excluir(Integer id) {
        usuarioRepository.deleteById(id);
    }
}