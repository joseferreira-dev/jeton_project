package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.Conselheiro;
import br.com.cremepe.jeton.dominio.Pessoa;
import br.com.cremepe.jeton.dominio.Usuario;
import br.com.cremepe.jeton.repositorio.ConselheiroRepository;
import br.com.cremepe.jeton.repositorio.PessoaRepository;
import br.com.cremepe.jeton.repositorio.UsuarioRepository;
import br.com.cremepe.jeton.util.CpfValidador;
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

    @Autowired
    private ConselheiroRepository conselheiroRepository;
    @Autowired
    private PessoaRepository pessoaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    // =========================================================================
    // OPERAÇÕES DE ESCRITA
    // =========================================================================

    @Transactional
    public Conselheiro salvar(Conselheiro conselheiro) {
        // 1. Valida e limpa o CPF (a entidade Pessoa normalizará)
        Pessoa pessoa = conselheiro.getPessoa();
        if (pessoa == null) {
            throw new RuntimeException("Os dados da pessoa são obrigatórios.");
        }
        String cpfLimpo = pessoa.getCpf() != null ? pessoa.getCpf().replaceAll("[^0-9]", "") : "";
        pessoa.setCpf(cpfLimpo);

        // Validação matemática do CPF
        if (cpfLimpo.isEmpty() || !CpfValidador.isCpfValido(cpfLimpo)) {
            throw new RuntimeException("O número de CPF informado é inválido. Verifique os dígitos.");
        }

        // 2. Verifica duplicidade de CPF na tabela pessoa
        validarCpfUnico(cpfLimpo, conselheiro.getIdPessoa());

        // 3. Validação de CRM (se informado)
        if (conselheiro.getCrm() != null) {
            validarCrmUnico(conselheiro.getCrm(), conselheiro.getIdPessoa());
        }

        // 4. Define o tipo da pessoa como Conselheiro ('C')
        pessoa.setInTipoPessoa(Pessoa.TIPO_CONSELHEIRO);

        // 5. Salva o conselheiro (cascade salvará a pessoa)
        Conselheiro conselheiroSalvo = conselheiroRepository.save(conselheiro);

        // 6. Garante a criação/atualização do usuário vinculado
        Usuario usuario = usuarioRepository.findById(conselheiroSalvo.getIdPessoa()).orElse(new Usuario());
        usuario.setPessoa(conselheiroSalvo.getPessoa());
        usuario.setInSituacao(conselheiroSalvo.getInSituacao());

        if (conselheiro.getSenhaAcesso() != null && !conselheiro.getSenhaAcesso().trim().isEmpty()) {
            usuario.setSenha(gerarSHA256(conselheiro.getSenhaAcesso()));
        } else if (usuario.getSenha() == null) {
            throw new RuntimeException("A senha é obrigatória para criar o acesso no sistema.");
        }
        usuarioRepository.save(usuario);

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
    public void excluir(Integer id) {
        // 1. Remove as permissões (usuario_acesso) primeiro
        conselheiroRepository.deletarPermissoesNativo(id);

        // 2. Remove o conselheiro (se existir)
        conselheiroRepository.deletarConselheiroNativo(id);

        // 3. Remove o usuário
        conselheiroRepository.deletarUsuarioNativo(id);

        // 4. Remove a pessoa
        conselheiroRepository.deletarPessoaNativa(id);
    }
}