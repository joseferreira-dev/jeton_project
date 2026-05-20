package br.com.cremepe.jeton.controlador;

import br.com.cremepe.jeton.dominio.Gestao;
import br.com.cremepe.jeton.servico.GestaoService;
import br.com.cremepe.jeton.servico.JetonService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Optional;

@Controller
@RequestMapping("/jeton")
public class JetonController {

    @Autowired
    private JetonService jetonService;
    @Autowired
    private GestaoService gestaoService;

    @GetMapping
    public String listar(Model model, HttpSession session) {
        if (session.getAttribute("usuarioLogado") == null)
            return "redirect:/login";

        // Lista o histórico de Jetons já processados
        model.addAttribute("lista", jetonService.listarTodos());

        // Carrega as Gestões para o dropdown do formulário
        model.addAttribute("listaGestoes", gestaoService.listarTodos());

        // Preenche automaticamente o Mês e Ano atuais para facilitar a usabilidade
        LocalDate hoje = LocalDate.now();
        model.addAttribute("mesAtual", hoje.getMonthValue());
        model.addAttribute("anoAtual", hoje.getYear());

        return "jeton/lista";
    }

    @PostMapping("/processar")
    public String processar(
            @RequestParam("idGestao") Integer idGestao,
            @RequestParam("mes") Integer mes,
            @RequestParam("ano") Integer ano,
            RedirectAttributes ra) {

        try {
            Optional<Gestao> gestaoOpt = gestaoService.buscarPorId(idGestao);
            if (gestaoOpt.isEmpty()) {
                throw new RuntimeException("Gestão não encontrada.");
            }

            // Dispara o Motor de Cálculo Transacional
            jetonService.processarFechamentoMensal(gestaoOpt.get(), mes, ano);
            ra.addFlashAttribute("sucesso",
                    "Processamento concluído com sucesso para o período " + mes + "/" + ano + "!");

        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao processar folha: " + e.getMessage());
        }

        return "redirect:/jeton";
    }

    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable("id") Integer id, RedirectAttributes ra) {
        try {
            jetonService.excluirJeton(id);
            ra.addFlashAttribute("sucesso", "Pagamento (Jeton) eliminado do histórico!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao remover Jeton. Pode estar vinculado a outras entidades.");
        }
        return "redirect:/jeton";
    }
}