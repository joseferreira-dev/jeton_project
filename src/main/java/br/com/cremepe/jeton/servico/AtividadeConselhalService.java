package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.*;
import br.com.cremepe.jeton.repositorio.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class AtividadeConselhalService {

    private static final Logger log = LoggerFactory.getLogger(AtividadeConselhalService.class);

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
    private ComprovanteService comprovanteService;
    @Autowired
    private LogJetonService logJetonService;
    @Autowired
    private PessoaRepository pessoaRepository;
    @Autowired
    private RegrasRepository regrasRepository;

    // =========================================================================
    // OPERAÇÕES DE ESCRITA (CRUD)
    // =========================================================================

    @Transactional
    public AtividadeConselhal salvarAtividade(AtividadeConselhal atividade, Integer idUsuarioLogado) {
        validarAtividadeNaoFechada(atividade.getIdAtividade());
        Gestao gestao = validarGestaoEvinculo(atividade);
        validarDataDentroDoMandato(atividade.getDataHoraAtividade().toLocalDate(), gestao);

        boolean isNova = atividade.getIdAtividade() == null;
        if (isNova) {
            atividade.setDataHoraRegistro(LocalDateTime.now());
            if (atividade.getInSituacao() == null || atividade.getInSituacao().isEmpty()) {
                atividade.setInSituacao(AtividadeConselhal.SITUACAO_PENDENTE);
            }
        }

        if (atividade.getQtdAtividade() == null || atividade.getQtdAtividade() <= 0) {
            atividade.setQtdAtividade(1);
        }

        AtividadeConselhal salva = atividadeRepository.save(atividade);

        // Buscar nomes diretamente dos repositórios para evitar lazy loading
        // problemático
        String nomeConselheiro = buscarNomeConselheiro(salva.getConselheiro().getIdPessoa());
        String nomeGestao = buscarNomeGestao(salva.getGestao().getIdGestao());
        String nomeRegra = buscarNomeRegra(salva.getRegra().getIdRegra());
        String dataHora = salva.getDataHoraAtividade().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        String situacaoDesc = salva.getInSituacao().equals(AtividadeConselhal.SITUACAO_PENDENTE) ? "Pendente"
                : salva.getInSituacao().equals(AtividadeConselhal.SITUACAO_VALIDADA) ? "Validada" : "Fechada";

        if (isNova) {
            String textoLog = String.format(
                    "Nova atividade criada: ID=%d, Conselheiro='%s' (ID=%d), Gestão='%s' (ID=%d), Regra='%s', Data/Hora=%s, Quantidade=%d, Situação=%s",
                    salva.getIdAtividade(), nomeConselheiro, salva.getConselheiro().getIdPessoa(),
                    nomeGestao, salva.getGestao().getIdGestao(), nomeRegra, dataHora,
                    salva.getQtdAtividade(), situacaoDesc);
            logJetonService.registrarLog("atividade_conselhal", idUsuarioLogado, textoLog);
            log.info("Atividade criada: ID={}, conselheiro={}, gestão={}", salva.getIdAtividade(), nomeConselheiro,
                    nomeGestao);
        } else {
            String textoLog = String.format(
                    "Atividade editada: ID=%d, Conselheiro='%s' (ID=%d), Gestão='%s' (ID=%d), Regra='%s', Data/Hora=%s, Quantidade=%d, Situação=%s",
                    salva.getIdAtividade(), nomeConselheiro, salva.getConselheiro().getIdPessoa(),
                    nomeGestao, salva.getGestao().getIdGestao(), nomeRegra, dataHora,
                    salva.getQtdAtividade(), situacaoDesc);
            logJetonService.registrarLog("atividade_conselhal", idUsuarioLogado, textoLog);
            log.debug("Atividade editada: ID={}", salva.getIdAtividade());
        }

        return salva;
    }

    @Transactional
    public void salvarAtividadeComComprovante(AtividadeConselhal atividade,
            MultipartFile file,
            Integer idTipoAnexo,
            String nomeComprovanteUsuario,
            Integer idComprovanteAntigo,
            Integer idUsuarioLogado) {

        // 1. Se há um comprovante antigo, desvincula da atividade (mas não apaga ainda)
        if (idComprovanteAntigo != null) {
            atividadeRepository.desvincularComprovante(atividade.getIdAtividade());
        }

        // 2. Se um novo arquivo foi enviado, cria e salva o novo comprovante
        Comprovante novoComprovante = null;
        if (file != null && !file.isEmpty()) {
            novoComprovante = criarComprovante(file, idTipoAnexo, nomeComprovanteUsuario, idUsuarioLogado);
            atividade.setComprovante(novoComprovante);
        } else if (atividade.getIdAtividade() != null && idComprovanteAntigo != null) {
            // Mantém o antigo, mas atualiza nome se necessário
            Comprovante antigo = comprovanteRepository.findById(idComprovanteAntigo).orElse(null);
            if (antigo != null && nomeComprovanteUsuario != null
                    && !nomeComprovanteUsuario.equals(antigo.getNomeComprovante())) {
                antigo.setNomeComprovante(nomeComprovanteUsuario);
                comprovanteRepository.save(antigo);
                logJetonService.registrarLog("comprovante", idUsuarioLogado,
                        "Nome do comprovante alterado: ID " + antigo.getIdComprovante() + " para '"
                                + nomeComprovanteUsuario + "'");
            }
            atividade.setComprovante(antigo);
        }

        // 3. Salva a atividade (com o comprovante já gerenciado)
        salvarAtividade(atividade, idUsuarioLogado);

        // 4. Após salvar, exclui o comprovante antigo se não for mais usado
        if (idComprovanteAntigo != null && file != null && !file.isEmpty()) {
            long outrasAtividades = atividadeRepository.countByComprovanteIdComprovante(idComprovanteAntigo);
            if (outrasAtividades == 0) {
                comprovanteRepository.findById(idComprovanteAntigo).ifPresent(comp -> {
                    fileStorageService.deleteFile(comp.getNomeArquivo(), comp.getAno(), comp.getMes(), idUsuarioLogado);
                    comprovanteRepository.delete(comp);
                    logJetonService.registrarLog("comprovante", idUsuarioLogado,
                            "Comprovante antigo excluído (ID " + idComprovanteAntigo + ") - sem vínculos");
                });
            }
        }

        // Log adicional sobre o vínculo do comprovante
        if (novoComprovante != null && atividade.getIdAtividade() != null) {
            String textoLog = String.format(
                    "Atividade ID %d vinculada a novo comprovante ID %d (arquivo: %s, tipo: %s)",
                    atividade.getIdAtividade(), novoComprovante.getIdComprovante(),
                    novoComprovante.getNomeArquivo(), novoComprovante.getTipoAnexo().getNome());
            logJetonService.registrarLog("atividade_conselhal", idUsuarioLogado, textoLog);
        }
    }

    private Comprovante criarComprovante(MultipartFile file, Integer idTipoAnexo,
            String nomeComprovanteUsuario, Integer idUsuarioLogado) {
        return comprovanteService.guardarComprovante(file, idTipoAnexo, nomeComprovanteUsuario, idUsuarioLogado);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void validarAtividade(Integer id, Integer idUsuarioLogado) {
        AtividadeConselhal atividade = buscarAtividadeOuLancarExcecao(id);
        if (AtividadeConselhal.SITUACAO_FECHADA.equals(atividade.getInSituacao())) {
            throw new RuntimeException("Operação negada: Esta atividade está fechada em folha.");
        }
        String situacaoAntiga = atividade.getInSituacao();
        atividade.setInSituacao(AtividadeConselhal.SITUACAO_VALIDADA);
        atividadeRepository.save(atividade);

        String nomeConselheiro = buscarNomeConselheiro(atividade.getConselheiro().getIdPessoa());
        String nomeGestao = buscarNomeGestao(atividade.getGestao().getIdGestao());

        String textoLog = String.format(
                "Atividade ID %d validada (situação alterada de %s para %s). Conselheiro: '%s' (ID=%d), Gestão: '%s' (ID=%d)",
                id,
                situacaoAntiga.equals(AtividadeConselhal.SITUACAO_PENDENTE) ? "Pendente" : situacaoAntiga,
                "Validada",
                nomeConselheiro, atividade.getConselheiro().getIdPessoa(),
                nomeGestao, atividade.getGestao().getIdGestao());
        logJetonService.registrarLog("atividade_conselhal", idUsuarioLogado, textoLog);
        log.info("Atividade validada: ID={}, conselheiro={}", id, nomeConselheiro);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void desvalidarAtividade(Integer id, Integer idUsuarioLogado) {
        AtividadeConselhal atividade = buscarAtividadeOuLancarExcecao(id);
        if (AtividadeConselhal.SITUACAO_FECHADA.equals(atividade.getInSituacao())) {
            throw new RuntimeException("Operação negada: Esta atividade está fechada em folha.");
        }
        if (AtividadeConselhal.COMPUTADA_SIM.equals(atividade.getInComputada())) {
            throw new RuntimeException(
                    "Operação negada: Esta atividade já foi computada em um processamento financeiro.");
        }
        String situacaoAntiga = atividade.getInSituacao();
        atividade.setInSituacao(AtividadeConselhal.SITUACAO_PENDENTE);
        atividadeRepository.save(atividade);

        String nomeConselheiro = buscarNomeConselheiro(atividade.getConselheiro().getIdPessoa());
        String nomeGestao = buscarNomeGestao(atividade.getGestao().getIdGestao());

        String textoLog = String.format(
                "Atividade ID %d desvalidada (situação alterada de %s para %s). Conselheiro: '%s' (ID=%d), Gestão: '%s' (ID=%d)",
                id,
                situacaoAntiga.equals(AtividadeConselhal.SITUACAO_VALIDADA) ? "Validada" : situacaoAntiga,
                "Pendente",
                nomeConselheiro, atividade.getConselheiro().getIdPessoa(),
                nomeGestao, atividade.getGestao().getIdGestao());
        logJetonService.registrarLog("atividade_conselhal", idUsuarioLogado, textoLog);
        log.info("Atividade desvalidada: ID={}, conselheiro={}", id, nomeConselheiro);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void excluirAtividade(Integer id, Integer idUsuarioLogado) {
        AtividadeConselhal atividade = buscarAtividadeOuLancarExcecao(id);

        // Impede exclusão se estiver fechada OU computada
        if (AtividadeConselhal.SITUACAO_FECHADA.equals(atividade.getInSituacao())
                || AtividadeConselhal.COMPUTADA_SIM.equals(atividade.getInComputada())) {
            throw new RuntimeException(
                    "Operação negada: Esta atividade não pode ser excluída pois já foi processada (computada ou fechada).");
        }

        // Coleta dados do comprovante (se houver) antes de excluir
        Integer idComprovante = null;
        Comprovante comprovanteBackup = null;
        if (atividade.getComprovante() != null) {
            idComprovante = atividade.getComprovante().getIdComprovante();
            comprovanteBackup = atividade.getComprovante();
        }

        // 1. Exclui a atividade (isso desvincula o comprovante automaticamente)
        try {
            atividadeRepository.deleteById(id);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new RuntimeException(
                    "Não é possível remover esta atividade pois ela já possui vínculos com o histórico financeiro (Pagamentos de Jeton).");
        }

        // 2. Se havia um comprovante e nenhuma outra atividade o usa, exclui o
        // comprovante
        if (idComprovante != null) {
            long outrasAtividades = atividadeRepository.countByComprovanteIdComprovante(idComprovante);
            if (outrasAtividades == 0) {
                try {
                    comprovanteRepository.deleteById(idComprovante);
                    fileStorageService.deleteFile(comprovanteBackup.getNomeArquivo(),
                            comprovanteBackup.getAno(),
                            comprovanteBackup.getMes(),
                            idUsuarioLogado);
                    logJetonService.registrarLog("comprovante", idUsuarioLogado,
                            "Comprovante ID " + idComprovante + " excluído (sem outras atividades vinculadas)");
                } catch (Exception e) {
                    log.warn("Falha ao excluir comprovante ID {} durante exclusão da atividade {}: {}",
                            idComprovante, id, e.getMessage());
                }
            }
        }

        // 3. Registra log da exclusão
        String nomeConselheiro = buscarNomeConselheiro(atividade.getConselheiro().getIdPessoa());
        String nomeGestao = buscarNomeGestao(atividade.getGestao().getIdGestao());
        String nomeRegra = buscarNomeRegra(atividade.getRegra().getIdRegra());
        String dataHora = atividade.getDataHoraAtividade().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        String textoLog = String.format(
                "Atividade excluída: ID=%d, Conselheiro='%s' (ID=%d), Gestão='%s' (ID=%d), Regra='%s', Data/Hora=%s, Quantidade=%d, Situação=%s%s",
                id, nomeConselheiro, atividade.getConselheiro().getIdPessoa(),
                nomeGestao, atividade.getGestao().getIdGestao(), nomeRegra, dataHora,
                atividade.getQtdAtividade(),
                atividade.getInSituacao().equals(AtividadeConselhal.SITUACAO_PENDENTE) ? "Pendente"
                        : atividade.getInSituacao().equals(AtividadeConselhal.SITUACAO_VALIDADA) ? "Validada"
                                : "Fechada",
                idComprovante != null
                        ? ", Comprovante associado ID=" + idComprovante + " (" + comprovanteBackup.getNomeComprovante()
                                + ")"
                        : "");
        logJetonService.registrarLog("atividade_conselhal", idUsuarioLogado, textoLog);
        log.info("Atividade excluída: ID={}, conselheiro={}, gestão={}", id, nomeConselheiro, nomeGestao);
    }

    @Transactional
    public void desvincularComprovante(Integer idAtividade, Integer idUsuarioLogado) {
        atividadeRepository.desvincularComprovante(idAtividade);
        logJetonService.registrarLog("atividade_conselhal", idUsuarioLogado,
                "Comprovante desvinculado da atividade ID " + idAtividade);
        log.debug("Comprovante desvinculado da atividade {}", idAtividade);
    }

    // =========================================================================
    // OPERAÇÕES DE LEITURA (CONSULTAS)
    // =========================================================================

    @Transactional(readOnly = true)
    public Page<AtividadeConselhal> listarComPaginacaoEPesquisa(String termo, String situacao, String turno,
            String comprovanteFiltro, LocalDate dataInicio, LocalDate dataFim,
            int page, int size, String sortField, String sortDir) {
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, sortField);
        Pageable pageable = (size == 0) ? PageRequest.of(0, Integer.MAX_VALUE, sort) : PageRequest.of(page, size, sort);
        return atividadeRepository.pesquisarPaginado(termo, situacao, turno, comprovanteFiltro,
                dataInicio, dataFim, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<AtividadeConselhal> buscarPorId(Integer id) {
        return atividadeRepository.findById(id);
    }

    // =========================================================================
    // MÉTODOS PRIVADOS AUXILIARES PARA LOG (BUSCA DE NOMES)
    // =========================================================================

    private String buscarNomeConselheiro(Integer idPessoa) {
        return pessoaRepository.findById(idPessoa)
                .map(Pessoa::getNome)
                .orElse("Conselheiro ID " + idPessoa);
    }

    private String buscarNomeRegra(Integer idRegra) {
        return regrasRepository.findById(idRegra)
                .map(Regras::getNomeRegra)
                .orElse("Regra ID " + idRegra);
    }

    private String buscarNomeGestao(Integer idGestao) {
        return gestaoRepository.findById(idGestao)
                .map(Gestao::getNomeGestao)
                .orElse("Gestão ID " + idGestao);
    }

    // =========================================================================
    // MÉTODOS PRIVADOS AUXILIARES (VALIDAÇÕES)
    // =========================================================================

    private void validarAtividadeNaoFechada(Integer idAtividade) {
        if (idAtividade == null)
            return;
        AtividadeConselhal existente = atividadeRepository.findById(idAtividade)
                .orElseThrow(() -> new RuntimeException("Atividade não encontrada no sistema."));
        if (AtividadeConselhal.SITUACAO_FECHADA.equals(existente.getInSituacao())) {
            throw new RuntimeException(
                    "Operação negada: Esta atividade já foi processada e FECHADA na folha de pagamento e não pode ser modificada.");
        }
    }

    private Gestao validarGestaoEvinculo(AtividadeConselhal atividade) {
        Gestao gestao = gestaoRepository.findById(atividade.getGestao().getIdGestao())
                .orElseThrow(() -> new RuntimeException("A gestão informada não foi encontrada no sistema."));

        boolean vinculado = gestaoConselheiroRepository.findByIdIdGestao(gestao.getIdGestao()).stream()
                .anyMatch(v -> v.getConselheiro().getIdPessoa().equals(atividade.getConselheiro().getIdPessoa()));
        if (!vinculado) {
            throw new RuntimeException("O médico selecionado não possui vínculo ativo com a Gestão informada.");
        }
        return gestao;
    }

    private void validarDataDentroDoMandato(LocalDate dataAtividade, Gestao gestao) {
        if (dataAtividade.isBefore(gestao.getDtInicio()) || dataAtividade.isAfter(gestao.getDtFim())) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            throw new RuntimeException("A data da atividade (" + dataAtividade.format(formatter) +
                    ") não é permitida. Ela deve estar dentro do período da Gestão selecionada (" +
                    gestao.getDtInicio().format(formatter) + " a " + gestao.getDtFim().format(formatter) + ").");
        }
    }

    private AtividadeConselhal buscarAtividadeOuLancarExcecao(Integer id) {
        return atividadeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Atividade não encontrada."));
    }

    public Page<AtividadeConselhal> listarPorConselheiro(Integer idPessoa, Pageable pageable) {
        return atividadeRepository.findByConselheiroIdPessoa(idPessoa, pageable);
    }

    public Page<AtividadeConselhal> listarPorConselheiroEGestao(Integer idPessoa, Integer idGestao, Pageable pageable) {
        return atividadeRepository.findByConselheiroIdPessoaAndGestaoIdGestao(idPessoa, idGestao, pageable);
    }

}