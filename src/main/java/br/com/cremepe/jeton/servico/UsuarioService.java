package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.anotacao.Auditar;
import br.com.cremepe.jeton.dominio.Conselheiro;
import br.com.cremepe.jeton.dominio.Pessoa;
import br.com.cremepe.jeton.dominio.Usuario;
import br.com.cremepe.jeton.dominio.ViewUserLogin;
import br.com.cremepe.jeton.repositorio.ConselheiroRepository;
import br.com.cremepe.jeton.repositorio.PessoaRepository;
import br.com.cremepe.jeton.repositorio.UsuarioRepository;
import br.com.cremepe.jeton.repositorio.ViewUserLoginRepository;
import br.com.cremepe.jeton.util.CpfValidador;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private AcessoService acessoService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    // =========================================================================
    // CRIAÇÃO
    // =========================================================================
    @Auditar(tabela = "usuario", acao = "CRIAR", descricao = "Criação de novo usuário", dadosParametros = "{ 'nome': #usuario.pessoa.nome, 'cpf': #usuario.pessoa.cpf, 'email': #usuario.pessoa.email, 'tipo': #usuario.iseConselheiro() ? 'Conselheiro' : 'Funcionário' }", dadosRetorno = "#result", auditarExcecao = true)
    @Transactional
    public Usuario criarUsuario(Usuario usuario, List<String> niveisAcessoSelecionados) {
        // Configura como novo
        usuario.setIdUsuarioPessoa(null);
        if (usuario.getPessoa() != null) {
            usuario.getPessoa().setIdPessoa(null);
        }

        // Validações e preparação (reaproveita lógica do salvar)
        prepararUsuario(usuario, true);

        // Salva
        usuarioRepository.save(usuario);
        Usuario salvo = usuarioRepository.findById(usuario.getIdUsuarioPessoa())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado para retorno."));

        // Sincroniza tabela conselheiro (se necessário)
        sincronizarConselheiro(salvo, usuario.getCrm(), usuario.getInSituacao());

        // Concede permissões
        if (niveisAcessoSelecionados != null) {
            for (String nivel : niveisAcessoSelecionados) {
                acessoService.concederPermissao(salvo.getIdUsuarioPessoa(), nivel);
            }
        }

        log.info("Usuário criado: ID={}, nome='{}'", salvo.getIdUsuarioPessoa(), salvo.getPessoa().getNome());
        return salvo;
    }

    // =========================================================================
    // ATUALIZAÇÃO
    // =========================================================================
    @Auditar(tabela = "usuario", acao = "ATUALIZAR", descricao = "Atualização de usuário existente", capturarEstadoAnterior = true, dadosParametros = "{ 'idUsuario': #usuario.idUsuarioPessoa, 'nome': #usuario.pessoa.nome, 'email': #usuario.pessoa.email, 'situacao': #usuario.inSituacao, 'tipo': #usuario.iseConselheiro() ? 'Conselheiro' : 'Funcionário' }", dadosRetorno = "#result", auditarExcecao = true)
    @Transactional
    public Usuario atualizarUsuario(Usuario usuario, List<String> niveisAcessoSelecionados) {
        if (usuario.getIdUsuarioPessoa() == null) {
            throw new RuntimeException("ID do usuário é obrigatório para atualização.");
        }

        // Carrega existente para validações
        Usuario existente = usuarioRepository.findById(usuario.getIdUsuarioPessoa())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado para atualização."));

        // Mantém senha original se não for alterada
        if (usuario.getSenha() == null || usuario.getSenha().trim().isEmpty()) {
            usuario.setSenha(existente.getSenha());
        } else {
            usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        }

        // Preserva CPF e tipo de pessoa
        // usuario.getPessoa().setCpf(existente.getPessoa().getCpf());
        // usuario.getPessoa().setInTipoPessoa(existente.getPessoa().getInTipoPessoa());
        usuario.getPessoa().setIdPessoa(usuario.getIdUsuarioPessoa());

        // Validações (menos rigorosas que criação)
        prepararUsuario(usuario, false);

        // Salva
        Usuario salvo = usuarioRepository.save(usuario);

        // Sincroniza tabela conselheiro
        sincronizarConselheiro(salvo, usuario.getCrm(), usuario.getInSituacao());

        // Gerencia permissões: remove as existentes e concede as novas (ou sincroniza)
        List<String> niveisAtuais = acessoService.listarPermissoesDoUsuario(salvo.getIdUsuarioPessoa());
        for (String nivelAtual : niveisAtuais) {
            if (niveisAcessoSelecionados == null || !niveisAcessoSelecionados.contains(nivelAtual)) {
                acessoService.revogarPermissao(salvo.getIdUsuarioPessoa(), nivelAtual);
            }
        }
        if (niveisAcessoSelecionados != null) {
            for (String nivel : niveisAcessoSelecionados) {
                if (!niveisAtuais.contains(nivel)) {
                    acessoService.concederPermissao(salvo.getIdUsuarioPessoa(), nivel);
                }
            }
        }

        log.info("Usuário atualizado: ID={}, nome='{}'", salvo.getIdUsuarioPessoa(), salvo.getPessoa().getNome());
        return salvo;
    }

    // =========================================================================
    // EXCLUSÃO
    // =========================================================================
    @Auditar(tabela = "usuario", acao = "EXCLUIR", descricao = "Exclusão de usuário", capturarEstadoAnterior = true, dadosParametros = "{ 'idUsuario': #id }", auditarExcecao = true)
    @Transactional
    public void excluirUsuario(Integer id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado para exclusão."));

        // Remove permissões
        usuarioRepository.deletarPermissoesNativo(id);
        // Remove conselheiro (se existir)
        usuarioRepository.deletarConselheiroNativo(id);
        // Remove usuário
        usuarioRepository.deletarUsuarioNativo(id);
        // Remove pessoa
        usuarioRepository.deletarPessoaNativa(id);

        log.info("Usuário excluído: ID={}, nome='{}'", id, usuario.getPessoa().getNome());
    }

    // =========================================================================
    // PERFIL (atualização restrita)
    // =========================================================================
    @Auditar(tabela = "usuario", acao = "ATUALIZAR_PERFIL", descricao = "Atualização do próprio perfil", capturarEstadoAnterior = true, dadosParametros = "{ 'idUsuario': #usuario.idUsuarioPessoa, 'nome': #usuario.pessoa.nome, 'email': #usuario.pessoa.email }", auditarExcecao = true)
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

        usuarioRepository.save(existente);
        log.info("Perfil do usuário ID={} atualizado", existente.getIdUsuarioPessoa());
    }

    // =========================================================================
    // LEITURA (sem auditoria)
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

    @Transactional(readOnly = true)
    public Optional<ViewUserLogin> autenticar(String cpf, String senha) {
        String cpfLimpo = cpf.replaceAll("[^0-9]", "");
        Optional<ViewUserLogin> userOpt = viewUserLoginRepository.findByCpf(cpfLimpo);
        if (userOpt.isEmpty())
            return Optional.empty();

        ViewUserLogin usuario = userOpt.get();
        String senhaStored = usuario.getSenha();

        if (senhaStored != null && senhaStored.matches("^[A-F0-9]{64}$")) {
            String hashInput = gerarSHA256(senha);
            if (hashInput.equalsIgnoreCase(senhaStored)) {
                String newBcryptHash = passwordEncoder.encode(senha);
                usuarioRepository.findByPessoaCpf(cpfLimpo).ifPresent(u -> {
                    u.setSenha(newBcryptHash);
                    usuarioRepository.save(u);
                });
                return userOpt;
            } else {
                return Optional.empty();
            }
        } else {
            if (passwordEncoder.matches(senha, senhaStored)) {
                return userOpt;
            } else {
                return Optional.empty();
            }
        }
    }

    // =========================================================================
    // MÉTODOS PRIVADOS AUXILIARES
    // =========================================================================
    private void prepararUsuario(Usuario usuario, boolean isNovo) {
        Pessoa pessoa = usuario.getPessoa();
        if (pessoa == null) {
            throw new RuntimeException("Os dados da pessoa são obrigatórios.");
        }
        String cpfLimpo = pessoa.getCpf() != null ? pessoa.getCpf().replaceAll("[^0-9]", "") : "";
        pessoa.setCpf(cpfLimpo);

        if (cpfLimpo.isEmpty() || !CpfValidador.isCpfValido(cpfLimpo)) {
            throw new RuntimeException("O número de CPF informado é inválido.");
        }

        validarCpfUnico(cpfLimpo, isNovo ? null : usuario.getIdUsuarioPessoa());
        validarEmailUnico(pessoa.getEmail(), isNovo ? null : usuario.getIdUsuarioPessoa());

        if (usuario.iseConselheiro()) {
            if (usuario.getCrm() == null) {
                throw new RuntimeException("O número do CRM é obrigatório para médicos conselheiros.");
            }
            validarCrmUnico(usuario.getCrm(), isNovo ? null : usuario.getIdUsuarioPessoa());
            pessoa.setInTipoPessoa(Pessoa.TIPO_CONSELHEIRO);
        } else {
            pessoa.setInTipoPessoa(Pessoa.TIPO_FUNCIONARIO);
            usuario.setCrm(null);
        }
    }

    private void sincronizarConselheiro(Usuario usuario, Integer crm, String situacao) {
        if (usuario.iseConselheiro()) {
            Conselheiro c = conselheiroRepository.findById(usuario.getIdUsuarioPessoa()).orElse(new Conselheiro());
            c.setIdPessoa(usuario.getIdUsuarioPessoa());
            c.setPessoa(usuario.getPessoa());
            c.setCrm(crm);
            c.setInSituacao(situacao);
            conselheiroRepository.save(c);
        } else {
            usuarioRepository.deletarConselheiroNativo(usuario.getIdUsuarioPessoa());
        }
    }

    private void validarCpfUnico(String cpf, Integer idAtual) {
        Optional<Pessoa> existente = pessoaRepository.findByCpf(cpf);
        if (existente.isPresent() && (idAtual == null || !idAtual.equals(existente.get().getIdPessoa()))) {
            throw new RuntimeException("Já existe um cadastro no sistema com este CPF.");
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