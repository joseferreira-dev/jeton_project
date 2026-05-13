package br.com.cremepe.jeton.repositorio;

import br.com.cremepe.jeton.dominio.Usuario;
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

    // O Spring consegue navegar pelos relacionamentos da entidade!
    // Esta assinatura faz um JOIN invisível entre Usuario e Pessoa para procurar pelo CPF
    Optional<Usuario> findByPessoaCpf(String cpf);

    // Se preferir ter controlo total, pode escrever a sua própria query em JPQL (Java Persistence Query Language).
    // Note que aqui utilizamos os nomes das CLASSES e não das tabelas da base de dados.
    @Query("SELECT u FROM Usuario u JOIN FETCH u.pessoa p WHERE p.email = :email AND u.inSituacao = 'A'")
    Optional<Usuario> buscarUsuarioAtivoPorEmail(@Param("email") String email);
}