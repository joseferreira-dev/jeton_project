package br.com.cremepe.jeton.servico;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.cremepe.jeton.dominio.Usuario;
import br.com.cremepe.jeton.dominio.ViewUserLogin;
import br.com.cremepe.jeton.repositorio.UsuarioRepository;
import br.com.cremepe.jeton.repositorio.ViewUserLoginRepository;

@Service
public class UsuarioService {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ViewUserLoginRepository viewUserLoginRepository;

    @Transactional
    public Usuario salvar(Usuario usuario) {
        
        // 1. Limpa o CPF garantindo que tenha apenas números
        String cpfLimpo = "";
        if (usuario.getPessoa() != null && usuario.getPessoa().getCpf() != null) {
            cpfLimpo = usuario.getPessoa().getCpf().replaceAll("[^0-9]", "");
            usuario.getPessoa().setCpf(cpfLimpo);
        }

        // 2. VALIDAÇÃO DE CPF DUPLICADO
        if (!cpfLimpo.isEmpty()) {
            Optional<Usuario> usuarioExistente = usuarioRepository.findByPessoaCpf(cpfLimpo);
            
            if (usuarioExistente.isPresent()) {
                // Se for um NOVO cadastro ou edição de um usuário diferente do existente -> É duplicidade!
                if (usuario.getIdUsuarioPessoa() == null || 
                    !usuario.getIdUsuarioPessoa().equals(usuarioExistente.get().getIdUsuarioPessoa())) {
                    throw new RuntimeException("Já existe um usuário cadastrado com o CPF informado.");
                }
            }
        }

        // 3. Regras de Edição
        if (usuario.getIdUsuarioPessoa() != null && usuario.getIdUsuarioPessoa() > 0) {
            Usuario existente = usuarioRepository.findById(usuario.getIdUsuarioPessoa())
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

            if (usuario.getSenha() == null || usuario.getSenha().trim().isEmpty()) {
                usuario.setSenha(existente.getSenha());
            } else {
                usuario.setSenha(gerarSHA256(usuario.getSenha()));
            }

            usuario.getPessoa().setIdPessoa(usuario.getIdUsuarioPessoa());
            usuario.getPessoa().setInTipoPessoa(existente.getPessoa().getInTipoPessoa());
        } 
        // 4. Regras de Novo Cadastro
        else {
            usuario.setIdUsuarioPessoa(null);
            if (usuario.getPessoa() != null) {
                usuario.getPessoa().setIdPessoa(null);
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
    public Page<Usuario> listarComPaginacaoEPesquisa(String termo, String situacao, int page, int size, String sortField, String sortDir) {
        
        // Cria a regra de ordenação (ex: pessoa.nome ASC)
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();
        
        // Define a paginação com a ordenação
        Pageable pageable = (size == 0) ? Pageable.unpaged(sort) : PageRequest.of(page, size, sort);

        String cpfLimpo = "";
        if (termo != null && !termo.trim().isEmpty()) {
            cpfLimpo = termo.replaceAll("[^0-9]", "");
            if (cpfLimpo.isEmpty()) cpfLimpo = "###";
        }

        return usuarioRepository.pesquisarPaginado(termo, cpfLimpo, situacao, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorId(Integer id) { return usuarioRepository.findById(id); }

    @Transactional
    public void excluir(Integer id) {
        usuarioRepository.deletarConselheiroNativo(id);
        usuarioRepository.deletarUsuarioNativo(id);
        usuarioRepository.deletarPessoaNativa(id);
    }
}