package br.com.cremepe.jeton.dto;

public record ConselheiroResponseDTO(
        Integer id,
        String nome,
        String email,
        String cpf,
        Integer crm,
        String situacao) {
}