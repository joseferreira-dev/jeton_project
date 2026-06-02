package br.com.cremepe.jeton.controlador;

import br.com.cremepe.jeton.dominio.NivelAcesso;
import br.com.cremepe.jeton.dominio.ViewUserLogin;
import br.com.cremepe.jeton.servico.LogJetonService;
import br.com.cremepe.jeton.servico.ParametrosService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/bloqueio")
public class BloqueioController {

    @Autowired
    private ParametrosService parametrosService;
    @Autowired
    private LogJetonService logJetonService;

    private boolean temPermissaoBloqueio(HttpSession session) {
        ViewUserLogin usuario = (ViewUserLogin) session.getAttribute("usuarioLogado");
        return usuario != null && (usuario.hasPermissao(NivelAcesso.NIVEL_SUPER_ADMIN) ||
                usuario.hasPermissao(NivelAcesso.NIVEL_BLOQUEIO_SISTEMA));
    }

    @GetMapping("/status")
    @ResponseBody
    public Map<String, Object> getStatusBloqueio(HttpSession session) {
        boolean bloqueado = parametrosService.isSistemaBloqueado();
        Map<String, Object> resposta = new HashMap<>();
        resposta.put("bloqueado", bloqueado);
        resposta.put("status", bloqueado ? "BLOQUEADO" : "LIBERADO");
        return resposta;
    }

    @PostMapping("/alternar")
    public String alternarBloqueio(HttpSession session, RedirectAttributes ra) {
        if (!temPermissaoBloqueio(session)) {
            ra.addFlashAttribute("erro", "Você não tem permissão para alterar o bloqueio do sistema.");
            return "redirect:/index";
        }

        String statusAntes = parametrosService.obterStatus();
        parametrosService.alternarBloqueio();
        String statusDepois = parametrosService.obterStatus();

        ViewUserLogin usuario = (ViewUserLogin) session.getAttribute("usuarioLogado");
        String textoLog = String.format("Sistema %s por %s (ID=%d). Status anterior: %s",
                "S".equals(statusDepois) ? "BLOQUEADO" : "LIBERADO",
                usuario.getNome(), usuario.getIdPessoa(), statusAntes);
        logJetonService.registrarLog("parametros", usuario.getIdPessoa(), textoLog);

        ra.addFlashAttribute("sucesso",
                "Sistema " + ("S".equals(statusDepois) ? "bloqueado" : "liberado") + " com sucesso.");
        return "redirect:/index";
    }

    @GetMapping
    public String paginaBloqueio() {
        return "bloqueio";
    }
}