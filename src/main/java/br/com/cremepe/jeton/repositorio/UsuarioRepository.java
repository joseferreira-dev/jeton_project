package br.com.cremepe.jeton.repositorio;

import br.com.cremepe.jeton.dominio.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositório para a entidade Usuario.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByPessoaCpf(String cpf);

    // Nova Query: Busca por Nome (ignorando caixa) OU por CPF
    @Query("SELECT u FROM Usuario u WHERE LOWER(u.pessoa.nome) LIKE LOWER(CONCAT('%', :termo, '%')) OR u.pessoa.cpf LIKE CONCAT('%', :cpf, '%')")
    Page<Usuario> pesquisarPorNomeOuCpf(@Param("termo") String termo, @Param("cpf") String cpf, Pageable pageable);
}