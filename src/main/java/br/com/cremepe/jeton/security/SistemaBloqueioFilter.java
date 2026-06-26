package br.com.cremepe.jeton.security;

import br.com.cremepe.jeton.domain.NivelAcesso;
import br.com.cremepe.jeton.service.ParametrosService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class SistemaBloqueioFilter extends OncePerRequestFilter {

    private static final List<String> ROTAS_PERMITIDAS_DURANTE_BLOQUEIO = List.of(
            "/login", "/autenticar", "/sair", "/bloqueio", "/bloqueio/status",
            "/css/", "/js/", "/images/", "/favicon", "/webjars/");

    private final ParametrosService parametrosService;

    SistemaBloqueioFilter(ParametrosService parametrosService) {
        this.parametrosService = parametrosService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();

        // Permite rotas públicas / estáticas sempre
        if (isRotaPublica(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean sistemaBloqueado = parametrosService.isSistemaBloqueado();
        if (sistemaBloqueado) {
            // Verifica se o usuário tem permissão para acessar durante bloqueio
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean temPermissaoBloqueio = false;

            if (auth != null && auth.isAuthenticated() && !(auth.getPrincipal() instanceof String)) {
                Object principal = auth.getPrincipal();
                if (principal instanceof CustomUserDetails userDetails) {
                    temPermissaoBloqueio = userDetails.getAuthorities().stream()
                            .anyMatch(granted -> granted.getAuthority().equals(NivelAcesso.NIVEL_SUPER_ADMIN) ||
                                    granted.getAuthority().equals(NivelAcesso.NIVEL_BLOQUEIO_SISTEMA));
                }
            }

            if (!temPermissaoBloqueio) {
                // Redireciona para a página de bloqueio
                response.sendRedirect(request.getContextPath() + "/bloqueio");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isRotaPublica(String uri) {
        return ROTAS_PERMITIDAS_DURANTE_BLOQUEIO.stream().anyMatch(uri::startsWith);
    }
}