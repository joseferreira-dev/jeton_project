package br.com.cremepe.jeton.repository;

import br.com.cremepe.jeton.dominio.RegrasConjuntas;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegrasConjuntasRepository extends JpaRepository<RegrasConjuntas, Integer> {

    List<RegrasConjuntas> findByInTipoLimite(String inTipoLimite);

    @Query("SELECT rc FROM RegrasConjuntas rc WHERE " +
            "(:termo IS NULL OR :termo = '' OR LOWER(rc.nomeRegra) LIKE LOWER(CONCAT('%', :termo, '%'))) AND " +
            "(:tipoLimite IS NULL OR :tipoLimite = '' OR rc.inTipoLimite = :tipoLimite)")
    Page<RegrasConjuntas> pesquisarPaginado(@Param("termo") String termo,
            @Param("tipoLimite") String tipoLimite,
            Pageable pageable);

    boolean existsByNomeRegraAndIdRegraConjuntaNot(String nomeRegra, Integer idRegraConjunta);
}