package br.com.cremepe.jeton.dto;

public record UsuarioAcessoDTO(
        Integer idUsuario,
        String idNivel,
        String usuarioNome,
        String nivelNome) {
}