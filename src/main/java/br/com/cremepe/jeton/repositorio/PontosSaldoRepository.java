package br.com.cremepe.jeton.repositorio;

import br.com.cremepe.jeton.dominio.PontosSaldo;
import br.com.cremepe.jeton.dto.PontosRemanescentesDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PontosSaldoRepository extends JpaRepository<PontosSaldo, Integer> {

    List<PontosSaldo> findByJetonIdJeton(Integer idJeton);

    @Query("SELECT new br.com.cremepe.jeton.dto.PontosRemanescentesDTO(c.idPessoa, p.nome, SUM(ps.pontosSobrando), COALESCE(SUM(j.valor), 0)) "
            +
            "FROM PontosSaldo ps JOIN ps.conselheiro c JOIN c.pessoa p LEFT JOIN ps.jeton j " +
            "WHERE ps.inSituacao = 'A' GROUP BY c.idPessoa, p.nome")
    List<PontosRemanescentesDTO> buscarSaldosAgrupadosPorConselheiro();

    @Query("SELECT ps FROM PontosSaldo ps WHERE ps.conselheiro.idPessoa = :idPessoa " +
            "AND ps.gestao.idGestao = :idGestao AND ps.inSituacao = 'A' AND ps.pontosSobrando > 0 " +
            "ORDER BY ps.dataHora ASC, ps.idPontosSaldo ASC")
    List<PontosSaldo> buscarSaldosDisponiveisOrdenadosFIFO(@Param("idPessoa") Integer idPessoa,
            @Param("idGestao") Integer idGestao);

    @Query("SELECT COALESCE(SUM(ps.pontosSobrando), 0) FROM PontosSaldo ps " +
            "WHERE ps.conselheiro.idPessoa = :idPessoa AND ps.gestao.idGestao = :idGestao AND ps.inSituacao = 'A'")
    Integer somarPontosSobrandoAtivos(@Param("idPessoa") Integer idPessoa,
            @Param("idGestao") Integer idGestao);

    @Query("SELECT ps FROM PontosSaldo ps WHERE ps.conselheiro.idPessoa = :idPessoa " +
            "AND ps.atividade IS NOT NULL " +
            "AND MONTH(ps.atividade.dataHoraRegistro) = :mes AND YEAR(ps.atividade.dataHoraRegistro) = :ano")
    List<PontosSaldo> buscarSaldosDeAtividadesDoMes(@Param("idPessoa") Integer idPessoa,
            @Param("mes") Integer mes,
            @Param("ano") Integer ano);

    boolean existsByConselheiroIdPessoaAndGestaoIdGestaoAndInSituacao(Integer idPessoa, Integer idGestao,
            String inSituacao);

    long countByConselheiroIdPessoaAndGestaoIdGestaoAndInSituacao(Integer idPessoa, Integer idGestao,
            String inSituacao);

    @Query("SELECT COALESCE(SUM(ps.pontosUtilizados), 0) FROM PontosSaldo ps " +
            "WHERE ps.conselheiro.idPessoa = :idPessoa " +
            "AND FUNCTION('DATE', ps.dataHora) = :data " +
            "AND ps.atividade.inTurno = :turno")
    Integer sumPontosUtilizadosPorConselheiroDataTurno(@Param("idPessoa") Integer idPessoa,
            @Param("data") LocalDate data,
            @Param("turno") String turno);

    @Query("SELECT COALESCE(SUM(ps.pontosSobrando), 0) FROM PontosSaldo ps " +
            "WHERE ps.conselheiro.idPessoa = :idPessoa AND ps.inSituacao = 'A'")
    Integer somarPontosSobrandoTotal(@Param("idPessoa") Integer idPessoa);
}