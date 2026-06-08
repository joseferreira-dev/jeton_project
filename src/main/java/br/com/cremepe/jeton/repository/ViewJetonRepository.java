package br.com.cremepe.jeton.repository;

import br.com.cremepe.jeton.dominio.ViewJeton;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ViewJetonRepository extends JpaRepository<ViewJeton, Integer> {

    List<ViewJeton> findByCpfAndAnoOrderByMesDesc(String cpf, Integer ano);
}