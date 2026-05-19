package br.com.cremepe.jeton.repositorio;

import br.com.cremepe.jeton.dominio.Portaria;
import br.com.cremepe.jeton.dominio.Regras;
import br.com.cremepe.jeton.dominio.Resolucao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public interface RegrasRepository extends JpaRepository<Regras, Integer> {

    // Procura todas as regras não revogadas que pertencem a uma Resolução específica
    List<Regras> findByResolucaoIdResolucaoAndInRevogado(Integer idResolucao, String inRevogado);

    // Procura regras pelo identificador se são atividades judicantes ou não
    List<Regras> findByInJudicante(String inJudicante);

    List<Regras> findByInRevogado(String string);

    // 1. Traz apenas Resoluções que possuem regras ativas
    @Query("SELECT DISTINCT r.resolucao FROM Regras r WHERE r.resolucao IS NOT NULL AND r.inRevogado = 'N'")
    List<Resolucao> findResolucoesComRegras();

    // 2. Traz apenas Portarias que possuem regras ativas
    @Query("SELECT DISTINCT r.portaria FROM Regras r WHERE r.portaria IS NOT NULL AND r.inRevogado = 'N'")
    List<Portaria> findPortariasComRegras();

    // 3. Traz Portarias que partilham regras com uma Resolução específica
    @Query("SELECT DISTINCT r.portaria FROM Regras r WHERE r.resolucao.idResolucao = :idResolucao AND r.portaria IS NOT NULL AND r.inRevogado = 'N'")
    List<Portaria> findPortariasCompativeis(@Param("idResolucao") Integer idResolucao);

    // 4. Traz Resoluções que partilham regras com uma Portaria específica
    @Query("SELECT DISTINCT r.resolucao FROM Regras r WHERE r.portaria.idPortaria = :idPortaria AND r.resolucao IS NOT NULL AND r.inRevogado = 'N'")
    List<Resolucao> findResolucoesCompativeis(@Param("idPortaria") Integer idPortaria);

    // 5. Traz as regras que batem EXATAMENTE com a combinação selecionada
    @Query("SELECT r FROM Regras r WHERE r.inRevogado = 'N' AND " +
           "((:idResolucao IS NULL AND r.resolucao IS NULL) OR (r.resolucao.idResolucao = :idResolucao)) AND " +
           "((:idPortaria IS NULL AND r.portaria IS NULL) OR (r.portaria.idPortaria = :idPortaria))")
    List<Regras> findRegrasExatas(@Param("idResolucao") Integer idResolucao, @Param("idPortaria") Integer idPortaria);

    // Conta regras vinculadas a uma Portaria
    long countByPortariaIdPortaria(Integer idPortaria);
    
    // Conta regras vinculadas a uma Resolução
    long countByResolucaoIdResolucao(Integer idResolucao);

    @Query("SELECT r FROM Regras r WHERE " +
           "(:termo IS NULL OR :termo = '' OR LOWER(r.nomeRegra) LIKE LOWER(CONCAT('%', :termo, '%'))) AND " +
           "(:situacao IS NULL OR :situacao = '' OR r.inRevogado = :situacao)")
    Page<Regras> pesquisarPaginado(@Param("termo") String termo, @Param("situacao") String situacao, Pageable pageable);

    @Query("SELECT r FROM Regras r WHERE " +
       "(:termo IS NULL OR :termo = '' OR LOWER(r.nomeRegra) LIKE LOWER(CONCAT('%', :termo, '%'))) AND " +
       "(:situacao IS NULL OR :situacao = '' OR r.inRevogado = :situacao) AND " +
       "(:judicante IS NULL OR :judicante = '' OR r.inJudicante = :judicante)")
    Page<Regras> pesquisarPaginado(@Param("termo") String termo, 
                                @Param("situacao") String situacao, 
                                @Param("judicante") String judicante, 
                                Pageable pageable);

    @Modifying
    @Query("UPDATE Regras r SET r.inRevogado = 'S' WHERE r.resolucao.idResolucao = :idResolucao")
    void revogarRegrasPorResolucao(@Param("idResolucao") Integer idResolucao);

    @Modifying
    @Query("UPDATE Regras r SET r.inRevogado = 'S' WHERE r.portaria.idPortaria = :idPortaria")
    void revogarRegrasPorPortaria(@Param("idPortaria") Integer idPortaria);

    List<Regras> findByNomeRegraAndResolucaoIdResolucao(String nomeRegra, Integer idResolucao);
    
    List<Regras> findByNomeRegraAndPortariaIdPortaria(String nomeRegra, Integer idPortaria);
}