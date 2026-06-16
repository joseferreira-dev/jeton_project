package br.com.cremepe.jeton.util;

import br.com.cremepe.jeton.domain.Gestao;
import br.com.cremepe.jeton.repository.GestaoRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class GestaoValidator {

    private final GestaoRepository gestaoRepository;

    public GestaoValidator(GestaoRepository gestaoRepository) {
        this.gestaoRepository = gestaoRepository;
    }

    public void validarDatas(Gestao gestao) {
        if (gestao.getDtInicio() == null || gestao.getDtFim() == null) {
            throw new RuntimeException("As datas de início e fim são obrigatórias.");
        }
        if (!gestao.getDtFim().isAfter(gestao.getDtInicio())) {
            throw new RuntimeException("A data de fim deve ser posterior à data de início.");
        }
    }

    public void validarNomeUnico(Gestao gestao) {
        if (gestao.getNomeGestao() == null || gestao.getNomeGestao().trim().isEmpty()) {
            return;
        }
        String nome = gestao.getNomeGestao().trim();
        Integer idAtual = gestao.getIdGestao();
        boolean existe = gestaoRepository.existsByNomeGestaoIgnorandoId(nome, idAtual != null ? idAtual : 0);
        if (existe) {
            throw new RuntimeException("Já existe uma gestão cadastrada com o nome '" + nome + "'.");
        }
    }

    public void validarSobreposicao(Gestao gestao) {
        LocalDate inicio = gestao.getDtInicio();
        LocalDate fim = gestao.getDtFim();
        if (inicio == null || fim == null) {
            return;
        }
        Integer idAtual = gestao.getIdGestao();
        boolean sobrepoe = gestaoRepository.existsPeriodoSobreposto(idAtual != null ? idAtual : 0, inicio, fim);
        if (sobrepoe) {
            throw new RuntimeException("O período selecionado coincide com uma gestão já cadastrada.");
        }
    }

    public void validarGestao(Gestao gestao) {
        validarDatas(gestao);
        validarNomeUnico(gestao);
        validarSobreposicao(gestao);
    }
}