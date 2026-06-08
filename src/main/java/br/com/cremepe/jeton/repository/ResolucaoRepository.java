package br.com.cremepe.jeton.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.cremepe.jeton.domain.Resolucao;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ResolucaoRepository extends JpaRepository<Resolucao, Integer> {

    Optional<Resolucao> findByNumeroAndAno(Integer numero, Integer ano);

    List<Resolucao> findByInRevogado(String inRevogado);

    @Query("SELECT r FROM Resolucao r WHERE " +
            "(:termo IS NULL OR :termo = '' OR CAST(r.numero AS string) LIKE CONCAT('%', :termo, '%') OR " +
            "CAST(r.ano AS string) LIKE CONCAT('%', :termo, '%') OR LOWER(r.ementa) LIKE LOWER(CONCAT('%', :termo, '%'))) AND "
            +
            "(:situacao IS NULL OR :situacao = '' OR r.inRevogado = :situacao)")
    Page<Resolucao> pesquisarPaginado(@Param("termo") String termo,
            @Param("situacao") String situacao,
            Pageable pageable);

    @Query("SELECT r FROM Resolucao r WHERE r.dtInicioVigencia <= :dataBase " +
            "AND (r.dtFimVigencia IS NULL OR r.dtFimVigencia >= :dataBase) " +
            "ORDER BY r.dtInicioVigencia DESC, r.idResolucao DESC")
    List<Resolucao> findResolucoesVigentesNaData(@Param("dataBase") LocalDate dataBase);

    boolean existsByNumeroAndAnoAndIdResolucaoNot(Integer numero, Integer ano, Integer idResolucao);

    @Query("SELECT COUNT(r) > 0 FROM Resolucao r WHERE r.idResolucao != :idResolucao " +
            "AND ((r.dtInicioVigencia <= :fim AND (r.dtFimVigencia IS NULL OR r.dtFimVigencia >= :inicio)))")
    boolean existePeriodoSobreposto(@Param("idResolucao") Integer idResolucao,
            @Param("inicio") LocalDate inicio,
            @Param("fim") LocalDate fim);
}