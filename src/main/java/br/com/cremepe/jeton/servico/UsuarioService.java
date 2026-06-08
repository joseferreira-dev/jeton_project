package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.anotacao.Auditar;
import br.com.cremepe.jeton.dominio.Conselheiro;
import br.com.cremepe.jeton.dominio.Pessoa;
import br.com.cremepe.jeton.dominio.Usuario;
import br.com.cremepe.jeton.dominio.ViewUserLogin;
import br.com.cremepe.jeton.repository.ConselheiroRepository;
import br.com.cremepe.jeton.repository.PessoaRepository;
import br.com.cremepe.jeton.repository.UsuarioRepository;
import br.com.cremepe.jeton.repository.ViewUserLoginRepository;
import br.com.cremepe.jeton.util.CpfValidador;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private ViewUserLoginRepository viewUserLoginRepository;
    @Autowired
    private PessoaRepository pessoaRepository;
    @Autowired
    private ConselheiroRepository conselheiroRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    // =========================================================================
    // OPERAÇÕES DE ESCRITA
    // =========================================================================

    @Auditar(tabela = "usuario", acao = "CRIAR", descricao = "Criação de novo usuário", dadosParametros = "{ 'usuario': #usuario }", dadosRetorno = "#result", capturarEstadoAnterior = false, auditarExcecao = true)
    @Transactional
    public Usuario criar(Usuario usuario) {
        usuario.setIdUsuarioPessoa(null); // força criação
        return salvarUsuario(usuario, true);
    }

    @Auditar(tabela = "usuario", acao = "ATUALIZAR", descricao = "Atualização de usuário existente", dadosParametros = "{ 'usuario': #usuario }", dadosRetorno = "#result", capturarEstadoAnterior = true, auditarExcecao = true)
    @Transactional
    public Usuario atualizar(Usuario usuario) {
        if (usuario.getIdUsuarioPessoa() == null) {
            throw new RuntimeException("ID do usuário não informado para atualização.");
        }
        return salvarUsuario(usuario, false);
    }

    /**
     * Método privado que contém a lógica comum de persistência.
     * 
     * @param isNovo indica se é criação (true) ou atualização (false)
     */
    private Usuario salvarUsuario(Usuario usuario, boolean isNovo) {
        String nome = usuario.getPessoa().getNome();
        String tipoPessoa = usuario.iseConselheiro() ? "Conselheiro" : "Funcionário";
        String situacao = usuario.getInSituacao();

        // 1. Prepara CPF
        Pessoa pessoa = usuario.getPessoa();
        if (pessoa == null) {
            throw new RuntimeException("Os dados da pessoa são obrigatórios.");
        }
        String cpfLimpo = pessoa.getCpf() != null ? pessoa.getCpf().replaceAll("[^0-9]", "") : "";
        pessoa.setCpf(cpfLimpo);

        // 2. Valida CPF
        if (cpfLimpo.isEmpty() || !CpfValidador.isCpfValido(cpfLimpo)) {
            throw new RuntimeException("O número de CPF informado é inválido. Verifique os dígitos.");
        }

        // 3. Verifica duplicidade de CPF
        validarCpfUnico(cpfLimpo, usuario.getIdUsuarioPessoa());

        // 4. Verifica duplicidade de e-mail
        validarEmailUnico(pessoa.getEmail(), isNovo ? null : usuario.getIdUsuarioPessoa());

        // 5. Valida CRM se for conselheiro
        if (usuario.iseConselheiro()) {
            if (usuario.getCrm() == null) {
                throw new RuntimeException("O número do CRM é obrigatório para médicos conselheiros.");
            }
            validarCrmUnico(usuario.getCrm(), usuario.getIdUsuarioPessoa());
        }

        // 6. Tratamento de senha
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
            if (usuario.getPessoa() != null)
                usuario.getPessoa().setIdPessoa(null);
            if (usuario.getSenha() == null || usuario.getSenha().trim().isEmpty()) {
                throw new RuntimeException("A senha é obrigatória para novos usuários.");
            }
            usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        }

        // 7. Define tipo de pessoa
        pessoa.setInTipoPessoa(usuario.iseConselheiro() ? Pessoa.TIPO_CONSELHEIRO : Pessoa.TIPO_FUNCIONARIO);

        // 8. Salva usuário (cascade salva pessoa)
        Usuario usuarioSalvo = usuarioRepository.save(usuario);
        log.info("Usuário {}: ID={}, nome='{}', tipo={}, situação={}",
                isNovo ? "criado" : "atualizado",
                usuarioSalvo.getIdUsuarioPessoa(), nome, tipoPessoa, situacao);

        // 9. Gerencia tabela conselheiro (se necessário)
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

    private void validarCpfUnico(String cpf, Integer idAtual) {
        Optional<Pessoa> existente = pessoaRepository.findByCpf(cpf);
        if (existente.isPresent()) {
            if (idAtual == null || !idAtual.equals(existente.get().getIdPessoa())) {
                throw new RuntimeException("Já existe um cadastro no sistema com este CPF.");
            }
        }
    }

    private void validarCrmUnico(Integer crm, Integer idAtual) {
        Optional<Conselheiro> existente = conselheiroRepository.findByCrm(crm);
        if (existente.isPresent() && (idAtual == null || !idAtual.equals(existente.get().getIdPessoa()))) {
            throw new RuntimeException("Já existe um médico conselheiro cadastrado com o CRM " + crm + ".");
        }
    }

    private void validarEmailUnico(String email, Integer idAtual) {
        if (email == null || email.trim().isEmpty())
            return;

        if (pessoaRepository.existsByEmailAndIdPessoaNot(email, idAtual != null ? idAtual : 0)) {
            throw new RuntimeException("Já existe um cadastro no sistema com o e-mail '" + email + "'.");
        }
    }

    @Auditar(tabela = "usuario", acao = "ATUALIZAR_PERFIL", descricao = "Atualização do próprio perfil pelo usuário logado", dadosParametros = "{ 'usuario': #usuario }", dadosRetorno = "#result", capturarEstadoAnterior = true, auditarExcecao = true)
    @Transactional
    public void atualizarPerfil(Usuario usuario) {
        Usuario existente = usuarioRepository.findById(usuario.getIdUsuarioPessoa())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado para atualização de perfil"));

        // Valida se o e-mail já não está em uso por outro usuário
        String novoEmail = usuario.getPessoa().getEmail();
        if (novoEmail != null && !novoEmail.equals(existente.getPessoa().getEmail())) {
            validarEmailUnico(novoEmail, existente.getIdUsuarioPessoa());
        }

        // Atualiza apenas os campos permitidos
        Pessoa pessoa = existente.getPessoa();
        pessoa.setNome(usuario.getPessoa().getNome());
        pessoa.setEmail(usuario.getPessoa().getEmail());
        // CPF não é alterado

        // Se uma nova senha foi fornecida, atualiza
        if (usuario.getSenha() != null && !usuario.getSenha().trim().isEmpty()) {
            existente.setSenha(passwordEncoder.encode(usuario.getSenha()));
        }

        // Salva (cascade salva a pessoa também)
        usuarioRepository.save(existente);

        // Log de auditoria é gerado automaticamente pelo aspecto
        log.info("Perfil do usuário ID={} ({}) atualizado", existente.getIdUsuarioPessoa(),
                existente.getPessoa().getNome());
    }

    // =========================================================================
    // OPERAÇÕES DE LEITURA
    // =========================================================================

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
        return usuarioRepository.pesquisarPaginado(termo, cpfPesquisa, situacao, pageable);
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
    public Optional<ViewUserLogin> autenticar(String cpf, String senha) {
        String cpfLimpo = cpf.replaceAll("[^0-9]", "");
        Optional<ViewUserLogin> userOpt = viewUserLoginRepository.findByCpf(cpfLimpo);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }
        ViewUserLogin usuario = userOpt.get();
        String senhaStored = usuario.getSenha();

        // Verifica se a senha armazenada é SHA-256 (64 caracteres hexadecimais)
        if (senhaStored != null && senhaStored.matches("^[A-F0-9]{64}$")) {
            // Senha antiga em SHA-256
            String hashInput = gerarSHA256(senha);
            if (hashInput.equalsIgnoreCase(senhaStored)) {
                // Migra para BCrypt
                String newBcryptHash = passwordEncoder.encode(senha);
                // Atualiza no banco
                usuarioRepository.findByPessoaCpf(cpfLimpo).ifPresent(u -> {
                    u.setSenha(newBcryptHash);
                    usuarioRepository.save(u);
                });
                // Retorna o usuário (já que a autenticação foi bem‑sucedida)
                return userOpt;
            } else {
                return Optional.empty();
            }
        } else {
            // Senha já em BCrypt (ou nula)
            if (passwordEncoder.matches(senha, senhaStored)) {
                return userOpt;
            } else {
                return Optional.empty();
            }
        }
    }

    // =========================================================================
    // EXCLUSÃO
    // =========================================================================

    @Auditar(tabela = "usuario", acao = "EXCLUIR", descricao = "Exclusão física de usuário", dadosParametros = "{ 'id': #id }", capturarEstadoAnterior = true, auditarExcecao = true)
    @Transactional
    public void excluir(Integer id) {
        // Busca o usuário antes de excluir para obter dados para o log (o aspecto
        // também
        // captura estado anterior, mas mantemos para compatibilidade com logs antigos)
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);
        if (usuarioOpt.isEmpty()) {
            log.warn("Tentativa de excluir usuário inexistente ID={}", id);
            throw new RuntimeException("Usuário não encontrado para exclusão.");
        }
        Usuario usuario = usuarioOpt.get();
        String nome = usuario.getPessoa().getNome();
        String cpf = usuario.getPessoa().getCpf();
        String tipoPessoa = usuario.getPessoa().isConselheiro() ? "Conselheiro" : "Funcionário";
        String situacao = usuario.getInSituacao();

        // 1. Remove as permissões (usuario_acesso) primeiro
        usuarioRepository.deletarPermissoesNativo(id);

        // 2. Remove o conselheiro (se existir)
        usuarioRepository.deletarConselheiroNativo(id);

        // 3. Remove o usuário
        usuarioRepository.deletarUsuarioNativo(id);

        // 4. Remove a pessoa
        usuarioRepository.deletarPessoaNativa(id);

        log.info("Usuário excluído: ID={}, nome='{}', CPF={}, tipo={}, situação={}",
                id, nome, cpf, tipoPessoa, situacao);
    }

    // =========================================================================
    // MÉTODOS AUXILIARES PRIVADOS
    // =========================================================================

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