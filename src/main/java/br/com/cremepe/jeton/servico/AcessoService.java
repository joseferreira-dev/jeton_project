package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.NivelAcesso;
import br.com.cremepe.jeton.dominio.Usuario;
import br.com.cremepe.jeton.dominio.UsuarioAcesso;
import br.com.cremepe.jeton.dominio.UsuarioAcessoId;
import br.com.cremepe.jeton.repositorio.NivelAcessoRepository;
import br.com.cremepe.jeton.repositorio.UsuarioAcessoRepository;
import br.com.cremepe.jeton.repositorio.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AcessoService {

    private static final Logger log = LoggerFactory.getLogger(AcessoService.class);

    @Autowired
    private UsuarioAcessoRepository usuarioAcessoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private NivelAcessoRepository nivelAcessoRepository;

    @Transactional
    public UsuarioAcesso concederPermissao(Integer idUsuario, String idNivel) {
        UsuarioAcessoId idComposto = new UsuarioAcessoId(idUsuario, idNivel);

        return usuarioAcessoRepository.findById(idComposto).orElseGet(() -> {
            Usuario usuario = usuarioRepository.findById(idUsuario)
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado com ID: " + idUsuario));
            NivelAcesso nivelAcesso = nivelAcessoRepository.findById(idNivel)
                    .orElseThrow(() -> new RuntimeException("Nível de acesso não encontrado: " + idNivel));

            UsuarioAcesso novaPermissao = new UsuarioAcesso();
            novaPermissao.setId(idComposto);
            novaPermissao.setUsuario(usuario);
            novaPermissao.setNivelAcesso(nivelAcesso);

            UsuarioAcesso salvo = usuarioAcessoRepository.save(novaPermissao);
            log.info("Permissão concedida: usuário {} -> nível {}", idUsuario, idNivel);
            return salvo;
        });
    }

    @Transactional
    public void revogarPermissao(Integer idUsuario, String idNivel) {
        usuarioAcessoRepository.deleteById(new UsuarioAcessoId(idUsuario, idNivel));
        log.info("Permissão revogada: usuário {} -> nível {}", idUsuario, idNivel);
    }

    @Transactional
    public void revogarTodasPermissoes(Integer idUsuario) {
        usuarioAcessoRepository.deleteByUsuarioId(idUsuario);
        log.info("Todas as permissões do usuário {} foram revogadas", idUsuario);
    }

    @Transactional(readOnly = true)
    public boolean hasPermissao(Integer idUsuario, String idNivel) {
        return usuarioAcessoRepository.existsById(new UsuarioAcessoId(idUsuario, idNivel));
    }

    @Transactional(readOnly = true)
    public List<String> listarPermissoesDoUsuario(Integer idUsuario) {
        return usuarioAcessoRepository.findByIdIdUsuarioPessoa(idUsuario).stream()
                .map(ua -> ua.getId().getIdNivel())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UsuarioAcesso> listarPermissoesCompletas(Integer idUsuario) {
        return usuarioAcessoRepository.findByIdIdUsuarioPessoa(idUsuario);
    }

    @Transactional(readOnly = true)
    public List<UsuarioAcesso> listarTodos() {
        return usuarioAcessoRepository.findAll();
    }
}