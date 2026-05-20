package br.com.cremepe.jeton.repositorio;

import br.com.cremepe.jeton.dominio.GestaoConselheiro;
import br.com.cremepe.jeton.dominio.GestaoConselheiroId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GestaoConselheiroRepository extends JpaRepository<GestaoConselheiro, GestaoConselheiroId> {

    List<GestaoConselheiro> findByIdIdGestao(Integer idGestao);

    List<GestaoConselheiro> findByIdIdPessoa(Integer idPessoa);

    List<GestaoConselheiro> findByIdIdGestaoAndInSituacao(Integer idGestao, String inSituacao);

    List<GestaoConselheiro> findByGestaoIdGestao(Integer idGestao);

    @Query("SELECT gc FROM GestaoConselheiro gc WHERE " +
            "(LOWER(gc.gestao.nomeGestao) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
            "LOWER(gc.conselheiro.pessoa.nome) LIKE LOWER(CONCAT('%', :termo, '%'))) AND " +
            "(:situacao IS NULL OR :situacao = '' OR gc.inSituacao = :situacao)")
    Page<GestaoConselheiro> pesquisarPaginado(@Param("termo") String termo,
            @Param("situacao") String situacao,
            Pageable pageable);
}