package br.com.cremepe.jeton.security;

import br.com.cremepe.jeton.domain.ViewUserLogin;
import br.com.cremepe.jeton.service.LogJetonService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {

    private final LogJetonService logJetonService;

    CustomLogoutSuccessHandler(LogJetonService logJetonService) {
        this.logJetonService = logJetonService;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        Integer usuarioId = null;
        String usuarioNome = null;

        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            usuarioId = userDetails.getViewUserLogin().getIdPessoa();
            usuarioNome = userDetails.getViewUserLogin().getNome();
        } else {
            ViewUserLogin sessaoUser = (ViewUserLogin) request.getSession().getAttribute("usuarioLogado");
            if (sessaoUser != null) {
                usuarioId = sessaoUser.getIdPessoa();
                usuarioNome = sessaoUser.getNome();
            }
        }

        if (usuarioId != null) {
            logJetonService.logLogout(usuarioId, usuarioNome);
        }

        response.sendRedirect(request.getContextPath() + "/login?logout");
    }
}