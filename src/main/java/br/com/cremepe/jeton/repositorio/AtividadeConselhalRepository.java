package br.com.cremepe.jeton.repositorio;

import br.com.cremepe.jeton.dominio.AtividadeConselhal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AtividadeConselhalRepository extends JpaRepository<AtividadeConselhal, Integer> {

    List<AtividadeConselhal> findByConselheiroIdPessoaOrderByDataHoraAtividadeDesc(Integer idConselheiro);

    List<AtividadeConselhal> findByDataHoraAtividadeBetween(LocalDateTime inicio, LocalDateTime fim);

    long countByGestaoIdGestaoAndConselheiroIdPessoa(Integer idGestao, Integer idPessoa);

    // Conta quantas atividades usam uma determinada regra (Trava de segurança)
    long countByRegraIdRegra(Integer idRegra);

    // NOVO: Pesquisa inteligente com paginação
    @Query("SELECT a FROM AtividadeConselhal a WHERE " +
            "(LOWER(a.conselheiro.pessoa.nome) LIKE LOWER(CONCAT('%', :termo, '%'))) AND " +
            "(:situacao IS NULL OR :situacao = '' OR a.inSituacao = :situacao) AND " +
            "(:turno IS NULL OR :turno = '' OR a.inTurno = :turno)")
    Page<AtividadeConselhal> pesquisarPaginado(@Param("termo") String termo,
            @Param("situacao") String situacao,
            @Param("turno") String turno,
            Pageable pageable);

    // Procura atividades pendentes ('P') num determinado mês e ano que já tenham
    // comprovativo
    @Query("SELECT a FROM AtividadeConselhal a WHERE a.conselheiro.idPessoa = :idPessoa " +
            "AND a.inSituacao = 'P' AND a.comprovante IS NOT NULL " +
            "AND MONTH(a.dataHoraAtividade) = :mes AND YEAR(a.dataHoraAtividade) = :ano")
    List<AtividadeConselhal> findPendentesParaProcessamento(
            @Param("idPessoa") Integer idPessoa,
            @Param("mes") Integer mes,
            @Param("ano") Integer ano);

    // =========================================================================================
    // NOVO: Validador de Teto por Turno
    // Calcula a soma de pontos (pontos da regra * quantidade) para um conselheiro,
    // num dia e turno específicos
    // Utiliza funções nativas de YEAR, MONTH e DAY do JPQL para comparar com a data
    // recebida (LocalDate)
    // =========================================================================================
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

}