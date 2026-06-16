package br.com.cremepe.jeton.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.cremepe.jeton.domain.Conselheiro;

import java.util.Optional;

public interface ConselheiroRepository extends JpaRepository<Conselheiro, Integer> {

    Optional<Conselheiro> findByCrm(Integer crm);

    long countByInSituacao(String inSituacao);

    @Query("SELECT c FROM Conselheiro c WHERE " +
            "(LOWER(c.pessoa.nome) LIKE LOWER(CONCAT('%', :termo, '%')) OR CAST(c.crm AS string) LIKE CONCAT('%', :termo, '%')) "
            +
            "AND (:situacao IS NULL OR :situacao = '' OR c.inSituacao = :situacao)")
    Page<Conselheiro> findAllByFilters(@Param("termo") String termo,
            @Param("situacao") String situacao,
            Pageable pageable);
}