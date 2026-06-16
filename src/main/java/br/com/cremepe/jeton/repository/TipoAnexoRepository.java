package br.com.cremepe.jeton.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.cremepe.jeton.domain.TipoAnexo;

import java.util.Optional;

public interface TipoAnexoRepository extends JpaRepository<TipoAnexo, Integer> {

    Optional<TipoAnexo> findByNome(String nome);
}