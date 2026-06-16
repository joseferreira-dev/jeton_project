package br.com.cremepe.jeton.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.cremepe.jeton.domain.Comprovante;

import java.util.List;

public interface ComprovanteRepository extends JpaRepository<Comprovante, Integer> {

    List<Comprovante> findByTipoAnexoIdTipo(Integer idTipo);
}