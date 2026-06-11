package br.com.cremepe.jeton.dto;

import java.time.LocalDateTime;

public record AtividadeConselhalDTO(
        Integer id,
        Integer idGestao,
        String nomeGestao,
        Integer idConselheiro,
        String nomeConselheiro,
        Integer idRegra,
        String nomeRegra,
        String descricaoRegra,
        Integer pontosRegra,
        Integer idComprovante,
        String nomeComprovanteUsuario,
        String nomeArquivo,
        Integer idTipoAnexo,
        String nomeTipoAnexo,
        Integer qtdAtividade,
        LocalDateTime dataHoraAtividade,
        LocalDateTime dataHoraRegistro,
        String turno,
        String situacao,
        String computada) {
}