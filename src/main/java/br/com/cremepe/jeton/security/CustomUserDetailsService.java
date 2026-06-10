package br.com.cremepe.jeton.security;

import br.com.cremepe.jeton.domain.Conselheiro;
import br.com.cremepe.jeton.domain.Pessoa;
import br.com.cremepe.jeton.domain.Usuario;
import br.com.cremepe.jeton.domain.ViewUserLogin;
import br.com.cremepe.jeton.repository.ConselheiroRepository;
import br.com.cremepe.jeton.repository.PessoaRepository;
import br.com.cremepe.jeton.repository.UsuarioRepository;
import br.com.cremepe.jeton.repository.ViewUserLoginRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(CustomUserDetailsService.class);

    @Autowired
    private ViewUserLoginRepository viewUserLoginRepository;

    @Autowired
    private PessoaRepository pessoaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ConselheiroRepository conselheiroRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String cpf) throws UsernameNotFoundException {
        String cpfLimpo = cpf.replaceAll("[^0-9]", "");

        // 1. Tenta buscar na view (funciona para usuários com pelo menos uma permissão)
        Optional<ViewUserLogin> opt = viewUserLoginRepository.findByCpf(cpfLimpo);
        if (opt.isPresent()) {
            return new CustomUserDetails(opt.get());
        }

        // 2. Se não encontrado, verifica se é um conselheiro (que pode não ter
        // permissões)
        Optional<Pessoa> pessoaOpt = pessoaRepository.findByCpf(cpfLimpo);
        if (pessoaOpt.isEmpty()) {
            throw new UsernameNotFoundException("Usuário não encontrado com CPF: " + cpf);
        }

        Pessoa pessoa = pessoaOpt.get();

        // Verifica se é conselheiro
        Optional<Conselheiro> conselheiroOpt = conselheiroRepository.findById(pessoa.getIdPessoa());
        if (conselheiroOpt.isEmpty()) {
            throw new UsernameNotFoundException(
                    "CPF não corresponde a um conselheiro ou funcionário sem permissões: " + cpf);
        }

        // Busca o usuário (tabela usuario)
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(pessoa.getIdPessoa());
        if (usuarioOpt.isEmpty()) {
            throw new UsernameNotFoundException("Usuário não possui cadastro na tabela usuario: " + cpf);
        }

        Usuario usuario = usuarioOpt.get();
        if (!"A".equals(usuario.getInSituacao())) {
            throw new UsernameNotFoundException("Usuário inativo: " + cpf);
        }

        // Constrói manualmente um ViewUserLogin (apenas com os campos mínimos)
        ViewUserLogin viewUser = new ViewUserLogin();
        viewUser.setIdPessoa(pessoa.getIdPessoa());
        viewUser.setNome(pessoa.getNome());
        viewUser.setCpf(pessoa.getCpf());
        viewUser.setEmail(pessoa.getEmail());
        viewUser.setInTipoPessoa(pessoa.getInTipoPessoa());
        viewUser.setSenha(usuario.getSenha());
        viewUser.setPermissoes(""); // Sem permissões

        log.info("Conselheiro sem permissões carregado via fallback: CPF={}, ID={}", cpfLimpo, pessoa.getIdPessoa());
        return new CustomUserDetails(viewUser);
    }

    /**
     * Método auxiliar para migrar senha SHA-256 para BCrypt.
     * Chamado após a autenticação bem‑sucedida.
     * 
     * @param cpf        CPF do usuário
     * @param senhaPlain Senha em texto plano fornecida no login
     * @return true se a migração foi realizada, false caso contrário
     */
    @Transactional
    public boolean migratePasswordIfNeeded(String cpf, String senhaPlain) {
        String cpfLimpo = cpf.replaceAll("[^0-9]", "");
        Optional<Usuario> usuarioOpt = usuarioRepository.findByPessoaCpf(cpfLimpo);
        if (usuarioOpt.isEmpty())
            return false;

        Usuario usuario = usuarioOpt.get();
        String senhaStored = usuario.getSenha();
        if (senhaStored == null)
            return false;

        // Verifica se é SHA-256 (64 caracteres hexa)
        if (senhaStored.matches("^[A-F0-9]{64}$")) {
            String hashInput = gerarSHA256(senhaPlain);
            if (hashInput.equalsIgnoreCase(senhaStored)) {
                String newBcryptHash = passwordEncoder.encode(senhaPlain);
                usuario.setSenha(newBcryptHash);
                usuarioRepository.save(usuario);
                log.info("Senha do usuário {} migrada de SHA-256 para BCrypt", cpfLimpo);
                return true;
            }
        }
        return false;
    }

    private String gerarSHA256(String senha) {
        try {
            MessageDigest algoritmo = MessageDigest.getInstance("SHA-256");
            byte[] messageDigest = algoritmo.digest(senha.getBytes(StandardCharsets.UTF_8));
            StringBuilder stringHexa = new StringBuilder();
            for (byte b : messageDigest) {
                stringHexa.append(String.format("%02X", 0xFF & b));
            }
            return stringHexa.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Erro ao processar segurança da senha", e);
        }
    }
}