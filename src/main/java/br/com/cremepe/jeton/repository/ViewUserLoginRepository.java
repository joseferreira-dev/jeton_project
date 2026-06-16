package br.com.cremepe.jeton.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.cremepe.jeton.domain.ViewUserLogin;

import java.util.Optional;

public interface ViewUserLoginRepository extends JpaRepository<ViewUserLogin, Integer> {

    Optional<ViewUserLogin> findByCpf(String cpf);
}