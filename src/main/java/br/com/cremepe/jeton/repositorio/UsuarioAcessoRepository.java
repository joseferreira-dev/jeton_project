package br.com.cremepe.jeton.repositorio;

import br.com.cremepe.jeton.dominio.UsuarioAcesso;
import br.com.cremepe.jeton.dominio.UsuarioAcessoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsuarioAcessoRepository extends JpaRepository<UsuarioAcesso, UsuarioAcessoId> {

    // Método bónus para o Spring Data trazer as permissões de um utilizador
    // específico automaticamente
    List<UsuarioAcesso> findByIdIdUsuarioPessoa(Integer idUsuarioPessoa);

    boolean existsByIdIdNivel(String idNivel);

}