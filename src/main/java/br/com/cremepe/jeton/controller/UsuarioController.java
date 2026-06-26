package br.com.cremepe.jeton.controller;

import br.com.cremepe.jeton.domain.Usuario;
import br.com.cremepe.jeton.domain.ViewUserLogin;
import br.com.cremepe.jeton.repository.UsuarioAcessoRepository;
import br.com.cremepe.jeton.service.ConselheiroService;
import br.com.cremepe.jeton.service.NivelAcessoService;
import br.com.cremepe.jeton.service.PermissaoService;
import br.com.cremepe.jeton.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(UsuarioController.class);

    private final UsuarioService usuarioService;
    private final ConselheiroService conselheiroService;
    private final NivelAcessoService nivelAcessoService;
    private final PermissaoService permissaoService;
    private final UsuarioAcessoRepository usuarioAcessoRepository;

    UsuarioController(UsuarioService usuarioService, NivelAcessoService nivelAcessoService,
            PermissaoService permissaoService, UsuarioAcessoRepository usuarioAcessoRepository,
            ConselheiroService conselheiroService) {
        this.usuarioService = usuarioService;
        this.conselheiroService = conselheiroService;
        this.nivelAcessoService = nivelAcessoService;
        this.permissaoService = permissaoService;
        this.usuarioAcessoRepository = usuarioAcessoRepository;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('U') or hasAuthority('S')")
    public String listar(
            @RequestParam(value = "termo", required = false, defaultValue = "") String termo,
            @RequestParam(value = "situacao", required = false, defaultValue = "") String situacao,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            @RequestParam(value = "sort", required = false, defaultValue = "pessoa.nome") String sort,
            @RequestParam(value = "dir", required = false, defaultValue = "asc") String dir,
            Model model, HttpSession session) {

        if (naoAutenticado(session))
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

    @GetMapping("/novo")
    @PreAuthorize("hasAuthority('U') or hasAuthority('S')")
    public String prepararNovo(Model model, HttpSession session) {
        if (naoAutenticado(session))
            return "redirect:/login";
        model.addAttribute("usuario", new Usuario());
        carregarListasApoio(model, null);
        return "usuario/formulario";
    }

    @GetMapping("/editar/{id}")
    @PreAuthorize("hasAuthority('U') or hasAuthority('S')")
    public String prepararEditar(@PathVariable("id") Integer id, Model model, HttpSession session) {
        if (naoAutenticado(session))
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
    @PreAuthorize("hasAuthority('U') or hasAuthority('S')")
    public String salvar(@Valid @ModelAttribute("usuario") Usuario usuario,
            @RequestParam(value = "niveisAcesso", required = false) List<String> niveisAcessoSelecionados,
            HttpSession session,
            RedirectAttributes ra) {
        Usuario userSalvo;

        if (usuario.getIdUsuarioPessoa() == null) {
            userSalvo = usuarioService.criar(usuario);
        } else {
            userSalvo = usuarioService.atualizar(usuario);
        }

        // Sincroniza permissões (níveis de acesso)
        Integer id = userSalvo.getIdUsuarioPessoa();
        List<String> niveisAtuais = usuarioAcessoRepository.findAll().stream()
                .filter(ua -> ua.getId().getIdUsuarioPessoa().equals(id))
                .map(ua -> ua.getId().getIdNivel())
                .toList();

        List<String> selecionados = niveisAcessoSelecionados != null ? niveisAcessoSelecionados : new ArrayList<>();

        for (String nivel : selecionados) {
            if (!niveisAtuais.contains(nivel)) {
                permissaoService.concederPermissao(id, nivel);
            }
        }
        for (String nivelAtual : niveisAtuais) {
            if (!selecionados.contains(nivelAtual)) {
                permissaoService.revogarPermissao(id, nivelAtual);
            }
        }

        log.info("Usuário salvo e permissões atualizadas: ID={}", id);
        ra.addFlashAttribute("sucesso", "Usuário e permissões atualizados com sucesso!");
        return "redirect:/usuarios";
    }

    @PostMapping("/excluir/{id}")
    @PreAuthorize("hasAuthority('U') or hasAuthority('S')")
    public String excluir(@PathVariable("id") Integer id, HttpSession session, RedirectAttributes ra) {
        usuarioService.excluir(id);
        ra.addFlashAttribute("sucesso", "Usuário removido com sucesso!");
        return "redirect:/usuarios";
    }

    private void carregarListasApoio(Model model, Integer idUsuario) {
        model.addAttribute("listaNiveisAcesso", nivelAcessoService.listarTodos());

        List<String> niveisAtuais = new ArrayList<>();
        if (idUsuario != null) {
            niveisAtuais = usuarioAcessoRepository.findAll().stream()
                    .filter(ua -> ua.getId().getIdUsuarioPessoa().equals(idUsuario))
                    .map(ua -> ua.getId().getIdNivel())
                    .toList();
        }
        model.addAttribute("niveisAtuais", niveisAtuais);
    }

    @GetMapping("/perfil")
    @PreAuthorize("isAuthenticated()")
    public String perfil(HttpSession session, Model model) {
        ViewUserLogin usuarioLogado = getUsuarioLogado(session);
        if (usuarioLogado == null) {
            return "redirect:/login";
        }
        Usuario usuario = usuarioService.buscarPorId(usuarioLogado.getIdPessoa())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        model.addAttribute("usuario", usuario);

        // Se for conselheiro, carrega o CRM para exibição
        if (isConselheiro(session)) {
            usuario.seteConselheiro(true);
            conselheiroService.buscarPorId(usuarioLogado.getIdPessoa())
                    .ifPresent(c -> usuario.setCrm(c.getCrm()));
        }
        return "usuario/perfil";
    }

    @PostMapping("/perfil/salvar")
    @PreAuthorize("isAuthenticated()")
    public String salvarPerfil(@Valid @ModelAttribute("usuario") Usuario usuario,
            HttpSession session,
            RedirectAttributes ra) {
        ViewUserLogin usuarioLogado = getUsuarioLogado(session);
        if (usuarioLogado == null) {
            return "redirect:/login";
        }
        Integer idLogado = usuarioLogado.getIdPessoa();
        usuario.setIdUsuarioPessoa(idLogado);

        Usuario existente = usuarioService.buscarPorId(idLogado)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        usuario.setInSituacao(existente.getInSituacao());
        if (usuario.getPessoa() != null) {
            usuario.getPessoa().setInTipoPessoa(existente.getPessoa().getInTipoPessoa());
            usuario.getPessoa().setIdPessoa(idLogado);
            usuario.getPessoa().setCpf(existente.getPessoa().getCpf());
        }

        if (isConselheiro(session)) {
            usuario.setCrm(null);
            usuario.seteConselheiro(true);
        }

        usuarioService.atualizarPerfil(usuario);
        ra.addFlashAttribute("sucesso", "Perfil atualizado com sucesso!");
        return "redirect:/usuarios/perfil";
    }

    private boolean naoAutenticado(HttpSession session) {
        return session.getAttribute("usuarioLogado") == null;
    }
}