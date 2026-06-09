package br.com.cremepe.jeton.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.JoinPoint;
import org.hibernate.proxy.HibernateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;

@Component
public class EstadoAnteriorCapturador {

    private static final Logger log = LoggerFactory.getLogger(EstadoAnteriorCapturador.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private JsonConverter jsonConverter;

    public Map<String, Object> capturar(JoinPoint joinPoint, String nomeTabela) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args == null || args.length == 0)
                return null;

            Object primeiroParam = args[0];
            if (primeiroParam instanceof HibernateProxy) {
                primeiroParam = ((HibernateProxy) primeiroParam).getHibernateLazyInitializer().getImplementation();
            }

            Object id = extrairId(primeiroParam);
            Class<?> entityType = determinarTipoEntidade(primeiroParam, nomeTabela);

            if (id != null && entityType != null) {
                Object estadoAtual = entityManager.find(entityType, id);
                if (estadoAtual != null) {
                    return jsonConverter.converterParaMap(estadoAtual);
                } else {
                    log.warn("Estado anterior não encontrado para entidade {} ID={}", entityType.getSimpleName(), id);
                }
            } else {
                log.debug("Não foi possível extrair ID ou tipo de entidade para auditoria anterior");
            }
        } catch (Exception e) {
            log.error("Erro ao capturar estado anterior para auditoria", e);
        }
        return null;
    }

    private Object extrairId(Object obj) {
        if (obj == null)
            return null;
        if (obj instanceof Number)
            return obj;

        // Tenta getters padrão
        try {
            for (Method method : obj.getClass().getMethods()) {
                String name = method.getName();
                if (name.startsWith("getId") && method.getParameterCount() == 0
                        && method.getReturnType() != Void.class) {
                    return method.invoke(obj);
                }
            }
        } catch (Exception e) {
            log.debug("Erro ao extrair ID via getter: {}", e.getMessage());
        }

        // Fallback para campos comuns
        String[] nomesCampos = { "id", "idAtividade" };
        for (String campo : nomesCampos) {
            try {
                java.lang.reflect.Field field = obj.getClass().getDeclaredField(campo);
                field.setAccessible(true);
                return field.get(obj);
            } catch (NoSuchFieldException ignored) {
            } catch (Exception e) {
                log.debug("Erro ao acessar campo {}: {}", campo, e.getMessage());
            }
        }
        return null;
    }

    private Class<?> determinarTipoEntidade(Object primeiroParam, String nomeTabela) {
        if (primeiroParam != null && !(primeiroParam instanceof Number)) {
            return primeiroParam.getClass();
        }

        // Mapeamento de nomes de tabela para classes
        String className = switch (nomeTabela) {
            case "atividade_conselhal" -> "br.com.cremepe.jeton.domain.AtividadeConselhal";
            case "gestao_conselheiro" -> "br.com.cremepe.jeton.domain.GestaoConselheiro";
            case "pontos_saldo" -> "br.com.cremepe.jeton.domain.PontosSaldo";
            case "usuario_acesso" -> "br.com.cremepe.jeton.domain.UsuarioAcesso";
            default -> {
                String[] parts = nomeTabela.split("_");
                StringBuilder camel = new StringBuilder();
                for (String part : parts) {
                    camel.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase());
                }
                yield "br.com.cremepe.jeton.domain." + camel.toString();
            }
        };
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            log.error("Não foi possível determinar a classe da entidade para tabela: {}", nomeTabela);
            return null;
        }
    }
}