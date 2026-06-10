package br.com.cremepe.jeton.controller;

import br.com.cremepe.jeton.domain.AtividadeConselhal;
import br.com.cremepe.jeton.dto.LoteAtividadeDTO;
import br.com.cremepe.jeton.service.AtividadeConselhalService;
import br.com.cremepe.jeton.service.AtividadeLoteService;
import br.com.cremepe.jeton.service.ConselheiroService;
import br.com.cremepe.jeton.service.GestaoService;
import br.com.cremepe.jeton.service.RegrasService;
import br.com.cremepe.jeton.service.TipoAnexoService;
import br.com.cremepe.jeton.util.DataUtils;
import br.com.cremepe.jeton.util.TurnoUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/atividades")
@PreAuthorize("hasAuthority('A') or hasAuthority('S')")
public class AtividadeConselhalController {

    @Autowired
    private AtividadeConselhalService atividadeService;
    @Autowired
    private AtividadeLoteService atividadeLoteService;
    @Autowired
    private ConselheiroService conselheiroService;
    @Autowired
    private GestaoService gestaoService;
    @Autowired
    private RegrasService regrasService;
    @Autowired
    private TipoAnexoService tipoAnexoService;

    @GetMapping
    public String listar(
            @RequestParam(value = "termo", required = false, defaultValue = "") String termo,
            @RequestParam(value = "situacao", required = false, defaultValue = "") String situacao,
            @RequestParam(value = "turno", required = false, defaultValue = "") String turno,
            @RequestParam(value = "comprovanteFiltro", required = false, defaultValue = "") String comprovanteFiltro,
            @RequestParam(value = "dataInicio", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(value = "dataFim", required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            @RequestParam(value = "sort", required = false, defaultValue = "dataHoraAtividade") String sort,
            @RequestParam(value = "dir", required = false, defaultValue = "desc") String dir,
            Model model) {

        Page<AtividadeConselhal> pagina = atividadeService.listarComPaginacaoEPesquisa(
                termo, situacao, turno, comprovanteFiltro, dataInicio, dataFim, page, size, sort, dir);

        model.addAttribute("paginaAtividades", pagina);
        model.addAttribute("termo", termo);
        model.addAttribute("situacao", situacao);
        model.addAttribute("turno", turno);
        model.addAttribute("comprovanteFiltro", comprovanteFiltro);
        model.addAttribute("dataInicio", dataInicio);
        model.addAttribute("dataFim", dataFim);
        model.addAttribute("size", size);
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);
        model.addAttribute("reverseSortDir", dir.equalsIgnoreCase("asc") ? "desc" : "asc");

        return "atividadeconselhal/lista";
    }

    @GetMapping("/novo")
    public String prepararNovo(Model model, HttpSession session) {
        if (naoAutenticado(session))
            return "redirect:/login";
        model.addAttribute("atividade", new AtividadeConselhal());
        carregarListasDeApoio(model);
        return "atividadeconselhal/formulario";
    }

    @GetMapping("/editar/{id}")
    public String prepararEditar(@PathVariable("id") Integer id, Model model, HttpSession session,
            RedirectAttributes ra) {
        if (naoAutenticado(session))
            return "redirect:/login";
        try {
            AtividadeConselhal atividade = atividadeService.buscarPorId(id)
                    .orElseThrow(() -> new IllegalArgumentException("Atividade não encontrada"));

            if (atividade.getComprovante() != null
                    && atividadeService
                            .contarAtividadesPorComprovante(atividade.getComprovante().getIdComprovante()) > 1) {
                ra.addFlashAttribute("info",
                        "Esta atividade compartilha o mesmo comprovante com outras atividades. Para editar todas de uma vez, utilize a edição em lote.");
                return "redirect:/atividades/lote/editar/" + atividade.getComprovante().getIdComprovante();
            }

            model.addAttribute("atividade", atividade);
            carregarListasDeApoio(model);
            return "atividadeconselhal/formulario";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("erro", e.getMessage());
            return "redirect:/atividades";
        }
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute("atividade") AtividadeConselhal atividade,
            @RequestParam("dataAtividadePura") String dataAtividadePura,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "idTipoAnexo", required = false) Integer idTipoAnexo,
            @RequestParam(value = "nomeComprovanteUsuario", required = false) String nomeComprovanteUsuario,
            HttpSession session,
            RedirectAttributes ra) {
        try {
            LocalDateTime dataHora = DataUtils.parseDataHora(dataAtividadePura,
                    "A data e o horário da atividade são obrigatórios para o enquadramento.");
            atividade.setDataHoraAtividade(dataHora);
            atividade.setInTurno(TurnoUtils.calcularTurno(dataHora.getHour()));

            if (atividade.getIdAtividade() == null) {
                atividade.setInSituacao(AtividadeConselhal.SITUACAO_PENDENTE);
                atividadeService.criar(atividade, file, idTipoAnexo, nomeComprovanteUsuario);
                ra.addFlashAttribute("sucesso", "Atividade criada com sucesso!");
            } else {
                if (atividade.getInSituacao() == null || atividade.getInSituacao().trim().isEmpty()) {
                    atividade.setInSituacao(AtividadeConselhal.SITUACAO_PENDENTE);
                }
                Integer idComprovanteAntigo = atividadeService.buscarPorId(atividade.getIdAtividade())
                        .map(a -> a.getComprovante() != null ? a.getComprovante().getIdComprovante() : null)
                        .orElse(null);
                atividadeService.atualizar(atividade, file, idTipoAnexo, nomeComprovanteUsuario, idComprovanteAntigo);
                ra.addFlashAttribute("sucesso", "Atividade atualizada com sucesso!");
            }
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("erro", e.getMessage());
            return "redirect:/atividades/novo";
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/atividades";
    }

    @GetMapping("/validar/{id}")
    public String validar(@PathVariable("id") Integer id, RedirectAttributes ra) {
        try {
            atividadeService.validar(id);
            ra.addFlashAttribute("sucesso",
                    "Atividade validada com sucesso! Ela agora está apta a receber processamento financeiro.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/atividades";
    }

    @GetMapping("/desvalidar/{id}")
    public String desvalidar(@PathVariable("id") Integer id, RedirectAttributes ra) {
        try {
            atividadeService.desvalidar(id);
            ra.addFlashAttribute("sucesso", "Atividade retornada ao status Pendente.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/atividades";
    }

    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable("id") Integer id, RedirectAttributes ra) {
        try {
            atividadeService.excluir(id);
            ra.addFlashAttribute("sucesso", "Atividade removida com sucesso!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/atividades";
    }

    @GetMapping("/lote/novo")
    public String prepararLote(Model model, HttpSession session) {
        if (naoAutenticado(session))
            return "redirect:/login";
        model.addAttribute("listaGestoes", gestaoService.listarTodos());
        model.addAttribute("listaTiposAnexo", tipoAnexoService.listarTodos());
        return "atividadeconselhal/lote_formulario";
    }

    @PostMapping("/lote/salvar")
    public String salvarLote(@ModelAttribute LoteAtividadeDTO dto,
            HttpSession session,
            RedirectAttributes ra) {
        if (naoAutenticado(session))
            return "redirect:/login";
        try {
            if (dto.getInTurno() == null || dto.getInTurno().isEmpty()) {
                dto.setInTurno(null);
            }
            atividadeLoteService.criarLote(dto);
            ra.addFlashAttribute("sucesso", "Atividades criadas em lote com sucesso para "
                    + dto.getIdsConselheiros().size() + " conselheiros.");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao criar lote: " + e.getMessage());
        }
        return "redirect:/atividades";
    }

    @GetMapping("/lote/editar/{idComprovante}")
    public String prepararEdicaoLote(@PathVariable Integer idComprovante, Model model, HttpSession session,
            RedirectAttributes ra) {
        if (naoAutenticado(session)) {
            return "redirect:/login";
        }

        List<AtividadeConselhal> atividades = atividadeLoteService.listarPorComprovante(idComprovante);
        if (atividades.isEmpty()) {
            ra.addFlashAttribute("erro", "Nenhuma atividade encontrada para o comprovante informado.");
            return "redirect:/atividades";
        }

        AtividadeConselhal referencia = atividades.get(0);
        model.addAttribute("atividadeReferencia", referencia);
        model.addAttribute("quantidade", atividades.size());
        model.addAttribute("listaGestoes", gestaoService.listarTodos());
        model.addAttribute("listaTiposAnexo", tipoAnexoService.listarTodos());
        model.addAttribute("idComprovante", idComprovante);
        model.addAttribute("idsConselheirosAtuais", atividades.stream()
                .map(a -> a.getConselheiro().getIdPessoa())
                .distinct()
                .toList());
        return "atividadeconselhal/lote_edicao";
    }

    @PostMapping("/lote/atualizar/{idComprovante}")
    public String atualizarLote(@PathVariable Integer idComprovante,
            @ModelAttribute LoteAtividadeDTO dto,
            HttpSession session,
            RedirectAttributes ra) {
        if (naoAutenticado(session))
            return "redirect:/login";
        try {
            if (dto.getInTurno() == null || dto.getInTurno().isEmpty()) {
                dto.setInTurno(null);
            }
            atividadeLoteService.atualizarLote(idComprovante, dto);
            ra.addFlashAttribute("sucesso", "Todas as atividades vinculadas ao comprovante foram atualizadas.");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao atualizar lote: " + e.getMessage());
        }
        return "redirect:/atividades";
    }

    private boolean naoAutenticado(HttpSession session) {
        return session.getAttribute("usuarioLogado") == null;
    }

    private void carregarListasDeApoio(Model model) {
        model.addAttribute("listaConselheiros", conselheiroService.listarTodos());
        model.addAttribute("listaGestoes", gestaoService.listarTodos());
        model.addAttribute("listaResolucoes", regrasService.listarResolucoesComRegras());
        model.addAttribute("listaPortarias", regrasService.listarPortariasComRegras());
        model.addAttribute("listaTiposAnexo", tipoAnexoService.listarTodos());
    }
}