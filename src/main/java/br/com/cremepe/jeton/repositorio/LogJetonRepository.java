package br.com.cremepe.jeton.repositorio;

import br.com.cremepe.jeton.dominio.LogJeton;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogJetonRepository extends JpaRepository<LogJeton, Integer> {

    List<LogJeton> findByIdUsuarioOrderByDataHoraLogDesc(Integer idUsuario);

    List<LogJeton> findByNomeTabelaAndDataHoraLogBetween(String nomeTabela, LocalDateTime inicio, LocalDateTime fim);

    @Query("SELECT l FROM LogJeton l WHERE " +
            "(:nomeTabela IS NULL OR l.nomeTabela = :nomeTabela) AND " +
            "(:dataInicio IS NULL OR l.dataHoraLog >= :dataInicio) AND " +
            "(:dataFim IS NULL OR l.dataHoraLog <= :dataFim)")
    Page<LogJeton> pesquisarComFiltros(@Param("nomeTabela") String nomeTabela,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim,
            Pageable pageable);
}