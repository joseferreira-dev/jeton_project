package br.com.cremepe.jeton.dto;

public record ComprovanteDTO(
        Integer id,
        String nomeComprovante,
        String nomeArquivo,
        String contentType,
        Integer mes,
        Integer ano,
        Integer idTipoAnexo,
        String tipoAnexoNome) {
}