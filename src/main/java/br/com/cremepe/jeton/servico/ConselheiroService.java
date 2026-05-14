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
import br.com.cremepe.jeton.repositorio.ConselheiroRepository;

@Service
public class ConselheiroService {

    @Autowired
    private ConselheiroRepository conselheiroRepository;

    @Transactional
    public Conselheiro salvar(Conselheiro conselheiro) {
        
        // 1. Limpeza de dados apenas para CPF (que é String)
        if (conselheiro.getPessoa() != null && conselheiro.getPessoa().getCpf() != null) {
            conselheiro.getPessoa().setCpf(conselheiro.getPessoa().getCpf().replaceAll("[^0-9]", ""));
        }

        // 2. Validação de CRM Duplicado (O Spring Boot já garante que chegue numérico da tela)
        if (conselheiro.getCrm() != null) {
            Optional<Conselheiro> existente = conselheiroRepository.findByCrm(conselheiro.getCrm());
            if (existente.isPresent() && 
               (conselheiro.getIdPessoa() == null || !conselheiro.getIdPessoa().equals(existente.get().getIdPessoa()))) {
                throw new RuntimeException("Já existe um conselheiro registrado com o CRM-PE " + conselheiro.getCrm());
            }
        }

        // 3. Regras de Edição vs Novo (ID correto é idPessoa)
        if (conselheiro.getIdPessoa() != null && conselheiro.getIdPessoa() > 0) {
            Conselheiro doBanco = conselheiroRepository.findById(conselheiro.getIdPessoa())
                    .orElseThrow(() -> new RuntimeException("Conselheiro não encontrado"));
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

    // MÉTODO ADICIONADO PARA CORRIGIR ERROS NOS OUTROS CONTROLLERS
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