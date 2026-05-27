package br.com.cremepe.jeton.repositorio;

import br.com.cremepe.jeton.dominio.Jeton;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JetonRepository extends JpaRepository<Jeton, Integer> {

    Optional<Jeton> findByConselheiroIdPessoaAndMesAndAno(Integer idPessoa, Integer mes, Integer ano);

    List<Jeton> findByConselheiroIdPessoaAndAnoOrderByMesDesc(Integer idPessoa, Integer ano);

    List<Jeton> findByGestaoIdGestao(Integer idGestao);

    @Query("SELECT j FROM Jeton j WHERE j.gestao.idGestao = :idGestao AND j.mes = :mes AND j.ano = :ano")
    List<Jeton> findByGestaoIdGestaoAndMesAndAno(@Param("idGestao") Integer idGestao,
            @Param("mes") Integer mes,
            @Param("ano") Integer ano);

    @Query("SELECT j FROM Jeton j WHERE j.inSituacao = 'E' " +
            "AND (:idGestao IS NULL OR j.gestao.idGestao = :idGestao) " +
            "AND (:mes IS NULL OR j.mes = :mes) " +
            "AND (:ano IS NULL OR j.ano = :ano) " +
            "AND (:termo IS NULL OR LOWER(j.conselheiro.pessoa.nome) LIKE LOWER(CONCAT('%', :termo, '%')))")
    List<Jeton> pesquisarHistorico(@Param("idGestao") Integer idGestao,
            @Param("mes") Integer mes,
            @Param("ano") Integer ano,
            @Param("termo") String termo);
}