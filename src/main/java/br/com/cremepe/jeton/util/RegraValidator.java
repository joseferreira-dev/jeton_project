package br.com.cremepe.jeton.util;

import br.com.cremepe.jeton.domain.Portaria;
import br.com.cremepe.jeton.domain.Regras;
import br.com.cremepe.jeton.domain.RegrasConjuntas;
import br.com.cremepe.jeton.repository.RegrasConjuntasRepository;
import br.com.cremepe.jeton.repository.RegrasRepository;
import org.springframework.stereotype.Component;

@Component
public class RegraValidator {

    private final RegrasRepository regrasRepository;
    private final RegrasConjuntasRepository regrasConjuntasRepository;

    public RegraValidator(RegrasRepository regrasRepository,
            RegrasConjuntasRepository regrasConjuntasRepository) {
        this.regrasRepository = regrasRepository;
        this.regrasConjuntasRepository = regrasConjuntasRepository;
    }

    public void validarNomeRegraUnico(String nome, Integer idAtual) {
        if (nome == null || nome.trim().isEmpty()) {
            return;
        }
        String nomeTrim = nome.trim();
        Integer id = idAtual != null ? idAtual : 0;
        boolean existe = regrasRepository.existsByNomeRegraAndIdRegraNot(nomeTrim, id);
        if (existe) {
            throw new RuntimeException("Já existe uma regra cadastrada com o nome '" + nomeTrim + "'.");
        }
    }

    public void validarPortariaNaoRevogada(Portaria portaria) {
        if (portaria != null && portaria.isRevogado()) {
            throw new RuntimeException("Não é possível vincular a regra a uma portaria revogada.");
        }
    }

    public void validarRegraNaoRevogada(Regras regra) {
        if (regra.isRevogado()) {
            throw new RuntimeException("A regra já está revogada.");
        }
    }

    public void validarRegraRevogada(Regras regra) {
        if (!regra.isRevogado()) {
            throw new RuntimeException("A regra já está em vigor.");
        }
    }

    public void validarExclusaoRegra(Regras regra, long countAtividades) {
        if (!regra.isRevogado()) {
            throw new RuntimeException("Para excluir, a regra deve estar revogada primeiro.");
        }
        if (countAtividades > 0) {
            throw new RuntimeException("Não é possível excluir a regra pois existem " + countAtividades +
                    " atividade(s) vinculada(s) a ela.");
        }
    }

    public void validarNomeRegraConjuntaUnico(String nome, Integer idAtual) {
        if (nome == null || nome.trim().isEmpty()) {
            return;
        }
        String nomeTrim = nome.trim();
        Integer id = idAtual != null ? idAtual : 0;
        boolean existe = regrasConjuntasRepository.existsByNomeRegraAndIdRegraConjuntaNot(nomeTrim, id);
        if (existe) {
            throw new RuntimeException("Já existe uma regra conjunta cadastrada com o nome '" + nomeTrim + "'.");
        }
    }

    public void validarRegrasAgrupadasNaoVazias(RegrasConjuntas regrasConjuntas) {
        if (regrasConjuntas.getRegrasAgrupadas() == null || regrasConjuntas.getRegrasAgrupadas().isEmpty()) {
            throw new RuntimeException("A regra conjunta deve ter pelo menos uma regra associada.");
        }
    }

    public void validarPontosLimitePositivo(Integer pontosLimite) {
        if (pontosLimite == null || pontosLimite <= 0) {
            throw new RuntimeException("O limite de pontos deve ser maior que zero.");
        }
    }
}