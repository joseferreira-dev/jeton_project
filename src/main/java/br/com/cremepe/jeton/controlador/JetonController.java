package br.com.cremepe.jeton.controlador;

import br.com.cremepe.jeton.dominio.Gestao;
import br.com.cremepe.jeton.dominio.Jeton;
import br.com.cremepe.jeton.servico.GestaoService;
import br.com.cremepe.jeton.servico.JetonService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/jeton")
public class JetonController {

    @Autowired
    private JetonService jetonService;
    @Autowired
    private GestaoService gestaoService;

    @GetMapping
    public String listar(
            @RequestParam(value = "idGestao", required = false) Integer idGestao,
            @RequestParam(value = "mes", required = false) Integer mes,
            @RequestParam(value = "ano", required = false) Integer ano,
            Model model, HttpSession session) {

        if (session.getAttribute("usuarioLogado") == null)
            return "redirect:/login";

        LocalDate hoje = LocalDate.now();
        Integer mesFiltro = mes != null ? mes : hoje.getMonthValue();
        Integer anoFiltro = ano != null ? ano : hoje.getYear();

        List<Jeton> listaBruta = new ArrayList<>();
        if (idGestao != null) {
            listaBruta = jetonService.listarTodos().stream()
                    .filter(j -> j.getGestao().getIdGestao().equals(idGestao) && j.getMes().equals(mesFiltro)
                            && j.getAno().equals(anoFiltro))
                    .toList();
            model.addAttribute("idGestaoSelecionada", idGestao);
        }

        // AGRUPAMENTO VISUAL: Junta os valores em 1 linha por conselheiro
        Map<Integer, Jeton> agrupado = new LinkedHashMap<>();
        for (Jeton j : listaBruta) {
            Integer idPessoa = j.getConselheiro().getIdPessoa();
            if (agrupado.containsKey(idPessoa)) {
                Jeton existente = agrupado.get(idPessoa);
                existente.setTotalJeton(existente.getTotalJeton() + j.getTotalJeton());
                existente.setValor(existente.getValor().add(j.getValor()));
            } else {
                Jeton clone = new Jeton();
                clone.setConselheiro(j.getConselheiro());
                clone.setGestao(j.getGestao());
                clone.setMes(j.getMes());
                clone.setAno(j.getAno());
                clone.setTotalJeton(j.getTotalJeton());
                clone.setValor(j.getValor());
                clone.setInSituacao(j.getInSituacao());
                agrupado.put(idPessoa, clone);
            }
        }

        model.addAttribute("listaJetons", agrupado.values());
        model.addAttribute("listaGestoes", gestaoService.listarTodos());
        model.addAttribute("mesAtual", mesFiltro);
        model.addAttribute("anoAtual", anoFiltro);

        return "jeton/lista";
    }

    @PostMapping("/processar")
    public String processar(@RequestParam("idGestao") Integer idGestao, @RequestParam("mes") Integer mes,
            @RequestParam("ano") Integer ano, RedirectAttributes ra) {
        try {
            Optional<Gestao> gestaoOpt = gestaoService.buscarPorId(idGestao);
            jetonService.processarFechamentoMensal(gestaoOpt.get(), mes, ano);
            ra.addFlashAttribute("sucesso", "Cálculo da folha mensal executado com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao processar: " + e.getMessage());
        }
        // Redireciona aplicando o filtro automaticamente para a tela carregar os
        // resultados
        return "redirect:/jeton?idGestao=" + idGestao + "&mes=" + mes + "&ano=" + ano;
    }

    @GetMapping("/estornar/conselheiro/{idPessoa}/gestao/{idGestao}/mes/{mes}/ano/{ano}")
    public String estornarPorConselheiro(
            @PathVariable("idPessoa") Integer idPessoa, @PathVariable("idGestao") Integer idGestao,
            @PathVariable("mes") Integer mes, @PathVariable("ano") Integer ano, RedirectAttributes ra) {
        try {
            jetonService.estornarFolhaDoConselheiro(idPessoa, idGestao, mes, ano);
            ra.addFlashAttribute("sucesso", "Processamento do conselheiro estornado com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao processar o estorno: " + e.getMessage());
        }
        return "redirect:/jeton?idGestao=" + idGestao + "&mes=" + mes + "&ano=" + ano;
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

    @GetMapping("/estornar-lote/gestao/{idGestao}/mes/{mes}/ano/{ano}")
    public String estornarEmLote(
            @PathVariable("idGestao") Integer idGestao,
            @PathVariable("mes") Integer mes,
            @PathVariable("ano") Integer ano,
            RedirectAttributes ra) {
        try {
            // Invoca o serviço que limpa os processamentos de todos os conselheiros do mês
            jetonService.estornarFolhaEmLote(idGestao, mes, ano);
            ra.addFlashAttribute("sucesso",
                    "A folha inteira foi estornada com sucesso! Todas as atividades foram devolvidas.");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao estornar folha em lote: " + e.getMessage());
        }
        return "redirect:/jeton?idGestao=" + idGestao + "&mes=" + mes + "&ano=" + ano;
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

    @GetMapping("/estornar/{id}")
    public String estornarJetonIndividual(@PathVariable("id") Integer idJeton, RedirectAttributes ra) {
        try {
            jetonService.estornarJetonPontual(idJeton);
            ra.addFlashAttribute("sucesso",
                    "Processamento estornado com sucesso! Os pontos e atividades foram devolvidos ao estado anterior.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro interno ao processar o estorno: " + e.getMessage());
        }
        return "redirect:/jeton/lista"; // Certifique-se de que a rota de retorno coincide com a sua listagem
    }
}