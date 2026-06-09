package br.com.cremepe.jeton.util;

import br.com.cremepe.jeton.annotation.Auditar;
import br.com.cremepe.jeton.annotation.AuditoriaUser;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class DadosAuditoriaBuilder {

    public Map<String, Object> construir(
            AuditoriaUser usuario,
            Auditar auditar,
            JoinPoint joinPoint,
            HttpServletRequest request,
            MethodSignature signature,
            Object retorno,
            Exception ex,
            Map<String, Object> estadoAnterior,
            SpelEvaluator spelEvaluator,
            JsonConverter jsonConverter) {

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

        if (auditar.capturarEstadoAnterior() && estadoAnterior != null) {
            dados.put("valoresAnteriores", estadoAnterior);
        }

        // Parâmetros
        if (!auditar.dadosParametros().isEmpty()) {
            dados.put("parametros", spelEvaluator.avaliar(joinPoint, auditar.dadosParametros(), null));
        } else {
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                Object primeiroParam = args[0];
                if (primeiroParam != null && !(primeiroParam instanceof Number)) {
                    try {
                        dados.put("parametros", jsonConverter.converterParaMap(primeiroParam));
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
        if (retorno != null && returnType != void.class && returnType != Void.class && auditar.incluirRetorno()) {
            if (!auditar.dadosRetorno().isEmpty()) {
                dados.put("retorno", spelEvaluator.avaliar(joinPoint, auditar.dadosRetorno(), retorno));
            } else if (isEntidade(retorno)) {
                try {
                    dados.put("valoresAtuais", jsonConverter.converterParaMap(retorno));
                } catch (Exception e) {
                    dados.put("retorno", retorno.toString());
                }
            } else {
                dados.put("retorno", retorno);
            }
        }

        return dados;
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

    private boolean isEntidade(Object obj) {
        if (obj == null)
            return false;
        String packageName = obj.getClass().getPackageName();
        return packageName.startsWith("br.com.cremepe.jeton.domain");
    }
}