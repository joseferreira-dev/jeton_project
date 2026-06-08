package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.annotation.Auditar;
import br.com.cremepe.jeton.domain.NivelAcesso;
import br.com.cremepe.jeton.domain.Usuario;
import br.com.cremepe.jeton.domain.UsuarioAcesso;
import br.com.cremepe.jeton.domain.UsuarioAcessoId;
import br.com.cremepe.jeton.repository.NivelAcessoRepository;
import br.com.cremepe.jeton.repository.UsuarioAcessoRepository;
import br.com.cremepe.jeton.repository.UsuarioRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PermissaoService {

    private static final Logger log = LoggerFactory.getLogger(PermissaoService.class);

    @Autowired
    private UsuarioAcessoRepository usuarioAcessoRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private NivelAcessoRepository nivelAcessoRepository;

    // =========================================================================
    // OPERAÇÕES DE ESCRITA
    // =========================================================================

    @Auditar(tabela = "usuario_acesso", acao = "CONCEDER", descricao = "Concessão de permissão (nível de acesso) a um usuário", dadosParametros = "{ 'usuarioId': #idUsuario, 'nivelId': #idNivel }", capturarEstadoAnterior = false, auditarExcecao = true, incluirRetorno = false)
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

            return salvo;
        });
    }

    @Auditar(tabela = "usuario_acesso", acao = "REVOGAR", descricao = "Revogação de permissão (nível de acesso) de um usuário", dadosParametros = "{ 'usuarioId': #idUsuario, 'nivelId': #idNivel }", capturarEstadoAnterior = false, auditarExcecao = true)
    @Transactional
    public void revogarPermissao(Integer idUsuario, String idNivel) {
        // Busca os dados antes de excluir para que o aspecto possa
        // capturá-los (estado anterior). O aspecto já fará isso via
        // entityManager.find(UsuarioAcesso.class, idComposto), mas
        // garantimos que a entidade existe antes de deletar
        UsuarioAcessoId idComposto = new UsuarioAcessoId(idUsuario, idNivel);
        if (!usuarioAcessoRepository.existsById(idComposto)) {
            log.warn("Tentativa de revogar permissão inexistente: usuário {}, nível {}", idUsuario, idNivel);
            return;
        }

        usuarioAcessoRepository.deleteById(idComposto);
        log.info("Permissão revogada: usuário {} -> nível {}", idUsuario, idNivel);
    }

    @Auditar(tabela = "usuario_acesso", acao = "REVOGAR_TODAS", descricao = "Revogação de todas as permissões de um usuário", dadosParametros = "{ 'usuarioId': #idUsuario }", capturarEstadoAnterior = false, auditarExcecao = true)
    @Transactional
    public void revogarTodasPermissoes(Integer idUsuario) {
        // Busca todas as permissões do usuário para que o aspecto possa
        // registrar o estado anterior. O aspecto não captura automaticamente
        // coleções, então usamos dadosParametros para registrar
        List<UsuarioAcesso> permissoes = usuarioAcessoRepository.findByIdIdUsuarioPessoa(idUsuario);
        if (permissoes.isEmpty()) {
            log.info("Nenhuma permissão para revogar do usuário {}", idUsuario);
            return;
        }

        usuarioAcessoRepository.deleteByUsuarioId(idUsuario);
        log.info("Todas as permissões do usuário {} foram revogadas", idUsuario);
    }

    // =========================================================================
    // OPERAÇÕES DE LEITURA
    // =========================================================================

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