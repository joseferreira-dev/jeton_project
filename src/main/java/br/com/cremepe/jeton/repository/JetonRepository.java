package br.com.cremepe.jeton.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.cremepe.jeton.domain.Jeton;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface JetonRepository extends JpaRepository<Jeton, Integer> {

    Optional<Jeton> findByConselheiroIdPessoaAndMesAndAno(Integer idPessoa, Integer mes, Integer ano);

    List<Jeton> findByConselheiroIdPessoaAndAnoOrderByMesDesc(Integer idPessoa, Integer ano);

    List<Jeton> findByGestaoIdGestao(Integer idGestao);

    List<Jeton> findByConselheiroIdPessoaOrderByAnoDescMesDesc(Integer idPessoa);

    Page<Jeton> findByConselheiroIdPessoa(Integer idPessoa, Pageable pageable);

    @Query("SELECT j FROM Jeton j WHERE j.gestao.idGestao = :idGestao AND j.mes = :mes AND j.ano = :ano")
    List<Jeton> findByGestaoIdGestaoAndMesAndAno(@Param("idGestao") Integer idGestao,
            @Param("mes") Integer mes,
            @Param("ano") Integer ano);

    @Query("SELECT COALESCE(SUM(j.valor), 0) FROM Jeton j WHERE j.conselheiro.idPessoa = :idPessoa AND j.inSituacao = 'E'")
    BigDecimal sumValorRecebidoPorConselheiro(@Param("idPessoa") Integer idPessoa);

    @Query("SELECT j FROM Jeton j WHERE j.inSituacao = 'E' " +
            "AND (:idGestao IS NULL OR j.gestao.idGestao = :idGestao) " +
            "AND (:mes IS NULL OR j.mes = :mes) " +
            "AND (:ano IS NULL OR j.ano = :ano) " +
            "AND (:termo IS NULL OR LOWER(j.conselheiro.pessoa.nome) LIKE LOWER(CONCAT('%', :termo, '%')))")
    List<Jeton> pesquisarHistorico(@Param("idGestao") Integer idGestao,
            @Param("mes") Integer mes,
            @Param("ano") Integer ano,
            @Param("termo") String termo);

    @Query("SELECT j FROM Jeton j WHERE j.inSituacao = 'E' " +
            "AND (:idGestao IS NULL OR j.gestao.idGestao = :idGestao) " +
            "AND (:mes IS NULL OR j.mes = :mes) " +
            "AND (:ano IS NULL OR j.ano = :ano) " +
            "AND (:termo IS NULL OR LOWER(j.conselheiro.pessoa.nome) LIKE LOWER(CONCAT('%', :termo, '%')))")
    Page<Jeton> pesquisarHistoricoPaginado(@Param("idGestao") Integer idGestao,
            @Param("mes") Integer mes,
            @Param("ano") Integer ano,
            @Param("termo") String termo,
            Pageable pageable);
}