package br.com.cremepe.jeton.repositorio;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.cremepe.jeton.dominio.Conselheiro;

@Repository
public interface ConselheiroRepository extends JpaRepository<Conselheiro, Integer> {

    // Alterado para Integer, pois no banco de dados CRM é numérico
    Optional<Conselheiro> findByCrm(Integer crm);

    // Usa CAST para permitir que a pesquisa LIKE funcione em campos numéricos
    @Query("SELECT c FROM Conselheiro c WHERE " +
           "(LOWER(c.pessoa.nome) LIKE LOWER(CONCAT('%', :termo, '%')) OR CAST(c.crm AS string) LIKE CONCAT('%', :termo, '%')) " +
           "AND (:situacao IS NULL OR :situacao = '' OR c.inSituacao = :situacao)")
    Page<Conselheiro> pesquisarPaginado(@Param("termo") String termo, 
                                        @Param("situacao") String situacao, 
                                        Pageable pageable);
}