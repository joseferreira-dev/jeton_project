package br.com.cremepe.jeton.seguranca;

import br.com.cremepe.jeton.dominio.ViewUserLogin;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AutorizacaoInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        
        String uri = request.getRequestURI();
        
        // Rotas públicas que não precisam de login
        if (uri.startsWith("/login") || uri.startsWith("/autenticar") || uri.startsWith("/css") || 
            uri.startsWith("/js") || uri.startsWith("/images") || uri.startsWith("/error")) {
            return true;
        }

        HttpSession session = request.getSession();
        ViewUserLogin usuarioLogado = (ViewUserLogin) session.getAttribute("usuarioLogado");

        // 1. Verificação de Autenticação (Login)
        if (usuarioLogado == null) {
            response.sendRedirect("/login");
            return false;
        }

        // 2. Verificação de Autorização (Níveis de Acesso)
        String permissoes = usuarioLogado.getPermissoes() != null ? usuarioLogado.getPermissoes() : "";
        boolean isSuperAdmin = permissoes.contains("S"); // 'S' gere tudo
        
        // Mapeamento das rotas vs Letras de Permissão
        if (uri.startsWith("/atividades") && !(isSuperAdmin || permissoes.contains("A"))) {
            response.sendRedirect("/index?erro=acesso_negado");
            return false;
        }
        if (uri.startsWith("/comprovantes") && !(isSuperAdmin || permissoes.contains("C"))) {
            response.sendRedirect("/index?erro=acesso_negado");
            return false;
        }
        if (uri.startsWith("/jeton") && !(isSuperAdmin || permissoes.contains("J"))) {
            response.sendRedirect("/index?erro=acesso_negado");
            return false;
        }
        if ((uri.startsWith("/gestoes") || uri.startsWith("/gestao-conselheiros") || uri.startsWith("/conselheiros")) 
             && !(isSuperAdmin || permissoes.contains("G"))) {
            response.sendRedirect("/index?erro=acesso_negado");
            return false;
        }
        if ((uri.startsWith("/portarias") || uri.startsWith("/resolucoes") || uri.startsWith("/regras")) 
             && !(isSuperAdmin || permissoes.contains("R"))) {
            response.sendRedirect("/index?erro=acesso_negado");
            return false;
        }
        if (uri.startsWith("/usuarios") && !(isSuperAdmin || permissoes.contains("U"))) {
            response.sendRedirect("/index?erro=acesso_negado");
            return false;
        }

        return true; // Acesso Permitido
    }
}