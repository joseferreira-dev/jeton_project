package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.annotation.Auditar;
import br.com.cremepe.jeton.domain.*;
import br.com.cremepe.jeton.repository.*;

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
    private ComprovanteRepository comprovanteRepository;
    @Autowired
    private GestaoRepository gestaoRepository;
    @Autowired
    private GestaoConselheiroRepository gestaoConselheiroRepository;
    @Autowired
    private ComprovanteService comprovanteService;

    @Auditar(tabela = "atividade_conselhal", acao = "CRIAR", descricao = "Criação de nova atividade", dadosParametros = "{ 'idAtividade': #atividade.idAtividade }", dadosRetorno = "#result", auditarExcecao = true)
    @Transactional
    public AtividadeConselhal criar(AtividadeConselhal atividade,
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
            Comprovante novoComprovante = criarComprovante(file, idTipoAnexo, nomeComprovanteUsuario);
            atividade.setComprovante(novoComprovante);
        }

        AtividadeConselhal salva = atividadeRepository.save(atividade);
        log.info("Atividade criada: ID={}", salva.getIdAtividade());
        return salva;
    }

    @Auditar(tabela = "atividade_conselhal", acao = "EDITAR", descricao = "Edição de atividade existente", capturarEstadoAnterior = true, dadosParametros = "{ 'idAtividade': #atividade.idAtividade }", dadosRetorno = "#result", auditarExcecao = true)
    @Transactional
    public AtividadeConselhal atualizar(AtividadeConselhal atividade,
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
            novoComprovante = criarComprovante(file, idTipoAnexo, nomeComprovanteUsuario);
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
                    comprovanteService.excluirComprovante(comp.getIdComprovante());
                });
            }
        }

        log.info("Atividade atualizada: ID={}", salva.getIdAtividade());
        return salva;
    }

    @Auditar(tabela = "atividade_conselhal", acao = "VALIDAR", descricao = "Validar atividade pendente", dadosParametros = "{ 'idAtividade': #id }", auditarExcecao = true)
    @Transactional
    public void validar(Integer id) {
        AtividadeConselhal atividade = buscarAtividadeOuLancarExcecao(id);
        if (AtividadeConselhal.SITUACAO_FECHADA.equals(atividade.getInSituacao())) {
            throw new RuntimeException("Operação negada: Esta atividade está fechada em folha.");
        }
        atividade.setInSituacao(AtividadeConselhal.SITUACAO_VALIDADA);
        atividadeRepository.save(atividade);
        log.info("Atividade validada: ID={}", id);
    }

    @Auditar(tabela = "atividade_conselhal", acao = "DESVALIDAR", descricao = "Desvalidar atividade", dadosParametros = "{ 'idAtividade': #id }", auditarExcecao = true)
    @Transactional
    public void desvalidar(Integer id) {
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

    @Auditar(tabela = "atividade_conselhal", acao = "EXCLUIR", capturarEstadoAnterior = true, descricao = "Excluir atividade", dadosParametros = "{ 'idAtividade': #id }", auditarExcecao = true)
    @Transactional
    public void excluir(Integer id) {
        AtividadeConselhal atividade = buscarAtividadeOuLancarExcecao(id);

        if (AtividadeConselhal.SITUACAO_FECHADA.equals(atividade.getInSituacao())
                || AtividadeConselhal.COMPUTADA_SIM.equals(atividade.getInComputada())) {
            throw new RuntimeException(
                    "Operação negada: Esta atividade não pode ser excluída pois já foi processada (computada ou fechada).");
        }

        Integer idComprovante = null;
        if (atividade.getComprovante() != null) {
            idComprovante = atividade.getComprovante().getIdComprovante();
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
                    comprovanteService.excluirComprovante(idComprovante);
                } catch (Exception e) {
                    log.warn("Falha ao excluir comprovante ID {} durante exclusão da atividade {}: {}", idComprovante,
                            id, e.getMessage());
                }
            }
        }

        log.info("Atividade excluída: ID={}", id);
    }

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

    private Comprovante criarComprovante(MultipartFile file, Integer idTipoAnexo,
            String nomeComprovanteUsuario) {
        return comprovanteService.criarComprovante(file, idTipoAnexo, nomeComprovanteUsuario);
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

    public int sumPontosValidadasNaoComputadas(Integer idPessoa) {
        Integer soma = atividadeRepository.sumPontosAtividadesValidadasNaoComputadas(idPessoa);
        return soma != null ? soma : 0;
    }

    public long countPendentesPorConselheiro(Integer idPessoa) {
        return atividadeRepository.countByConselheiroIdPessoaAndInSituacao(idPessoa, "P");
    }

    public long countTotalPorConselheiro(Integer idPessoa) {
        return atividadeRepository.countByConselheiroIdPessoa(idPessoa);
    }

    @Transactional(readOnly = true)
    public long contarAtividadesPorComprovante(Integer idComprovante) {
        return atividadeRepository.countByComprovanteIdComprovante(idComprovante);
    }
}