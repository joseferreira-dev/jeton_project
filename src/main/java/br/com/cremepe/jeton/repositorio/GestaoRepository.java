package br.com.cremepe.jeton.repositorio;

import br.com.cremepe.jeton.dominio.Gestao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface GestaoRepository extends JpaRepository<Gestao, Integer> {

    Optional<Gestao> findByNomeGestao(String nomeGestao);

    @Query("SELECT COUNT(g) > 0 FROM Gestao g WHERE g.nomeGestao = :nome AND (:id IS NULL OR g.idGestao != :id)")
    boolean existsByNomeGestaoIgnorandoId(@Param("nome") String nome, @Param("id") Integer id);

    @Query("SELECT COUNT(g) > 0 FROM Gestao g WHERE (:id IS NULL OR g.idGestao != :id) " +
            "AND g.dtInicio <= :fim AND g.dtFim >= :inicio")
    boolean existsPeriodoSobreposto(@Param("id") Integer id,
            @Param("inicio") LocalDate inicio,
            @Param("fim") LocalDate fim);

    @Query("SELECT g FROM Gestao g WHERE g.dtInicio <= CURRENT_DATE AND g.dtFim >= CURRENT_DATE")
    Optional<Gestao> findAtual();

    @Query("SELECT g FROM Gestao g WHERE LOWER(g.nomeGestao) LIKE LOWER(CONCAT('%', :termo, '%'))")
    Page<Gestao> pesquisarPaginado(@Param("termo") String termo, Pageable pageable);
}