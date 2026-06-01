package br.com.cremepe.jeton.servico;

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
    private LogJetonService logJetonService;

    // =========================================================================
    // OPERAÇÕES DE ESCRITA
    // =========================================================================

    @Transactional
    public Usuario salvar(Usuario usuario, Integer idUsuarioLogado) {
        boolean isNovo = usuario.getIdUsuarioPessoa() == null;
        String nome = usuario.getPessoa().getNome();
        String tipoPessoa = usuario.iseConselheiro() ? "Conselheiro" : "Funcionário";
        String situacao = usuario.getInSituacao();
        Integer crm = usuario.getCrm();

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
        if (usuario.getIdUsuarioPessoa() != null && usuario.getIdUsuarioPessoa() > 0) {
            Usuario existente = usuarioRepository.findById(usuario.getIdUsuarioPessoa())
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));
            if (usuario.getSenha() == null || usuario.getSenha().trim().isEmpty()) {
                usuario.setSenha(existente.getSenha());
            } else {
                usuario.setSenha(gerarSHA256(usuario.getSenha()));
            }
            usuario.getPessoa().setIdPessoa(usuario.getIdUsuarioPessoa());
        } else {
            usuario.setIdUsuarioPessoa(null);
            if (usuario.getPessoa() != null)
                usuario.getPessoa().setIdPessoa(null);
            if (usuario.getSenha() == null || usuario.getSenha().trim().isEmpty()) {
                throw new RuntimeException("A senha é obrigatória para novos usuários.");
            }
            usuario.setSenha(gerarSHA256(usuario.getSenha()));
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
            Conselheiro c = conselheiroRepository.findById(usuarioSalvo.getIdUsuarioPessoa())
                    .orElse(new Conselheiro());
            c.setIdPessoa(usuarioSalvo.getIdUsuarioPessoa());
            c.setPessoa(usuarioSalvo.getPessoa());
            c.setCrm(usuario.getCrm());
            c.setInSituacao(usuarioSalvo.getInSituacao());
            conselheiroRepository.save(c);
            log.debug("Registro de conselheiro sincronizado para ID={}", usuarioSalvo.getIdUsuarioPessoa());
        } else {
            usuarioRepository.deletarConselheiroNativo(usuarioSalvo.getIdUsuarioPessoa());
        }

        // Log de auditoria
        String textoLog = String.format(
                "Usuário %s: ID=%d, Nome='%s', CPF=%s, Tipo='%s', Situação='%s'%s",
                isNovo ? "criado" : "atualizado",
                usuarioSalvo.getIdUsuarioPessoa(), nome, cpfLimpo, tipoPessoa, situacao,
                crm != null ? ", CRM=" + crm : "");
        logJetonService.registrarLog("usuario", idUsuarioLogado, textoLog);

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

    @Transactional
    public void atualizarPerfil(Usuario usuario, Integer idUsuarioLogado) {
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
            existente.setSenha(gerarSHA256(usuario.getSenha()));
        }

        // Salva (cascade salva a pessoa também)
        usuarioRepository.save(existente);

        // Log da alteração de perfil
        String textoLog = String.format(
                "Perfil atualizado: ID=%d, Nome='%s', Email='%s'",
                existente.getIdUsuarioPessoa(), pessoa.getNome(), pessoa.getEmail());
        logJetonService.registrarLog("usuario", idUsuarioLogado, textoLog);
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

    @Transactional(readOnly = true)
    public Optional<ViewUserLogin> autenticar(String cpf, String senha) {
        String cpfLimpo = cpf.replaceAll("[^0-9]", "");
        String senhaHash = gerarSHA256(senha);
        return viewUserLoginRepository.findByCpf(cpfLimpo)
                .filter(u -> u.getSenha().equals(senhaHash));
    }

    // =========================================================================
    // EXCLUSÃO
    // =========================================================================
    @Transactional
    public void excluir(Integer id, Integer idUsuarioLogado) {
        // Busca o usuário antes de excluir para obter dados para o log
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

        String textoLog = String.format(
                "Usuário excluído: ID=%d, Nome='%s', CPF=%s, Tipo='%s', Situação='%s'",
                id, nome, cpf, tipoPessoa, situacao);
        logJetonService.registrarLog("usuario", idUsuarioLogado, textoLog);
    }
}