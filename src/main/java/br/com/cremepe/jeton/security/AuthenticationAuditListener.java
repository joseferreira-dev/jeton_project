package br.com.cremepe.jeton.security;

import br.com.cremepe.jeton.service.LogJetonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationAuditListener {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationAuditListener.class);

    private final LogJetonService logJetonService;

    AuthenticationAuditListener(LogJetonService logJetonService) {
        this.logJetonService = logJetonService;
    }

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        if (event.getAuthentication().getPrincipal() instanceof CustomUserDetails userDetails) {
            Integer id = userDetails.getViewUserLogin().getIdPessoa();
            String nome = userDetails.getViewUserLogin().getNome();
            logJetonService.logLogin(id, nome);
            log.info("Auditoria de login registrada para usuário {} (ID {})", nome, id);
        }
    }

    @EventListener
    public void onLogoutSuccess(LogoutSuccessEvent event) {
        log.debug(
                "Evento de logout recebido, mas sem detalhes do usuário. Auditoria feita no LogoutSuccessHandler.");
    }
}