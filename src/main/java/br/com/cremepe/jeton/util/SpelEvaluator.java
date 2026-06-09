package br.com.cremepe.jeton.util;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

@Component
public class SpelEvaluator {

    private static final Logger log = LoggerFactory.getLogger(SpelEvaluator.class);
    private final ExpressionParser parser = new SpelExpressionParser();

    public Object avaliar(JoinPoint joinPoint, String expressao, Object retorno) {
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
            return parser.parseExpression(expressao).getValue(context);
        } catch (Exception e) {
            log.error("Erro ao avaliar expressão SpEL: '{}' - {}", expressao, e.getMessage());
            return "ERRO: " + e.getMessage();
        }
    }
}