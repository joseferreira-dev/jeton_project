package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.annotation.Auditar;
import br.com.cremepe.jeton.domain.*;
import br.com.cremepe.jeton.dto.LoteAtividadeDTO;
import br.com.cremepe.jeton.repository.*;
import br.com.cremepe.jeton.util.TurnoUtils;

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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
    @Autowired
    private ConselheiroService conselheiroService;
    @Autowired
    private RegrasService regrasService;

    // =========================================================================
    // CRIAÇÃO
    // =========================================================================
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

    // =========================================================================
    // EDIÇÃO
    // =========================================================================
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

    // =========================================================================
    // VALIDAÇÃO
    // =========================================================================
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

    // =========================================================================
    // DESVALIDAR
    // =========================================================================
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

    // =========================================================================
    // EXCLUIR
    // =========================================================================
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
    // MÉTODOS PRIVADOS
    // =========================================================================
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

    // =========================================================================
    // CRIAÇÃO EM LOTE
    // =========================================================================

    @Auditar(tabela = "atividade_conselhal", acao = "CRIAR_LOTE", descricao = "Criação de múltiplas atividades com mesmo comprovante", dadosParametros = "{ 'idGestao': #dto.idGestao, 'idsConselheiros': #dto.idsConselheiros }", auditarExcecao = true, incluirRetorno = false)
    @Transactional
    public List<AtividadeConselhal> criarLote(LoteAtividadeDTO dto) {
        // 1. Validações básicas
        Gestao gestao = gestaoRepository.findById(dto.getIdGestao())
                .orElseThrow(() -> new RuntimeException("Gestão não encontrada"));
        Regras regra = regrasService.buscarOuFalhar(dto.getIdRegra());
        LocalDateTime dataHora = dto.getDataHoraAtividade();
        validarDataDentroDoMandato(dataHora.toLocalDate(), gestao);

        // 2. Calcula turno automaticamente usando TurnoUtils
        String turno = TurnoUtils.calcularTurno(dataHora.getHour());

        // 3. Cria UM comprovante (se houver arquivo)
        Comprovante comprovante = null;
        if (dto.getFile() != null && !dto.getFile().isEmpty()) {
            comprovante = comprovanteService.criarComprovante(
                    dto.getFile(), dto.getIdTipoAnexo(), dto.getNomeComprovanteUsuario());
        }

        // 4. Cria uma atividade para cada conselheiro
        List<AtividadeConselhal> criadas = new ArrayList<>();
        for (Integer idPessoa : dto.getIdsConselheiros()) {
            Conselheiro conselheiro = conselheiroService.buscarPorId(idPessoa)
                    .orElseThrow(() -> new RuntimeException("Conselheiro não encontrado: " + idPessoa));

            boolean vinculado = gestaoConselheiroRepository.existsByGestaoAndConselheiro(gestao.getIdGestao(),
                    idPessoa);
            if (!vinculado) {
                throw new RuntimeException("Conselheiro " + conselheiro.getPessoa().getNome() +
                        " não está vinculado à gestão selecionada.");
            }

            AtividadeConselhal atividade = new AtividadeConselhal();
            atividade.setGestao(gestao);
            atividade.setConselheiro(conselheiro);
            atividade.setRegra(regra);
            atividade.setComprovante(comprovante);
            atividade.setQtdAtividade(1);
            atividade.setDataHoraAtividade(dataHora);
            atividade.setDataHoraRegistro(LocalDateTime.now());
            atividade.setInTurno(turno);
            atividade.setInSituacao(AtividadeConselhal.SITUACAO_PENDENTE);
            atividade.setInComputada(AtividadeConselhal.COMPUTADA_NAO);

            criadas.add(atividadeRepository.save(atividade));
        }

        log.info("Lote de {} atividades criado para gestão {} com comprovante ID {}",
                criadas.size(), gestao.getNomeGestao(), comprovante != null ? comprovante.getIdComprovante() : null);
        return criadas;
    }

    // =========================================================================
    // CONSULTA POR COMPROVANTE (PARA EDIÇÃO EM LOTE)
    // =========================================================================

    @Transactional(readOnly = true)
    public List<AtividadeConselhal> listarPorComprovante(Integer idComprovante) {
        return atividadeRepository.findByComprovanteIdComprovante(idComprovante);
    }

    // =========================================================================
    // EDIÇÃO EM LOTE (TODAS ATIVIDADES QUE COMPARTILHAM O MESMO COMPROVANTE)
    // =========================================================================

    @Auditar(tabela = "atividade_conselhal", acao = "EDITAR_LOTE", descricao = "Edição em massa de atividades que compartilham o mesmo comprovante, com adição/remoção de conselheiros", capturarEstadoAnterior = true, auditarExcecao = true)
    @Transactional
    public void atualizarLote(Integer idComprovante, LoteAtividadeDTO dto) {
        // 1. Atividades atuais vinculadas ao comprovante
        List<AtividadeConselhal> atividadesAtuais = atividadeRepository.findByComprovanteIdComprovante(idComprovante);
        if (atividadesAtuais.isEmpty()) {
            throw new RuntimeException("Nenhuma atividade encontrada para o comprovante informado.");
        }

        // 2. IDs atuais e novos
        Set<Integer> idsAtuais = atividadesAtuais.stream()
                .map(a -> a.getConselheiro().getIdPessoa())
                .collect(Collectors.toSet());
        Set<Integer> idsNovos = new HashSet<>(dto.getIdsConselheiros());

        Set<Integer> idsRemover = new HashSet<>(idsAtuais);
        idsRemover.removeAll(idsNovos);

        Set<Integer> idsAdicionar = new HashSet<>(idsNovos);
        idsAdicionar.removeAll(idsAtuais);

        // 3. Validações comuns
        Gestao gestao = gestaoRepository.findById(dto.getIdGestao())
                .orElseThrow(() -> new RuntimeException("Gestão não encontrada"));
        Regras regra = regrasService.buscarOuFalhar(dto.getIdRegra());
        LocalDateTime dataHora = dto.getDataHoraAtividade();
        validarDataDentroDoMandato(dataHora.toLocalDate(), gestao);
        String turno = TurnoUtils.calcularTurno(dataHora.getHour());

        // 4. Tratamento do comprovante
        Comprovante comprovanteAtual = comprovanteRepository.findById(idComprovante)
                .orElseThrow(() -> new RuntimeException("Comprovante não encontrado"));
        Comprovante comprovanteFinal = comprovanteAtual;

        if (dto.getFile() != null && !dto.getFile().isEmpty()) {
            comprovanteFinal = comprovanteService.criarComprovante(
                    dto.getFile(), dto.getIdTipoAnexo(), dto.getNomeComprovanteUsuario());
        } else if (dto.getNomeComprovanteUsuario() != null
                && !dto.getNomeComprovanteUsuario().equals(comprovanteAtual.getNomeComprovante())) {
            comprovanteAtual.setNomeComprovante(dto.getNomeComprovanteUsuario());
            comprovanteFinal = comprovanteRepository.save(comprovanteAtual);
        }

        // 5. Atualiza dados comuns em todas as atividades atuais
        for (AtividadeConselhal at : atividadesAtuais) {
            if (AtividadeConselhal.SITUACAO_FECHADA.equals(at.getInSituacao())) {
                throw new RuntimeException(
                        "A atividade ID " + at.getIdAtividade() + " está fechada e não pode ser editada.");
            }
            at.setGestao(gestao);
            at.setRegra(regra);
            at.setDataHoraAtividade(dataHora);
            at.setInTurno(turno);
            at.setComprovante(comprovanteFinal);
            atividadeRepository.save(at);
        }

        // 6. Remove atividades dos conselheiros desselecionados
        for (Integer idPessoa : idsRemover) {
            Optional<AtividadeConselhal> atividadeRemover = atividadesAtuais.stream()
                    .filter(a -> a.getConselheiro().getIdPessoa().equals(idPessoa))
                    .findFirst();
            if (atividadeRemover.isPresent()) {
                atividadeRepository.delete(atividadeRemover.get());
                log.info("Atividade removida do lote: conselheiro ID {}, atividade ID {}", idPessoa,
                        atividadeRemover.get().getIdAtividade());
            }
        }

        // 7. Cria novas atividades para os conselheiros adicionados
        for (Integer idPessoa : idsAdicionar) {
            Conselheiro conselheiro = conselheiroService.buscarPorId(idPessoa)
                    .orElseThrow(() -> new RuntimeException("Conselheiro não encontrado: " + idPessoa));

            boolean vinculado = gestaoConselheiroRepository.existsByGestaoAndConselheiro(gestao.getIdGestao(),
                    idPessoa);
            if (!vinculado) {
                throw new RuntimeException("Conselheiro " + conselheiro.getPessoa().getNome() +
                        " não está vinculado à gestão selecionada.");
            }

            AtividadeConselhal novaAtividade = new AtividadeConselhal();
            novaAtividade.setGestao(gestao);
            novaAtividade.setConselheiro(conselheiro);
            novaAtividade.setRegra(regra);
            novaAtividade.setComprovante(comprovanteFinal);
            novaAtividade.setQtdAtividade(1);
            novaAtividade.setDataHoraAtividade(dataHora);
            novaAtividade.setDataHoraRegistro(LocalDateTime.now());
            novaAtividade.setInTurno(turno);
            novaAtividade.setInSituacao(AtividadeConselhal.SITUACAO_PENDENTE);
            novaAtividade.setInComputada(AtividadeConselhal.COMPUTADA_NAO);

            atividadeRepository.save(novaAtividade);
            log.info("Nova atividade adicionada ao lote: conselheiro ID {}", idPessoa);
        }

        // 8. Se houve criação de novo comprovante, tenta excluir o antigo
        if (dto.getFile() != null && !dto.getFile().isEmpty()) {
            long outras = atividadeRepository.countByComprovanteIdComprovante(idComprovante);
            if (outras == 0) {
                comprovanteService.excluirComprovante(idComprovante);
            }
        }

        log.info("Lote atualizado: comprovante ID {}, {} atividades mantidas, {} removidas, {} adicionadas",
                idComprovante, atividadesAtuais.size() - idsRemover.size(), idsRemover.size(), idsAdicionar.size());
    }

    @Transactional(readOnly = true)
    public long contarAtividadesPorComprovante(Integer idComprovante) {
        return atividadeRepository.countByComprovanteIdComprovante(idComprovante);
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
}