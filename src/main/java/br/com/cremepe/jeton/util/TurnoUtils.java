package br.com.cremepe.jeton.util;

import br.com.cremepe.jeton.domain.AtividadeConselhal;

public final class TurnoUtils {

    private TurnoUtils() {
    }

    public static String calcularTurno(int hora) {
        if (hora >= 6 && hora < 12) {
            return AtividadeConselhal.TURNO_MANHA;
        }
        if (hora >= 12 && hora < 18) {
            return AtividadeConselhal.TURNO_TARDE;
        }
        return AtividadeConselhal.TURNO_NOITE;
    }
}