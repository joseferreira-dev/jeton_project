package br.com.cremepe.jeton.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.cremepe.jeton.domain.Parametros;

@Repository
public interface ParametrosRepository extends JpaRepository<Parametros, Integer> {
}