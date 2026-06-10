package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.annotation.Auditar;
import br.com.cremepe.jeton.domain.*;
import br.com.cremepe.jeton.dto.LoteAtividadeDTO;
import br.com.cremepe.jeton.repository.*;
import br.com.cremepe.jeton.util.TurnoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AtividadeLoteService {

    private static final Logger log = LoggerFactory.getLogger(AtividadeLoteService.class);

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

    @Auditar(tabela = "atividade_conselhal", acao = "CRIAR_LOTE", descricao = "Criação de múltiplas atividades com mesmo comprovante", dadosParametros = "{ 'idGestao': #dto.idGestao, 'idsConselheiros': #dto.idsConselheiros }", auditarExcecao = true, incluirRetorno = false)
    @Transactional
    public List<AtividadeConselhal> criarLote(LoteAtividadeDTO dto) {
        Gestao gestao = gestaoRepository.findById(dto.getIdGestao())
                .orElseThrow(() -> new RuntimeException("Gestão não encontrada"));
        Regras regra = regrasService.buscarOuFalhar(dto.getIdRegra());
        LocalDateTime dataHora = dto.getDataHoraAtividade();
        validarDataDentroDoMandato(dataHora.toLocalDate(), gestao);

        String turno = TurnoUtils.calcularTurno(dataHora.getHour());

        Comprovante comprovante = null;
        if (dto.getFile() != null && !dto.getFile().isEmpty()) {
            comprovante = comprovanteService.criarComprovante(
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
        return criadas;
    }

    @Transactional(readOnly = true)
    public List<AtividadeConselhal> listarPorComprovante(Integer idComprovante) {
        return atividadeRepository.findByComprovanteIdComprovante(idComprovante);
    }

    @Transactional(readOnly = true)
    public long contarAtividadesPorComprovante(Integer idComprovante) {
        return atividadeRepository.countByComprovanteIdComprovante(idComprovante);
    }

    @Auditar(tabela = "atividade_conselhal", acao = "EDITAR_LOTE", descricao = "Edição em massa de atividades que compartilham o mesmo comprovante, com adição/remoção de conselheiros", capturarEstadoAnterior = true, auditarExcecao = true)
    @Transactional
    public void atualizarLote(Integer idComprovante, LoteAtividadeDTO dto) {
        List<AtividadeConselhal> atividadesAtuais = atividadeRepository.findByComprovanteIdComprovante(idComprovante);
        if (atividadesAtuais.isEmpty()) {
            throw new RuntimeException("Nenhuma atividade encontrada para o comprovante informado.");
        }

        Set<Integer> idsAtuais = atividadesAtuais.stream()
                .map(a -> a.getConselheiro().getIdPessoa())
                .collect(Collectors.toSet());
        Set<Integer> idsNovos = new HashSet<>(dto.getIdsConselheiros());

        Set<Integer> idsRemover = new HashSet<>(idsAtuais);
        idsRemover.removeAll(idsNovos);

        Set<Integer> idsAdicionar = new HashSet<>(idsNovos);
        idsAdicionar.removeAll(idsAtuais);

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
            comprovanteFinal = comprovanteService.criarComprovante(
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
                comprovanteService.excluirComprovante(idComprovante);
            }
        }

        log.info("Lote atualizado: comprovante ID {}, {} atividades mantidas, {} removidas, {} adicionadas",
                idComprovante, atividadesAtuais.size() - idsRemover.size(), idsRemover.size(), idsAdicionar.size());
    }

    private void validarDataDentroDoMandato(LocalDate dataAtividade, Gestao gestao) {
        if (dataAtividade.isBefore(gestao.getDtInicio()) || dataAtividade.isAfter(gestao.getDtFim())) {
            throw new RuntimeException("A data da atividade (" + dataAtividade +
                    ") não está dentro do período da Gestão (" + gestao.getDtInicio() + " a " + gestao.getDtFim()
                    + ").");
        }
    }
}