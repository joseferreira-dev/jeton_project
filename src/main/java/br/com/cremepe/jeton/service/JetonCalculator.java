package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.domain.PontosSaldo;
import br.com.cremepe.jeton.domain.Resolucao;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class JetonCalculator {

    public ResultadoAbsorcao calcularJetons(List<PontosSaldo> filaUnificada, int maxJetonsPermitidos) {
        int bufferPontos = 0;
        int totalJetons = 0;
        int totalPontosConsumidos = 0;
        Map<Resolucao, Integer> demonstrativo = new LinkedHashMap<>();

        for (PontosSaldo saldo : filaUnificada) {
            bufferPontos += saldo.getPontosSobrando();

            Resolucao norma = saldo.getResolucao();
            int pontosPorJeton = (norma.getPontosPorJeton() != null && norma.getPontosPorJeton() > 0)
                    ? norma.getPontosPorJeton()
                    : 3;

            int jetonsPossiveis = bufferPontos / pontosPorJeton;
            int jetonsEfetivos = Math.min(jetonsPossiveis, maxJetonsPermitidos - totalJetons);

            if (jetonsEfetivos > 0) {
                demonstrativo.merge(norma, jetonsEfetivos, Integer::sum);
                totalJetons += jetonsEfetivos;
                int pontosConsumidos = jetonsEfetivos * pontosPorJeton;
                bufferPontos -= pontosConsumidos;
                totalPontosConsumidos += pontosConsumidos;
            }
        }

        return new ResultadoAbsorcao(demonstrativo, totalJetons, totalPontosConsumidos);
    }

    public record ResultadoAbsorcao(
            Map<Resolucao, Integer> demonstrativo,
            int totalJetons,
            int totalPontosConsumidos) {
    }
}