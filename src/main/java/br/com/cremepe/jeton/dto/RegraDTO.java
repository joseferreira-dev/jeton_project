package br.com.cremepe.jeton.dto;

public record RegraDTO(
        Integer id,
        String nome,
        String descricao,
        Integer pontos,
        String inRevogado,
        Integer pontosLimitesTurno,
        String inJudicante,
        Integer resolucaoId,
        Integer portariaId) {
}