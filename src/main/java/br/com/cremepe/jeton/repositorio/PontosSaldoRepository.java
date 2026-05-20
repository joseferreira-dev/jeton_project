package br.com.cremepe.jeton.repositorio;

import br.com.cremepe.jeton.dominio.PontosSaldo;
import br.com.cremepe.jeton.dto.PontosRemanescentesDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PontosSaldoRepository extends JpaRepository<PontosSaldo, Integer> {

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

    // Busca saldos positivos e ativos de um conselheiro
    @Query("SELECT p FROM PontosSaldo p WHERE p.atividade.conselheiro.idPessoa = :idPessoa AND p.inSituacao = 'A' AND p.pontosSobrando > 0 ORDER BY p.dataHora ASC")
    List<PontosSaldo> findSaldosAtivosPorConselheiro(
            @org.springframework.data.repository.query.Param("idPessoa") Integer idPessoa);
}