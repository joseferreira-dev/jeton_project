package br.com.cremepe.jeton.dto;

public record VinculoConselheiroDTO(
        Integer id,
        String nome,
        Integer crm,
        boolean temAtividade,
        String situacao) {
}