package br.com.cremepe.jeton.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.cremepe.jeton.domain.Comprovante;

import java.util.List;
import java.util.Optional;

public interface ComprovanteRepository extends JpaRepository<Comprovante, Integer> {

    Optional<Comprovante> findByNomeArquivo(String nomeArquivo);

    List<Comprovante> findByMesAndAno(Integer mes, Integer ano);

    List<Comprovante> findByTipoAnexoIdTipo(Integer idTipo);
}