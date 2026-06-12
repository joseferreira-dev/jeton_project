package br.com.cremepe.jeton.dto;

import java.util.List;

public record RelatorioConselheiroDTO(
        String nomeConselheiro,
        List<AtividadeVinculadaDTO> atividades,
        Integer saldoExistente,
        Integer saldoAtividades,
        Integer saldoUtilizado,
        Integer saldoFuturo) {
}