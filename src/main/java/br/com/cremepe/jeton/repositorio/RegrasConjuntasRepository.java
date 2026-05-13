package br.com.cremepe.jeton.repositorio;

import br.com.cremepe.jeton.dominio.RegrasConjuntas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegrasConjuntasRepository extends JpaRepository<RegrasConjuntas, Integer> {

    // Filtra grupos de regras pelo tipo de limite (diário, mensal, etc.)
    List<RegrasConjuntas> findByInTipoLimite(String inTipoLimite);
}