package br.com.cremepe.jeton.repositorio;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.cremepe.jeton.dominio.Conselheiro;

@Repository
public interface ConselheiroRepository extends JpaRepository<Conselheiro, Integer> {

    Optional<Conselheiro> findByCrm(Integer crm);

    long countByInSituacao(String inSituacao);

    @Query("SELECT c FROM Conselheiro c WHERE " +
            "(LOWER(c.pessoa.nome) LIKE LOWER(CONCAT('%', :termo, '%')) OR CAST(c.crm AS string) LIKE CONCAT('%', :termo, '%')) "
            +
            "AND (:situacao IS NULL OR :situacao = '' OR c.inSituacao = :situacao)")
    Page<Conselheiro> pesquisarPaginado(@Param("termo") String termo,
            @Param("situacao") String situacao,
            Pageable pageable);

    @Modifying
    @Query(value = "DELETE FROM usuario WHERE idUsuarioPessoa = :id", nativeQuery = true)
    void deletarUsuarioNativo(@Param("id") Integer id);

    @Modifying
    @Query(value = "DELETE FROM conselheiro WHERE idPessoa = :id", nativeQuery = true)
    void deletarConselheiroNativo(@Param("id") Integer id);

    @Modifying
    @Query(value = "DELETE FROM pessoa WHERE idPessoa = :id", nativeQuery = true)
    void deletarPessoaNativa(@Param("id") Integer id);
}