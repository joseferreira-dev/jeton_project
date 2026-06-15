package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.domain.Conselheiro;
import br.com.cremepe.jeton.domain.GestaoConselheiro;
import br.com.cremepe.jeton.domain.Pessoa;
import br.com.cremepe.jeton.domain.Usuario;
import br.com.cremepe.jeton.repository.ConselheiroRepository;
import br.com.cremepe.jeton.repository.GestaoConselheiroRepository;
import br.com.cremepe.jeton.repository.PessoaRepository;
import br.com.cremepe.jeton.repository.UsuarioRepository;
import br.com.cremepe.jeton.util.CpfValidador;
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
    private final PermissaoService permissaoService;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioService usuarioService;
    private final LogJetonService logJetonService;

    public ConselheiroService(ConselheiroRepository conselheiroRepository,
            GestaoConselheiroRepository gestaoConselheiroRepository,
            PessoaRepository pessoaRepository,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            UsuarioService usuarioService,
            PermissaoService permissaoService,
            LogJetonService logJetonService) {
        this.conselheiroRepository = conselheiroRepository;
        this.gestaoConselheiroRepository = gestaoConselheiroRepository;
        this.pessoaRepository = pessoaRepository;
        this.usuarioRepository = usuarioRepository;
        this.permissaoService = permissaoService;
        this.passwordEncoder = passwordEncoder;
        this.usuarioService = usuarioService;
        this.logJetonService = logJetonService;
    }

    @Transactional
    public Conselheiro criar(Conselheiro conselheiro) {
        conselheiro.setIdPessoa(null);
        Conselheiro salvo = salvarConselheiro(conselheiro, true);
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

        Conselheiro atualizado = salvarConselheiro(conselheiro, false);
        logJetonService.logConselheiroAtualizado(copiaAntigo, atualizado);
        return atualizado;
    }

    private Conselheiro salvarConselheiro(Conselheiro conselheiro, boolean isNovo) {
        Conselheiro conselheiroExistente = null;
        if (!isNovo) {
            conselheiroExistente = conselheiroRepository.findById(conselheiro.getIdPessoa())
                    .orElseThrow(() -> new RuntimeException("Conselheiro não encontrado para edição."));
        }

        Pessoa pessoa;
        if (isNovo) {
            pessoa = conselheiro.getPessoa();
            if (pessoa == null) {
                throw new RuntimeException("Os dados da pessoa são obrigatórios.");
            }
            pessoa.setInTipoPessoa(Pessoa.TIPO_CONSELHEIRO);
        } else {
            pessoa = conselheiroExistente.getPessoa();
            pessoa.setNome(conselheiro.getPessoa().getNome());
            pessoa.setCpf(conselheiro.getPessoa().getCpf());
            pessoa.setEmail(conselheiro.getPessoa().getEmail());
            pessoa.setInTipoPessoa(Pessoa.TIPO_CONSELHEIRO);
        }

        // Normaliza e valida CPF
        String cpfLimpo = pessoa.getCpf() != null ? pessoa.getCpf().replaceAll("[^0-9]", "") : "";
        pessoa.setCpf(cpfLimpo);
        if (cpfLimpo.isEmpty() || !CpfValidador.isCpfValido(cpfLimpo)) {
            throw new RuntimeException("O número de CPF informado é inválido. Verifique os dígitos.");
        }

        validarCpfUnico(cpfLimpo, isNovo ? null : conselheiroExistente.getIdPessoa());

        Integer crm = conselheiro.getCrm();
        if (crm != null) {
            validarCrmUnico(crm, isNovo ? null : conselheiroExistente.getIdPessoa());
        }

        Conselheiro conselheiroFinal;
        if (isNovo) {
            conselheiroFinal = conselheiro;
            conselheiroFinal.setPessoa(pessoa);
            conselheiroFinal.setIdPessoa(null); // será gerado pelo JPA
            if (conselheiroFinal.getInSituacao() == null || conselheiroFinal.getInSituacao().isEmpty()) {
                conselheiroFinal.setInSituacao(Conselheiro.SITUACAO_ATIVO);
            }
        } else {
            conselheiroFinal = conselheiroExistente;
            conselheiroFinal.setCrm(crm);
            conselheiroFinal.setInSituacao(conselheiro.getInSituacao());
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

        usuarioRepository.save(usuario);
        log.debug("Usuário sincronizado para conselheiro ID={}", conselheiro.getIdPessoa());
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

    private void validarCpfUnico(String cpf, Integer idAtual) {
        Optional<Pessoa> existente = pessoaRepository.findByCpf(cpf);
        if (existente.isPresent()) {
            if (existente.get().getInTipoPessoa() == Pessoa.TIPO_CONSELHEIRO) {
                if (idAtual == null || !idAtual.equals(existente.get().getIdPessoa())) {
                    throw new RuntimeException("Já existe um conselheiro registrado com este CPF.");
                }
            } else {
                throw new RuntimeException("Já existe um usuário registrado com este CPF.");
            }
        }
    }

    private void validarCrmUnico(Integer crm, Integer idAtual) {
        Optional<Conselheiro> existente = conselheiroRepository.findByCrm(crm);
        if (existente.isPresent() && (idAtual == null || !idAtual.equals(existente.get().getIdPessoa()))) {
            throw new RuntimeException("Já existe um conselheiro registrado com o CRM-PE " + crm);
        }
    }

    @Transactional(readOnly = true)
    public Page<Conselheiro> listarComPaginacaoEPesquisa(String termo, String situacao, int page, int size,
            String sortField, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();
        Pageable pageable = (size == 0) ? Pageable.unpaged(sort) : PageRequest.of(page, size, sort);
        return conselheiroRepository.pesquisarPaginado(termo, situacao, pageable);
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