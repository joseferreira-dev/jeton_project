package br.com.cremepe.jeton.repository;

import br.com.cremepe.jeton.dominio.ViewUserLogin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ViewUserLoginRepository extends JpaRepository<ViewUserLogin, Integer> {

    Optional<ViewUserLogin> findByCpf(String cpf);

    Optional<ViewUserLogin> findByEmail(String email);
}