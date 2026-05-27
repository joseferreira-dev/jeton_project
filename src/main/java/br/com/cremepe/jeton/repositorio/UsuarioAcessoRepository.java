package br.com.cremepe.jeton.repositorio;

import br.com.cremepe.jeton.dominio.UsuarioAcesso;
import br.com.cremepe.jeton.dominio.UsuarioAcessoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface UsuarioAcessoRepository extends JpaRepository<UsuarioAcesso, UsuarioAcessoId> {

    List<UsuarioAcesso> findByIdIdUsuarioPessoa(Integer idUsuarioPessoa);

    boolean existsByIdIdNivel(String idNivel);

    @Modifying
    @Transactional
    @Query("DELETE FROM UsuarioAcesso ua WHERE ua.id.idUsuarioPessoa = :idUsuario")
    void deleteByUsuarioId(@Param("idUsuario") Integer idUsuario);
}