package br.com.cremepe.jeton.repositorio;

import br.com.cremepe.jeton.dominio.PontosSaldo;
import br.com.cremepe.jeton.dto.PontosRemanescentesDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PontosSaldoRepository extends JpaRepository<PontosSaldo, Integer> {

    List<PontosSaldo> findByJetonIdJeton(Integer idJeton);

    @Query("""
            SELECT new br.com.cremepe.jeton.dto.PontosRemanescentesDTO(
                c.idPessoa,
                p.nome,
                SUM(ps.pontosSobrando),
                COALESCE(SUM(j.valor), 0)
            )
            FROM PontosSaldo ps
            JOIN ps.atividade a
            JOIN a.conselheiro c
            JOIN c.pessoa p
            LEFT JOIN ps.jeton j
            WHERE ps.inSituacao = 'A'
            GROUP BY c.idPessoa, p.nome
            """)
    List<PontosRemanescentesDTO> buscarSaldosAgrupadosPorConselheiro();

    // Busca os saldos remanescentes ordenando estritamente pela normativa mais
    // antiga (FIFO Cronológico de Resoluções)
    @Query("SELECT ps FROM PontosSaldo ps WHERE ps.atividade.conselheiro.idPessoa = :idPessoa " +
            "AND ps.gestao.idGestao = :idGestao AND ps.inSituacao = 'A' AND ps.pontosSobrando > 0 " +
            "ORDER BY ps.resolucao.ano ASC, ps.resolucao.numero ASC")
    List<PontosSaldo> findSaldosAtivosPorGestaoFifo(
            @Param("idPessoa") Integer idPessoa,
            @Param("idGestao") Integer idGestao);
}