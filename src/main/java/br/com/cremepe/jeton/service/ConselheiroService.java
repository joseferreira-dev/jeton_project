package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.domain.Conselheiro;
import br.com.cremepe.jeton.domain.GestaoConselheiro;
import br.com.cremepe.jeton.domain.Pessoa;
import br.com.cremepe.jeton.domain.Usuario;
import br.com.cremepe.jeton.repository.ConselheiroRepository;
import br.com.cremepe.jeton.repository.GestaoConselheiroRepository;
import br.com.cremepe.jeton.repository.PessoaRepository;
import br.com.cremepe.jeton.repository.UsuarioRepository;
import br.com.cremepe.jeton.util.PessoaValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ConselheiroService {

    private static final Logger log = LoggerFactory.getLogger(ConselheiroService.class);

    private final ConselheiroRepository conselheiroRepository;
    private final GestaoConselheiroRepository gestaoConselheiroRepository;
    private final PessoaRepository pessoaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final PermissaoService permissaoService;
    private final UsuarioService usuarioService;
    private final LogJetonService logJetonService;
    private final PessoaValidator pessoaValidator;

    public ConselheiroService(ConselheiroRepository conselheiroRepository,
            GestaoConselheiroRepository gestaoConselheiroRepository,
            PessoaRepository pessoaRepository,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            PermissaoService permissaoService,
            UsuarioService usuarioService,
            LogJetonService logJetonService,
            PessoaValidator pessoaValidator) {
        this.conselheiroRepository = conselheiroRepository;
        this.gestaoConselheiroRepository = gestaoConselheiroRepository;
        this.pessoaRepository = pessoaRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.permissaoService = permissaoService;
        this.usuarioService = usuarioService;
        this.logJetonService = logJetonService;
        this.pessoaValidator = pessoaValidator;
    }

    @Transactional
    public Conselheiro criar(Conselheiro conselheiro) {
        conselheiro.setIdPessoa(null);
        Conselheiro salvo = salvar(conselheiro, true);
        logJetonService.logConselheiroCriado(salvo);
        return salvo;
    }

    @Transactional
    public Conselheiro atualizar(Conselheiro conselheiro) {
        if (conselheiro.getIdPessoa() == null) {
            throw new RuntimeException("ID do conselheiro não informado para atualização.");
        }
        Conselheiro antigo = conselheiroRepository.findById(conselheiro.getIdPessoa())
                .orElseThrow(() -> new RuntimeException("Conselheiro não encontrado para edição."));

        Conselheiro copiaAntigo = copiarConselheiro(antigo);
        Conselheiro atualizado = salvar(conselheiro, false);
        logJetonService.logConselheiroAtualizado(copiaAntigo, atualizado);
        return atualizado;
    }

    private Conselheiro salvar(Conselheiro conselheiro, boolean isNovo) {
        // Validações delegadas ao PessoaValidator
        Pessoa pessoa = conselheiro.getPessoa();
        if (pessoa == null) {
            throw new RuntimeException("Os dados da pessoa são obrigatórios.");
        }

        // Normaliza e valida CPF
        String cpfLimpo = pessoa.getCpf() != null ? pessoa.getCpf().replaceAll("[^0-9]", "") : "";
        pessoa.setCpf(cpfLimpo);
        pessoaValidator.validarCpf(cpfLimpo); // valida dígitos
        pessoaValidator.validarCpfUnico(cpfLimpo, isNovo ? null : conselheiro.getIdPessoa());

        // Valida e-mail único
        String email = pessoa.getEmail();
        if (email != null && !email.trim().isEmpty()) {
            pessoaValidator.validarEmailUnico(email.trim(), isNovo ? null : conselheiro.getIdPessoa());
        }

        // Define tipo de pessoa
        pessoa.setInTipoPessoa(Pessoa.TIPO_CONSELHEIRO);

        // Valida CRM
        Integer crm = conselheiro.getCrm();
        if (crm != null) {
            pessoaValidator.validarCrmUnico(crm, isNovo ? null : conselheiro.getIdPessoa());
        }

        // Se for atualização, carrega a entidade existente
        Conselheiro conselheiroFinal;
        if (isNovo) {
            conselheiroFinal = conselheiro;
            conselheiroFinal.setIdPessoa(null); // será gerado pelo JPA
            if (conselheiroFinal.getInSituacao() == null || conselheiroFinal.getInSituacao().isEmpty()) {
                conselheiroFinal.setInSituacao(Conselheiro.SITUACAO_ATIVO);
            }
            conselheiroFinal.setPessoa(pessoa);
        } else {
            Conselheiro existente = conselheiroRepository.findById(conselheiro.getIdPessoa())
                    .orElseThrow(() -> new RuntimeException("Conselheiro não encontrado para edição."));
            Pessoa pessoaExistente = existente.getPessoa();
            pessoaExistente.setNome(pessoa.getNome());
            pessoaExistente.setEmail(pessoa.getEmail());
            pessoaExistente.setCpf(cpfLimpo);
            existente.setCrm(crm);
            existente.setInSituacao(conselheiro.getInSituacao());
            conselheiroFinal = existente;
        }

        Conselheiro conselheiroSalvo = conselheiroRepository.save(conselheiroFinal);
        log.info("Conselheiro {}: ID={}, nome='{}', CRM={}, situação={}",
                isNovo ? "criado" : "atualizado",
                conselheiroSalvo.getIdPessoa(), pessoa.getNome(), crm != null ? crm : 0,
                conselheiroSalvo.getInSituacao());

        sincronizarUsuario(conselheiroSalvo, conselheiro.getSenhaAcesso(), isNovo);
        return conselheiroSalvo;
    }

    private void sincronizarUsuario(Conselheiro conselheiro, String senhaInformada, boolean isNovo) {
        Usuario usuario = usuarioRepository.findById(conselheiro.getIdPessoa()).orElse(new Usuario());
        usuario.setPessoa(conselheiro.getPessoa());
        usuario.setInSituacao(conselheiro.getInSituacao());

        if (senhaInformada != null && !senhaInformada.trim().isEmpty()) {
            usuario.setSenha(passwordEncoder.encode(senhaInformada));
        } else if (usuario.getSenha() == null && isNovo) {
            throw new RuntimeException("A senha é obrigatória para criar o acesso no sistema.");
        }

        Usuario usuarioSalvo = usuarioRepository.save(usuario);

        if (isNovo) {
            permissaoService.concederPermissao(usuarioSalvo.getIdUsuarioPessoa(), "C");
            log.debug("Permissão 'C' concedida automaticamente para conselheiro ID {}",
                    usuarioSalvo.getIdUsuarioPessoa());
        } else {
            if (!permissaoService.hasPermissao(usuarioSalvo.getIdUsuarioPessoa(), "C")) {
                permissaoService.concederPermissao(usuarioSalvo.getIdUsuarioPessoa(), "C");
                log.debug("Permissão 'C' concedida a conselheiro existente ID {}", usuarioSalvo.getIdUsuarioPessoa());
            }
        }
    }

    @Transactional
    public void excluir(Integer id) {
        Optional<Conselheiro> conselheiroOpt = conselheiroRepository.findById(id);
        if (conselheiroOpt.isEmpty()) {
            log.warn("Tentativa de excluir conselheiro inexistente ID={}", id);
            throw new RuntimeException("Conselheiro não encontrado para exclusão.");
        }
        Conselheiro conselheiro = conselheiroOpt.get();
        Conselheiro copia = copiarConselheiro(conselheiro);

        // Delega a remoção completa para o UsuarioService
        usuarioService.excluir(id);

        logJetonService.logConselheiroExcluido(copia);
        log.info("Conselheiro excluído: ID={}, nome='{}', CPF={}, CRM={}, situação={}",
                id, copia.getPessoa().getNome(), copia.getPessoa().getCpf(), copia.getCrm(), copia.getInSituacao());
    }

    @Transactional(readOnly = true)
    public Page<Conselheiro> listarComPaginacaoEPesquisa(String termo, String situacao, int page, int size,
            String sortField, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();
        Pageable pageable = (size == 0) ? Pageable.unpaged(sort) : PageRequest.of(page, size, sort);
        return conselheiroRepository.findAllByFilters(termo, situacao, pageable);
    }

    @Transactional(readOnly = true)
    public List<Conselheiro> listarTodos() {
        return conselheiroRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Conselheiro> buscarPorId(Integer id) {
        return conselheiroRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Conselheiro> listarPorGestao(Integer gestaoId) {
        return gestaoConselheiroRepository.findByIdIdGestao(gestaoId).stream()
                .map(GestaoConselheiro::getConselheiro)
                .sorted(Comparator.comparing(c -> c.getPessoa().getNome()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Conselheiro> listarNaoVinculados(Integer idGestao, String termo) {
        List<Integer> idsVinculados = gestaoConselheiroRepository.findByGestaoIdGestao(idGestao)
                .stream()
                .map(gc -> gc.getId().getIdPessoa())
                .collect(Collectors.toList());

        Pageable pageable = PageRequest.of(0, 20, Sort.by("pessoa.nome"));
        Page<Conselheiro> page = conselheiroRepository.findNaoVinculados(
                termo != null ? termo : "",
                idsVinculados.isEmpty() ? List.of(0) : idsVinculados,
                pageable);
        return page.getContent();
    }

    private Conselheiro copiarConselheiro(Conselheiro original) {
        Conselheiro copia = new Conselheiro();
        copia.setIdPessoa(original.getIdPessoa());
        copia.setCrm(original.getCrm());
        copia.setInSituacao(original.getInSituacao());
        if (original.getPessoa() != null) {
            Pessoa p = new Pessoa();
            p.setIdPessoa(original.getPessoa().getIdPessoa());
            p.setNome(original.getPessoa().getNome());
            p.setEmail(original.getPessoa().getEmail());
            p.setCpf(original.getPessoa().getCpf());
            p.setInTipoPessoa(original.getPessoa().getInTipoPessoa());
            copia.setPessoa(p);
        }
        return copia;
    }
}