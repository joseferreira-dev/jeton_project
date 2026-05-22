package br.com.cremepe.jeton.repositorio;

import br.com.cremepe.jeton.dominio.ViewAtividadeConselhal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ViewAtividadeConselhalRepository extends JpaRepository<ViewAtividadeConselhal, Integer> {

    List<ViewAtividadeConselhal> findByIdPessoaOrderByDataHoraAtividadeDesc(Integer idPessoa);

    List<ViewAtividadeConselhal> findByIdGestao(Integer idGestao);

    // NOVA QUERY: Permite filtros opcionais de Conselheiro, Data Início e Data Fim
    @Query("SELECT v FROM ViewAtividadeConselhal v WHERE v.idGestao = :idGestao " +
            "AND (:idConselheiro IS NULL OR v.idPessoa = :idConselheiro) " +
            "AND (CAST(:dataInicio AS date) IS NULL OR CAST(v.dataHoraAtividade AS date) >= :dataInicio) " +
            "AND (CAST(:dataFim AS date) IS NULL OR CAST(v.dataHoraAtividade AS date) <= :dataFim) " +
            "ORDER BY v.dataHoraAtividade DESC")
    List<ViewAtividadeConselhal> buscarParaRelatorioDynamic(
            @Param("idGestao") Integer idGestao,
            @Param("idConselheiro") Integer idConselheiro,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim);
}