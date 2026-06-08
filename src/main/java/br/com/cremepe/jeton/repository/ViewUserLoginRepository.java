package br.com.cremepe.jeton.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.cremepe.jeton.domain.ViewUserLogin;

import java.util.Optional;

@Repository
public interface ViewUserLoginRepository extends JpaRepository<ViewUserLogin, Integer> {

    Optional<ViewUserLogin> findByCpf(String cpf);

    Optional<ViewUserLogin> findByEmail(String email);
}