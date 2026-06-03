package br.com.cremepe.jeton.aspecto;

import br.com.cremepe.jeton.anotacao.Auditar;
import br.com.cremepe.jeton.anotacao.AuditoriaContext;
import br.com.cremepe.jeton.anotacao.AuditoriaUser;
import br.com.cremepe.jeton.servico.LogJetonService;
import br.com.cremepe.jeton.servico.UsuarioLogadoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Aspect
@Component
public class AuditoriaAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaAspect.class);
    private final ExpressionParser spelParser = new SpelExpressionParser();
    private final ObjectMapper mapper;

    @Autowired
    private UsuarioLogadoService usuarioLogadoService;

    @Autowired
    private LogJetonService logJetonService;

    public AuditoriaAspect() {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Before("@annotation(auditar)")
    public void beforeAudit(JoinPoint joinPoint, Auditar auditar) {
        AuditoriaUser usuario = usuarioLogadoService.getUsuarioLogado();
        if (usuario == null) {
            log.warn("Tentativa de auditar ação sem usuário logado: {}", auditar.acao());
            return;
        }
        AuditoriaContext.setUsuario(usuario);
    }

    @AfterReturning(pointcut = "@annotation(auditar)", returning = "retorno")
    public void afterReturning(JoinPoint joinPoint, Auditar auditar, Object retorno) {
        try {
            registrarAuditoria(joinPoint, auditar, retorno, null);
        } catch (Exception e) {
            log.error("Erro ao registrar auditoria (afterReturning)", e);
        } finally {
            AuditoriaContext.clear();
        }
    }

    @AfterThrowing(pointcut = "@annotation(auditar)", throwing = "ex")
    public void afterThrowing(JoinPoint joinPoint, Auditar auditar, Exception ex) {
        if (auditar.auditarExcecao()) {
            try {
                registrarAuditoria(joinPoint, auditar, null, ex);
            } catch (Exception e) {
                log.error("Erro ao registrar auditoria (afterThrowing)", e);
            } finally {
                AuditoriaContext.clear();
            }
        } else {
            AuditoriaContext.clear();
        }
    }

    private void registrarAuditoria(JoinPoint joinPoint, Auditar auditar, Object retorno, Exception ex) {
        AuditoriaUser usuario = AuditoriaContext.getUsuario();
        if (usuario == null)
            return;

        HttpServletRequest request = obterRequest();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        Map<String, Object> dados = new HashMap<>();
        dados.put("idUnico", UUID.randomUUID().toString());
        dados.put("timestamp", LocalDateTime.now().toString());
        dados.put("usuarioId", usuario.id());
        dados.put("usuarioNome", usuario.nome());
        dados.put("acao", auditar.acao());
        dados.put("tabela", auditar.tabela());
        dados.put("descricao", auditar.descricao());
        dados.put("sucesso", ex == null);
        dados.put("classe", signature.getDeclaringTypeName());
        dados.put("metodo", signature.getName());
        dados.put("ip", obterIpCliente(request));
        dados.put("url", request != null ? request.getRequestURL().toString() : null);
        dados.put("httpMethod", request != null ? request.getMethod() : null);
        dados.put("userAgent", request != null ? request.getHeader("User-Agent") : null);

        if (ex != null) {
            dados.put("erro", ex.getMessage());
            dados.put("stackTrace", obterStackTrace(ex));
        }

        // --- Parâmetros (SpEL ou automático) ---
        if (!auditar.dadosParametros().isEmpty()) {
            Object paramValue = avaliarSpel(joinPoint, auditar.dadosParametros(), null);
            dados.put("parametros", paramValue);
        } else {
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                try {
                    Map<String, Object> params = converterParaMap(args[0]);
                    dados.put("parametros", params);
                } catch (Exception e) {
                    log.debug("Não foi possível serializar automaticamente o primeiro parâmetro", e);
                    dados.put("parametros", "Não foi possível extrair os parâmetros");
                }
            }
        }

        // --- Retorno (SpEL ou automático) ---
        if (retorno != null) {
            if (!auditar.dadosRetorno().isEmpty()) {
                Object retValue = avaliarSpel(joinPoint, auditar.dadosRetorno(), retorno);
                dados.put("retorno", retValue);
            } else {
                try {
                    Map<String, Object> retMap = converterParaMap(retorno);
                    dados.put("retorno", retMap);
                } catch (Exception e) {
                    log.debug("Não foi possível serializar automaticamente o retorno", e);
                    dados.put("retorno", "Não foi possível extrair o retorno");
                }
            }
        }

        String json = toJson(dados);
        logJetonService.registrarLog(auditar.tabela(), usuario.id(), json);
        log.debug("Auditoria registrada com sucesso para ação: {} - usuário: {}", auditar.acao(), usuario.nome());
    }

    // =========================================================================
    // Métodos auxiliares (idênticos aos anteriores)
    // =========================================================================
    private HttpServletRequest obterRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs != null ? attrs.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String obterIpCliente(HttpServletRequest request) {
        if (request == null)
            return null;
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private String obterStackTrace(Exception ex) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : ex.getStackTrace()) {
            sb.append(element.toString()).append("\n");
        }
        return sb.toString();
    }

    private Object avaliarSpel(JoinPoint joinPoint, String expressao, Object retorno) {
        try {
            StandardEvaluationContext context = new StandardEvaluationContext();
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Object[] args = joinPoint.getArgs();
            String[] paramNames = signature.getParameterNames();
            for (int i = 0; i < args.length; i++) {
                context.setVariable("p" + i, args[i]);
                if (paramNames != null && i < paramNames.length) {
                    context.setVariable(paramNames[i], args[i]);
                }
            }
            context.setVariable("result", retorno);
            return spelParser.parseExpression(expressao).getValue(context);
        } catch (Exception e) {
            log.error("Erro ao avaliar expressão SpEL: '{}' - {}", expressao, e.getMessage());
            return "ERRO: " + e.getMessage();
        }
    }

    private Map<String, Object> converterParaMap(Object obj) {
        try {
            return mapper.convertValue(obj, Map.class);
        } catch (Exception e) {
            log.error("Erro ao converter objeto para Map (auditoria)", e);
            return Map.of("erro", "Não foi possível serializar o objeto");
        }
    }

    private String toJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Erro ao serializar objeto para JSON (auditoria)", e);
            return "{}";
        }
    }
}