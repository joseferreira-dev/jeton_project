package br.com.cremepe.jeton.servico;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.cremepe.jeton.dominio.Conselheiro;
import br.com.cremepe.jeton.dominio.Pessoa;
import br.com.cremepe.jeton.dominio.Usuario;
import br.com.cremepe.jeton.dominio.ViewUserLogin;
import br.com.cremepe.jeton.repositorio.ConselheiroRepository;
import br.com.cremepe.jeton.repositorio.PessoaRepository;
import br.com.cremepe.jeton.repositorio.UsuarioRepository;
import br.com.cremepe.jeton.repositorio.ViewUserLoginRepository;

@Service
public class UsuarioService {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ViewUserLoginRepository viewUserLoginRepository;
    @Autowired private PessoaRepository pessoaRepository;
    @Autowired private ConselheiroRepository conselheiroRepository;

    @Transactional
    public Usuario salvar(Usuario usuario) {
        
        // 1. Limpeza e padronização do CPF
        String cpfLimpo = "";
        if (usuario.getPessoa() != null && usuario.getPessoa().getCpf() != null) {
            cpfLimpo = usuario.getPessoa().getCpf().replaceAll("[^0-9]", "");
            usuario.getPessoa().setCpf(cpfLimpo);
        }

        // 2. Validação de CPF Duplicado (em toda a tabela Pessoa)
        if (!cpfLimpo.isEmpty()) {
            Optional<Pessoa> pessoaExistente = pessoaRepository.findByCpf(cpfLimpo);
            if (pessoaExistente.isPresent()) {
                if (usuario.getIdUsuarioPessoa() == null || 
                    !usuario.getIdUsuarioPessoa().equals(pessoaExistente.get().getIdPessoa())) {
                    throw new RuntimeException("Já existe um cadastro no sistema com este CPF.");
                }
            }
        }

        // 3. Regras de Senha e Sincronização de IDs
        if (usuario.getIdUsuarioPessoa() != null && usuario.getIdUsuarioPessoa() > 0) {
            // EDIÇÃO
            Usuario existente = usuarioRepository.findById(usuario.getIdUsuarioPessoa())
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

            if (usuario.getSenha() == null || usuario.getSenha().trim().isEmpty()) {
                usuario.setSenha(existente.getSenha());
            } else {
                usuario.setSenha(gerarSHA256(usuario.getSenha()));
            }
            usuario.getPessoa().setIdPessoa(usuario.getIdUsuarioPessoa());
        } else {
            // NOVO CADASTRO
            usuario.setIdUsuarioPessoa(null);
            if (usuario.getPessoa() != null) {
                usuario.getPessoa().setIdPessoa(null);
            }
            if (usuario.getSenha() == null || usuario.getSenha().trim().isEmpty()) {
                throw new RuntimeException("A senha é obrigatória para novos usuários.");
            }
            usuario.setSenha(gerarSHA256(usuario.getSenha()));
        }

        // 4. Define o tipo de pessoa ('C' para Conselheiro ou 'F' para Funcionário)
        usuario.getPessoa().setInTipoPessoa(usuario.iseConselheiro() ? "C" : "F");

        // 5. Salva o Usuário (Cascade salva a Pessoa)
        Usuario usuarioSalvo = usuarioRepository.save(usuario);

        // 6. LÓGICA HÍBRIDA DE CONSELHEIRO
        if (usuario.iseConselheiro()) {
            if (usuario.getCrm() == null) {
                throw new RuntimeException("O número do CRM é obrigatório para médicos conselheiros.");
            }
            
            // Busca se já existe registro na tabela conselheiro ou cria um novo
            Conselheiro c = conselheiroRepository.findById(usuarioSalvo.getIdUsuarioPessoa())
                                                 .orElse(new Conselheiro());
            
            c.setIdPessoa(usuarioSalvo.getIdUsuarioPessoa());
            c.setPessoa(usuarioSalvo.getPessoa());
            c.setCrm(usuario.getCrm());
            c.setInSituacao(usuarioSalvo.getInSituacao());
            conselheiroRepository.save(c);
        } else {
            // Se o checkbox não estiver marcado, removemos da tabela conselheiro se existir (Downgrade)
            usuarioRepository.deletarConselheiroNativo(usuarioSalvo.getIdUsuarioPessoa());
        }

        return usuarioSalvo;
    }

    @Transactional(readOnly = true)
    public Page<Usuario> listarComPaginacaoEPesquisa(String termo, String situacao, int page, int size, String sortField, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();
        Pageable pageable = (size == 0) ? Pageable.unpaged(sort) : PageRequest.of(page, size, sort);

        String cpfPesquisa = "";
        if (termo != null && !termo.trim().isEmpty()) {
            cpfPesquisa = termo.replaceAll("[^0-9]", "");
            if (cpfPesquisa.isEmpty()) cpfPesquisa = "###";
        }

        return usuarioRepository.pesquisarPaginado(termo, cpfPesquisa, situacao, pageable);
    }

    @Transactional
    public void excluir(Integer id) {
        // Ordem segura de exclusão para evitar erros de Foreign Key
        usuarioRepository.deletarConselheiroNativo(id);
        usuarioRepository.deletarUsuarioNativo(id);
        usuarioRepository.deletarPessoaNativa(id);
    }

    @Transactional(readOnly = true)
    public Optional<ViewUserLogin> autenticar(String cpf, String senha) {
        String cpfLimpo = cpf.replaceAll("[^0-9]", "");
        return viewUserLoginRepository.findByCpf(cpfLimpo)
                .filter(u -> u.getSenha().equals(gerarSHA256(senha)));
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarTodos() { 
        return usuarioRepository.findAll(); 
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorId(Integer id) { 
        return usuarioRepository.findById(id); 
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