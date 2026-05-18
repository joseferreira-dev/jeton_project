package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.NivelAcesso;
import br.com.cremepe.jeton.dominio.Usuario;
import br.com.cremepe.jeton.dominio.UsuarioAcesso;
import br.com.cremepe.jeton.dominio.UsuarioAcessoId;
import br.com.cremepe.jeton.repositorio.NivelAcessoRepository;
import br.com.cremepe.jeton.repositorio.UsuarioAcessoRepository;
import br.com.cremepe.jeton.repositorio.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Serviço focado na gestão de permissões de usuários (UsuarioAcesso).
 */
@Service
public class AcessoService {

    @Autowired
    private UsuarioAcessoRepository usuarioAcessoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private NivelAcessoRepository nivelAcessoRepository;

    /**
     * Atribui um novo perfil de acesso a um utilizador salvando a entidade com as amarrações relacionais.
     */
    @Transactional
    public UsuarioAcesso concederPermissao(Integer idUsuario, String idNivel) {
        UsuarioAcessoId idComposto = new UsuarioAcessoId(idUsuario, idNivel);
        
        return usuarioAcessoRepository.findById(idComposto).orElseGet(() -> {
            UsuarioAcesso novaPermissao = new UsuarioAcesso();
            novaPermissao.setId(idComposto);
            
            // CORREÇÃO: Busca as entidades correspondentes para preencher os mapeamentos do @MapsId
            Usuario usuario = usuarioRepository.findById(idUsuario)
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado com ID: " + idUsuario));
            
            NivelAcesso nivelAcesso = nivelAcessoRepository.findById(idNivel)
                    .orElseThrow(() -> new RuntimeException("Nível de acesso não encontrado com ID: " + idNivel));
            
            // Vincula os objetos reais para que o Hibernate consiga computar a persistência
            novaPermissao.setUsuario(usuario);
            novaPermissao.setNivelAcesso(nivelAcesso);
            
            return usuarioAcessoRepository.save(novaPermissao);
        });
    }

    @Transactional
    public void revogarPermissao(Integer idUsuario, String idNivel) {
        usuarioAcessoRepository.deleteById(new UsuarioAcessoId(idUsuario, idNivel));
    }

    @Transactional(readOnly = true)
    public List<UsuarioAcesso> listarTodos() {
        return usuarioAcessoRepository.findAll();
    }
}