package br.com.cremepe.jeton.security;

import br.com.cremepe.jeton.annotation.AuditoriaUser;
import br.com.cremepe.jeton.service.LogJetonService;
import br.com.cremepe.jeton.util.JsonConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class AuthenticationAuditListener {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationAuditListener.class);

    @Autowired
    private LogJetonService logJetonService;

    @Autowired
    private JsonConverter jsonConverter;

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        if (event.getAuthentication().getPrincipal() instanceof CustomUserDetails userDetails) {
            AuditoriaUser usuario = new AuditoriaUser(
                    userDetails.getViewUserLogin().getIdPessoa(),
                    userDetails.getViewUserLogin().getNome());
            Map<String, Object> dados = new LinkedHashMap<>();
            dados.put("idUnico", UUID.randomUUID().toString());
            dados.put("timestamp", LocalDateTime.now().toString());
            dados.put("usuarioId", usuario.id());
            dados.put("usuarioNome", usuario.nome());
            dados.put("acao", "LOGIN");
            dados.put("tabela", "login");
            dados.put("descricao", "Login bem-sucedido via Spring Security");
            dados.put("sucesso", true);
            String json = jsonConverter.toJson(dados);
            logJetonService.registrarLog("login", usuario.id(), json);
            log.info("Auditoria de login registrada para usuário {} (ID {})", usuario.nome(), usuario.id());
        }
    }

    @EventListener
    public void onLogoutSuccess(LogoutSuccessEvent event) {
        log.debug(
                "Evento de logout recebido, mas sem detalhes do usuário. Auditoria feita no LogoutSuccessHandler.");
    }
}