package br.com.cremepe.jeton.repositorio;

import br.com.cremepe.jeton.dominio.ViewUserLogin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ViewUserLoginRepository extends JpaRepository<ViewUserLogin, Integer> {

    // Método crucial para o Passo 7 (Spring Security).
    // Carrega o utilizador, a sua senha encriptada e a string de permissões em
    // apenas um hit à base de dados.
    Optional<ViewUserLogin> findByCpf(String cpf);

    Optional<ViewUserLogin> findByEmail(String email);
}