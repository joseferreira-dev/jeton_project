package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.anotacao.Auditar;
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
    private UsuarioLogadoService usuarioLogadoService;

    // =========================================================================
    // CRIAÇÃO – retorna a atividade salva (dados completos)
    // =========================================================================
    @Auditar(tabela = "atividade_conselhal", acao = "CRIAR", descricao = "Criação de nova atividade com comprovante", dadosParametros = "{ 'idAtividade': #atividade.idAtividade }", dadosRetorno = "#result", auditarExcecao = true)
    @Transactional
    public AtividadeConselhal criarAtividadeComComprovante(AtividadeConselhal atividade,
            MultipartFile file,
            Integer idTipoAnexo,
            String nomeComprovanteUsuario) {
        validarAtividadeNaoFechada(atividade.getIdAtividade());
        Gestao gestao = validarGestaoEVinculo(atividade);
        validarDataDentroDoMandato(atividade.getDataHoraAtividade().toLocalDate(), gestao);

        atividade.setDataHoraRegistro(LocalDateTime.now());
        if (atividade.getInSituacao() == null || atividade.getInSituacao().isEmpty()) {
            atividade.setInSituacao(AtividadeConselhal.SITUACAO_PENDENTE);
        }
        if (atividade.getQtdAtividade() == null || atividade.getQtdAtividade() <= 0) {
            atividade.setQtdAtividade(1);
        }

        if (file != null && !file.isEmpty()) {
            Integer idUsuario = usuarioLogadoService.getIdUsuarioLogado();
            Comprovante novoComprovante = criarComprovante(file, idTipoAnexo, nomeComprovanteUsuario, idUsuario);
            atividade.setComprovante(novoComprovante);
        }

        AtividadeConselhal salva = atividadeRepository.save(atividade);
        log.info("Atividade criada: ID={}", salva.getIdAtividade());
        return salva;
    }

    // =========================================================================
    // EDIÇÃO – retorna a atividade atualizada (dados completos)
    // =========================================================================
    @Auditar(tabela = "atividade_conselhal", acao = "EDITAR", descricao = "Edição de atividade existente com comprovante", capturarEstadoAnterior = true, dadosParametros = "{ 'idAtividade': #atividade.idAtividade }", dadosRetorno = "#result", auditarExcecao = true)
    @Transactional
    public AtividadeConselhal atualizarAtividadeComComprovante(AtividadeConselhal atividade,
            MultipartFile file,
            Integer idTipoAnexo,
            String nomeComprovanteUsuario,
            Integer idComprovanteAntigo) {
        AtividadeConselhal existente = atividadeRepository.findById(atividade.getIdAtividade())
                .orElseThrow(() -> new RuntimeException("Atividade não encontrada para edição"));

        if (AtividadeConselhal.SITUACAO_FECHADA.equals(existente.getInSituacao())) {
            throw new RuntimeException("Operação negada: Esta atividade está fechada em folha.");
        }

        if (idComprovanteAntigo != null) {
            atividadeRepository.desvincularComprovante(atividade.getIdAtividade());
        }

        Comprovante novoComprovante = null;
        if (file != null && !file.isEmpty()) {
            Integer idUsuario = usuarioLogadoService.getIdUsuarioLogado();
            novoComprovante = criarComprovante(file, idTipoAnexo, nomeComprovanteUsuario, idUsuario);
            atividade.setComprovante(novoComprovante);
        } else if (idComprovanteAntigo != null) {
            Comprovante antigo = comprovanteRepository.findById(idComprovanteAntigo).orElse(null);
            if (antigo != null && nomeComprovanteUsuario != null
                    && !nomeComprovanteUsuario.equals(antigo.getNomeComprovante())) {
                antigo.setNomeComprovante(nomeComprovanteUsuario);
                comprovanteRepository.save(antigo);
            }
            atividade.setComprovante(antigo);
        } else {
            atividade.setComprovante(existente.getComprovante());
        }

        atividade.setDataHoraRegistro(existente.getDataHoraRegistro());
        if (atividade.getQtdAtividade() == null || atividade.getQtdAtividade() <= 0) {
            atividade.setQtdAtividade(1);
        }

        Gestao gestao = validarGestaoEVinculo(atividade);
        validarDataDentroDoMandato(atividade.getDataHoraAtividade().toLocalDate(), gestao);

        AtividadeConselhal salva = atividadeRepository.save(atividade);

        if (idComprovanteAntigo != null && file != null && !file.isEmpty()) {
            long outrasAtividades = atividadeRepository.countByComprovanteIdComprovante(idComprovanteAntigo);
            if (outrasAtividades == 0) {
                comprovanteRepository.findById(idComprovanteAntigo).ifPresent(comp -> {
                    fileStorageService.excluirArquivo(comp.getNomeArquivo(), comp.getAno(), comp.getMes());
                    comprovanteRepository.delete(comp);
                });
            }
        }

        log.info("Atividade atualizada: ID={}", salva.getIdAtividade());
        return salva;
    }

    // =========================================================================
    // VALIDAÇÃO – sem estado anterior, apenas ID
    // =========================================================================
    @Auditar(tabela = "atividade_conselhal", acao = "VALIDAR", descricao = "Validar atividade pendente", dadosParametros = "{ 'idAtividade': #id }", auditarExcecao = true)
    @Transactional
    public void validarAtividade(Integer id) {
        AtividadeConselhal atividade = buscarAtividadeOuLancarExcecao(id);
        if (AtividadeConselhal.SITUACAO_FECHADA.equals(atividade.getInSituacao())) {
            throw new RuntimeException("Operação negada: Esta atividade está fechada em folha.");
        }
        atividade.setInSituacao(AtividadeConselhal.SITUACAO_VALIDADA);
        atividadeRepository.save(atividade);
        log.info("Atividade validada: ID={}", id);
    }

    // =========================================================================
    // DESVALIDAR – sem estado anterior, apenas ID
    // =========================================================================
    @Auditar(tabela = "atividade_conselhal", acao = "DESVALIDAR", descricao = "Desvalidar atividade", dadosParametros = "{ 'idAtividade': #id }", auditarExcecao = true)
    @Transactional
    public void desvalidarAtividade(Integer id) {
        AtividadeConselhal atividade = buscarAtividadeOuLancarExcecao(id);
        if (AtividadeConselhal.SITUACAO_FECHADA.equals(atividade.getInSituacao())) {
            throw new RuntimeException("Operação negada: Esta atividade está fechada em folha.");
        }
        if (AtividadeConselhal.COMPUTADA_SIM.equals(atividade.getInComputada())) {
            throw new RuntimeException(
                    "Operação negada: Esta atividade já foi computada em um processamento financeiro.");
        }
        atividade.setInSituacao(AtividadeConselhal.SITUACAO_PENDENTE);
        atividadeRepository.save(atividade);
        log.info("Atividade desvalidada: ID={}", id);
    }

    // =========================================================================
    // EXCLUIR – captura estado anterior (entidade antes de excluir)
    // =========================================================================
    @Auditar(tabela = "atividade_conselhal", acao = "EXCLUIR", capturarEstadoAnterior = true, descricao = "Excluir atividade", dadosParametros = "{ 'idAtividade': #id }", auditarExcecao = true)
    @Transactional
    public void excluirAtividade(Integer id) {
        AtividadeConselhal atividade = buscarAtividadeOuLancarExcecao(id);

        if (AtividadeConselhal.SITUACAO_FECHADA.equals(atividade.getInSituacao())
                || AtividadeConselhal.COMPUTADA_SIM.equals(atividade.getInComputada())) {
            throw new RuntimeException(
                    "Operação negada: Esta atividade não pode ser excluída pois já foi processada (computada ou fechada).");
        }

        Integer idComprovante = null;
        Comprovante comprovanteBackup = null;
        if (atividade.getComprovante() != null) {
            idComprovante = atividade.getComprovante().getIdComprovante();
            comprovanteBackup = atividade.getComprovante();
        }

        try {
            atividadeRepository.deleteById(id);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new RuntimeException(
                    "Não é possível remover esta atividade pois ela já possui vínculos com o histórico financeiro (Pagamentos de Jeton).");
        }

        if (idComprovante != null) {
            long outrasAtividades = atividadeRepository.countByComprovanteIdComprovante(idComprovante);
            if (outrasAtividades == 0) {
                try {
                    comprovanteRepository.deleteById(idComprovante);
                    fileStorageService.excluirArquivo(comprovanteBackup.getNomeArquivo(),
                            comprovanteBackup.getAno(),
                            comprovanteBackup.getMes());
                } catch (Exception e) {
                    log.warn("Falha ao excluir comprovante ID {} durante exclusão da atividade {}: {}", idComprovante,
                            id, e.getMessage());
                }
            }
        }

        log.info("Atividade excluída: ID={}", id);
    }

    // =========================================================================
    // LEITURA (sem alterações)
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

    @Transactional(readOnly = true)
    public Page<AtividadeConselhal> listarPorConselheiroComFiltros(Integer idPessoa, LocalDate dataInicio,
            LocalDate dataFim, String situacao, Pageable pageable) {
        return atividadeRepository.findByConselheiroAndFiltros(idPessoa, dataInicio, dataFim, situacao, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AtividadeConselhal> listarPorConselheiro(Integer idPessoa, Pageable pageable) {
        return atividadeRepository.findByConselheiroIdPessoa(idPessoa, pageable);
    }

    // =========================================================================
    // MÉTODOS PRIVADOS (sem alterações)
    // =========================================================================
    private Comprovante criarComprovante(MultipartFile file, Integer idTipoAnexo,
            String nomeComprovanteUsuario, Integer idUsuarioLogado) {
        return comprovanteService.guardarComprovante(file, idTipoAnexo, nomeComprovanteUsuario, idUsuarioLogado);
    }

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

    private Gestao validarGestaoEVinculo(AtividadeConselhal atividade) {
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
}