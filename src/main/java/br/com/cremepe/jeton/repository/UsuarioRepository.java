package br.com.cremepe.jeton.repository;

import br.com.cremepe.jeton.dominio.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByPessoaCpf(String cpf);

    @Query("SELECT COUNT(u) > 0 FROM Usuario u WHERE u.pessoa.cpf = :cpf AND u.idUsuarioPessoa != :id")
    boolean existsByPessoaCpfAndIdUsuarioPessoaNot(@Param("cpf") String cpf, @Param("id") Integer id);

    @Query("SELECT u FROM Usuario u WHERE " +
            "(LOWER(u.pessoa.nome) LIKE LOWER(CONCAT('%', :termo, '%')) OR u.pessoa.cpf LIKE CONCAT('%', :cpf, '%')) " +
            "AND (:situacao IS NULL OR :situacao = '' OR u.inSituacao = :situacao)")
    Page<Usuario> pesquisarPaginado(@Param("termo") String termo,
            @Param("cpf") String cpf,
            @Param("situacao") String situacao,
            Pageable pageable);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM conselheiro WHERE idPessoa = :id", nativeQuery = true)
    void deletarConselheiroNativo(@Param("id") Integer id);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM usuario WHERE idUsuarioPessoa = :id", nativeQuery = true)
    void deletarUsuarioNativo(@Param("id") Integer id);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM pessoa WHERE idPessoa = :id", nativeQuery = true)
    void deletarPessoaNativa(@Param("id") Integer id);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM usuario_acesso WHERE idUsuarioPessoa = :id", nativeQuery = true)
    void deletarPermissoesNativo(@Param("id") Integer id);
}