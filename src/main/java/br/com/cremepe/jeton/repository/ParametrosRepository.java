package br.com.cremepe.jeton.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.cremepe.jeton.domain.Parametros;

public interface ParametrosRepository extends JpaRepository<Parametros, Integer> {
}