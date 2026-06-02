package br.com.cremepe.jeton.seguranca;

import br.com.cremepe.jeton.dominio.NivelAcesso;
import br.com.cremepe.jeton.dominio.ViewUserLogin;
import br.com.cremepe.jeton.servico.LogJetonService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AutorizacaoInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AutorizacaoInterceptor.class);

    @Autowired
    private LogJetonService logJetonService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String uri = request.getRequestURI();
        String method = request.getMethod();

        // Rotas públicas
        if (uri.startsWith("/login") || uri.startsWith("/autenticar") ||
                uri.startsWith("/css") || uri.startsWith("/js") || uri.startsWith("/images") ||
                uri.startsWith("/error")) {
            return true;
        }

        HttpSession session = request.getSession();
        ViewUserLogin usuarioLogado = (ViewUserLogin) session.getAttribute("usuarioLogado");

        // Autenticação
        if (usuarioLogado == null) {
            log.warn("Acesso não autenticado à URI: {} {}", method, uri);
            response.sendRedirect("/login");
            return false;
        }

        if (uri.startsWith("/atividades/api/")) {
            return true;
        }

        // Autorização
        boolean isSuperAdmin = usuarioLogado.hasPermissao(NivelAcesso.NIVEL_SUPER_ADMIN);
        String permissaoFaltante = null;

        // Portal do Conselheiro
        if (uri.startsWith("/conselheiro")) {
            boolean isConselheiro = "C".equals(usuarioLogado.getInTipoPessoa());
            if (!isConselheiro && !isSuperAdmin) {
                log.warn("Acesso negado ao portal do conselheiro: usuário {} (tipo {})",
                        usuarioLogado.getNome(), usuarioLogado.getInTipoPessoa());
                response.sendRedirect("/index?erro=acesso_negado");
                return false;
            }
            return true;
        }

        // Demais rotas (incluindo /atividades, /comprovantes, etc.)
        if (uri.startsWith("/atividades")) {
            // Conselheiros não podem acessar a área administrativa de atividades
            if ("C".equals(usuarioLogado.getInTipoPessoa())) {
                permissaoFaltante = "ACESSO_RESTRITO_CONSELHEIRO";
                response.sendRedirect("/index?erro=acesso_negado");
                return false;
            }
            if (!(isSuperAdmin || usuarioLogado.hasPermissao(NivelAcesso.NIVEL_ATIVIDADE_CONSELHAL))) {
                permissaoFaltante = NivelAcesso.NIVEL_ATIVIDADE_CONSELHAL;
                log.warn("Acesso negado: usuário {} tentou acessar {} sem permissão A", usuarioLogado.getNome(), uri);
            }
        } else if (uri.startsWith("/comprovantes")
                && !(isSuperAdmin || usuarioLogado.hasPermissao(NivelAcesso.NIVEL_COMPROVANTES))) {
            permissaoFaltante = NivelAcesso.NIVEL_COMPROVANTES;
            log.warn("Acesso negado: usuário {} tentou acessar {} sem permissão C", usuarioLogado.getNome(), uri);
        } else if (uri.startsWith("/jeton")
                && !(isSuperAdmin || usuarioLogado.hasPermissao(NivelAcesso.NIVEL_JETONS))) {
            permissaoFaltante = NivelAcesso.NIVEL_JETONS;
            log.warn("Acesso negado: usuário {} tentou acessar {} sem permissão J", usuarioLogado.getNome(), uri);
        } else if ((uri.startsWith("/gestoes") || uri.startsWith("/gestao-conselheiros")
                || uri.startsWith("/conselheiros"))
                && !(isSuperAdmin || usuarioLogado.hasPermissao(NivelAcesso.NIVEL_GESTAO_CONSELHEIROS))) {
            permissaoFaltante = NivelAcesso.NIVEL_GESTAO_CONSELHEIROS;
            log.warn("Acesso negado: usuário {} tentou acessar {} sem permissão G", usuarioLogado.getNome(), uri);
        } else if ((uri.startsWith("/portarias") || uri.startsWith("/resolucoes") || uri.startsWith("/regras"))
                && !(isSuperAdmin || usuarioLogado.hasPermissao(NivelAcesso.NIVEL_PORTARIAS_RESOLUCOES))) {
            permissaoFaltante = NivelAcesso.NIVEL_PORTARIAS_RESOLUCOES;
            log.warn("Acesso negado: usuário {} tentou acessar {} sem permissão R", usuarioLogado.getNome(), uri);
        } else if (uri.startsWith("/usuarios")
                && !(isSuperAdmin || usuarioLogado.hasPermissao(NivelAcesso.NIVEL_USUARIOS))) {
            permissaoFaltante = NivelAcesso.NIVEL_USUARIOS;
            log.warn("Acesso negado: usuário {} tentou acessar {} sem permissão U", usuarioLogado.getNome(), uri);
        } else if (uri.startsWith("/logs")
                && !(isSuperAdmin || usuarioLogado.hasPermissao(NivelAcesso.NIVEL_NIVEIS_ACESSO))) {
            log.warn("Acesso negado: usuário {} tentou acessar {} sem permissão N", usuarioLogado.getNome(), uri);
            response.sendRedirect("/index?erro=acesso_negado");
            return false;
        }

        if (permissaoFaltante != null) {
            String textoLog = String.format(
                    "Acesso negado: Usuário ID=%d (%s) tentou acessar %s %s - Nível necessário: '%s'",
                    usuarioLogado.getIdPessoa(), usuarioLogado.getNome(), method, uri, permissaoFaltante);
            logJetonService.registrarLog("acesso_negado", usuarioLogado.getIdPessoa(), textoLog);
            response.sendRedirect("/index?erro=acesso_negado");
            return false;
        }

        return true;
    }
}