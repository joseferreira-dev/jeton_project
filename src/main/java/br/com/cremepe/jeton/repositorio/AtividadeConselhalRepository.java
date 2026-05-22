package br.com.cremepe.jeton.repositorio;

import br.com.cremepe.jeton.dominio.AtividadeConselhal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AtividadeConselhalRepository extends JpaRepository<AtividadeConselhal, Integer> {

    List<AtividadeConselhal> findByConselheiroIdPessoaOrderByDataHoraAtividadeDesc(Integer idConselheiro);

    List<AtividadeConselhal> findByDataHoraAtividadeBetween(LocalDateTime inicio, LocalDateTime fim);

    long countByGestaoIdGestaoAndConselheiroIdPessoa(Integer idGestao, Integer idPessoa);

    long countByRegraIdRegra(Integer idRegra);

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
    Page<AtividadeConselhal> pesquisarPaginado(
            @Param("termo") String termo,
            @Param("situacao") String situacao,
            @Param("turno") String turno,
            @Param("comprovanteFiltro") String comprovanteFiltro,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim,
            Pageable pageable);

    @Query("SELECT a FROM AtividadeConselhal a WHERE a.conselheiro.idPessoa = :idPessoa " +
            "AND a.inSituacao = 'P' AND a.comprovante IS NOT NULL " +
            "AND MONTH(a.dataHoraAtividade) = :mes AND YEAR(a.dataHoraAtividade) = :ano")
    List<AtividadeConselhal> findPendentesParaProcessamento(
            @Param("idPessoa") Integer idPessoa,
            @Param("mes") Integer mes,
            @Param("ano") Integer ano);

    @Query("SELECT SUM(a.regra.pontos * a.qtdAtividade) FROM AtividadeConselhal a " +
            "WHERE a.conselheiro.idPessoa = :idPessoa " +
            "AND YEAR(a.dataHoraAtividade) = YEAR(:data) " +
            "AND MONTH(a.dataHoraAtividade) = MONTH(:data) " +
            "AND DAY(a.dataHoraAtividade) = DAY(:data) " +
            "AND a.inTurno = :turno")
    Integer sumPontosPorConselheiroDiaETurno(
            @Param("idPessoa") Integer idPessoa,
            @Param("data") LocalDate data,
            @Param("turno") String turno);

    @Query("SELECT a FROM AtividadeConselhal a WHERE a.gestao.idGestao = :idGestao " +
            "AND (a.inSituacao = 'P' OR a.comprovante IS NULL) " +
            "AND MONTH(a.dataHoraAtividade) = :mes AND YEAR(a.dataHoraAtividade) = :ano")
    List<AtividadeConselhal> findAtividadesInconsistentesDoMes(
            @Param("idGestao") Integer idGestao,
            @Param("mes") Integer mes,
            @Param("ano") Integer ano);

    @Query("SELECT a FROM AtividadeConselhal a WHERE a.conselheiro.idPessoa = :idPessoa " +
            "AND a.inSituacao = 'C' AND a.inComputada = 'N' " +
            "AND MONTH(a.dataHoraAtividade) = :mes AND YEAR(a.dataHoraAtividade) = :ano")
    List<AtividadeConselhal> findHomologadasParaCalculo(
            @Param("idPessoa") Integer idPessoa,
            @Param("mes") Integer mes,
            @Param("ano") Integer ano);

    @Modifying
    @Transactional
    @Query("UPDATE AtividadeConselhal a SET a.inSituacao = 'F' WHERE a.gestao.idGestao = :idGestao " +
            "AND MONTH(a.dataHoraAtividade) = :mes AND YEAR(a.dataHoraAtividade) = :ano " +
            "AND a.inSituacao = 'C' AND a.inComputada = 'S'")
    int fecharAtividadesEmFolha(
            @Param("idGestao") Integer idGestao,
            @Param("mes") Integer mes,
            @Param("ano") Integer ano);

    long countByComprovanteIdComprovante(Integer idComprovante);

    @Modifying
    @Query("UPDATE AtividadeConselhal a SET a.comprovante = null WHERE a.idAtividade = :id")
    void removerVinculoComprovante(@Param("id") Integer id);

    @Modifying
    @Query("UPDATE AtividadeConselhal a SET a.inComputada = 'N' WHERE a.conselheiro.idPessoa = :idPessoa " +
            "AND a.gestao.idGestao = :idGestao AND MONTH(a.dataHoraAtividade) = :mes AND YEAR(a.dataHoraAtividade) = :ano")
    void reverterAtividadesComputadas(
            @Param("idPessoa") Integer idPessoa,
            @Param("idGestao") Integer idGestao,
            @Param("mes") Integer mes,
            @Param("ano") Integer ano);

    @Modifying
    @Transactional
    @Query("UPDATE AtividadeConselhal a SET a.inComputada = 'S' WHERE a.idAtividade IN :ids")
    void marcarComoComputadaEmLote(@Param("ids") List<Integer> ids);
}