package br.com.cremepe.jeton.repositorio;

import br.com.cremepe.jeton.dominio.Resolucao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ResolucaoRepository extends JpaRepository<Resolucao, Integer> {

    // Retorna todas as resoluções ativas ("N" = Não revogado)
    List<Resolucao> findByInRevogado(String inRevogado);
    
    // Encontra uma resolução específica pelo seu número e ano
    Optional<Resolucao> findByNumeroAndAno(Integer numero, Integer ano);

    @Query("SELECT r FROM Resolucao r WHERE " +
           "(:termo IS NULL OR :termo = '' OR CAST(r.numero AS string) LIKE CONCAT('%', :termo, '%') OR CAST(r.ano AS string) LIKE CONCAT('%', :termo, '%') OR LOWER(r.ementa) LIKE LOWER(CONCAT('%', :termo, '%'))) AND " +
           "(:situacao IS NULL OR :situacao = '' OR r.inRevogado = :situacao)")
    Page<Resolucao> pesquisarPaginado(@Param("termo") String termo, @Param("situacao") String situacao, Pageable pageable);

    /**
     * Retorna as resoluções que estão vigentes em uma determinada data.
     * Filtra resoluções não revogadas cujo intervalo de vigência englobe a data informada.
     */
    @Query("SELECT r FROM Resolucao r WHERE r.dtInicioVigencia <= :dataBase " +
           "AND (r.dtFimVigencia IS NULL OR r.dtFimVigencia >= :dataBase) " +
           "AND r.inRevogado <> 'S' " +
           "ORDER BY r.dtInicioVigencia DESC, r.idResolucao DESC")
    List<Resolucao> findResoluesVigentesNaData(@Param("dataBase") LocalDate dataBase);
}