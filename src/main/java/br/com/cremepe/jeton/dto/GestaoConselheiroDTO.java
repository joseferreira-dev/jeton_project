package br.com.cremepe.jeton.dto;

public record GestaoConselheiroDTO(
        Integer idGestao,
        String nomeGestao,
        Integer idConselheiro,
        String nomeConselheiro,
        String situacao) {
}