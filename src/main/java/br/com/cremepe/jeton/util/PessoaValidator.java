package br.com.cremepe.jeton.util;

import br.com.cremepe.jeton.repository.ConselheiroRepository;
import br.com.cremepe.jeton.repository.PessoaRepository;
import org.springframework.stereotype.Component;

@Component
public class PessoaValidator {

    private final PessoaRepository pessoaRepository;
    private final ConselheiroRepository conselheiroRepository;

    public PessoaValidator(PessoaRepository pessoaRepository, ConselheiroRepository conselheiroRepository) {
        this.pessoaRepository = pessoaRepository;
        this.conselheiroRepository = conselheiroRepository;
    }

    public void validarCpfUnico(String cpf, Integer idAtual) {
        if (cpf == null || cpf.isEmpty())
            return;
        pessoaRepository.findByCpf(cpf).ifPresent(p -> {
            if (idAtual == null || !idAtual.equals(p.getIdPessoa())) {
                throw new RuntimeException("Já existe um cadastro com este CPF.");
            }
        });
    }

    public void validarCrmUnico(Integer crm, Integer idAtual) {
        if (crm == null)
            return;
        conselheiroRepository.findByCrm(crm).ifPresent(c -> {
            if (idAtual == null || !idAtual.equals(c.getIdPessoa())) {
                throw new RuntimeException("Já existe um conselheiro com o CRM " + crm);
            }
        });
    }

    public void validarEmailUnico(String email, Integer idAtual) {
        if (email == null || email.isEmpty())
            return;
        pessoaRepository.findByEmail(email).ifPresent(p -> {
            if (idAtual == null || !idAtual.equals(p.getIdPessoa())) {
                throw new RuntimeException("Já existe um cadastro com este e-mail.");
            }
        });
    }

    public void validarCpf(String cpf) {
        if (cpf == null || !CpfValidator.isCpfValido(cpf)) {
            throw new RuntimeException("CPF inválido.");
        }
    }
}