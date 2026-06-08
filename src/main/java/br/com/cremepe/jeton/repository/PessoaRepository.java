package br.com.cremepe.jeton.repository;

import br.com.cremepe.jeton.dominio.Pessoa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório para a entidade Pessoa.
 * Fornece métodos básicos e consultas personalizadas.
 */
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