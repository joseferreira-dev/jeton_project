package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.annotation.AuditoriaUser;
import br.com.cremepe.jeton.domain.ViewUserLogin;
import br.com.cremepe.jeton.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

@Service
public class UsuarioLogadoService {

    public AuditoriaUser getUsuarioLogado() {
        // 1. Tenta obter do SecurityContextHolder (Spring Security)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth.getPrincipal() instanceof String)) {
            Object principal = auth.getPrincipal();
            if (principal instanceof CustomUserDetails) {
                CustomUserDetails userDetails = (CustomUserDetails) principal;
                ViewUserLogin viewUser = userDetails.getViewUserLogin();
                if (viewUser != null) {
                    return new AuditoriaUser(viewUser.getIdPessoa(), viewUser.getNome());
                }
            }
        }

        // 2. Fallback: tenta obter da sessão (para compatibilidade)
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            ViewUserLogin usuario = (ViewUserLogin) request.getSession().getAttribute("usuarioLogado");
            if (usuario != null) {
                return new AuditoriaUser(usuario.getIdPessoa(), usuario.getNome());
            }
        }

        return null;
    }

    public Optional<ViewUserLogin> getViewUserLogin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth.getPrincipal() instanceof String)) {
            Object principal = auth.getPrincipal();
            if (principal instanceof CustomUserDetails) {
                return Optional.of(((CustomUserDetails) principal).getViewUserLogin());
            }
        }
        return Optional.empty();
    }
}