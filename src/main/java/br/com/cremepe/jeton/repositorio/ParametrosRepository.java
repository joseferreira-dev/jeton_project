package br.com.cremepe.jeton.repositorio;

import br.com.cremepe.jeton.dominio.Parametros;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParametrosRepository extends JpaRepository<Parametros, Integer> {
    // Os métodos padrão (findById, save) já resolvem
}