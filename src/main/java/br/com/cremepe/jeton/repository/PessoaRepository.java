package br.com.cremepe.jeton.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.cremepe.jeton.domain.Pessoa;

import java.util.List;
import java.util.Optional;

@Repository
public interface PessoaRepository extends JpaRepository<Pessoa, Integer> {

    Optional<Pessoa> findByCpf(String cpf);

    Optional<Pessoa> findByEmail(String email);

    List<Pessoa> findByInTipoPessoa(String inTipoPessoa);

    boolean existsByCpfAndIdPessoaNot(String cpf, Integer idPessoa);

    boolean existsByEmailAndIdPessoaNot(String email, Integer idPessoa);

    default List<Pessoa> findAllConselheiros() {
        return findByInTipoPessoa(Pessoa.TIPO_CONSELHEIRO);
    }

    default List<Pessoa> findAllFuncionarios() {
        return findByInTipoPessoa(Pessoa.TIPO_FUNCIONARIO);
    }
}