package br.com.cremepe.jeton.dto;

import java.time.LocalDate;

public record PortariaDTO(
        Integer id,
        Integer numero,
        Integer ano,
        LocalDate dtInicioVigencia,
        LocalDate dtFimVigencia,
        String linkPublicado,
        String inRevogado) {
}