package br.com.cremepe.jeton.repositorio;

import br.com.cremepe.jeton.dominio.Pessoa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório para a entidade Pessoa.
 * Ao estender JpaRepository, ganhamos métodos como save(), findAll(),
 * findById(), delete() automaticamente.
 */
@Repository
public interface PessoaRepository extends JpaRepository<Pessoa, Integer> {

    // Cria automaticamente um "SELECT * FROM pessoa WHERE cpf = ?"
    // O retorno Optional previne o famoso NullPointerException caso o CPF não
    // exista
    Optional<Pessoa> findByCpf(String cpf);

    // Cria automaticamente um "SELECT * FROM pessoa WHERE email = ?"
    Optional<Pessoa> findByEmail(String email);

    // Podemos também procurar pessoas por tipo (ex: 'C' para Conselheiro, 'F' para
    // Funcionário)
    // "SELECT * FROM pessoa WHERE inTipoPessoa = ?"
    List<Pessoa> findByInTipoPessoa(String inTipoPessoa);
}