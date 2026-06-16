package br.com.cremepe.jeton.util;

import br.com.cremepe.jeton.repository.PortariaRepository;
import br.com.cremepe.jeton.repository.ResolucaoRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class NormativaValidator {

    private final PortariaRepository portariaRepository;
    private final ResolucaoRepository resolucaoRepository;

    public NormativaValidator(PortariaRepository portariaRepository, ResolucaoRepository resolucaoRepository) {
        this.portariaRepository = portariaRepository;
        this.resolucaoRepository = resolucaoRepository;
    }

    public void validarUnicidadePortaria(Integer numero, Integer ano, Integer idAtual) {
        if (numero == null || ano == null) {
            throw new RuntimeException("Número e ano são obrigatórios para a portaria.");
        }
        Integer idIgnorar = idAtual != null ? idAtual : 0;
        boolean existe = portariaRepository.existsByNumeroAndAnoAndIdPortariaNot(numero, ano, idIgnorar);
        if (existe) {
            throw new RuntimeException("Já existe uma portaria cadastrada com o número " + numero + "/" + ano);
        }
    }

    public void validarSobreposicaoPortaria(LocalDate inicio, LocalDate fim, Integer idAtual) {
        if (inicio == null) {
            throw new RuntimeException("A data de início da vigência é obrigatória.");
        }
        LocalDate fimValidacao = (fim != null) ? fim : LocalDate.of(9999, 12, 31);
        Integer idIgnorar = idAtual != null ? idAtual : 0;
        boolean sobrepoe = portariaRepository.existePeriodoSobreposto(idIgnorar, inicio, fimValidacao);
        if (sobrepoe) {
            throw new RuntimeException(
                    "Já existe uma portaria cadastrada cujo período de vigência coincide com o informado. Verifique as datas.");
        }
    }

    public void validarPortaria(Integer numero, Integer ano, LocalDate inicio, LocalDate fim, Integer idAtual) {
        validarUnicidadePortaria(numero, ano, idAtual);
        validarSobreposicaoPortaria(inicio, fim, idAtual);
    }

    public void validarUnicidadeResolucao(Integer numero, Integer ano, Integer idAtual) {
        if (numero == null || ano == null) {
            throw new RuntimeException("Número e ano são obrigatórios para a resolução.");
        }
        Integer idIgnorar = idAtual != null ? idAtual : 0;
        boolean existe = resolucaoRepository.existsByNumeroAndAnoAndIdResolucaoNot(numero, ano, idIgnorar);
        if (existe) {
            throw new RuntimeException("Já existe uma resolução cadastrada com o número " + numero + "/" + ano);
        }
    }

    public void validarSobreposicaoResolucao(LocalDate inicio, LocalDate fim, Integer idAtual) {
        if (inicio == null) {
            throw new RuntimeException("A data de início da vigência é obrigatória.");
        }
        LocalDate fimValidacao = (fim != null) ? fim : LocalDate.of(9999, 12, 31);
        Integer idIgnorar = idAtual != null ? idAtual : 0;
        boolean sobrepoe = resolucaoRepository.existePeriodoSobreposto(idIgnorar, inicio, fimValidacao);
        if (sobrepoe) {
            throw new RuntimeException(
                    "Já existe uma resolução cadastrada cujo período de vigência coincide com o informado. Verifique as datas.");
        }
    }

    public void validarResolucao(Integer numero, Integer ano, LocalDate inicio, LocalDate fim, Integer idAtual) {
        validarUnicidadeResolucao(numero, ano, idAtual);
        validarSobreposicaoResolucao(inicio, fim, idAtual);
    }
}