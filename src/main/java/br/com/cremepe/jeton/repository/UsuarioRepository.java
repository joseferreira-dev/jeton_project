package br.com.cremepe.jeton.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import br.com.cremepe.jeton.domain.Usuario;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByPessoaCpf(String cpf);

    @Query("SELECT u FROM Usuario u WHERE " +
            "(LOWER(u.pessoa.nome) LIKE LOWER(CONCAT('%', :termo, '%')) OR u.pessoa.cpf LIKE CONCAT('%', :cpf, '%')) " +
            "AND (:situacao IS NULL OR :situacao = '' OR u.inSituacao = :situacao)")
    Page<Usuario> findAllByFilters(@Param("termo") String termo,
            @Param("cpf") String cpf,
            @Param("situacao") String situacao,
            Pageable pageable);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM conselheiro WHERE idPessoa = :id", nativeQuery = true)
    void deleteConselheiroNative(@Param("id") Integer id);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM usuario WHERE idUsuarioPessoa = :id", nativeQuery = true)
    void deleteUsuarioNative(@Param("id") Integer id);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM pessoa WHERE idPessoa = :id", nativeQuery = true)
    void deletePessoaNative(@Param("id") Integer id);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM usuario_acesso WHERE idUsuarioPessoa = :id", nativeQuery = true)
    void deletePermissoesNative(@Param("id") Integer id);
}