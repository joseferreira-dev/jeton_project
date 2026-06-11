package br.com.cremepe.jeton.dto;

public record TipoAnexoDTO(
        Integer id,
        String nome,
        boolean exigePublicacao) {
}