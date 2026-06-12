package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.annotation.Auditar;
import br.com.cremepe.jeton.domain.Conselheiro;
import br.com.cremepe.jeton.domain.Pessoa;
import br.com.cremepe.jeton.domain.Usuario;
import br.com.cremepe.jeton.repository.ConselheiroRepository;
import br.com.cremepe.jeton.repository.PessoaRepository;
import br.com.cremepe.jeton.repository.UsuarioRepository;
import br.com.cremepe.jeton.util.CpfValidador;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository usuarioRepository;
    private final PessoaRepository pessoaRepository;
    private final ConselheiroRepository conselheiroRepository;
    private final PasswordEncoder passwordEncoder;

    UsuarioService(UsuarioRepository usuarioRepository, PessoaRepository pessoaRepository,
            PasswordEncoder passwordEncoder, ConselheiroRepository conselheiroRepository) {
        this.usuarioRepository = usuarioRepository;
        this.pessoaRepository = pessoaRepository;
        this.conselheiroRepository = conselheiroRepository;
        this.passwordEncoder = passwordEncoder;
    }

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

    @Auditar(tabela = "usuario", acao = "EXCLUIR", descricao = "Exclusão física de usuário", dadosParametros = "{ 'id': #id }", capturarEstadoAnterior = true, auditarExcecao = true)
    @Transactional
    public void excluir(Integer id) {
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

        usuarioRepository.deletarPermissoesNativo(id);
        usuarioRepository.deletarConselheiroNativo(id);
        usuarioRepository.deletarUsuarioNativo(id);
        usuarioRepository.deletarPessoaNativa(id);

        log.info("Usuário excluído: ID={}, nome='{}', CPF={}, tipo={}, situação={}",
                id, nome, cpf, tipoPessoa, situacao);
    }
}