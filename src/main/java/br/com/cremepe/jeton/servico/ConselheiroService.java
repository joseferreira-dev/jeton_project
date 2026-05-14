package br.com.cremepe.jeton.servico;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.cremepe.jeton.dominio.Conselheiro;
import br.com.cremepe.jeton.dominio.Pessoa;
import br.com.cremepe.jeton.dominio.Usuario;
import br.com.cremepe.jeton.repositorio.ConselheiroRepository;
import br.com.cremepe.jeton.repositorio.PessoaRepository;
import br.com.cremepe.jeton.repositorio.UsuarioRepository;

@Service
public class ConselheiroService {

    @Autowired private ConselheiroRepository conselheiroRepository;
    @Autowired private PessoaRepository pessoaRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    @Transactional
    public Conselheiro salvar(Conselheiro conselheiro) {
        
        String cpfLimpo = "";
        if (conselheiro.getPessoa() != null && conselheiro.getPessoa().getCpf() != null) {
            cpfLimpo = conselheiro.getPessoa().getCpf().replaceAll("[^0-9]", "");
            conselheiro.getPessoa().setCpf(cpfLimpo);
        }

        // 1. MESCLAGEM DE PESSOA (Aproveita o ID se já existir no sistema)
        if (!cpfLimpo.isEmpty()) {
            Optional<Pessoa> pessoaExistente = pessoaRepository.findByCpf(cpfLimpo);
            if (pessoaExistente.isPresent()) {
                Pessoa p = pessoaExistente.get();
                if (conselheiro.getIdPessoa() == null || !conselheiro.getIdPessoa().equals(p.getIdPessoa())) {
                    // Impede duplicidade apenas se essa pessoa JÁ FOR conselheira
                    if (conselheiroRepository.existsById(p.getIdPessoa())) {
                        throw new RuntimeException("Já existe um conselheiro registrado com este CPF.");
                    }
                    // Se era apenas usuário, fazemos o "UPGRADE" aproveitando o ID
                    conselheiro.setIdPessoa(p.getIdPessoa());
                    conselheiro.getPessoa().setIdPessoa(p.getIdPessoa());
                }
            }
        }

        // 2. VALIDAÇÃO DE CRM
        if (conselheiro.getCrm() != null) {
            Optional<Conselheiro> conselExistente = conselheiroRepository.findByCrm(conselheiro.getCrm());
            if (conselExistente.isPresent() && !conselExistente.get().getIdPessoa().equals(conselheiro.getIdPessoa())) {
                throw new RuntimeException("Já existe um conselheiro registrado com o CRM-PE " + conselheiro.getCrm());
            }
        }

        // 3. DEFINE O TIPO E SALVA CONSELHEIRO (Cascade salva a Pessoa)
        conselheiro.getPessoa().setInTipoPessoa("C");
        Conselheiro conselheiroSalvo = conselheiroRepository.save(conselheiro);

        // 4. GARANTE A CRIAÇÃO/ATUALIZAÇÃO DO USUÁRIO VINCULADO
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

    private String gerarSHA256(String senha) {
        try {
            MessageDigest algoritmo = MessageDigest.getInstance("SHA-256");
            byte[] messageDigest = algoritmo.digest(senha.getBytes(StandardCharsets.UTF_8));
            StringBuilder stringHexa = new StringBuilder();
            for (byte b : messageDigest) stringHexa.append(String.format("%02X", 0xFF & b));
            return stringHexa.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Erro na criptografia", e);
        }
    }

    // ========== MÉTODOS DE LEITURA (Mantidos iguais) ==========
    @Transactional(readOnly = true)
    public Page<Conselheiro> listarComPaginacaoEPesquisa(String termo, String situacao, int page, int size, String sortField, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();
        Pageable pageable = (size == 0) ? Pageable.unpaged(sort) : PageRequest.of(page, size, sort);
        return conselheiroRepository.pesquisarPaginado(termo, situacao, pageable);
    }

    @Transactional(readOnly = true)
    public List<Conselheiro> listarTodos() { return conselheiroRepository.findAll(); }

    @Transactional(readOnly = true)
    public Optional<Conselheiro> buscarPorId(Integer id) { return conselheiroRepository.findById(id); }

    @Transactional
    public void excluir(Integer id) { conselheiroRepository.deleteById(id); }
}