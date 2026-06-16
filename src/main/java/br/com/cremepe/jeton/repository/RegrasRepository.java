package br.com.cremepe.jeton.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.cremepe.jeton.domain.Portaria;
import br.com.cremepe.jeton.domain.Regras;
import br.com.cremepe.jeton.domain.Resolucao;

import java.time.LocalDate;
import java.util.List;

public interface RegrasRepository extends JpaRepository<Regras, Integer> {

    List<Regras> findByResolucaoIdResolucao(Integer resolucaoId);

    boolean existsByNomeRegraAndIdRegraNot(String nomeRegra, Integer idRegra);

    long countByPortariaIdPortaria(Integer idPortaria);

    long countByResolucaoIdResolucao(Integer idResolucao);

    @Query("SELECT DISTINCT r.resolucao FROM Regras r WHERE r.resolucao IS NOT NULL AND r.inRevogado = 'N'")
    List<Resolucao> findResolucoesComRegras();

    @Query("SELECT DISTINCT r.portaria FROM Regras r WHERE r.portaria IS NOT NULL AND r.inRevogado = 'N'")
    List<Portaria> findPortariasComRegras();

    @Query("SELECT DISTINCT r.portaria FROM Regras r WHERE r.resolucao.idResolucao = :idResolucao AND r.portaria IS NOT NULL AND r.inRevogado = 'N'")
    List<Portaria> findPortariasCompativeis(@Param("idResolucao") Integer idResolucao);

    @Query("SELECT DISTINCT r.resolucao FROM Regras r WHERE r.portaria.idPortaria = :idPortaria AND r.resolucao IS NOT NULL AND r.inRevogado = 'N'")
    List<Resolucao> findResolucoesCompativeis(@Param("idPortaria") Integer idPortaria);

    @Query("SELECT r FROM Regras r WHERE r.inRevogado = 'N' AND " +
            "((:idResolucao IS NULL AND r.resolucao IS NULL) OR (r.resolucao.idResolucao = :idResolucao)) AND " +
            "((:idPortaria IS NULL AND r.portaria IS NULL) OR (r.portaria.idPortaria = :idPortaria))")
    List<Regras> findRegrasExatas(@Param("idResolucao") Integer idResolucao,
            @Param("idPortaria") Integer idPortaria);

    @Query("SELECT r FROM Regras r WHERE " +
            "(:termo IS NULL OR :termo = '' OR LOWER(r.nomeRegra) LIKE LOWER(CONCAT('%', :termo, '%'))) AND " +
            "(:situacao IS NULL OR :situacao = '' OR r.inRevogado = :situacao) AND " +
            "(:judicante IS NULL OR :judicante = '' OR r.inJudicante = :judicante)")
    Page<Regras> findAllByFilters(@Param("termo") String termo,
            @Param("situacao") String situacao,
            @Param("judicante") String judicante,
            Pageable pageable);

    @Query("SELECT r FROM Regras r WHERE r.resolucao.idResolucao = :idResolucao " +
            "AND (:idPortaria IS NULL AND r.portaria IS NULL OR r.portaria.idPortaria = :idPortaria)")
    List<Regras> findRegrasPorNormativasInclusiveRevogadas(@Param("idResolucao") Integer idResolucao,
            @Param("idPortaria") Integer idPortaria);

    @Query("SELECT r.resolucao FROM Regras r WHERE r.resolucao IS NOT NULL " +
            "AND :dataAtividade BETWEEN r.resolucao.dtInicioVigencia AND COALESCE(r.resolucao.dtFimVigencia, '2099-12-31')")
    List<Resolucao> findResolucaoPorData(@Param("dataAtividade") LocalDate dataAtividade);

    @Query("SELECT r.portaria FROM Regras r WHERE r.portaria IS NOT NULL " +
            "AND :dataAtividade BETWEEN r.portaria.dtInicioVigencia AND COALESCE(r.portaria.dtFimVigencia, '2099-12-31')")
    List<Portaria> findPortariaPorData(@Param("dataAtividade") LocalDate dataAtividade);

    @Modifying
    @Query("UPDATE Regras r SET r.inRevogado = 'S' WHERE r.resolucao.idResolucao = :idResolucao")
    void revogarRegrasPorResolucao(@Param("idResolucao") Integer idResolucao);

    @Modifying
    @Query("UPDATE Regras r SET r.inRevogado = 'S' WHERE r.portaria.idPortaria = :idPortaria")
    void revogarRegrasPorPortaria(@Param("idPortaria") Integer idPortaria);

    @Modifying
    @Query("UPDATE Regras r SET r.inRevogado = 'N' WHERE r.resolucao.idResolucao = :idResolucao")
    void restaurarRegrasPorResolucao(@Param("idResolucao") Integer idResolucao);
}