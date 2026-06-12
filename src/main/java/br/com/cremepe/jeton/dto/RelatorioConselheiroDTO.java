package br.com.cremepe.jeton.dto;

import java.util.List;

public record RelatorioConselheiroDTO(
        String nomeConselheiro,
        List<AtividadeVinculadaDTO> atividades,
        Integer saldoAnterior,
        Integer pontosAcumuladosMes,
        Integer saldoFuturo) {
}