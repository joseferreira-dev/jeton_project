package br.com.cremepe.jeton.anotacao;

public final class AuditoriaContext {

    private static final ThreadLocal<AuditoriaUser> USUARIO = new ThreadLocal<>();

    private AuditoriaContext() {
    }

    public static void setUsuario(AuditoriaUser usuario) {
        USUARIO.set(usuario);
    }

    public static AuditoriaUser getUsuario() {
        return USUARIO.get();
    }

    public static void clear() {
        USUARIO.remove();
    }
}