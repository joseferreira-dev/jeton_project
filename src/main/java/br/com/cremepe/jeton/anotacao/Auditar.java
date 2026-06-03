package br.com.cremepe.jeton.anotacao;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditar {

    String tabela();

    String acao(); // Ex: "CRIAR", "ATUALIZAR", "EXCLUIR", "VALIDAR"

    String descricao() default "";

    /**
     * Expressão SpEL para extrair dados adicionais do retorno.
     */
    String dadosRetorno() default "";

    /**
     * Expressão SpEL para extrair dados dos parâmetros.
     */
    String dadosParametros() default "";

    /**
     * Indica se o método é de atualização (para buscar estado anterior).
     * Se true, tenta obter o ID da entidade para consulta prévia.
     */
    boolean isUpdate() default false;

    /**
     * Auditar mesmo em caso de exceção.
     */
    boolean auditarExcecao() default false;
}