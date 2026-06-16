package br.com.cremepe.jeton.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.cremepe.jeton.domain.Gestao;

import java.time.LocalDate;

public interface GestaoRepository extends JpaRepository<Gestao, Integer> {

    boolean existsByNomeGestaoAndIdGestaoNot(String nome, Integer id);

    @Query("SELECT COUNT(g) > 0 FROM Gestao g WHERE (:id IS NULL OR g.idGestao != :id) " +
            "AND g.dtInicio <= :fim AND g.dtFim >= :inicio")
    boolean existsPeriodoSobreposto(@Param("id") Integer id,
            @Param("inicio") LocalDate inicio,
            @Param("fim") LocalDate fim);

    Page<Gestao> findByNomeGestaoContainingIgnoreCase(String nome, Pageable pageable);
}