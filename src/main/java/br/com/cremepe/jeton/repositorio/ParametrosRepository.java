package br.com.cremepe.jeton.repositorio;

import br.com.cremepe.jeton.dominio.Parametros;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParametrosRepository extends JpaRepository<Parametros, String> {

    // Usamos SQL nativo para ignorar as validações de mapeamento de ID do Hibernate
    @Query(value = "SELECT bloqueaSistema FROM parametros LIMIT 1", nativeQuery = true)
    Optional<String> obterStatusBloqueioSistema();
}