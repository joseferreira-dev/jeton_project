package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.annotation.AuditoriaUser;
import br.com.cremepe.jeton.domain.ViewUserLogin;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class UsuarioLogadoService {

    public AuditoriaUser getUsuarioLogado() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null)
            return null;
        HttpServletRequest request = attrs.getRequest();
        ViewUserLogin usuario = (ViewUserLogin) request.getSession().getAttribute("usuarioLogado");
        if (usuario == null)
            return null;
        return new AuditoriaUser(usuario.getIdPessoa(), usuario.getNome());
    }

}