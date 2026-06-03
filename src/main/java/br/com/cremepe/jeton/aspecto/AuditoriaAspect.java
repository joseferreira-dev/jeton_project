package br.com.cremepe.jeton.aspecto;

import br.com.cremepe.jeton.anotacao.Auditar;
import br.com.cremepe.jeton.anotacao.AuditoriaContext;
import br.com.cremepe.jeton.anotacao.AuditoriaUser;
import br.com.cremepe.jeton.servico.LogJetonService;
import br.com.cremepe.jeton.servico.UsuarioLogadoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private UsuarioLogadoService usuarioLogadoService;

    @Autowired
    private LogJetonService logJetonService;

    private final ThreadLocal<Map<String, Object>> estadoAnteriorThreadLocal = new ThreadLocal<>();

    public AuditoriaAspect() {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // =========================================================================
    // BEFORE: captura o estado anterior (se necessário) e configura o usuário
    // =========================================================================
    @Before("@annotation(auditar)")
    public void beforeAudit(JoinPoint joinPoint, Auditar auditar) {
        AuditoriaUser usuario = usuarioLogadoService.getUsuarioLogado();
        if (usuario == null) {
            log.warn("Tentativa de auditar ação sem usuário logado: {}", auditar.acao());
            return;
        }
        AuditoriaContext.setUsuario(usuario);

        if (auditar.capturarEstadoAnterior() || auditar.acao().equals("EXCLUIR")) {
            capturarEstadoAnterior(joinPoint, auditar);
        }
    }

    /**
     * Captura o estado atual da entidade no banco de dados antes da
     * modificação/exclusão.
     * O primeiro parâmetro do método pode ser a entidade (com ID) ou o próprio ID.
     */
    private void capturarEstadoAnterior(JoinPoint joinPoint, Auditar auditar) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args == null || args.length == 0)
                return;

            Object primeiroParam = args[0];
            Object id = extrairId(primeiroParam);
            Class<?> entityType = determinarTipoEntidade(joinPoint, primeiroParam, auditar.tabela());

            if (id != null && entityType != null) {
                Object estadoAtual = entityManager.find(entityType, id);
                if (estadoAtual != null) {
                    // Cria uma cópia imutável (Map) em vez de guardar a referência
                    Map<String, Object> copia = converterParaMap(estadoAtual);
                    estadoAnteriorThreadLocal.set(copia);
                    log.debug("Estado anterior capturado (cópia) para auditoria: {} ID={}",
                            entityType.getSimpleName(), id);
                }
            }
        } catch (Exception e) {
            log.error("Erro ao capturar estado anterior para auditoria", e);
        }
    }

    /**
     * Extrai o ID do objeto (entidade) ou do próprio ID numérico.
     * Suporta métodos getter como getIdGestao(), getIdPessoa(), etc.
     */
    private Object extrairId(Object obj) {
        if (obj == null)
            return null;
        if (obj instanceof Number)
            return obj;

        // Tenta encontrar qualquer método público que comece com "getId" e não tenha
        // parâmetros
        try {
            java.lang.reflect.Method[] methods = obj.getClass().getMethods();
            for (java.lang.reflect.Method method : methods) {
                String name = method.getName();
                if (name.startsWith("getId") && method.getParameterCount() == 0 &&
                        method.getReturnType() != Void.class) {
                    return method.invoke(obj);
                }
            }
        } catch (Exception e) {
            log.debug("Erro ao extrair ID via getter: {}", e.getMessage());
        }

        // Fallback: campos públicos 'id' ou 'idGestao', etc.
        try {
            java.lang.reflect.Field field = obj.getClass().getDeclaredField("id");
            field.setAccessible(true);
            return field.get(obj);
        } catch (NoSuchFieldException e) {
            try {
                java.lang.reflect.Field field = obj.getClass().getDeclaredField("idGestao");
                field.setAccessible(true);
                return field.get(obj);
            } catch (NoSuchFieldException ex) {
                // ignora
            } catch (Exception ex) {
                log.debug("Erro ao acessar campo id: {}", ex.getMessage());
            }
        } catch (Exception e) {
            log.debug("Erro ao acessar campo id: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Determina o tipo da entidade a partir do primeiro parâmetro (se for entidade)
     * ou do nome da tabela.
     */
    private Class<?> determinarTipoEntidade(JoinPoint joinPoint, Object primeiroParam, String nomeTabela) {
        if (primeiroParam != null && !(primeiroParam instanceof Number)) {
            return primeiroParam.getClass();
        }

        // Se o primeiro parâmetro é um ID numérico, tenta obter a classe pelo nome da
        // tabela.
        // Converte "gestao" -> "Gestao", "portaria" -> "Portaria", etc.
        String className = "br.com.cremepe.jeton.dominio." +
                Character.toUpperCase(nomeTabela.charAt(0)) +
                nomeTabela.substring(1).toLowerCase();
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            log.error("Não foi possível determinar a classe da entidade para tabela: {}", nomeTabela);
            return null;
        }
    }

    // =========================================================================
    // AFTER RETURNING: registra auditoria com sucesso
    // =========================================================================
    @AfterReturning(pointcut = "@annotation(auditar)", returning = "retorno")
    public void afterReturning(JoinPoint joinPoint, Auditar auditar, Object retorno) {
        try {
            registrarAuditoria(joinPoint, auditar, retorno, null);
        } catch (Exception e) {
            log.error("Erro ao registrar auditoria (afterReturning)", e);
        } finally {
            limparContexto();
        }
    }

    // =========================================================================
    // AFTER THROWING: registra auditoria com erro (se configurado)
    // =========================================================================
    @AfterThrowing(pointcut = "@annotation(auditar)", throwing = "ex")
    public void afterThrowing(JoinPoint joinPoint, Auditar auditar, Exception ex) {
        if (auditar.auditarExcecao()) {
            try {
                registrarAuditoria(joinPoint, auditar, null, ex);
            } catch (Exception e) {
                log.error("Erro ao registrar auditoria (afterThrowing)", e);
            } finally {
                limparContexto();
            }
        } else {
            limparContexto();
        }
    }

    private void limparContexto() {
        AuditoriaContext.clear();
        estadoAnteriorThreadLocal.remove();
    }

    // =========================================================================
    // REGISTRO CENTRAL
    // =========================================================================
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
        }

        // Estado anterior (capturado no @Before)
        Map<String, Object> estadoAnterior = estadoAnteriorThreadLocal.get();
        if (estadoAnterior != null) {
            dados.put("valoresAnteriores", estadoAnterior);
        }

        // --- Parâmetros (SpEL ou automático) ---
        if (!auditar.dadosParametros().isEmpty()) {
            Object paramValue = avaliarSpel(joinPoint, auditar.dadosParametros(), null);
            dados.put("parametros", paramValue);
        } else {
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                Object primeiroParam = args[0];
                if (primeiroParam != null && !(primeiroParam instanceof Number)) {
                    try {
                        Map<String, Object> params = converterParaMap(primeiroParam);
                        dados.put("parametros", params);
                    } catch (Exception e) {
                        dados.put("parametros", primeiroParam.toString());
                    }
                } else {
                    dados.put("parametros", primeiroParam);
                }
            }
        }

        // --- Retorno (apenas se existir e não for void) ---
        Class<?> returnType = signature.getReturnType();
        if (retorno != null && returnType != void.class && returnType != Void.class) {
            if (!auditar.dadosRetorno().isEmpty()) {
                Object retValue = avaliarSpel(joinPoint, auditar.dadosRetorno(), retorno);
                dados.put("retorno", retValue);
            } else if (isEntidade(retorno)) {
                try {
                    Map<String, Object> retMap = converterParaMap(retorno);
                    dados.put("valoresAtuais", retMap);
                } catch (Exception e) {
                    dados.put("retorno", retorno.toString());
                }
            } else {
                dados.put("retorno", retorno);
            }
        }

        String json = toJson(dados);
        logJetonService.registrarLog(auditar.tabela(), usuario.id(), json);
        log.debug("Auditoria registrada: {} - {}", auditar.acao(), usuario.nome());
    }

    // =========================================================================
    // MÉTODOS AUXILIARES (já existentes, mantidos)
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

    private boolean isEntidade(Object obj) {
        if (obj == null)
            return false;
        String packageName = obj.getClass().getPackageName();
        return packageName.startsWith("br.com.cremepe.jeton.dominio");
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