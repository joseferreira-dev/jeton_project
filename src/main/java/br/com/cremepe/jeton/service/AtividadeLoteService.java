package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.domain.*;
import br.com.cremepe.jeton.dto.LoteAtividadeDTO;
import br.com.cremepe.jeton.repository.AtividadeConselhalRepository;
import br.com.cremepe.jeton.repository.ComprovanteRepository;
import br.com.cremepe.jeton.repository.GestaoConselheiroRepository;
import br.com.cremepe.jeton.repository.GestaoRepository;
import br.com.cremepe.jeton.util.TurnoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AtividadeLoteService {

    private static final Logger log = LoggerFactory.getLogger(AtividadeLoteService.class);

    private final AtividadeConselhalRepository atividadeRepository;
    private final ComprovanteRepository comprovanteRepository;
    private final GestaoRepository gestaoRepository;
    private final GestaoConselheiroRepository gestaoConselheiroRepository;
    private final ComprovanteService comprovanteService;
    private final ConselheiroService conselheiroService;
    private final RegrasService regrasService;
    private final LogJetonService logJetonService;

    public AtividadeLoteService(AtividadeConselhalRepository atividadeRepository,
            GestaoConselheiroRepository gestaoConselheiroRepository,
            GestaoRepository gestaoRepository,
            ComprovanteService comprovanteService,
            RegrasService regrasService,
            ConselheiroService conselheiroService,
            ComprovanteRepository comprovanteRepository,
            LogJetonService logJetonService) {
        this.atividadeRepository = atividadeRepository;
        this.comprovanteRepository = comprovanteRepository;
        this.gestaoConselheiroRepository = gestaoConselheiroRepository;
        this.gestaoRepository = gestaoRepository;
        this.comprovanteService = comprovanteService;
        this.regrasService = regrasService;
        this.conselheiroService = conselheiroService;
        this.logJetonService = logJetonService;
    }

    @Transactional
    public List<AtividadeConselhal> criar(LoteAtividadeDTO dto) {
        Gestao gestao = gestaoRepository.findById(dto.getIdGestao())
                .orElseThrow(() -> new RuntimeException("Gestão não encontrada"));
        Regras regra = regrasService.buscarOuFalhar(dto.getIdRegra());
        LocalDateTime dataHora = dto.getDataHoraAtividade();
        validarDataDentroDoMandato(dataHora.toLocalDate(), gestao);

        String turno = TurnoUtils.calcularTurno(dataHora.getHour());

        Comprovante comprovante = null;
        if (dto.getFile() != null && !dto.getFile().isEmpty()) {
            comprovante = comprovanteService.criar(
                    dto.getFile(), dto.getIdTipoAnexo(), dto.getNomeComprovanteUsuario());
        }

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

        Integer idComprovante = comprovante != null ? comprovante.getIdComprovante() : null;
        logJetonService.logLoteCriado(idComprovante, gestao.getIdGestao(), regra.getIdRegra(),
                dto.getIdsConselheiros(), dataHora);

        return criadas;
    }

    @Transactional
    public void atualizar(Integer idComprovante, LoteAtividadeDTO dto) {
        List<AtividadeConselhal> atividadesAtuais = atividadeRepository.findByComprovanteIdComprovante(idComprovante);
        if (atividadesAtuais.isEmpty()) {
            throw new RuntimeException("Nenhuma atividade encontrada para o comprovante informado.");
        }

        List<Integer> idsAtuais = atividadesAtuais.stream()
                .map(a -> a.getConselheiro().getIdPessoa())
                .collect(Collectors.toList());

        Set<Integer> idsNovosSet = new HashSet<>(dto.getIdsConselheiros());
        Set<Integer> idsAtuaisSet = new HashSet<>(idsAtuais);

        Set<Integer> idsRemover = new HashSet<>(idsAtuaisSet);
        idsRemover.removeAll(idsNovosSet);
        Set<Integer> idsAdicionar = new HashSet<>(idsNovosSet);
        idsAdicionar.removeAll(idsAtuaisSet);

        Gestao gestao = gestaoRepository.findById(dto.getIdGestao())
                .orElseThrow(() -> new RuntimeException("Gestão não encontrada"));
        Regras regra = regrasService.buscarOuFalhar(dto.getIdRegra());
        LocalDateTime dataHora = dto.getDataHoraAtividade();
        validarDataDentroDoMandato(dataHora.toLocalDate(), gestao);
        String turno = TurnoUtils.calcularTurno(dataHora.getHour());

        Comprovante comprovanteAtual = comprovanteRepository.findById(idComprovante)
                .orElseThrow(() -> new RuntimeException("Comprovante não encontrado"));
        Comprovante comprovanteFinal = comprovanteAtual;

        if (dto.getFile() != null && !dto.getFile().isEmpty()) {
            comprovanteFinal = comprovanteService.criar(
                    dto.getFile(), dto.getIdTipoAnexo(), dto.getNomeComprovanteUsuario());
        } else if (dto.getNomeComprovanteUsuario() != null
                && !dto.getNomeComprovanteUsuario().equals(comprovanteAtual.getNomeComprovante())) {
            comprovanteAtual.setNomeComprovante(dto.getNomeComprovanteUsuario());
            comprovanteFinal = comprovanteRepository.save(comprovanteAtual);
        }

        // Atualiza dados comuns nas atividades existentes
        for (AtividadeConselhal at : atividadesAtuais) {
            if (AtividadeConselhal.SITUACAO_FECHADA.equals(at.getInSituacao())) {
                throw new RuntimeException(
                        "Atividade ID " + at.getIdAtividade() + " está fechada e não pode ser editada.");
            }
            at.setGestao(gestao);
            at.setRegra(regra);
            at.setDataHoraAtividade(dataHora);
            at.setInTurno(turno);
            at.setComprovante(comprovanteFinal);
            atividadeRepository.save(at);
        }

        // Remove atividades dos conselheiros desselecionados
        for (Integer idPessoa : idsRemover) {
            atividadesAtuais.stream()
                    .filter(a -> a.getConselheiro().getIdPessoa().equals(idPessoa))
                    .findFirst()
                    .ifPresent(atividadeRepository::delete);
        }

        // Cria novas atividades para os conselheiros adicionados
        for (Integer idPessoa : idsAdicionar) {
            Conselheiro conselheiro = conselheiroService.buscarPorId(idPessoa)
                    .orElseThrow(() -> new RuntimeException("Conselheiro não encontrado: " + idPessoa));

            boolean vinculado = gestaoConselheiroRepository.existsByGestaoAndConselheiro(gestao.getIdGestao(),
                    idPessoa);
            if (!vinculado) {
                throw new RuntimeException("Conselheiro " + conselheiro.getPessoa().getNome() +
                        " não está vinculado à gestão selecionada.");
            }

            AtividadeConselhal nova = new AtividadeConselhal();
            nova.setGestao(gestao);
            nova.setConselheiro(conselheiro);
            nova.setRegra(regra);
            nova.setComprovante(comprovanteFinal);
            nova.setQtdAtividade(1);
            nova.setDataHoraAtividade(dataHora);
            nova.setDataHoraRegistro(LocalDateTime.now());
            nova.setInTurno(turno);
            nova.setInSituacao(AtividadeConselhal.SITUACAO_PENDENTE);
            nova.setInComputada(AtividadeConselhal.COMPUTADA_NAO);

            atividadeRepository.save(nova);
        }

        // Se um novo comprovante foi carregado, tenta excluir o antigo
        if (dto.getFile() != null && !dto.getFile().isEmpty()) {
            long outras = atividadeRepository.countByComprovanteIdComprovante(idComprovante);
            if (outras == 0) {
                comprovanteService.excluir(idComprovante);
            }
        }

        log.info("Lote atualizado: comprovante ID {}, {} atividades mantidas, {} removidas, {} adicionadas",
                idComprovante, atividadesAtuais.size() - idsRemover.size(), idsRemover.size(), idsAdicionar.size());

        logJetonService.logLoteAtualizado(idComprovante, idsAtuais, dto.getIdsConselheiros(),
                gestao.getIdGestao(), regra.getIdRegra(), dataHora);
    }

    @Transactional(readOnly = true)
    public List<AtividadeConselhal> listarPorComprovante(Integer idComprovante) {
        return atividadeRepository.findByComprovanteIdComprovante(idComprovante);
    }

    @Transactional(readOnly = true)
    public long contarAtividadesPorComprovante(Integer idComprovante) {
        return atividadeRepository.countByComprovanteIdComprovante(idComprovante);
    }

    private void validarDataDentroDoMandato(LocalDate dataAtividade, Gestao gestao) {
        if (dataAtividade.isBefore(gestao.getDtInicio()) || dataAtividade.isAfter(gestao.getDtFim())) {
            throw new RuntimeException("A data da atividade (" + dataAtividade +
                    ") não está dentro do período da Gestão (" + gestao.getDtInicio() + " a " + gestao.getDtFim()
                    + ").");
        }
    }
}