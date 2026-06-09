package br.com.cremepe.jeton.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

public final class DataUtils {

    private DataUtils() {
    }

    public static LocalDateTime parseDataHora(String dataHoraStr, String mensagemErro) {
        if (dataHoraStr == null || dataHoraStr.trim().isEmpty()) {
            throw new IllegalArgumentException(mensagemErro != null ? mensagemErro : "Data e hora são obrigatórias.");
        }
        try {
            return LocalDateTime.parse(dataHoraStr);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    mensagemErro != null ? mensagemErro : "Formato de data/hora inválido. Use 'YYYY-MM-DDTHH:mm'.");
        }
    }
}