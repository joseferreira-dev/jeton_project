package br.com.cremepe.jeton.util;

import br.com.cremepe.jeton.domain.AtividadeConselhal;
import br.com.cremepe.jeton.domain.Gestao;
import br.com.cremepe.jeton.domain.GestaoConselheiro;
import br.com.cremepe.jeton.repository.GestaoConselheiroRepository;
import br.com.cremepe.jeton.repository.GestaoRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AtividadeValidator {

    private final GestaoRepository gestaoRepository;
    private final GestaoConselheiroRepository gestaoConselheiroRepository;

    public AtividadeValidator(GestaoRepository gestaoRepository,
            GestaoConselheiroRepository gestaoConselheiroRepository) {
        this.gestaoRepository = gestaoRepository;
        this.gestaoConselheiroRepository = gestaoConselheiroRepository;
    }

    public void validarDataDentroDoMandato(LocalDate dataAtividade, Integer idGestao) {
        Gestao gestao = gestaoRepository.findById(idGestao)
                .orElseThrow(() -> new RuntimeException("Gestão não encontrada com ID: " + idGestao));

        if (dataAtividade.isBefore(gestao.getDtInicio()) || dataAtividade.isAfter(gestao.getDtFim())) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            throw new RuntimeException("A data da atividade (" + dataAtividade.format(formatter) +
                    ") não está dentro do período da Gestão selecionada (" +
                    gestao.getDtInicio().format(formatter) + " a " + gestao.getDtFim().format(formatter) + ").");
        }
    }

    public void validarVinculoConselheiroGestao(Integer idConselheiro, Integer idGestao) {
        List<GestaoConselheiro> vinculos = gestaoConselheiroRepository
                .findByIdIdGestaoAndInSituacao(idGestao, GestaoConselheiro.SITUACAO_ATIVO)
                .stream()
                .filter(vc -> vc.getConselheiro().getIdPessoa().equals(idConselheiro))
                .collect(Collectors.toList());

        if (vinculos.isEmpty()) {
            throw new RuntimeException("O médico selecionado não possui vínculo ATIVO com a Gestão informada.");
        }
    }

    public void validarAtividadeNaoFechada(AtividadeConselhal atividade) {
        if (atividade != null && AtividadeConselhal.SITUACAO_FECHADA.equals(atividade.getInSituacao())) {
            throw new RuntimeException(
                    "Operação negada: Esta atividade já foi processada e FECHADA na folha de pagamento e não pode ser modificada.");
        }
    }

    public void validarAtividadeNaoComputada(AtividadeConselhal atividade) {
        if (atividade != null && AtividadeConselhal.COMPUTADA_SIM.equals(atividade.getInComputada())) {
            throw new RuntimeException(
                    "Operação negada: Esta atividade já foi computada em um processamento financeiro e não pode ser desvalidada.");
        }
    }

    public void validarAtividadePendente(AtividadeConselhal atividade) {
        if (atividade == null) {
            throw new RuntimeException("Atividade não encontrada.");
        }
        if (!AtividadeConselhal.SITUACAO_PENDENTE.equals(atividade.getInSituacao())) {
            throw new RuntimeException("Apenas atividades pendentes podem ser editadas ou excluídas.");
        }
    }

    public void validarAtividadeValidada(AtividadeConselhal atividade) {
        if (atividade == null) {
            throw new RuntimeException("Atividade não encontrada.");
        }
        if (!AtividadeConselhal.SITUACAO_VALIDADA.equals(atividade.getInSituacao())) {
            throw new RuntimeException("Apenas atividades validadas podem ser processadas.");
        }
    }

    public void validarConselheiroProprietario(AtividadeConselhal atividade, Integer idConselheiroLogado) {
        if (atividade == null || idConselheiroLogado == null) {
            throw new RuntimeException("Dados inválidos para validação de propriedade.");
        }
        if (!atividade.getConselheiro().getIdPessoa().equals(idConselheiroLogado)) {
            throw new RuntimeException("Você só pode acessar ou modificar suas próprias atividades.");
        }
    }

    public void validarDataHoraObrigatoria(LocalDateTime dataHora) {
        if (dataHora == null) {
            throw new RuntimeException("A data e horário da atividade são obrigatórios.");
        }
    }

    public Gestao validarGestaoExistente(Integer idGestao) {
        return gestaoRepository.findById(idGestao)
                .orElseThrow(() -> new RuntimeException("Gestão não encontrada com ID: " + idGestao));
    }

    public boolean isComprovanteCompartilhado(Integer idComprovante, long totalAtividades) {
        return idComprovante != null && totalAtividades > 1;
    }

    public void validarExclusaoPermitida(AtividadeConselhal atividade) {
        if (atividade == null) {
            throw new RuntimeException("Atividade não encontrada.");
        }
        if (AtividadeConselhal.SITUACAO_FECHADA.equals(atividade.getInSituacao()) ||
                AtividadeConselhal.COMPUTADA_SIM.equals(atividade.getInComputada())) {
            throw new RuntimeException(
                    "Operação negada: Esta atividade não pode ser excluída pois já foi processada (computada ou fechada).");
        }
    }
}