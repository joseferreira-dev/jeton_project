package br.com.cremepe.jeton.repositorio;

import br.com.cremepe.jeton.dominio.UsuarioAcesso;
import br.com.cremepe.jeton.dominio.UsuarioAcessoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsuarioAcessoRepository extends JpaRepository<UsuarioAcesso, UsuarioAcessoId> {

    // Lista todas as permissões concedidas a um utilizador específico
    List<UsuarioAcesso> findByIdIdUsuarioPessoa(Integer idUsuarioPessoa);

    // Lista todos os utilizadores que possuem um determinado nível de acesso (ex: quem tem nível 'A')
    List<UsuarioAcesso> findByIdIdNivel(String idNivel);
}