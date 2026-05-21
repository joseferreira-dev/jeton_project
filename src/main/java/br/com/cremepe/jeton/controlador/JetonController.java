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
                throw new RuntimeException("A gestão informada não foi localizada.");
            }

            // Invoca o motor de fechamento com as travas FIFO e integralidade amarradas
            jetonService.processarFechamentoMensal(gestaoOpt.get(), mes, ano);
            ra.addFlashAttribute("sucesso",
                    "Fechamento mensal processado com sucesso para a competência " + mes + "/" + ano + "!");

        } catch (RuntimeException e) {
            // Captura os bloqueios de atividades pendentes ou falta de resoluções
            ra.addFlashAttribute("erro", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro interno ao processar a folha de pagamento: " + e.getMessage());
        }

        return "redirect:/jeton";
    }

    // ==========================================================
    // ESTORNO PONTUAL (Um conselheiro clicando na lixeira)
    // ==========================================================
    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable("id") Integer id, RedirectAttributes ra) {
        try {
            jetonService.estornarJetonPontual(id);
            ra.addFlashAttribute("sucesso",
                    "Jeton estornado com sucesso! As atividades retornaram para C+N e os pontos foram devolvidos ao conselheiro.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao estornar Jeton: " + e.getMessage());
        }
        return "redirect:/jeton";
    }

    // ==========================================================
    // ESTORNO EM LOTE (Botão geral da folha mensal)
    // ==========================================================
    @PostMapping("/estornar-lote")
    public String estornarLote(
            @RequestParam("idGestao") Integer idGestao,
            @RequestParam("mes") Integer mes,
            @RequestParam("ano") Integer ano,
            RedirectAttributes ra) {
        try {
            Optional<Gestao> gestaoOpt = gestaoService.buscarPorId(idGestao);
            if (gestaoOpt.isEmpty())
                throw new RuntimeException("A gestão informada não foi localizada.");

            jetonService.estornarFolhaEmLote(gestaoOpt.get(), mes, ano);
            ra.addFlashAttribute("sucesso", "A folha de " + mes + "/" + ano
                    + " foi integralmente estornada. Todos os saldos e atividades foram revertidos!");

        } catch (RuntimeException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro interno ao estornar a folha em lote: " + e.getMessage());
        }
        return "redirect:/jeton";
    }

    @PostMapping("/fechar-definitivo")
    public String fecharDefinitivo(
            @RequestParam("idGestao") Integer idGestao,
            @RequestParam("mes") Integer mes,
            @RequestParam("ano") Integer ano,
            RedirectAttributes ra) {

        try {
            Optional<Gestao> gestaoOpt = gestaoService.buscarPorId(idGestao);
            if (gestaoOpt.isEmpty()) {
                throw new RuntimeException("A gestão informada não foi localizada.");
            }

            // Invoca o serviço para mudar o status de C+S para F+S
            jetonService.realizarFechamentoDefinitivoFolha(gestaoOpt.get(), mes, ano);

            ra.addFlashAttribute("sucesso",
                    "Folha de pagamento fechada e homologada definitivamente para a competência " + mes + "/" + ano
                            + "! Todas as atividades foram bloqueadas.");

        } catch (RuntimeException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro interno ao homologar o fechamento: " + e.getMessage());
        }

        return "redirect:/jeton";
    }
}