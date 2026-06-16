package br.com.cremepe.jeton.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.cremepe.jeton.domain.LogJeton;

import java.time.LocalDateTime;
import java.util.List;

public interface LogJetonRepository extends JpaRepository<LogJeton, Integer> {

    List<LogJeton> findByUsuarioIdUsuarioPessoaOrderByDataHoraLogDesc(Integer idUsuario);

    List<LogJeton> findByNomeTabelaAndDataHoraLogBetween(String nomeTabela, LocalDateTime inicio, LocalDateTime fim);

    @Query("SELECT l FROM LogJeton l JOIN FETCH l.usuario u JOIN FETCH u.pessoa WHERE " +
            "(:nomeTabela IS NULL OR l.nomeTabela = :nomeTabela) AND " +
            "(:dataInicio IS NULL OR l.dataHoraLog >= :dataInicio) AND " +
            "(:dataFim IS NULL OR l.dataHoraLog <= :dataFim) AND " +
            "(:termo IS NULL OR :termo = '' OR " +
            "LOWER(u.pessoa.nome) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
            "LOWER(l.textoLog) LIKE LOWER(CONCAT('%', :termo, '%')))")
    Page<LogJeton> pesquisarComFiltros(@Param("nomeTabela") String nomeTabela,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim,
            @Param("termo") String termo,
            Pageable pageable);
}