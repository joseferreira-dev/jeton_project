package br.com.cremepe.jeton.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import br.com.cremepe.jeton.domain.AtividadeConselhal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface AtividadeConselhalRepository extends JpaRepository<AtividadeConselhal, Integer> {

    List<AtividadeConselhal> findTop5ByOrderByDataHoraRegistroDesc();

    List<AtividadeConselhal> findByComprovanteIdComprovante(Integer idComprovante);

    long countByGestaoIdGestaoAndConselheiroIdPessoa(Integer idGestao, Integer idPessoa);

    long countByRegraIdRegra(Integer idRegra);

    long countByInSituacao(String inSituacao);

    long countByComprovanteIdComprovante(Integer idComprovante);

    @Query("SELECT COUNT(a) FROM AtividadeConselhal a WHERE MONTH(a.dataHoraAtividade) = :mes AND YEAR(a.dataHoraAtividade) = :ano")
    long countAtividadesDoMes(@Param("mes") Integer mes, @Param("ano") Integer ano);

    long countByConselheiroIdPessoaAndInSituacao(Integer idPessoa, String inSituacao);

    long countByConselheiroIdPessoa(Integer idPessoa);

    @Query("SELECT COUNT(a) FROM AtividadeConselhal a WHERE a.gestao.idGestao = :idGestao " +
            "AND MONTH(a.dataHoraRegistro) = :mes AND YEAR(a.dataHoraRegistro) = :ano " +
            "AND a.inSituacao = 'F'")
    long countAtividadesFechadasNoPeriodo(@Param("idGestao") Integer idGestao,
            @Param("mes") Integer mes,
            @Param("ano") Integer ano);

    @Query("SELECT a FROM AtividadeConselhal a WHERE " +
            "(LOWER(a.conselheiro.pessoa.nome) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
            "LOWER(a.regra.nomeRegra) LIKE LOWER(CONCAT('%', :termo, '%'))) AND " +
            "(:situacao IS NULL OR :situacao = '' OR a.inSituacao = :situacao) AND " +
            "(:turno IS NULL OR :turno = '' OR a.inTurno = :turno) AND " +
            "(:comprovanteFiltro IS NULL OR :comprovanteFiltro = '' OR " +
            " (:comprovanteFiltro = 'S' AND a.comprovante IS NOT NULL) OR " +
            " (:comprovanteFiltro = 'N' AND a.comprovante IS NULL)) AND " +
            "(CAST(:dataInicio AS date) IS NULL OR CAST(a.dataHoraAtividade AS date) >= CAST(:dataInicio AS date)) AND "
            +
            "(CAST(:dataFim AS date) IS NULL OR CAST(a.dataHoraAtividade AS date) <= CAST(:dataFim AS date))")
    Page<AtividadeConselhal> findAllByFilters(
            @Param("termo") String termo,
            @Param("situacao") String situacao,
            @Param("turno") String turno,
            @Param("comprovanteFiltro") String comprovanteFiltro,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim,
            Pageable pageable);

    Page<AtividadeConselhal> findByConselheiroIdPessoa(Integer idPessoa, Pageable pageable);

    @Query("SELECT a FROM AtividadeConselhal a WHERE a.conselheiro.idPessoa = :idPessoa AND a.inComputada = 'S' AND MONTH(a.dataHoraRegistro) = :mes AND YEAR(a.dataHoraRegistro) = :ano")
    List<AtividadeConselhal> findComputadasPorConselheiroEMes(@Param("idPessoa") Integer idPessoa,
            @Param("mes") Integer mes, @Param("ano") Integer ano);

    @Modifying
    @Transactional
    @Query("UPDATE AtividadeConselhal a SET a.inComputada = 'S' WHERE a.idAtividade IN :ids")
    void marcarComoComputadaEmLote(@Param("ids") List<Integer> ids);

    @Modifying
    @Transactional
    @Query("UPDATE AtividadeConselhal a SET a.comprovante = NULL WHERE a.idAtividade = :id")
    void desvincularComprovante(@Param("id") Integer idAtividade);

    @Query("SELECT a FROM AtividadeConselhal a WHERE a.conselheiro.idPessoa = :idPessoa " +
            "AND a.inSituacao = 'C' AND a.inComputada = 'N' " +
            "AND MONTH(a.dataHoraRegistro) = :mes AND YEAR(a.dataHoraRegistro) = :ano")
    List<AtividadeConselhal> findHomologadasParaCalculo(
            @Param("idPessoa") Integer idPessoa,
            @Param("mes") Integer mes,
            @Param("ano") Integer ano);

    @Query("SELECT COUNT(a) FROM AtividadeConselhal a WHERE a.gestao.idGestao = :idGestao " +
            "AND MONTH(a.dataHoraRegistro) = :mes AND YEAR(a.dataHoraRegistro) = :ano " +
            "AND a.inSituacao = 'P'")
    long countAtividadesPendentesNoMes(
            @Param("idGestao") Integer idGestao,
            @Param("mes") Integer mes,
            @Param("ano") Integer ano);

    @Query("SELECT COUNT(a) FROM AtividadeConselhal a WHERE a.gestao.idGestao = :idGestao " +
            "AND a.dataHoraRegistro < :inicioDoMes " +
            "AND a.inSituacao != 'F'")
    long countAtividadesAnterioresNaoFechadas(
            @Param("idGestao") Integer idGestao,
            @Param("inicioDoMes") LocalDateTime inicioDoMes);

    @Query("SELECT a FROM AtividadeConselhal a WHERE a.gestao.idGestao = :idGestao " +
            "AND MONTH(a.dataHoraRegistro) = :mes AND YEAR(a.dataHoraRegistro) = :ano " +
            "AND a.inComputada = 'S'")
    List<AtividadeConselhal> findComputadasDoMes(
            @Param("idGestao") Integer idGestao,
            @Param("mes") Integer mes,
            @Param("ano") Integer ano);

    @Modifying
    @Transactional
    @Query("UPDATE AtividadeConselhal a SET a.inSituacao = 'F' WHERE a.gestao.idGestao = :idGestao " +
            "AND MONTH(a.dataHoraRegistro) = :mes AND YEAR(a.dataHoraRegistro) = :ano " +
            "AND a.inSituacao = 'C' AND a.inComputada = 'S'")
    int fecharAtividadesEmFolha(
            @Param("idGestao") Integer idGestao,
            @Param("mes") Integer mes,
            @Param("ano") Integer ano);

    @Modifying
    @Transactional
    @Query("UPDATE AtividadeConselhal a SET a.inComputada = 'N' WHERE a.conselheiro.idPessoa = :idPessoa " +
            "AND a.gestao.idGestao = :idGestao AND MONTH(a.dataHoraRegistro) = :mes AND YEAR(a.dataHoraRegistro) = :ano")
    void reverterAtividadesComputadas(
            @Param("idPessoa") Integer idPessoa,
            @Param("idGestao") Integer idGestao,
            @Param("mes") Integer mes,
            @Param("ano") Integer ano);

    @Query("SELECT SUM(a.qtdAtividade * a.regra.pontos) FROM AtividadeConselhal a " +
            "WHERE a.conselheiro.idPessoa = :idPessoa " +
            "AND a.inSituacao = 'C' AND a.inComputada = 'N'")
    Integer sumPontosAtividadesValidadasNaoComputadas(@Param("idPessoa") Integer idPessoa);

    @Query("SELECT a FROM AtividadeConselhal a WHERE a.conselheiro.idPessoa = :idPessoa " +
            "AND (:dataInicio IS NULL OR a.dataHoraAtividade >= :dataInicio) " +
            "AND (:dataFim IS NULL OR a.dataHoraAtividade <= :dataFim) " +
            "AND (:situacao IS NULL OR :situacao = '' OR a.inSituacao = :situacao) " +
            "AND (:turno IS NULL OR :turno = '' OR a.inTurno = :turno) " +
            "AND (:comprovanteFiltro IS NULL OR :comprovanteFiltro = '' OR " +
            "    (:comprovanteFiltro = 'S' AND a.comprovante IS NOT NULL) OR " +
            "    (:comprovanteFiltro = 'N' AND a.comprovante IS NULL)) " +
            "AND (:termo IS NULL OR :termo = '' OR LOWER(a.regra.nomeRegra) LIKE LOWER(CONCAT('%', :termo, '%')))")
    Page<AtividadeConselhal> findByConselheiroAndFiltros(
            @Param("idPessoa") Integer idPessoa,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim,
            @Param("situacao") String situacao,
            @Param("turno") String turno,
            @Param("comprovanteFiltro") String comprovanteFiltro,
            @Param("termo") String termo,
            Pageable pageable);

    @Query("SELECT SUM(a.qtdAtividade * a.regra.pontos) FROM AtividadeConselhal a " +
            "WHERE a.conselheiro.idPessoa = :idPessoa " +
            "AND MONTH(a.dataHoraAtividade) = :mes " +
            "AND YEAR(a.dataHoraAtividade) = :ano " +
            "AND a.inSituacao = 'C'")
    Integer sumPontosAtividadesValidadasDoMes(@Param("idPessoa") Integer idPessoa,
            @Param("mes") Integer mes,
            @Param("ano") Integer ano);
}