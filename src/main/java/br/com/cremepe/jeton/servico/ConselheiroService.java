package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.Conselheiro;
import br.com.cremepe.jeton.dominio.NivelAcesso;
import br.com.cremepe.jeton.dominio.Pessoa;
import br.com.cremepe.jeton.dominio.Usuario;
import br.com.cremepe.jeton.repositorio.ConselheiroRepository;
import br.com.cremepe.jeton.repositorio.PessoaRepository;
import br.com.cremepe.jeton.repositorio.UsuarioRepository;
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
public class ConselheiroService {

    private static final Logger log = LoggerFactory.getLogger(ConselheiroService.class);

    @Autowired
    private ConselheiroRepository conselheiroRepository;
    @Autowired
    private PessoaRepository pessoaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private AcessoService acessoService;
    @Autowired
    private LogJetonService logJetonService;

    // =========================================================================
    // OPERAÇÕES DE ESCRITA
    // =========================================================================

    @Transactional
    public Conselheiro salvar(Conselheiro conselheiro, Integer idUsuarioLogado) {
        // Se for uma edição (ID informado), carrega a entidade existente
        boolean isNovo = conselheiro.getIdPessoa() == null;
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
            // Define o tipo como Conselheiro (CORREÇÃO AQUI)
            pessoa.setInTipoPessoa(Pessoa.TIPO_CONSELHEIRO);
        } else {
            pessoa = conselheiroExistente.getPessoa();
            // Atualiza os campos da pessoa com os dados vindos do formulário
            pessoa.setNome(conselheiro.getPessoa().getNome());
            pessoa.setCpf(conselheiro.getPessoa().getCpf());
            pessoa.setEmail(conselheiro.getPessoa().getEmail());
            // O tipo permanece CONSELHEIRO
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
            // Garante situação padrão se não informada
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

        // Sincroniza o usuário vinculado
        Usuario usuario = usuarioRepository.findById(conselheiroSalvo.getIdPessoa()).orElse(new Usuario());
        usuario.setPessoa(conselheiroSalvo.getPessoa());
        usuario.setInSituacao(conselheiroSalvo.getInSituacao());

        if (conselheiro.getSenhaAcesso() != null && !conselheiro.getSenhaAcesso().trim().isEmpty()) {
            usuario.setSenha(gerarSHA256(conselheiro.getSenhaAcesso()));
        } else if (usuario.getSenha() == null && isNovo) {
            throw new RuntimeException("A senha é obrigatória para criar o acesso no sistema.");
        }
        usuarioRepository.save(usuario);

        // Concede permissões padrão apenas para novos conselheiros
        if (isNovo) {
            acessoService.concederPermissao(conselheiroSalvo.getIdPessoa(), NivelAcesso.NIVEL_ATIVIDADE_CONSELHAL);
            acessoService.concederPermissao(conselheiroSalvo.getIdPessoa(), NivelAcesso.NIVEL_COMPROVANTES);
            acessoService.concederPermissao(conselheiroSalvo.getIdPessoa(), NivelAcesso.NIVEL_JETONS);
            acessoService.concederPermissao(conselheiroSalvo.getIdPessoa(), NivelAcesso.NIVEL_PONTOS_REMANESCENTES);
            log.info("Permissões padrão concedidas para o novo conselheiro ID={}", conselheiroSalvo.getIdPessoa());
        }

        // Log de auditoria
        String textoLog = String.format(
                "Conselheiro %s: ID=%d, Nome='%s', CPF=%s, CRM=%d, Situação='%s'",
                isNovo ? "criado" : "atualizado",
                conselheiroSalvo.getIdPessoa(), pessoa.getNome(), cpfLimpo, crm != null ? crm : 0,
                conselheiroSalvo.getInSituacao());
        logJetonService.registrarLog("conselheiro", idUsuarioLogado, textoLog);

        return conselheiroSalvo;
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
            throw new RuntimeException("Erro na criptografia da senha", e);
        }
    }

    // =========================================================================
    // OPERAÇÕES DE LEITURA
    // =========================================================================

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

    // =========================================================================
    // EXCLUSÃO
    // =========================================================================
    @Transactional
    public void excluir(Integer id, Integer idUsuarioLogado) {
        // Busca o conselheiro antes de excluir para obter dados para o log
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

        // 1. Remove as permissões (usuario_acesso) primeiro
        conselheiroRepository.deletarPermissoesNativo(id);

        // 2. Remove o conselheiro (se existir)
        conselheiroRepository.deletarConselheiroNativo(id);

        // 3. Remove o usuário
        conselheiroRepository.deletarUsuarioNativo(id);

        // 4. Remove a pessoa
        conselheiroRepository.deletarPessoaNativa(id);

        log.info("Conselheiro excluído: ID={}, nome='{}', CPF={}, CRM={}, situação={}",
                id, nome, cpf, crm, situacao);

        String textoLog = String.format(
                "Conselheiro excluído: ID=%d, Nome='%s', CPF=%s, CRM=%d, Situação='%s'",
                id, nome, cpf, crm, situacao);
        logJetonService.registrarLog("conselheiro", idUsuarioLogado, textoLog);
    }
}