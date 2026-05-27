package br.com.cremepe.jeton.seguranca;

import br.com.cremepe.jeton.dominio.NivelAcesso;
import br.com.cremepe.jeton.dominio.ViewUserLogin;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AutorizacaoInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AutorizacaoInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String uri = request.getRequestURI();

        // Rotas públicas
        if (uri.startsWith("/login") || uri.startsWith("/autenticar") ||
                uri.startsWith("/css") || uri.startsWith("/js") || uri.startsWith("/images") ||
                uri.startsWith("/error")) {
            return true;
        }

        HttpSession session = request.getSession();
        ViewUserLogin usuarioLogado = (ViewUserLogin) session.getAttribute("usuarioLogado");

        // 1. Autenticação
        if (usuarioLogado == null) {
            response.sendRedirect("/login");
            return false;
        }

        // 2. Autorização
        boolean isSuperAdmin = usuarioLogado.hasPermissao(NivelAcesso.NIVEL_SUPER_ADMIN);

        // Mapeamento de rotas vs permissões
        if (uri.startsWith("/atividades")
                && !(isSuperAdmin || usuarioLogado.hasPermissao(NivelAcesso.NIVEL_ATIVIDADE_CONSELHAL))) {
            log.warn("Acesso negado: usuário {} tentou acessar {} sem permissão A", usuarioLogado.getNome(), uri);
            response.sendRedirect("/index?erro=acesso_negado");
            return false;
        }
        if (uri.startsWith("/comprovantes")
                && !(isSuperAdmin || usuarioLogado.hasPermissao(NivelAcesso.NIVEL_COMPROVANTES))) {
            log.warn("Acesso negado: usuário {} tentou acessar {} sem permissão C", usuarioLogado.getNome(), uri);
            response.sendRedirect("/index?erro=acesso_negado");
            return false;
        }
        if (uri.startsWith("/jeton") && !(isSuperAdmin || usuarioLogado.hasPermissao(NivelAcesso.NIVEL_JETONS))) {
            log.warn("Acesso negado: usuário {} tentou acessar {} sem permissão J", usuarioLogado.getNome(), uri);
            response.sendRedirect("/index?erro=acesso_negado");
            return false;
        }
        if ((uri.startsWith("/gestoes") || uri.startsWith("/gestao-conselheiros") || uri.startsWith("/conselheiros")) &&
                !(isSuperAdmin || usuarioLogado.hasPermissao(NivelAcesso.NIVEL_GESTAO_CONSELHEIROS))) {
            log.warn("Acesso negado: usuário {} tentou acessar {} sem permissão G", usuarioLogado.getNome(), uri);
            response.sendRedirect("/index?erro=acesso_negado");
            return false;
        }
        if ((uri.startsWith("/portarias") || uri.startsWith("/resolucoes") || uri.startsWith("/regras")) &&
                !(isSuperAdmin || usuarioLogado.hasPermissao(NivelAcesso.NIVEL_PORTARIAS_RESOLUCOES))) {
            log.warn("Acesso negado: usuário {} tentou acessar {} sem permissão R", usuarioLogado.getNome(), uri);
            response.sendRedirect("/index?erro=acesso_negado");
            return false;
        }
        if (uri.startsWith("/usuarios") && !(isSuperAdmin || usuarioLogado.hasPermissao(NivelAcesso.NIVEL_USUARIOS))) {
            log.warn("Acesso negado: usuário {} tentou acessar {} sem permissão U", usuarioLogado.getNome(), uri);
            response.sendRedirect("/index?erro=acesso_negado");
            return false;
        }

        return true;
    }
}