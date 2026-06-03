package br.com.cremepe.jeton.aspecto;

import br.com.cremepe.jeton.anotacao.Auditar;
import br.com.cremepe.jeton.anotacao.AuditoriaContext;
import br.com.cremepe.jeton.dominio.Gestao;
import br.com.cremepe.jeton.dto.AuditoriaDTO;
import br.com.cremepe.jeton.servico.LogJetonService;
import br.com.cremepe.jeton.util.AuditoriaUtils;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Aspect
@Component
public class AuditoriaAspect {

    @Autowired
    private LogJetonService logJetonService;

    @AfterReturning(value = "@annotation(auditar)", returning = "retorno")
    public void auditar(
            JoinPoint joinPoint,
            Auditar auditar,
            Object retorno) {

        Integer idUsuario = AuditoriaContext.getUsuario();
        System.out.println("AUDITORIA EXECUTADA");

        if (idUsuario == null) {
            return;
        }

        AuditoriaDTO dto = new AuditoriaDTO();

        Map<String, Object> dados = new LinkedHashMap<>();

        if (retorno instanceof Gestao gestao) {

            dados.put("idGestao", gestao.getIdGestao());
            dados.put("nomeGestao", gestao.getNomeGestao());
            dados.put("dtInicio", gestao.getDtInicio());
            dados.put("dtFim", gestao.getDtFim());
        }

        dto.setTabela(auditar.tabela());
        dto.setAcao(auditar.acao());
        dto.setUsuario(AuditoriaContext.getUsuario());
        dto.setDataHora(LocalDateTime.now());
        dto.setDados(dados);

        String json = AuditoriaUtils.toJson(dto);

        logJetonService.registrarLog(
                auditar.tabela(),
                idUsuario,
                json);
    }
}