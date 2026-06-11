package br.com.cremepe.jeton.dto;

import java.time.LocalDate;

public record GestaoDTO(
        Integer id,
        String nome,
        LocalDate dataInicio,
        LocalDate dataFim) {
}