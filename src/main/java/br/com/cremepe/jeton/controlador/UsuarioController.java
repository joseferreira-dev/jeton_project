package br.com.cremepe.jeton.controlador;

import br.com.cremepe.jeton.dominio.Usuario;
import br.com.cremepe.jeton.repositorio.UsuarioAcessoRepository;
import br.com.cremepe.jeton.servico.AcessoService;
import br.com.cremepe.jeton.servico.ConselheiroService;
import br.com.cremepe.jeton.servico.NivelAcessoService;
import br.com.cremepe.jeton.servico.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private ConselheiroService conselheiroService;
    @Autowired
    private NivelAcessoService nivelAcessoService;
    @Autowired
    private AcessoService acessoService;
    @Autowired
    private UsuarioAcessoRepository usuarioAcessoRepository;

    @GetMapping
    public String listar(
            @RequestParam(value = "termo", required = false, defaultValue = "") String termo,
            @RequestParam(value = "situacao", required = false, defaultValue = "") String situacao,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            @RequestParam(value = "sort", required = false, defaultValue = "pessoa.nome") String sort,
            @RequestParam(value = "dir", required = false, defaultValue = "asc") String dir,
            Model model, HttpSession session) {

        if (session.getAttribute("usuarioLogado") == null)
            return "redirect:/login";

        Page<Usuario> paginaUsuarios = usuarioService.listarComPaginacaoEPesquisa(termo, situacao, page, size, sort,
                dir);
        model.addAttribute("paginaUsuarios", paginaUsuarios);
        model.addAttribute("termo", termo);
        model.addAttribute("situacao", situacao);
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);

        return "usuario/lista";
    }

    private void carregarListasApoio(Model model, Integer idUsuario) {
        model.addAttribute("listaNiveisAcesso", nivelAcessoService.listarTodos());

        List<String> niveisAtuais = new ArrayList<>();
        if (idUsuario != null) {
            // Traz as letras (A, C, J...) que este utilizador já possui
            niveisAtuais = usuarioAcessoRepository.findAll().stream()
                    .filter(ua -> ua.getId().getIdUsuarioPessoa().equals(idUsuario))
                    .map(ua -> ua.getId().getIdNivel())
                    .toList();
        }
        model.addAttribute("niveisAtuais", niveisAtuais);
    }

    @GetMapping("/novo")
    public String prepararNovo(Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null)
            return "redirect:/login";
        model.addAttribute("usuario", new Usuario());
        carregarListasApoio(model, null);
        return "usuario/formulario";
    }

    @GetMapping("/editar/{id}")
    public String prepararEditar(@PathVariable("id") Integer id, Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null)
            return "redirect:/login";

        Usuario usuario = usuarioService.buscarPorId(id).orElse(new Usuario());

        if ("C".equals(usuario.getPessoa().getInTipoPessoa())) {
            usuario.seteConselheiro(true);
            conselheiroService.buscarPorId(id).ifPresent(c -> usuario.setCrm(c.getCrm()));
        }

        model.addAttribute("usuario", usuario);
        carregarListasApoio(model, id);
        return "usuario/formulario";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute("usuario") Usuario usuario,
            @RequestParam(value = "niveisAcesso", required = false) List<String> niveisAcessoSelecionados,
            RedirectAttributes ra) {
        try {
            // Grava os dados base do utilizador (Nome, CPF, Senha...)
            Usuario userSalvo = usuarioService.salvar(usuario);

            // Lógica de Sincronização de Permissões
            Integer id = userSalvo.getIdUsuarioPessoa();
            List<String> niveisAtuais = usuarioAcessoRepository.findAll().stream()
                    .filter(ua -> ua.getId().getIdUsuarioPessoa().equals(id))
                    .map(ua -> ua.getId().getIdNivel()).toList();

            List<String> selecionados = niveisAcessoSelecionados != null ? niveisAcessoSelecionados : new ArrayList<>();

            // 1. Concede os novos
            for (String nivel : selecionados) {
                if (!niveisAtuais.contains(nivel)) {
                    acessoService.concederPermissao(id, nivel);
                }
            }
            // 2. Revoga os que foram desmarcados
            for (String nivelAtual : niveisAtuais) {
                if (!selecionados.contains(nivelAtual)) {
                    acessoService.revogarPermissao(id, nivelAtual);
                }
            }

            ra.addFlashAttribute("sucesso", "Utilizador e permissões atualizados com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao salvar: " + e.getMessage());
        }
        return "redirect:/usuarios";
    }

    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable("id") Integer id, RedirectAttributes ra) {
        try {
            usuarioService.excluir(id);
            ra.addFlashAttribute("sucesso", "Utilizador removido com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro: O utilizador possui registos financeiros ou atividades vinculadas.");
        }
        return "redirect:/usuarios";
    }
}