package br.com.cremepe.jeton.anotacao;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditar {
    String tabela();

    String acao();

    String descricao() default "";

    String dadosRetorno() default "";

    String dadosParametros() default "";

    boolean isUpdate() default false;

    boolean auditarExcecao() default false;
}