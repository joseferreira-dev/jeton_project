package br.com.cremepe.jeton.dto;

import java.math.BigDecimal;

public record JetonDTO(
        Integer id,
        Integer idConselheiro,
        String nomeConselheiro,
        Integer idGestao,
        String nomeGestao,
        Integer mes,
        Integer ano,
        Integer totalJeton,
        BigDecimal valor,
        String situacao) {
}