package br.com.cremepe.jeton.security;

import br.com.cremepe.jeton.domain.ViewUserLogin;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomAuthenticationSuccessHandler.class);

    private final CustomUserDetailsService customUserDetailsService;

    @Autowired
    public CustomAuthenticationSuccessHandler(CustomUserDetailsService customUserDetailsService) {
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetails)) {
            response.sendRedirect(request.getContextPath() + "/index");
            return;
        }

        CustomUserDetails userDetails = (CustomUserDetails) principal;
        ViewUserLogin usuarioLogado = userDetails.getViewUserLogin();

        // Tenta migrar senha (se necessário)
        String senhaPlain = request.getParameter("senha");
        if (senhaPlain != null) {
            customUserDetailsService.migratePasswordIfNeeded(usuarioLogado.getCpf(), senhaPlain);
        }

        // Coloca o objeto na sessão (compatibilidade com código legado)
        HttpSession session = request.getSession();
        session.setAttribute("usuarioLogado", usuarioLogado);
        log.info("Login bem-sucedido: usuário {} (ID {})", usuarioLogado.getNome(), usuarioLogado.getIdPessoa());

        // Redireciona conforme o tipo de pessoa
        boolean isConselheiro = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CONSELHEIRO"));

        if (isConselheiro) {
            response.sendRedirect(request.getContextPath() + "/conselheiro/dashboard");
        } else {
            response.sendRedirect(request.getContextPath() + "/index");
        }
    }
}