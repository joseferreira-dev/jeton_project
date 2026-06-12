package br.com.cremepe.jeton.security;

import br.com.cremepe.jeton.domain.ViewUserLogin;
import br.com.cremepe.jeton.service.LogJetonService;
import br.com.cremepe.jeton.util.JsonConverter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {

    private final LogJetonService logJetonService;
    private final JsonConverter jsonConverter;

    CustomLogoutSuccessHandler(LogJetonService logJetonService, JsonConverter jsonConverter) {
        this.logJetonService = logJetonService;
        this.jsonConverter = jsonConverter;
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
            // Fallback: tenta obter da sessão (ainda disponível durante o logout)
            ViewUserLogin sessaoUser = (ViewUserLogin) request.getSession().getAttribute("usuarioLogado");
            if (sessaoUser != null) {
                usuarioId = sessaoUser.getIdPessoa();
                usuarioNome = sessaoUser.getNome();
            }
        }

        if (usuarioId != null) {
            Map<String, Object> dados = new LinkedHashMap<>();
            dados.put("idUnico", UUID.randomUUID().toString());
            dados.put("timestamp", LocalDateTime.now().toString());
            dados.put("usuarioId", usuarioId);
            dados.put("usuarioNome", usuarioNome);
            dados.put("acao", "LOGOUT");
            dados.put("tabela", "login");
            dados.put("descricao", "Logout do sistema");
            dados.put("sucesso", true);
            String json = jsonConverter.toJson(dados);
            logJetonService.registrarLog("login", usuarioId, json);
        }

        // Redireciona para a página de login com parâmetro logout
        response.sendRedirect(request.getContextPath() + "/login?logout");
    }
}