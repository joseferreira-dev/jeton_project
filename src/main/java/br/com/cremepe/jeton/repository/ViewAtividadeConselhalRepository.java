package br.com.cremepe.jeton.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.cremepe.jeton.domain.ViewAtividadeConselhal;

import java.time.LocalDateTime;
import java.util.List;

public interface ViewAtividadeConselhalRepository extends JpaRepository<ViewAtividadeConselhal, Integer> {

    List<ViewAtividadeConselhal> findByIdPessoaOrderByDataHoraAtividadeDesc(Integer idPessoa);

    List<ViewAtividadeConselhal> findByIdGestao(Integer idGestao);

    @Query("SELECT v FROM ViewAtividadeConselhal v WHERE v.idGestao = :idGestao " +
            "AND (:idConselheiro IS NULL OR v.idPessoa = :idConselheiro) " +
            "AND (:inicio IS NULL OR v.dataHoraAtividade >= :inicio) " +
            "AND (:fim IS NULL OR v.dataHoraAtividade <= :fim) " +
            "ORDER BY v.dataHoraAtividade DESC")
    List<ViewAtividadeConselhal> buscarParaRelatorio(
            @Param("idGestao") Integer idGestao,
            @Param("idConselheiro") Integer idConselheiro,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);
}