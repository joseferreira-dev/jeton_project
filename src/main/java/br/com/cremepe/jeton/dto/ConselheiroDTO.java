package br.com.cremepe.jeton.dto;

public record ConselheiroDTO(
        Integer id,
        String nome,
        String email,
        String cpf,
        Integer crm,
        String situacao,
        String senha) {
}