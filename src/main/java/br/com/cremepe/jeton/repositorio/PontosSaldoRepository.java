package br.com.cremepe.jeton.repositorio;

import br.com.cremepe.jeton.dominio.PontosSaldo;
import br.com.cremepe.jeton.dto.PontosRemanescentesDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PontosSaldoRepository extends JpaRepository<PontosSaldo, Integer> {

    /**
     * Esta query realiza uma agregação analítica massiva diretamente na base de dados e 
     * instancia os nossos DTOs de leitura sem sobrecarregar a memória do servidor Java.
     * Isto substitui completamente a necessidade de Stored Procedures complexas ou Views físicas.
     */
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
}