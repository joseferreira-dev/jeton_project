package br.com.cremepe.jeton.dto;

public record UsuarioDTO(
        Integer id,
        String nome,
        String email,
        String situacao) {
}