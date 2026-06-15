package br.com.cremepe.jeton.aspect;

import br.com.cremepe.jeton.service.LogJetonService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ServiceExceptionLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(ServiceExceptionLoggingAspect.class);

    private final LogJetonService logJetonService;

    public ServiceExceptionLoggingAspect(LogJetonService logJetonService) {
        this.logJetonService = logJetonService;
    }

    @Pointcut("within(@org.springframework.stereotype.Service *)")
    public void serviceBean() {
    }

    @AfterThrowing(pointcut = "serviceBean()", throwing = "ex")
    public void logServiceException(JoinPoint joinPoint, Exception ex) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        String tabela = "service_" + className.toLowerCase();
        String acao = methodName.toUpperCase();
        String descricao = "Exceção não tratada em " + className + "." + methodName;

        logJetonService.logErro(tabela, acao, descricao, ex, args);
    }
}