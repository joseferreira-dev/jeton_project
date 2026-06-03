package br.com.cremepe.jeton.anotacao;

public final class AuditoriaContext {

    private static final ThreadLocal<Integer> USUARIO = new ThreadLocal<>();

    private AuditoriaContext() {
    }

    public static void setUsuario(Integer id) {
        USUARIO.set(id);
    }

    public static Integer getUsuario() {
        return USUARIO.get();
    }

    public static void clear() {
        USUARIO.remove();
    }
}