package br.com.cremepe.jeton.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.cremepe.jeton.domain.Portaria;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PortariaRepository extends JpaRepository<Portaria, Integer> {

    Optional<Portaria> findByNumeroAndAno(Integer numero, Integer ano);

    List<Portaria> findByInRevogado(String inRevogado);

    @Query("SELECT p FROM Portaria p WHERE " +
            "(:termo IS NULL OR :termo = '' OR CAST(p.numero AS string) LIKE CONCAT('%', :termo, '%') OR CAST(p.ano AS string) LIKE CONCAT('%', :termo, '%')) AND "
            +
            "(:situacao IS NULL OR :situacao = '' OR p.inRevogado = :situacao)")
    Page<Portaria> pesquisarPaginado(@Param("termo") String termo,
            @Param("situacao") String situacao,
            Pageable pageable);

    @Query("SELECT p FROM Portaria p WHERE p.dtInicioVigencia <= :dataBase " +
            "AND (p.dtFimVigencia IS NULL OR p.dtFimVigencia >= :dataBase) " +
            "AND p.inRevogado <> 'S' " +
            "ORDER BY p.dtInicioVigencia DESC, p.idPortaria DESC")
    List<Portaria> findPortariasVigentesNaData(@Param("dataBase") LocalDate dataBase);

    boolean existsByNumeroAndAnoAndIdPortariaNot(Integer numero, Integer ano, Integer idPortaria);

    @Query("SELECT COUNT(p) > 0 FROM Portaria p WHERE p.idPortaria != :idPortaria " +
            "AND ((p.dtInicioVigencia <= :fim AND (p.dtFimVigencia IS NULL OR p.dtFimVigencia >= :inicio)))")
    boolean existePeriodoSobreposto(@Param("idPortaria") Integer idPortaria,
            @Param("inicio") LocalDate inicio,
            @Param("fim") LocalDate fim);
}