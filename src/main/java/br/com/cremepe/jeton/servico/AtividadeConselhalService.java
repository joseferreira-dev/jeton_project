package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.AtividadeConselhal;
import br.com.cremepe.jeton.dominio.Gestao;
import br.com.cremepe.jeton.dominio.Resolucao;
import br.com.cremepe.jeton.dominio.Comprovante;
import br.com.cremepe.jeton.repositorio.AtividadeConselhalRepository;
import br.com.cremepe.jeton.repositorio.GestaoConselheiroRepository;
import br.com.cremepe.jeton.repositorio.GestaoRepository;
import br.com.cremepe.jeton.repositorio.ComprovanteRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class AtividadeConselhalService {

    @Autowired
    private AtividadeConselhalRepository atividadeRepository;
    @Autowired
    private GestaoRepository gestaoRepository;
    @Autowired
    private GestaoConselheiroRepository gestaoConselheiroRepository;
    @Autowired
    private ComprovanteRepository comprovanteRepository;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private RegrasService regrasService;

    @Transactional
    public AtividadeConselhal salvarAtividade(AtividadeConselhal atividade) {

        // ==============================================================================
        // TRAVA DE SEGURANÇA: Bloquear alteração de Atividades Fechadas em Folha (F)
        // ==============================================================================
        if (atividade.getIdAtividade() != null) {
            AtividadeConselhal atividadeBanco = atividadeRepository.findById(atividade.getIdAtividade())
                    .orElseThrow(() -> new RuntimeException("Atividade não encontrada no sistema."));

            if ("F".equals(atividadeBanco.getInSituacao())) {
                throw new RuntimeException(
                        "Operação negada: Esta atividade já foi processada e FECHADA na folha de pagamento e não pode ser modificada.");
            }
        }

        // 1. Validar se a Gestão existe e se a data da atividade está DENTRO do mandato
        Gestao gestao = gestaoRepository.findById(atividade.getGestao().getIdGestao())
                .orElseThrow(() -> new RuntimeException("A gestão informada não foi encontrada no sistema."));

        LocalDate dataAtividade = atividade.getDataHoraAtividade().toLocalDate();
        if (dataAtividade.isBefore(gestao.getDtInicio()) || dataAtividade.isAfter(gestao.getDtFim())) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            throw new RuntimeException("A data da atividade (" + dataAtividade.format(formatter) +
                    ") não é permitida. Ela deve estar dentro do período da Gestão selecionada (" +
                    gestao.getDtInicio().format(formatter) + " a " + gestao.getDtFim().format(formatter) + ").");
        }

        // 2. Garantir segurança extra: Verificar se o conselheiro selecionado realmente
        // possui vínculo com esta gestão
        boolean conselheiroVinculado = gestaoConselheiroRepository.findByIdIdGestao(gestao.getIdGestao()).stream()
                .anyMatch(v -> v.getConselheiro().getIdPessoa().equals(atividade.getConselheiro().getIdPessoa()));

        if (!conselheiroVinculado) {
            throw new RuntimeException("O médico selecionado não possui vínculo ativo com a Gestão informada.");
        }

        // ==============================================================================
        // ALTERAÇÃO: Descoberta Dinâmica de Resolução Histórica (Inclusive Revogadas)
        // ==============================================================================
        // Mudamos o repositório restrito para buscar a resolução que cobria esta data,
        // mesmo que revogada
        Resolucao resolucaoFinanceiraVigente = regrasService.buscarResolucaoPorData(dataAtividade)
                .orElseThrow(() -> {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                    return new IllegalStateException(
                            "Não foi encontrada nenhuma Resolução (ativa ou revogada) cadastrada que cubra o período da data desta atividade: "
                                    + dataAtividade.format(formatter));
                });

        // Realiza a validação de pontos do turno estabelecido pela resolução encontrada
        // na época
        validarLimitesTurno(atividade, resolucaoFinanceiraVigente);
        // ==============================================================================

        // 3. Regras para Novas Atividades
        if (atividade.getIdAtividade() == null) {
            atividade.setDataHoraRegistro(LocalDateTime.now());

            if (atividade.getInSituacao() == null || atividade.getInSituacao().isEmpty()) {
                atividade.setInSituacao("P");
            }
        }

        // 4. Validação de Regra Revogada desabilitada para permitir lançamentos
        // retroativos históricos
        // Conforme rascunho de necessidades, permitiremos as regras antigas.

        // 5. Normalização de Quantidade
        if (atividade.getQtdAtividade() == null || atividade.getQtdAtividade() <= 0) {
            atividade.setQtdAtividade(1);
        }

        return atividadeRepository.save(atividade);
    }

    private void validarLimitesTurno(AtividadeConselhal atividade, Resolucao resolucaoVigente) {
        LocalDate dataAtividade = atividade.getDataHoraAtividade().toLocalDate();

        Integer pontosRegistrados = atividadeRepository.sumPontosPorConselheiroDiaETurno(
                atividade.getConselheiro().getIdPessoa(),
                dataAtividade,
                atividade.getInTurno());

        if (pontosRegistrados == null) {
            pontosRegistrados = 0;
        }

        int pontosDaNovaAtividade = atividade.getRegra().getPontos()
                * (atividade.getQtdAtividade() != null ? atividade.getQtdAtividade() : 1);
        int somaTotalPontosTurno = pontosRegistrados + pontosDaNovaAtividade;

        if (somaTotalPontosTurno > resolucaoVigente.getPontosPorJeton()) {
            throw new RuntimeException("Inclusão bloqueada: A soma dos pontos neste turno ("
                    + somaTotalPontosTurno + " pontos) excede o limite estabelecido pela Resolução n. "
                    + resolucaoVigente.getNumero() + "/" + resolucaoVigente.getAno()
                    + " (" + resolucaoVigente.getPontosPorJeton() + " pontos por Jeton).");
        }
    }

    @Transactional(readOnly = true)
    public Page<AtividadeConselhal> listarComPaginacaoEPesquisa(String termo, String situacao, String turno,
            String comprovanteFiltro,
            LocalDate dataInicio, LocalDate dataFim, int page, int size,
            String sortField, String sortDir) {

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, sortField);

        Pageable pageable = (size == 0)
                ? PageRequest.of(0, Integer.MAX_VALUE, sort)
                : PageRequest.of(page, size, sort);

        return atividadeRepository.pesquisarPaginado(termo, situacao, turno, comprovanteFiltro, dataInicio, dataFim,
                pageable);
    }

    @Transactional
    public void validarAtividade(Integer id) {
        AtividadeConselhal atividade = atividadeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Atividade não encontrada."));

        if ("F".equals(atividade.getInSituacao())) {
            throw new RuntimeException("Operação negada: Esta atividade está fechada em folha.");
        }

        // Muda a situação para Validada (C)
        atividade.setInSituacao("C");
        atividadeRepository.save(atividade);
    }

    @Transactional
    public void desvalidarAtividade(Integer id) {
        AtividadeConselhal atividade = atividadeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Atividade não encontrada."));

        if ("F".equals(atividade.getInSituacao())) {
            throw new RuntimeException("Operação negada: Esta atividade está fechada em folha.");
        }

        // Regra: Só podemos desvalidar se ainda não foi processada no Jeton (Computada)
        if ("S".equals(atividade.getInComputada())) {
            throw new RuntimeException(
                    "Operação negada: Esta atividade já foi computada em um processamento financeiro.");
        }

        atividade.setInSituacao("P");
        atividadeRepository.save(atividade);
    }

    @Transactional
    public void excluirAtividade(Integer id) {
        Optional<AtividadeConselhal> optAtividade = atividadeRepository.findById(id);

        if (optAtividade.isPresent()) {
            AtividadeConselhal atividade = optAtividade.get();

            // ==============================================================================
            // TRAVA DE SEGURANÇA 1: Folha Fechada
            // ==============================================================================
            if ("F".equals(atividade.getInSituacao())) {
                throw new RuntimeException(
                        "Operação negada: Esta atividade está vinculada a uma folha de pagamento FECHADA e não pode ser eliminada.");
            }

            // Tenta resgatar os dados do comprovante antes de desvincular (protegido)
            Integer idComprovante = null;
            Comprovante comprovanteBackup = null;
            try {
                if (atividade.getComprovante() != null) {
                    idComprovante = atividade.getComprovante().getIdComprovante();
                    comprovanteBackup = atividade.getComprovante();
                }
            } catch (Exception e) {
                // Comprovante fantasma - ignorado
            }

            // 1. CORTA O VÍNCULO DIRETO NO BANCO
            atividadeRepository.removerVinculoComprovante(id);
            atividadeRepository.flush();

            // ==============================================================================
            // TRAVA DE SEGURANÇA 2: Integridade de Dados (Ex: Vínculo com Jeton)
            // ==============================================================================
            try {
                // 2. APAGA A ATIVIDADE
                atividadeRepository.deleteById(id);
                atividadeRepository.flush();
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                // Se o banco de dados bloquear a exclusão (por causa de uma chave estrangeira),
                // nós capturamos o erro feio do SQL e disparamos a mensagem amigável!
                // Como há um erro, o @Transactional fará o Rollback do passo 1 automaticamente.
                throw new RuntimeException(
                        "Não é possível remover esta atividade pois ela já possui vínculos com o histórico financeiro (Pagamentos de Jeton).");
            }

            // 3. TENTA APAGAR O COMPROVANTE (Se ele ainda existir no sistema e ninguém mais
            // estiver usando)
            if (idComprovante != null && comprovanteBackup != null) {
                try {
                    long outrasAtividades = atividadeRepository.countByComprovanteIdComprovante(idComprovante);
                    if (outrasAtividades == 0) {
                        comprovanteRepository.deleteById(idComprovante);
                        fileStorageService.deleteFile(comprovanteBackup.getNomeArquivo(), comprovanteBackup.getAno(),
                                comprovanteBackup.getMes());
                    }
                } catch (Exception e) {
                    // Ignora silenciosamente erros na limpeza de lixo para não frustrar a exclusão
                    // da atividade
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public Optional<AtividadeConselhal> buscarPorId(Integer id) {
        return atividadeRepository.findById(id);
    }
}