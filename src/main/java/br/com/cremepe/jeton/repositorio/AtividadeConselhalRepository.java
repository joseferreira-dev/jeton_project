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

    long countByInSituacao(String inSituacao);

    List<AtividadeConselhal> findTop5ByOrderByDataHoraRegistroDesc();

    // Método para contar todas as atividades lançadas no mês e ano correntes
    @Query("SELECT COUNT(a) FROM AtividadeConselhal a WHERE MONTH(a.dataHoraAtividade) = :mes AND YEAR(a.dataHoraAtividade) = :ano")
    long countAtividadesDoMes(@Param("mes") Integer mes, @Param("ano") Integer ano);

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

    // @Query("SELECT a FROM AtividadeConselhal a WHERE a.conselheiro.idPessoa =
    // :idPessoa " +
    // "AND a.inSituacao = 'P' AND a.comprovante IS NOT NULL " +
    // "AND MONTH(a.dataHoraAtividade) = :mes AND YEAR(a.dataHoraAtividade) = :ano")
    // List<AtividadeConselhal> findPendentesParaProcessamento(
    // @Param("idPessoa") Integer idPessoa,
    // @Param("mes") Integer mes,
    // @Param("ano") Integer ano);

    // @Query("SELECT SUM(a.regra.pontos * a.qtdAtividade) FROM AtividadeConselhal a
    // " +
    // "WHERE a.conselheiro.idPessoa = :idPessoa " +
    // "AND YEAR(a.dataHoraAtividade) = YEAR(:data) " +
    // "AND MONTH(a.dataHoraAtividade) = MONTH(:data) " +
    // "AND DAY(a.dataHoraAtividade) = DAY(:data) " +
    // "AND a.inTurno = :turno")
    // Integer sumPontosPorConselheiroDiaETurno(
    // @Param("idPessoa") Integer idPessoa,
    // @Param("data") LocalDate data,
    // @Param("turno") String turno);

    // @Query("SELECT a FROM AtividadeConselhal a WHERE a.gestao.idGestao =
    // :idGestao " +
    // "AND (a.inSituacao = 'P' OR a.comprovante IS NULL) " +
    // "AND MONTH(a.dataHoraAtividade) = :mes AND YEAR(a.dataHoraAtividade) = :ano")
    // List<AtividadeConselhal> findAtividadesInconsistentesDoMes(
    // @Param("idGestao") Integer idGestao,
    // @Param("mes") Integer mes,
    // @Param("ano") Integer ano);

    // @Query("SELECT a FROM AtividadeConselhal a WHERE a.conselheiro.idPessoa =
    // :idPessoa " +
    // "AND a.inSituacao = 'C' AND a.inComputada = 'N' " +
    // "AND MONTH(a.dataHoraAtividade) = :mes AND YEAR(a.dataHoraAtividade) = :ano")
    // List<AtividadeConselhal> findHomologadasParaCalculo(
    // @Param("idPessoa") Integer idPessoa,
    // @Param("mes") Integer mes,
    // @Param("ano") Integer ano);

    // @Modifying
    // @Transactional
    // @Query("UPDATE AtividadeConselhal a SET a.inSituacao = 'F' WHERE
    // a.gestao.idGestao = :idGestao " +
    // "AND MONTH(a.dataHoraAtividade) = :mes AND YEAR(a.dataHoraAtividade) = :ano "
    // +
    // "AND a.inSituacao = 'C' AND a.inComputada = 'S'")
    // int fecharAtividadesEmFolha(
    // @Param("idGestao") Integer idGestao,
    // @Param("mes") Integer mes,
    // @Param("ano") Integer ano);

    long countByComprovanteIdComprovante(Integer idComprovante);

    @Modifying
    @Query("UPDATE AtividadeConselhal a SET a.comprovante = null WHERE a.idAtividade = :id")
    void removerVinculoComprovante(@Param("id") Integer id);

    // @Modifying
    // @Query("UPDATE AtividadeConselhal a SET a.inComputada = 'N' WHERE
    // a.conselheiro.idPessoa = :idPessoa " +
    // "AND a.gestao.idGestao = :idGestao AND MONTH(a.dataHoraAtividade) = :mes AND
    // YEAR(a.dataHoraAtividade) = :ano")
    // void reverterAtividadesComputadas(
    // @Param("idPessoa") Integer idPessoa,
    // @Param("idGestao") Integer idGestao,
    // @Param("mes") Integer mes,
    // @Param("ano") Integer ano);

    @Modifying
    @Transactional
    @Query("UPDATE AtividadeConselhal a SET a.inComputada = 'S' WHERE a.idAtividade IN :ids")
    void marcarComoComputadaEmLote(@Param("ids") List<Integer> ids);

    // @Query("SELECT a FROM AtividadeConselhal a WHERE a.gestao.idGestao =
    // :idGestao " +
    // "AND MONTH(a.dataHoraAtividade) = :mes AND YEAR(a.dataHoraAtividade) = :ano "
    // +
    // "AND a.inComputada = 'S'")
    // List<AtividadeConselhal> findComputadasDoMes(
    // @Param("idGestao") Integer idGestao,
    // @Param("mes") Integer mes,
    // @Param("ano") Integer ano);

    // TRAVA 1: Verifica se há atividades pendentes (não validadas) no mês do
    // cálculo
    // @Query("SELECT COUNT(a) FROM AtividadeConselhal a WHERE a.gestao.idGestao =
    // :idGestao " +
    // "AND MONTH(a.dataHoraAtividade) = :mes AND YEAR(a.dataHoraAtividade) = :ano "
    // +
    // "AND a.inSituacao = 'P'")
    // long contarAtividadesPendentesNoMes(
    // @Param("idGestao") Integer idGestao,
    // @Param("mes") Integer mes,
    // @Param("ano") Integer ano);

    // TRAVA 2: Verifica se há atividades de meses anteriores que não estão com
    // status F (Fechada)
    // @Query("SELECT COUNT(a) FROM AtividadeConselhal a WHERE a.gestao.idGestao =
    // :idGestao " +
    // "AND a.dataHoraAtividade < :inicioDoMes " +
    // "AND a.inSituacao != 'F'")
    // long contarAtividadesAnterioresNaoFechadas(
    // @Param("idGestao") Integer idGestao,
    // @Param("inicioDoMes") LocalDateTime inicioDoMes);

    // =========================================================================
    // MÉTODOS ALTERADOS PARA USAR dataHoraRegistro (competência financeira)
    // =========================================================================

    /**
     * ALTERADO: Agora considera o MÊS/ANO da DATA DE REGISTRO, não da atividade.
     * Busca atividades homologadas (C) e não computadas para cálculo do Jeton.
     */
    @Query("SELECT a FROM AtividadeConselhal a WHERE a.conselheiro.idPessoa = :idPessoa " +
            "AND a.inSituacao = 'C' AND a.inComputada = 'N' " +
            "AND MONTH(a.dataHoraRegistro) = :mes AND YEAR(a.dataHoraRegistro) = :ano")
    List<AtividadeConselhal> findHomologadasParaCalculo(
            @Param("idPessoa") Integer idPessoa,
            @Param("mes") Integer mes,
            @Param("ano") Integer ano);

    /**
     * ALTERADO: Conta atividades pendentes (P) no mês/ano da DATA DE REGISTRO.
     * Usado como trava antes do processamento.
     */
    @Query("SELECT COUNT(a) FROM AtividadeConselhal a WHERE a.gestao.idGestao = :idGestao " +
            "AND MONTH(a.dataHoraRegistro) = :mes AND YEAR(a.dataHoraRegistro) = :ano " +
            "AND a.inSituacao = 'P'")
    long contarAtividadesPendentesNoMes(
            @Param("idGestao") Integer idGestao,
            @Param("mes") Integer mes,
            @Param("ano") Integer ano);

    /**
     * ALTERADO: Conta atividades de meses anteriores (baseado na DATA DE REGISTRO)
     * que ainda não estão fechadas (status != 'F'). Usado para garantir fechamento
     * cronológico.
     */
    @Query("SELECT COUNT(a) FROM AtividadeConselhal a WHERE a.gestao.idGestao = :idGestao " +
            "AND a.dataHoraRegistro < :inicioDoMes " +
            "AND a.inSituacao != 'F'")
    long contarAtividadesAnterioresNaoFechadas(
            @Param("idGestao") Integer idGestao,
            @Param("inicioDoMes") LocalDateTime inicioDoMes);

    /**
     * ALTERADO: Busca atividades que já foram computadas (inComputada = 'S') no
     * mês/ano da DATA DE REGISTRO.
     * Usado durante o estorno automático.
     */
    @Query("SELECT a FROM AtividadeConselhal a WHERE a.gestao.idGestao = :idGestao " +
            "AND MONTH(a.dataHoraRegistro) = :mes AND YEAR(a.dataHoraRegistro) = :ano " +
            "AND a.inComputada = 'S'")
    List<AtividadeConselhal> findComputadasDoMes(
            @Param("idGestao") Integer idGestao,
            @Param("mes") Integer mes,
            @Param("ano") Integer ano);

    /**
     * ALTERADO: Fecha as atividades em folha (status 'F') para aquelas que estão no
     * mês/ano da DATA DE REGISTRO,
     * estavam validadas (C) e já computadas (S).
     */
    @Modifying
    @Transactional
    @Query("UPDATE AtividadeConselhal a SET a.inSituacao = 'F' WHERE a.gestao.idGestao = :idGestao " +
            "AND MONTH(a.dataHoraRegistro) = :mes AND YEAR(a.dataHoraRegistro) = :ano " +
            "AND a.inSituacao = 'C' AND a.inComputada = 'S'")
    int fecharAtividadesEmFolha(
            @Param("idGestao") Integer idGestao,
            @Param("mes") Integer mes,
            @Param("ano") Integer ano);

    /**
     * ALTERADO: Reverte atividades computadas (inComputada = 'N') para um
     * conselheiro, gestão e competência (dataHoraRegistro).
     * Usado nos estornos.
     */
    @Modifying
    @Query("UPDATE AtividadeConselhal a SET a.inComputada = 'N' WHERE a.conselheiro.idPessoa = :idPessoa " +
            "AND a.gestao.idGestao = :idGestao AND MONTH(a.dataHoraRegistro) = :mes AND YEAR(a.dataHoraRegistro) = :ano")
    void reverterAtividadesComputadas(
            @Param("idPessoa") Integer idPessoa,
            @Param("idGestao") Integer idGestao,
            @Param("mes") Integer mes,
            @Param("ano") Integer ano);

    // =========================================================================
    // MÉTODOS QUE PERMANECEM USANDO dataHoraAtividade (regras de negócio local)
    // =========================================================================

    /**
     * NÃO ALTERADO: Soma pontos por conselheiro no mesmo DIA e TURNO (base na data
     * real).
     * Isso evita que uma atividade registrada fora de época extrapole o limite do
     * dia original.
     */
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

    /**
     * NÃO ALTERADO: Verifica se há atividades pendentes (P) com comprovante em
     * determinado mês/ano
     * (baseado na data real). Usado em processamento específico, mas mantido como
     * estava.
     */
    @Query("SELECT a FROM AtividadeConselhal a WHERE a.conselheiro.idPessoa = :idPessoa " +
            "AND a.inSituacao = 'P' AND a.comprovante IS NOT NULL " +
            "AND MONTH(a.dataHoraAtividade) = :mes AND YEAR(a.dataHoraAtividade) = :ano")
    List<AtividadeConselhal> findPendentesParaProcessamento(
            @Param("idPessoa") Integer idPessoa,
            @Param("mes") Integer mes,
            @Param("ano") Integer ano);
}