package br.com.cremepe.jeton.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.cremepe.jeton.domain.Pessoa;

import java.util.List;
import java.util.Optional;

public interface PessoaRepository extends JpaRepository<Pessoa, Integer> {

    Optional<Pessoa> findByCpf(String cpf);

    Optional<Pessoa> findByEmail(String email);

    List<Pessoa> findByInTipoPessoa(String inTipoPessoa);
}