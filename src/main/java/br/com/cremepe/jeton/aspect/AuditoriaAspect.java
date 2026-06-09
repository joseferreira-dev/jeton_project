package br.com.cremepe.jeton.aspect;

import br.com.cremepe.jeton.annotation.Auditar;
import br.com.cremepe.jeton.annotation.AuditoriaContext;
import br.com.cremepe.jeton.annotation.AuditoriaUser;
import br.com.cremepe.jeton.service.LogJetonService;
import br.com.cremepe.jeton.service.UsuarioLogadoService;
import br.com.cremepe.jeton.util.JsonConverter;
import br.com.cremepe.jeton.util.SpelEvaluator;
import br.com.cremepe.jeton.util.EstadoAnteriorCapturador;
import br.com.cremepe.jeton.util.DadosAuditoriaBuilder;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

@Aspect
@Component
public class AuditoriaAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaAspect.class);

    private final ThreadLocal<Integer> profundidade = ThreadLocal.withInitial(() -> 0);
    private final ThreadLocal<Map<String, Object>> estadoAnteriorThreadLocal = new ThreadLocal<>();

    @Autowired
    private UsuarioLogadoService usuarioLogadoService;

    @Autowired
    private LogJetonService logJetonService;

    @Autowired
    private EstadoAnteriorCapturador estadoAnteriorCapturador;

    @Autowired
    private SpelEvaluator spelEvaluator;

    @Autowired
    private JsonConverter jsonConverter;

    @Autowired
    private DadosAuditoriaBuilder dadosAuditoriaBuilder;

    @Before("@annotation(auditar)")
    public void beforeAudit(JoinPoint joinPoint, Auditar auditar) {
        profundidade.set(profundidade.get() + 1);

        AuditoriaUser usuario = usuarioLogadoService.getUsuarioLogado();
        if (usuario == null) {
            log.warn("Tentativa de auditar ação sem usuário logado: {}", auditar.acao());
            return;
        }
        AuditoriaContext.setUsuario(usuario);

        if (auditar.capturarEstadoAnterior()) {
            Map<String, Object> estadoAnterior = estadoAnteriorCapturador.capturar(joinPoint, auditar.tabela());
            if (estadoAnterior != null) {
                estadoAnteriorThreadLocal.set(estadoAnterior);
            }
        }
    }

    @AfterReturning(pointcut = "@annotation(auditar)", returning = "retorno")
    public void afterReturning(JoinPoint joinPoint, Auditar auditar, Object retorno) {
        try {
            registrarAuditoria(joinPoint, auditar, retorno, null);
        } catch (Exception e) {
            log.error("Erro ao registrar auditoria (afterReturning)", e);
        } finally {
            decrementarProfundidade();
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
                decrementarProfundidade();
            }
        } else {
            decrementarProfundidade();
        }
    }

    private void decrementarProfundidade() {
        int atual = profundidade.get() - 1;
        if (atual <= 0) {
            limparContexto();
            profundidade.remove();
        } else {
            profundidade.set(atual);
        }
    }

    private void limparContexto() {
        estadoAnteriorThreadLocal.remove();
        AuditoriaContext.clear();
        log.debug("Contexto de auditoria limpo (profundidade=0)");
    }

    private void registrarAuditoria(JoinPoint joinPoint, Auditar auditar, Object retorno, Exception ex) {
        AuditoriaUser usuario = AuditoriaContext.getUsuario();
        if (usuario == null) {
            log.warn("Usuário não encontrado no contexto para auditoria. Ação: {}", auditar.acao());
            return;
        }

        HttpServletRequest request = obterRequest();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        // Constrói os dados de auditoria usando o builder
        Map<String, Object> dados = dadosAuditoriaBuilder.construir(
                usuario, auditar, joinPoint, request, signature, retorno, ex,
                estadoAnteriorThreadLocal.get(), spelEvaluator, jsonConverter);

        // Remove o estado anterior da ThreadLocal após o uso
        if (auditar.capturarEstadoAnterior()) {
            estadoAnteriorThreadLocal.remove();
        }

        String json = jsonConverter.toJson(dados);
        logJetonService.registrarLog(auditar.tabela(), usuario.id(), json);
        log.debug("Auditoria registrada: {} - {}", auditar.acao(), usuario.nome());
    }

    private HttpServletRequest obterRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs != null ? attrs.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }
}