package br.com.cremepe.jeton.repositorio;

import br.com.cremepe.jeton.dominio.AtividadeConselhal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AtividadeConselhalRepository extends JpaRepository<AtividadeConselhal, Integer> {

    List<AtividadeConselhal> findByConselheiroIdPessoaOrderByDataHoraAtividadeDesc(Integer idConselheiro);

    List<AtividadeConselhal> findByDataHoraAtividadeBetween(LocalDateTime inicio, LocalDateTime fim);
    
    long countByGestaoIdGestaoAndConselheiroIdPessoa(Integer idGestao, Integer idPessoa);

    // NOVO: Pesquisa inteligente com paginação
    @Query("SELECT a FROM AtividadeConselhal a WHERE " +
           "(LOWER(a.conselheiro.pessoa.nome) LIKE LOWER(CONCAT('%', :termo, '%'))) AND " +
           "(:situacao IS NULL OR :situacao = '' OR a.inSituacao = :situacao) AND " +
           "(:turno IS NULL OR :turno = '' OR a.inTurno = :turno)")
    Page<AtividadeConselhal> pesquisarPaginado(@Param("termo") String termo, 
                                               @Param("situacao") String situacao, 
                                               @Param("turno") String turno,
                                               Pageable pageable);
}