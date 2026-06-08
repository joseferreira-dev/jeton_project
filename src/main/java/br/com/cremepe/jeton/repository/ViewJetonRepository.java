package br.com.cremepe.jeton.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.cremepe.jeton.domain.ViewJeton;

import java.util.List;

@Repository
public interface ViewJetonRepository extends JpaRepository<ViewJeton, Integer> {

    List<ViewJeton> findByCpfAndAnoOrderByMesDesc(String cpf, Integer ano);
}