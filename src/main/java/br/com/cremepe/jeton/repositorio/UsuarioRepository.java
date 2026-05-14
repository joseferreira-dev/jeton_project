package br.com.cremepe.jeton.repositorio;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.cremepe.jeton.dominio.Usuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByPessoaCpf(String cpf);

    // Query atualizada para incluir filtro de situação opcional
    @Query("SELECT u FROM Usuario u WHERE " +
           "(LOWER(u.pessoa.nome) LIKE LOWER(CONCAT('%', :termo, '%')) OR u.pessoa.cpf LIKE CONCAT('%', :cpf, '%')) " +
           "AND (:situacao IS NULL OR :situacao = '' OR u.inSituacao = :situacao)")
    Page<Usuario> pesquisarPaginado(@Param("termo") String termo, 
                                    @Param("cpf") String cpf, 
                                    @Param("situacao") String situacao, 
                                    Pageable pageable);
}