package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.domain.Conselheiro;
import br.com.cremepe.jeton.domain.Pessoa;
import br.com.cremepe.jeton.domain.Usuario;
import br.com.cremepe.jeton.repository.ConselheiroRepository;
import br.com.cremepe.jeton.repository.PessoaRepository;
import br.com.cremepe.jeton.repository.UsuarioRepository;
import br.com.cremepe.jeton.util.PessoaValidator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    @PersistenceContext
    private EntityManager entityManager;

    private final UsuarioRepository usuarioRepository;
    private final PessoaRepository pessoaRepository;
    private final ConselheiroRepository conselheiroRepository;
    private final PasswordEncoder passwordEncoder;
    private final LogJetonService logJetonService;
    private final PessoaValidator pessoaValidator;

    public UsuarioService(UsuarioRepository usuarioRepository,
            PessoaRepository pessoaRepository,
            PasswordEncoder passwordEncoder,
            ConselheiroRepository conselheiroRepository,
            LogJetonService logJetonService,
            PessoaValidator pessoaValidator) {
        this.usuarioRepository = usuarioRepository;
        this.pessoaRepository = pessoaRepository;
        this.conselheiroRepository = conselheiroRepository;
        this.passwordEncoder = passwordEncoder;
        this.logJetonService = logJetonService;
        this.pessoaValidator = pessoaValidator;
    }

    @Transactional
    public Usuario criar(Usuario usuario) {
        usuario.setIdUsuarioPessoa(null);
        Usuario salvo = salvar(usuario, true);
        logJetonService.logUsuarioCriado(salvo);
        return salvo;
    }

    @Transactional
    public Usuario atualizar(Usuario usuario) {
        if (usuario.getIdUsuarioPessoa() == null) {
            throw new RuntimeException("ID do usuário não informado para atualização.");
        }
        Usuario antigo = usuarioRepository.findById(usuario.getIdUsuarioPessoa())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado para atualização."));
        Usuario atualizado = salvar(usuario, false);
        logJetonService.logUsuarioAtualizado(antigo, atualizado);
        return atualizado;
    }

    private Usuario salvar(Usuario usuario, boolean isNovo) {
        Pessoa pessoa = usuario.getPessoa();
        if (pessoa == null) {
            throw new RuntimeException("Os dados da pessoa são obrigatórios.");
        }

        String cpfLimpo = pessoa.getCpf() != null ? pessoa.getCpf().replaceAll("[^0-9]", "") : "";
        pessoa.setCpf(cpfLimpo);
        pessoaValidator.validarCpf(cpfLimpo);
        pessoaValidator.validarCpfUnico(cpfLimpo, isNovo ? null : usuario.getIdUsuarioPessoa());
        pessoaValidator.validarEmailUnico(pessoa.getEmail(), isNovo ? null : usuario.getIdUsuarioPessoa());

        if (usuario.iseConselheiro()) {
            if (usuario.getCrm() == null) {
                throw new RuntimeException("O número do CRM é obrigatório para médicos conselheiros.");
            }
            pessoaValidator.validarCrmUnico(usuario.getCrm(), isNovo ? null : usuario.getIdUsuarioPessoa());
        }

        // Define tipo de pessoa
        pessoa.setInTipoPessoa(usuario.iseConselheiro() ? Pessoa.TIPO_CONSELHEIRO : Pessoa.TIPO_FUNCIONARIO);

        // Tratamento de senha
        if (!isNovo) {
            Usuario existente = usuarioRepository.findById(usuario.getIdUsuarioPessoa())
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));
            if (usuario.getSenha() == null || usuario.getSenha().trim().isEmpty()) {
                usuario.setSenha(existente.getSenha());
            } else {
                usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
            }
            usuario.getPessoa().setIdPessoa(usuario.getIdUsuarioPessoa());
        } else {
            usuario.setIdUsuarioPessoa(null);
            if (usuario.getPessoa() != null) {
                usuario.getPessoa().setIdPessoa(null);
            }
            if (usuario.getSenha() == null || usuario.getSenha().trim().isEmpty()) {
                throw new RuntimeException("A senha é obrigatória para novos usuários.");
            }
            usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        }

        // Salva usuário (cascade salva pessoa)
        Usuario usuarioSalvo = usuarioRepository.save(usuario);
        log.info("Usuário {}: ID={}, nome='{}', tipo={}, situação={}",
                isNovo ? "criado" : "atualizado",
                usuarioSalvo.getIdUsuarioPessoa(),
                pessoa.getNome(),
                usuario.iseConselheiro() ? "Conselheiro" : "Funcionário",
                usuarioSalvo.getInSituacao());

        // Gerencia tabela conselheiro (se necessário)
        if (usuario.iseConselheiro()) {
            usuarioRepository.flush();
            Integer idPessoa = usuarioSalvo.getIdUsuarioPessoa();

            Conselheiro conselheiro = conselheiroRepository.findById(idPessoa)
                    .orElse(new Conselheiro());

            conselheiro.setPessoa(usuarioSalvo.getPessoa());
            conselheiro.setCrm(usuario.getCrm());
            conselheiro.setInSituacao(usuarioSalvo.getInSituacao());

            conselheiroRepository.save(conselheiro);
            log.debug("Conselheiro salvo/atualizado para ID={}", idPessoa);
        } else {
            conselheiroRepository.findById(usuarioSalvo.getIdUsuarioPessoa())
                    .ifPresent(conselheiroRepository::delete);
        }

        return usuarioSalvo;
    }

    @Transactional
    public void atualizarPerfil(Usuario usuario) {
        Usuario existente = usuarioRepository.findById(usuario.getIdUsuarioPessoa())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado para atualização de perfil"));
        Usuario antigo = copiarUsuario(existente);

        // Valida e-mail único (se alterado)
        String novoEmail = usuario.getPessoa().getEmail();
        if (novoEmail != null && !novoEmail.equals(existente.getPessoa().getEmail())) {
            pessoaValidator.validarEmailUnico(novoEmail, existente.getIdUsuarioPessoa());
        }

        Pessoa pessoa = existente.getPessoa();
        pessoa.setNome(usuario.getPessoa().getNome());
        pessoa.setEmail(usuario.getPessoa().getEmail());

        if (usuario.getSenha() != null && !usuario.getSenha().trim().isEmpty()) {
            existente.setSenha(passwordEncoder.encode(usuario.getSenha()));
        }

        usuarioRepository.save(existente);
        logJetonService.logUsuarioAtualizado(antigo, existente);
    }

    @Transactional(readOnly = true)
    public Page<Usuario> listarComPaginacaoEPesquisa(String termo, String situacao, int page, int size,
            String sortField, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();
        Pageable pageable = (size == 0) ? Pageable.unpaged(sort) : PageRequest.of(page, size, sort);

        String cpfPesquisa = "";
        if (termo != null && !termo.trim().isEmpty()) {
            cpfPesquisa = termo.replaceAll("[^0-9]", "");
            if (cpfPesquisa.isEmpty())
                cpfPesquisa = "###";
        }
        return usuarioRepository.findAllByFilters(termo, cpfPesquisa, situacao, pageable);
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorId(Integer id) {
        return usuarioRepository.findById(id);
    }

    @Transactional
    public void excluir(Integer id) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        if (usuarioOpt.isEmpty()) {
            log.warn("Tentativa de excluir usuário inexistente ID={}", id);
            throw new RuntimeException("Usuário não encontrado para exclusão.");
        }

        Usuario usuario = usuarioOpt.get();
        Usuario copia = copiarUsuario(usuario);

        entityManager.detach(usuario);

        usuarioRepository.deletePermissoesNative(id);
        usuarioRepository.deleteConselheiroNative(id);
        usuarioRepository.deleteUsuarioNative(id);
        usuarioRepository.deletePessoaNative(id);

        logJetonService.logUsuarioExcluido(copia);
    }

    private Usuario copiarUsuario(Usuario original) {
        Usuario copia = new Usuario();
        copia.setIdUsuarioPessoa(original.getIdUsuarioPessoa());
        copia.setInSituacao(original.getInSituacao());
        copia.setSenha(original.getSenha());
        Pessoa p = new Pessoa();
        p.setIdPessoa(original.getPessoa().getIdPessoa());
        p.setNome(original.getPessoa().getNome());
        p.setEmail(original.getPessoa().getEmail());
        p.setCpf(original.getPessoa().getCpf());
        p.setInTipoPessoa(original.getPessoa().getInTipoPessoa());
        copia.setPessoa(p);
        return copia;
    }

    @Transactional
    public String gerarSenhaProvisoria(String cpfOuEmail) {
        Usuario usuario = buscarPorCpfOuEmail(cpfOuEmail)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (!usuario.isAtivo()) {
            throw new RuntimeException("Usuário inativo. Contate o administrador.");
        }

        String senhaProvisoria = gerarSenhaAleatoria();
        usuario.setSenha(passwordEncoder.encode(senhaProvisoria));
        usuarioRepository.save(usuario);

        return senhaProvisoria;
    }

    private String gerarSenhaAleatoria() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public Optional<Usuario> buscarPorCpfOuEmail(String cpfOuEmail) {
        String cpfLimpo = cpfOuEmail.replaceAll("\\D", "");
        Optional<Usuario> porCpf = usuarioRepository.findByPessoaCpf(cpfLimpo);
        if (porCpf.isPresent())
            return porCpf;

        Optional<Pessoa> porEmail = pessoaRepository.findByEmail(cpfOuEmail.trim());
        return porEmail.flatMap(p -> usuarioRepository.findById(p.getIdPessoa()));
    }
}