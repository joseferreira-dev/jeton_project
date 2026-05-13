package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.UsuarioAcesso;
import br.com.cremepe.jeton.dominio.UsuarioAcessoId;
import br.com.cremepe.jeton.repositorio.UsuarioAcessoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Serviço focado exclusivamente na gestão de permissões de usuários (UsuarioAcesso).
 * (As operações de NivelAcesso foram movidas para o NivelAcessoService).
 */
@Service
public class AcessoService {

    @Autowired
    private UsuarioAcessoRepository usuarioAcessoRepository;

    /**
     * Atribui um novo perfil de acesso a um utilizador.
     */
    @Transactional
    public UsuarioAcesso concederPermissao(Integer idUsuario, String idNivel) {
        UsuarioAcessoId idComposto = new UsuarioAcessoId(idUsuario, idNivel);
        
        return usuarioAcessoRepository.findById(idComposto).orElseGet(() -> {
            UsuarioAcesso novaPermissao = new UsuarioAcesso();
            novaPermissao.setId(idComposto);
            return usuarioAcessoRepository.save(novaPermissao);
        });
    }

    @Transactional
    public void revogarPermissao(Integer idUsuario, String idNivel) {
        usuarioAcessoRepository.deleteById(new UsuarioAcessoId(idUsuario, idNivel));
    }

    @Transactional(readOnly = true)
    public List<UsuarioAcesso> listarPermissoesDoUsuario(Integer idUsuario) {
        return usuarioAcessoRepository.findByIdIdUsuarioPessoa(idUsuario);
    }
}