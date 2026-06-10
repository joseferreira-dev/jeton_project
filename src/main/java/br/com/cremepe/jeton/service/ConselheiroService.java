package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.annotation.Auditar;
import br.com.cremepe.jeton.domain.Conselheiro;
import br.com.cremepe.jeton.domain.GestaoConselheiro;
import br.com.cremepe.jeton.domain.NivelAcesso;
import br.com.cremepe.jeton.domain.Pessoa;
import br.com.cremepe.jeton.domain.Usuario;
import br.com.cremepe.jeton.repository.ConselheiroRepository;
import br.com.cremepe.jeton.repository.GestaoConselheiroRepository;
import br.com.cremepe.jeton.repository.PessoaRepository;
import br.com.cremepe.jeton.repository.UsuarioRepository;
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

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ConselheiroService {

    private static final Logger log = LoggerFactory.getLogger(ConselheiroService.class);

    @Autowired
    private ConselheiroRepository conselheiroRepository;
    @Autowired
    private GestaoConselheiroRepository gestaoConselheiroRepository;
    @Autowired
    private PessoaRepository pessoaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PermissaoService permissaoService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UsuarioService usuarioService;

    @Auditar(tabela = "conselheiro", acao = "CRIAR", descricao = "Criação de novo conselheiro", dadosParametros = "{ 'conselheiro': #conselheiro }", dadosRetorno = "#result", capturarEstadoAnterior = false, auditarExcecao = true)
    @Transactional
    public Conselheiro criar(Conselheiro conselheiro) {
        conselheiro.setIdPessoa(null);
        return salvarConselheiro(conselheiro, true);
    }

    @Auditar(tabela = "conselheiro", acao = "ATUALIZAR", descricao = "Atualização de conselheiro existente", dadosParametros = "{ 'conselheiro': #conselheiro }", dadosRetorno = "#result", capturarEstadoAnterior = true, auditarExcecao = true)
    @Transactional
    public Conselheiro atualizar(Conselheiro conselheiro) {
        if (conselheiro.getIdPessoa() == null) {
            throw new RuntimeException("ID do conselheiro não informado para atualização.");
        }
        return salvarConselheiro(conselheiro, false);
    }

    private Conselheiro salvarConselheiro(Conselheiro conselheiro, boolean isNovo) {
        // Se for uma edição (ID informado), carrega a entidade existente
        Conselheiro conselheiroExistente = null;
        if (!isNovo) {
            conselheiroExistente = conselheiroRepository.findById(conselheiro.getIdPessoa())
                    .orElseThrow(() -> new RuntimeException("Conselheiro não encontrado para edição."));
        }

        // Obtém a pessoa a ser salva (pode ser a nova ou a existente)
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

        // Valida duplicidade de CPF (ignorando o próprio registro)
        validarCpfUnico(cpfLimpo, isNovo ? null : conselheiroExistente.getIdPessoa());

        // Valida CRM (se informado)
        Integer crm = conselheiro.getCrm();
        if (crm != null) {
            validarCrmUnico(crm, isNovo ? null : conselheiroExistente.getIdPessoa());
        }

        // Prepara o conselheiro final
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

        // Salva o conselheiro (cascade salvará a pessoa)
        Conselheiro conselheiroSalvo = conselheiroRepository.save(conselheiroFinal);
        log.info("Conselheiro {}: ID={}, nome='{}', CRM={}, situação={}",
                isNovo ? "criado" : "atualizado",
                conselheiroSalvo.getIdPessoa(), pessoa.getNome(), crm != null ? crm : 0,
                conselheiroSalvo.getInSituacao());

        // Sincroniza o usuário vinculado (cria ou atualiza)
        sincronizarUsuario(conselheiroSalvo, conselheiro.getSenhaAcesso(), isNovo);

        // Concede permissões padrão apenas para novos conselheiros
        if (isNovo) {
            concederPermissoesPadrao(conselheiroSalvo.getIdPessoa());
        }

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

    private void concederPermissoesPadrao(Integer idConselheiro) {
        permissaoService.concederPermissao(idConselheiro, NivelAcesso.NIVEL_ATIVIDADE_CONSELHAL);
        permissaoService.concederPermissao(idConselheiro, NivelAcesso.NIVEL_COMPROVANTES);
        log.info("Permissões padrão concedidas para o novo conselheiro ID={}", idConselheiro);
    }

    @Auditar(tabela = "conselheiro", acao = "EXCLUIR", descricao = "Exclusão física de conselheiro", dadosParametros = "{ 'id': #id }", capturarEstadoAnterior = true, auditarExcecao = true)
    @Transactional
    public void excluir(Integer id) {
        Optional<Conselheiro> conselheiroOpt = conselheiroRepository.findById(id);
        if (conselheiroOpt.isEmpty()) {
            log.warn("Tentativa de excluir conselheiro inexistente ID={}", id);
            throw new RuntimeException("Conselheiro não encontrado para exclusão.");
        }
        Conselheiro conselheiro = conselheiroOpt.get();
        String nome = conselheiro.getPessoa().getNome();
        String cpf = conselheiro.getPessoa().getCpf();
        Integer crm = conselheiro.getCrm();
        String situacao = conselheiro.getInSituacao();

        // Delega a remoção completa para o UsuarioService
        usuarioService.excluir(id);

        log.info("Conselheiro excluído: ID={}, nome='{}', CPF={}, CRM={}, situação={}",
                id, nome, cpf, crm, situacao);
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
}