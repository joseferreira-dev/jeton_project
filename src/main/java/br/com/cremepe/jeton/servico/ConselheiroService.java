package br.com.cremepe.jeton.servico;

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
import br.com.cremepe.jeton.repositorio.ConselheiroRepository;
import br.com.cremepe.jeton.repositorio.PessoaRepository;

@Service
public class ConselheiroService {

    @Autowired
    private ConselheiroRepository conselheiroRepository;

    // NOVO: Injetado para pesquisar se o CPF já pertence a qualquer pessoa (Usuário ou Conselheiro)
    @Autowired
    private PessoaRepository pessoaRepository;

    @Transactional
    public Conselheiro salvar(Conselheiro conselheiro) {
        
        // 1. Limpeza de dados (Deixa apenas os números do CPF)
        String cpfLimpo = "";
        if (conselheiro.getPessoa() != null && conselheiro.getPessoa().getCpf() != null) {
            cpfLimpo = conselheiro.getPessoa().getCpf().replaceAll("[^0-9]", "");
            conselheiro.getPessoa().setCpf(cpfLimpo);
        }

        // 2. VALIDAÇÃO DE CPF DUPLICADO (Verifica em toda a tabela Pessoa)
        if (!cpfLimpo.isEmpty()) {
            Optional<Pessoa> pessoaExistente = pessoaRepository.findByCpf(cpfLimpo);
            if (pessoaExistente.isPresent()) {
                // Se o ID for novo (null) ou diferente do ID da pessoa encontrada, é duplicidade!
                if (conselheiro.getIdPessoa() == null || 
                   !conselheiro.getIdPessoa().equals(pessoaExistente.get().getIdPessoa())) {
                    throw new RuntimeException("Já existe um cadastro no sistema (Conselheiro ou Usuário) utilizando o CPF informado.");
                }
            }
        }

        // 3. VALIDAÇÃO DE CRM DUPLICADO (O Spring Boot garante que já chega numérico)
        if (conselheiro.getCrm() != null) {
            Optional<Conselheiro> conselheiroExistente = conselheiroRepository.findByCrm(conselheiro.getCrm());
            if (conselheiroExistente.isPresent()) {
                if (conselheiro.getIdPessoa() == null || 
                   !conselheiro.getIdPessoa().equals(conselheiroExistente.get().getIdPessoa())) {
                    throw new RuntimeException("Já existe um conselheiro registrado com o CRM-PE " + conselheiro.getCrm());
                }
            }
        }

        // 4. Regras de Edição vs Novo
        if (conselheiro.getIdPessoa() != null && conselheiro.getIdPessoa() > 0) {
            Conselheiro doBanco = conselheiroRepository.findById(conselheiro.getIdPessoa())
                    .orElseThrow(() -> new RuntimeException("Conselheiro não encontrado para edição."));
            conselheiro.getPessoa().setIdPessoa(conselheiro.getIdPessoa());
            conselheiro.getPessoa().setInTipoPessoa(doBanco.getPessoa().getInTipoPessoa());
        } else {
            conselheiro.setIdPessoa(null);
            if (conselheiro.getPessoa() != null) {
                conselheiro.getPessoa().setIdPessoa(null);
                conselheiro.getPessoa().setInTipoPessoa("C"); 
            }
        }

        return conselheiroRepository.save(conselheiro);
    }

    @Transactional(readOnly = true)
    public Page<Conselheiro> listarComPaginacaoEPesquisa(String termo, String situacao, int page, int size, String sortField, String sortDir) {
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

    @Transactional
    public void excluir(Integer id) {
        conselheiroRepository.deleteById(id);
    }
}