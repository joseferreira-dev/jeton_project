package br.com.cremepe.jeton.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import br.com.cremepe.jeton.domain.Conselheiro;

import java.util.Optional;

@Repository
public interface ConselheiroRepository extends JpaRepository<Conselheiro, Integer> {

    Optional<Conselheiro> findByCrm(Integer crm);

    boolean existsByCrmAndIdPessoaNot(Integer crm, Integer idPessoa);

    long countByInSituacao(String inSituacao);

    @Query("SELECT c FROM Conselheiro c WHERE " +
            "(LOWER(c.pessoa.nome) LIKE LOWER(CONCAT('%', :termo, '%')) OR CAST(c.crm AS string) LIKE CONCAT('%', :termo, '%')) "
            +
            "AND (:situacao IS NULL OR :situacao = '' OR c.inSituacao = :situacao)")
    Page<Conselheiro> pesquisarPaginado(@Param("termo") String termo,
            @Param("situacao") String situacao,
            Pageable pageable);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM usuario WHERE idUsuarioPessoa = :id", nativeQuery = true)
    void deletarUsuarioNativo(@Param("id") Integer id);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM conselheiro WHERE idPessoa = :id", nativeQuery = true)
    void deletarConselheiroNativo(@Param("id") Integer id);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM pessoa WHERE idPessoa = :id", nativeQuery = true)
    void deletarPessoaNativa(@Param("id") Integer id);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM usuario_acesso WHERE idUsuarioPessoa = :id", nativeQuery = true)
    void deletarPermissoesNativo(@Param("id") Integer id);
}