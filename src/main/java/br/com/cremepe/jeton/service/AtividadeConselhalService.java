package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.domain.*;
import br.com.cremepe.jeton.repository.*;
import br.com.cremepe.jeton.util.AtividadeValidator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AtividadeConselhalService {

    private static final Logger log = LoggerFactory.getLogger(AtividadeConselhalService.class);

    private final AtividadeConselhalRepository atividadeRepository;
    private final ComprovanteRepository comprovanteRepository;
    private final GestaoRepository gestaoRepository;
    private final GestaoConselheiroRepository gestaoConselheiroRepository;
    private final ComprovanteService comprovanteService;
    private final LogJetonService logJetonService;
    private final AtividadeValidator atividadeValidator;

    AtividadeConselhalService(
            AtividadeConselhalRepository atividadeRepository,
            ComprovanteRepository comprovanteRepository,
            GestaoRepository gestaoRepository,
            GestaoConselheiroRepository gestaoConselheiroRepository,
            ComprovanteService comprovanteService,
            LogJetonService logJetonService,
            AtividadeValidator atividadeValidator) {
        this.atividadeRepository = atividadeRepository;
        this.comprovanteRepository = comprovanteRepository;
        this.gestaoRepository = gestaoRepository;
        this.gestaoConselheiroRepository = gestaoConselheiroRepository;
        this.comprovanteService = comprovanteService;
        this.logJetonService = logJetonService;
        this.atividadeValidator = atividadeValidator;
    }

    @Transactional
    public AtividadeConselhal criar(AtividadeConselhal atividade,
            MultipartFile file,
            Integer idTipoAnexo,
            String nomeComprovanteUsuario) {
        atividadeValidator.validarDataHoraObrigatoria(atividade.getDataHoraAtividade());

        Gestao gestao = atividadeValidator.validarGestaoExistente(atividade.getGestao().getIdGestao());
        atividadeValidator.validarVinculoConselheiroGestao(atividade.getConselheiro().getIdPessoa(),
                gestao.getIdGestao());
        atividadeValidator.validarDataDentroDoMandato(atividade.getDataHoraAtividade().toLocalDate(),
                gestao.getIdGestao());

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
        logJetonService.logAtividadeCriada(salva);
        return salva;
    }

    @Transactional
    public AtividadeConselhal atualizar(AtividadeConselhal atividade,
            MultipartFile file,
            Integer idTipoAnexo,
            String nomeComprovanteUsuario,
            Integer idComprovanteAntigo) {
        AtividadeConselhal existente = atividadeRepository.findById(atividade.getIdAtividade())
                .orElseThrow(() -> new RuntimeException("Atividade não encontrada para edição"));

        atividadeValidator.validarAtividadeNaoFechada(existente);
        atividadeValidator.validarDataHoraObrigatoria(atividade.getDataHoraAtividade());

        AtividadeConselhal copiaAnterior = copiarAtividade(existente);

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

        Gestao gestao = atividadeValidator.validarGestaoExistente(atividade.getGestao().getIdGestao());
        atividadeValidator.validarVinculoConselheiroGestao(atividade.getConselheiro().getIdPessoa(),
                gestao.getIdGestao());
        atividadeValidator.validarDataDentroDoMandato(atividade.getDataHoraAtividade().toLocalDate(),
                gestao.getIdGestao());

        AtividadeConselhal salva = atividadeRepository.save(atividade);

        if (idComprovanteAntigo != null && file != null && !file.isEmpty()) {
            long outrasAtividades = atividadeRepository.countByComprovanteIdComprovante(idComprovanteAntigo);
            if (outrasAtividades == 0) {
                comprovanteRepository.findById(idComprovanteAntigo).ifPresent(comp -> {
                    comprovanteService.excluir(comp.getIdComprovante());
                });
            }
        }

        log.info("Atividade atualizada: ID={}", salva.getIdAtividade());
        logJetonService.logAtividadeAtualizada(copiaAnterior, salva);
        return salva;
    }

    @Transactional
    public void validar(Integer id) {
        AtividadeConselhal atividade = buscarAtividadeOuLancarExcecao(id);
        atividadeValidator.validarAtividadeNaoFechada(atividade);
        atividade.setInSituacao(AtividadeConselhal.SITUACAO_VALIDADA);
        atividadeRepository.save(atividade);
        logJetonService.logAtividadeValidada(id);
        log.info("Atividade validada: ID={}", id);
    }

    @Transactional
    public void desvalidar(Integer id) {
        AtividadeConselhal atividade = buscarAtividadeOuLancarExcecao(id);
        atividadeValidator.validarAtividadeNaoFechada(atividade);
        atividadeValidator.validarAtividadeNaoComputada(atividade);
        atividade.setInSituacao(AtividadeConselhal.SITUACAO_PENDENTE);
        atividadeRepository.save(atividade);
        logJetonService.logAtividadeDesvalidada(id);
        log.info("Atividade desvalidada: ID={}", id);
    }

    @Transactional
    public void excluir(Integer id) {
        AtividadeConselhal atividade = buscarAtividadeOuLancarExcecao(id);
        atividadeValidator.validarExclusaoPermitida(atividade);
        AtividadeConselhal copia = copiarAtividade(atividade);

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
                    comprovanteService.excluir(idComprovante);
                } catch (Exception e) {
                    log.warn("Falha ao excluir comprovante ID {} durante exclusão da atividade {}: {}", idComprovante,
                            id, e.getMessage());
                }
            }
        }

        log.info("Atividade excluída: ID={}", id);
        logJetonService.logAtividadeExcluida(copia);
    }

    @Transactional(readOnly = true)
    public Page<AtividadeConselhal> listarComPaginacaoEPesquisa(String termo, String situacao, String turno,
            String comprovanteFiltro, LocalDate dataInicio, LocalDate dataFim,
            int page, int size, String sortField, String sortDir) {
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, sortField);
        Pageable pageable = (size == 0) ? PageRequest.of(0, Integer.MAX_VALUE, sort) : PageRequest.of(page, size, sort);
        return atividadeRepository.findAllByFilters(termo, situacao, turno, comprovanteFiltro,
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
        return comprovanteService.criar(file, idTipoAnexo, nomeComprovanteUsuario);
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

    @Transactional(readOnly = true)
    public List<AtividadeConselhal> listarTodas() {
        return atividadeRepository.findAll();
    }

    private AtividadeConselhal copiarAtividade(AtividadeConselhal original) {
        AtividadeConselhal copia = new AtividadeConselhal();
        copia.setIdAtividade(original.getIdAtividade());
        copia.setGestao(original.getGestao());
        copia.setConselheiro(original.getConselheiro());
        copia.setRegra(original.getRegra());
        copia.setComprovante(original.getComprovante());
        copia.setQtdAtividade(original.getQtdAtividade());
        copia.setDataHoraAtividade(original.getDataHoraAtividade());
        copia.setDataHoraRegistro(original.getDataHoraRegistro());
        copia.setInTurno(original.getInTurno());
        copia.setInSituacao(original.getInSituacao());
        copia.setInComputada(original.getInComputada());
        return copia;
    }
}