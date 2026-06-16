package br.com.cremepe.jeton.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.cremepe.jeton.domain.RegrasConjuntas;

public interface RegrasConjuntasRepository extends JpaRepository<RegrasConjuntas, Integer> {

    @Query("SELECT rc FROM RegrasConjuntas rc WHERE " +
            "(:termo IS NULL OR :termo = '' OR LOWER(rc.nomeRegra) LIKE LOWER(CONCAT('%', :termo, '%'))) AND " +
            "(:tipoLimite IS NULL OR :tipoLimite = '' OR rc.inTipoLimite = :tipoLimite)")
    Page<RegrasConjuntas> findAllByFilters(@Param("termo") String termo,
            @Param("tipoLimite") String tipoLimite,
            Pageable pageable);

    boolean existsByNomeRegraAndIdRegraConjuntaNot(String nomeRegra, Integer idRegraConjunta);
}