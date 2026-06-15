package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.domain.NivelAcesso;
import br.com.cremepe.jeton.domain.Usuario;
import br.com.cremepe.jeton.domain.UsuarioAcesso;
import br.com.cremepe.jeton.domain.UsuarioAcessoId;
import br.com.cremepe.jeton.repository.NivelAcessoRepository;
import br.com.cremepe.jeton.repository.UsuarioAcessoRepository;
import br.com.cremepe.jeton.repository.UsuarioRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PermissaoService {

    private static final Logger log = LoggerFactory.getLogger(PermissaoService.class);

    private final UsuarioAcessoRepository usuarioAcessoRepository;
    private final UsuarioRepository usuarioRepository;
    private final NivelAcessoRepository nivelAcessoRepository;
    private final LogJetonService logJetonService; // <-- INJETADO

    public PermissaoService(UsuarioAcessoRepository usuarioAcessoRepository,
            UsuarioRepository usuarioRepository,
            NivelAcessoRepository nivelAcessoRepository,
            LogJetonService logJetonService) {
        this.usuarioAcessoRepository = usuarioAcessoRepository;
        this.usuarioRepository = usuarioRepository;
        this.nivelAcessoRepository = nivelAcessoRepository;
        this.logJetonService = logJetonService;
    }

    @Transactional
    public UsuarioAcesso concederPermissao(Integer idUsuario, String idNivel) {
        UsuarioAcessoId idComposto = new UsuarioAcessoId(idUsuario, idNivel);

        // Se o vínculo já existir, retorna-o (evita duplicidade)
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
            logJetonService.logPermissaoConcedida(usuario, nivelAcesso);
            return salvo;
        });
    }

    @Transactional
    public void revogarPermissao(Integer idUsuario, String idNivel) {
        UsuarioAcessoId idComposto = new UsuarioAcessoId(idUsuario, idNivel);
        if (!usuarioAcessoRepository.existsById(idComposto)) {
            log.warn("Tentativa de revogar permissão inexistente: usuário {}, nível {}", idUsuario, idNivel);
            return;
        }

        usuarioAcessoRepository.deleteById(idComposto);
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado com ID: " + idUsuario));
        NivelAcesso nivelAcesso = nivelAcessoRepository.findById(idNivel)
                .orElseThrow(() -> new RuntimeException("Nível de acesso não encontrado: " + idNivel));
        log.info("Permissão revogada: usuário {} -> nível {}", idUsuario, idNivel);
        logJetonService.logPermissaoRevogada(usuario, nivelAcesso);
    }

    @Transactional
    public void revogarTodasPermissoes(Integer idUsuario) {
        List<UsuarioAcesso> permissoes = usuarioAcessoRepository.findByIdIdUsuarioPessoa(idUsuario);
        if (permissoes.isEmpty()) {
            log.info("Nenhuma permissão para revogar do usuário {}", idUsuario);
            return;
        }

        usuarioAcessoRepository.deleteByUsuarioId(idUsuario);
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado com ID: " + idUsuario));
        log.info("Todas as permissões do usuário {} foram revogadas", idUsuario);
        logJetonService.logTodasPermissoesRevogadas(usuario);
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