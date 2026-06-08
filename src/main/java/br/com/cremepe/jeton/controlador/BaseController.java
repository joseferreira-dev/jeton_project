package br.com.cremepe.jeton.controlador;

import br.com.cremepe.jeton.domain.NivelAcesso;
import br.com.cremepe.jeton.domain.ViewUserLogin;
import jakarta.servlet.http.HttpSession;

public abstract class BaseController {

    protected ViewUserLogin getUsuarioLogado(HttpSession session) {
        return (ViewUserLogin) session.getAttribute("usuarioLogado");
    }

    protected boolean isConselheiro(HttpSession session) {
        ViewUserLogin user = getUsuarioLogado(session);
        return user != null && "C".equals(user.getInTipoPessoa());
    }

    protected boolean isFuncionario(HttpSession session) {
        ViewUserLogin user = getUsuarioLogado(session);
        return user != null && "F".equals(user.getInTipoPessoa());
    }

    protected boolean isSuperAdmin(HttpSession session) {
        ViewUserLogin user = getUsuarioLogado(session);
        return user != null && user.hasPermissao(NivelAcesso.NIVEL_SUPER_ADMIN);
    }
}