package br.com.cremepe.jeton.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ResolucaoDTO(
        Integer id,
        Integer numero,
        Integer ano,
        LocalDate dtInicioVigencia,
        LocalDate dtFimVigencia,
        String linkPublicado,
        String inRevogado,
        String ementa,
        Integer pontosPorJeton,
        Integer maxJetonsDia,
        Integer maxJetonsPeriodo,
        Integer maxJetonsMes,
        BigDecimal valorJeton) {
}