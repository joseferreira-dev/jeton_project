package br.com.cremepe.jeton.repositorio;

import br.com.cremepe.jeton.dominio.ViewJeton;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ViewJetonRepository extends JpaRepository<ViewJeton, Integer> {

    // Lista consolidada financeira formatada para o ecrã (ex: "Janeiro", "R$ 1.500,00")
    List<ViewJeton> findByCpfAndAnoOrderByMesDesc(String cpf, Integer ano);
}