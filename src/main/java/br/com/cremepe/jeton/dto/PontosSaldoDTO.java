package br.com.cremepe.jeton.dto;

import java.time.LocalDateTime;

public record PontosSaldoDTO(
        Integer id,
        Integer pontosTrabalhados,
        Integer pontosUtilizados,
        Integer pontosSobrando,
        String situacao,
        LocalDateTime dataHora,
        Integer idAtividade,
        Integer idConselheiro,
        Integer idGestao,
        Integer idResolucao) {
}