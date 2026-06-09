package br.com.cremepe.jeton.api;

import br.com.cremepe.jeton.service.JetonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/jeton")
public class JetonApiController {

    @Autowired
    private JetonService jetonService;

    @GetMapping("/atividades/conselheiro/{idPessoa}/gestao/{idGestao}/mes/{mes}/ano/{ano}")
    public List<Map<String, Object>> obterAtividadesVinculadas(
            @PathVariable("idPessoa") Integer idPessoa,
            @PathVariable("idGestao") Integer idGestao,
            @PathVariable("mes") Integer mes,
            @PathVariable("ano") Integer ano) {
        return jetonService.listarAtividadesAgrupadasPorConselheiro(idPessoa, idGestao, mes, ano);
    }

    @GetMapping("/relatorio-conselheiro/{idPessoa}/gestao/{idGestao}/mes/{mes}/ano/{ano}")
    public Map<String, Object> relatorioConselheiro(
            @PathVariable Integer idPessoa,
            @PathVariable Integer idGestao,
            @PathVariable Integer mes,
            @PathVariable Integer ano) {
        return jetonService.gerarRelatorioIndividualConselheiro(idPessoa, idGestao, mes, ano);
    }
}