package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.ViewUserLogin;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class UsuarioLogadoService {

    public Integer getIdUsuarioLogado() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null)
            return null;
        HttpServletRequest request = attrs.getRequest();
        ViewUserLogin usuario = (ViewUserLogin) request.getSession().getAttribute("usuarioLogado");
        return usuario != null ? usuario.getIdPessoa() : null;
    }
}