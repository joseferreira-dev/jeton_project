package br.com.cremepe.jeton.aspecto;

import br.com.cremepe.jeton.anotacao.Auditar;
import br.com.cremepe.jeton.anotacao.AuditoriaContext;
import br.com.cremepe.jeton.anotacao.AuditoriaUser;
import br.com.cremepe.jeton.servico.LogJetonService;
import br.com.cremepe.jeton.servico.UsuarioLogadoService;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.hibernate.proxy.HibernateProxy;
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
import java.util.*;

@Aspect
@Component
public class AuditoriaAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaAspect.class);
    private final ExpressionParser spelParser = new SpelExpressionParser();
    private final ObjectMapper mapper;
    private final ThreadLocal<Map<String, Object>> estadoAnteriorThreadLocal = new ThreadLocal<>();
    private final ThreadLocal<Integer> profundidade = ThreadLocal.withInitial(() -> 0);

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private UsuarioLogadoService usuarioLogadoService;

    @Autowired
    private LogJetonService logJetonService;

    public AuditoriaAspect() {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        this.mapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
        SimpleFilterProvider filterProvider = new SimpleFilterProvider();
        filterProvider.addFilter("hibernateLazyFilter", SimpleBeanPropertyFilter.serializeAllExcept(
                "hibernateLazyInitializer", "handler", "fieldHandler"));
        this.mapper.setFilterProvider(filterProvider);
    }

    @Before("@annotation(auditar)")
    public void beforeAudit(JoinPoint joinPoint, Auditar auditar) {
        profundidade.set(profundidade.get() + 1);
        System.out.println(
                ">>> BeforeAudit chamado para: " + auditar.acao() + " (profundidade=" + profundidade.get() + ")");
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

    private void capturarEstadoAnterior(JoinPoint joinPoint, Auditar auditar) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args == null || args.length == 0)
                return;

            Object primeiroParam = args[0];
            // Desfaz proxy se necessário
            if (primeiroParam instanceof HibernateProxy) {
                primeiroParam = ((HibernateProxy) primeiroParam).getHibernateLazyInitializer().getImplementation();
            }

            Object id = extrairId(primeiroParam);
            Class<?> entityType = determinarTipoEntidade(joinPoint, primeiroParam, auditar.tabela());

            if (id != null && entityType != null) {
                Object estadoAtual = entityManager.find(entityType, id);
                if (estadoAtual != null) {
                    Map<String, Object> copia = converterParaMap(estadoAtual);
                    estadoAnteriorThreadLocal.set(copia);
                    log.debug("Estado anterior capturado (cópia) para auditoria: {} ID={}",
                            entityType.getSimpleName(), id);
                } else {
                    log.warn("Estado anterior não encontrado para entidade {} ID={}", entityType.getSimpleName(), id);
                }
            } else {
                log.debug("Não foi possível extrair ID ou tipo de entidade para auditoria anterior");
            }
        } catch (Exception e) {
            log.error("Erro ao capturar estado anterior para auditoria", e);
        }
    }

    private Object extrairId(Object obj) {
        if (obj == null)
            return null;
        if (obj instanceof Number)
            return obj;

        try {
            // Tenta qualquer método público que comece com "getId" e não tenha parâmetros
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

        // Fallback para campos públicos
        try {
            java.lang.reflect.Field field = obj.getClass().getDeclaredField("id");
            field.setAccessible(true);
            return field.get(obj);
        } catch (NoSuchFieldException e) {
            try {
                java.lang.reflect.Field field = obj.getClass().getDeclaredField("idAtividade");
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

    private Class<?> determinarTipoEntidade(JoinPoint joinPoint, Object primeiroParam, String nomeTabela) {
        if (primeiroParam != null && !(primeiroParam instanceof Number)) {
            return primeiroParam.getClass();
        }

        String className = switch (nomeTabela) {
            case "atividade_conselhal" -> "br.com.cremepe.jeton.dominio.AtividadeConselhal";
            case "gestao_conselheiro" -> "br.com.cremepe.jeton.dominio.GestaoConselheiro";
            case "pontos_saldo" -> "br.com.cremepe.jeton.dominio.PontosSaldo";
            case "usuario_acesso" -> "br.com.cremepe.jeton.dominio.UsuarioAcesso";
            default -> {
                String[] parts = nomeTabela.split("_");
                StringBuilder camel = new StringBuilder();
                for (String part : parts) {
                    if (camel.length() == 0) {
                        camel.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase());
                    } else {
                        camel.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase());
                    }
                }
                yield "br.com.cremepe.jeton.dominio." + camel.toString();
            }
        };
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            log.error("Não foi possível determinar a classe da entidade para tabela: {}", nomeTabela);
            return null;
        }
    }

    @AfterReturning(pointcut = "@annotation(auditar)", returning = "retorno")
    public void afterReturning(JoinPoint joinPoint, Auditar auditar, Object retorno) {
        try {
            System.out.println(">>> AfterReturning chamado para: " + auditar.acao() + " (profundidade="
                    + profundidade.get() + ")");
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

    // =========================================================================
    // REGISTRO CENTRAL
    // =========================================================================
    private void registrarAuditoria(JoinPoint joinPoint, Auditar auditar, Object retorno, Exception ex) {
        AuditoriaUser usuario = AuditoriaContext.getUsuario();
        if (usuario == null) {
            log.warn("Usuário não encontrado no contexto para auditoria. Ação: {}", auditar.acao());
            return;
        }

        HttpServletRequest request = obterRequest();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        Map<String, Object> dados = new LinkedHashMap<>();
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

        // Estado anterior
        Map<String, Object> estadoAnterior = estadoAnteriorThreadLocal.get();
        if (estadoAnterior != null) {
            dados.put("valoresAnteriores", estadoAnterior);
        }

        // Parâmetros (SpEL)
        Object paramValue = null;
        if (!auditar.dadosParametros().isEmpty()) {
            paramValue = avaliarSpel(joinPoint, auditar.dadosParametros(), null);
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

        // Retorno
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
    // MÉTODOS AUXILIARES
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
        if (obj == null)
            return Map.of();
        try {
            if (obj instanceof HibernateProxy) {
                obj = ((HibernateProxy) obj).getHibernateLazyInitializer().getImplementation();
            }
            // Usa o mapper já configurado com filtro lazy
            return mapper.convertValue(obj, Map.class);
        } catch (Exception e) {
            log.error("Erro ao converter objeto para Map (auditoria). Usando fallback.", e);
            return extractBasicFields(obj);
        }
    }

    private Map<String, Object> extractBasicFields(Object obj) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("_class", obj.getClass().getSimpleName());
        try {
            java.lang.reflect.Field idField = obj.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            map.put("id", idField.get(obj));
        } catch (Exception e) {
            try {
                java.lang.reflect.Method m = obj.getClass().getMethod("getId");
                map.put("id", m.invoke(obj));
            } catch (Exception ignored) {
            }
        }
        return map;
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