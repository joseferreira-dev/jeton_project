package br.com.cremepe.jeton.controlador;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.com.cremepe.jeton.dominio.Pessoa;
import br.com.cremepe.jeton.dominio.Usuario;
import br.com.cremepe.jeton.servico.UsuarioService;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired private UsuarioService usuarioService;

    @GetMapping
    public String listar(
            @RequestParam(value = "termo", required = false, defaultValue = "") String termo,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            Model model, HttpSession session) {

        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";

        // Chama o serviço passando a pesquisa e a paginação
        Page<Usuario> paginaUsuarios = usuarioService.listarComPaginacaoEPesquisa(termo, page, size);

        // Devolve os dados para a tela
        model.addAttribute("paginaUsuarios", paginaUsuarios);
        model.addAttribute("termo", termo);
        model.addAttribute("size", size);
        
        return "usuario/lista";
    }

    @GetMapping("/novo")
    public String prepararNovo(Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        Usuario usuario = new Usuario();
        usuario.setPessoa(new Pessoa()); // Importante para não dar erro no Thymeleaf
        model.addAttribute("usuario", usuario);
        return "usuario/formulario";
    }

    @GetMapping("/editar/{id}")
    public String prepararEditar(@PathVariable("id") Integer id, Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null) return "redirect:/login";
        Usuario usuario = usuarioService.buscarPorId(id).orElseThrow();
        model.addAttribute("usuario", usuario);
        return "usuario/formulario";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute("usuario") Usuario usuario, RedirectAttributes ra) {
        try {
            usuarioService.salvar(usuario);
            ra.addFlashAttribute("sucesso", "Dados atualizados com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao salvar: " + e.getMessage());
        }
        return "redirect:/usuarios";
    }

    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable("id") Integer id, RedirectAttributes ra) {
        try {
            usuarioService.excluir(id);
            ra.addFlashAttribute("sucesso", "Usuário removido com sucesso!");
        } catch (Exception e) {
            // Caso ocorra erro de integridade (ex: usuário tem vínculos no banco)
            ra.addFlashAttribute("erro", "Não foi possível excluir o usuário. Verifique se ele possui registros vinculados (atividades, jetons, etc).");
        }
        return "redirect:/usuarios";
    }
}