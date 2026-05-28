package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.Portaria;
import br.com.cremepe.jeton.repositorio.PortariaRepository;
import br.com.cremepe.jeton.repositorio.RegrasRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class PortariaService {

    private static final Logger log = LoggerFactory.getLogger(PortariaService.class);

    @Autowired
    private PortariaRepository repository;
    @Autowired
    private RegrasRepository regrasRepository;
    @Autowired
    private LogJetonService logJetonService;

    // =========================================================================
    // OPERAÇÕES DE ESCRITA
    // =========================================================================

    @Transactional
    public Portaria salvar(Portaria portaria, Integer idUsuarioLogado) {
        boolean isNovo = portaria.getIdPortaria() == null;
        Integer numero = portaria.getNumero();
        Integer ano = portaria.getAno();
        LocalDate dtInicio = portaria.getDtInicioVigencia();
        LocalDate dtFim = portaria.getDtFimVigencia();
        String link = portaria.getLinkPublicado();

        validarUnicidade(portaria);
        validarSobreposicao(portaria);
        if (portaria.getInRevogado() == null || portaria.getInRevogado().trim().isEmpty()) {
            portaria.setInRevogado(Portaria.REVOGADO_NAO);
        }
        Portaria salva = repository.save(portaria);

        log.info("Portaria {}: id={}, número={}/{}, vigência={} até {}, revogado={}",
                isNovo ? "criada" : "atualizada",
                salva.getIdPortaria(), numero, ano, dtInicio, dtFim, salva.getInRevogado());

        String textoLog = String.format(
                "Portaria %s: ID=%d, Número=%d/%d, Início Vigência=%s, Fim Vigência=%s, Link=%s, Revogado='%s'",
                isNovo ? "criada" : "atualizada",
                salva.getIdPortaria(), numero, ano,
                dtInicio != null ? dtInicio : "null",
                dtFim != null ? dtFim : "null",
                link != null ? link : "null",
                salva.getInRevogado());
        logJetonService.registrarLog("portaria", idUsuarioLogado, textoLog);

        return salva;
    }

    private void validarUnicidade(Portaria portaria) {
        Integer idAtual = portaria.getIdPortaria();
        Integer numero = portaria.getNumero();
        Integer ano = portaria.getAno();
        if (numero == null || ano == null)
            return;
        boolean existe = repository.existsByNumeroAndAnoAndIdPortariaNot(numero, ano, idAtual != null ? idAtual : 0);
        if (existe) {
            throw new RuntimeException("Já existe uma portaria cadastrada com o número " + numero + "/" + ano);
        }
    }

    private void validarSobreposicao(Portaria portaria) {
        LocalDate inicio = portaria.getDtInicioVigencia();
        LocalDate fim = portaria.getDtFimVigencia();
        if (inicio == null) {
            return;
        }
        // Se fim for nulo, considerar uma data muito distante (ex: 31/12/9999)
        LocalDate fimParaValidacao = (fim != null) ? fim : LocalDate.of(9999, 12, 31);

        Integer idPortaria = (portaria.getIdPortaria() != null) ? portaria.getIdPortaria() : 0;
        boolean sobrepoe = repository.existePeriodoSobreposto(idPortaria, inicio, fimParaValidacao);
        if (sobrepoe) {
            throw new RuntimeException(
                    "Já existe uma portaria cadastrada cujo período de vigência coincide com o informado. Verifique as datas.");
        }
    }

    @Transactional
    public void revogar(Integer id, Integer idUsuarioLogado) {
        Portaria portaria = buscarOuFalhar(id);
        if (portaria.isRevogado()) {
            throw new RuntimeException("A portaria já está revogada.");
        }
        String numeroAno = portaria.getNumero() + "/" + portaria.getAno();
        portaria.setInRevogado(Portaria.REVOGADO_SIM);
        repository.save(portaria);
        regrasRepository.revogarRegrasPorPortaria(id);
        log.info("Portaria revogada: id={}, número={}", id, numeroAno);

        String textoLog = String.format(
                "Portaria revogada: ID=%d, Número=%d/%d, Início Vigência=%s, Fim Vigência=%s",
                id, portaria.getNumero(), portaria.getAno(),
                portaria.getDtInicioVigencia(), portaria.getDtFimVigencia());
        logJetonService.registrarLog("portaria", idUsuarioLogado, textoLog);
    }

    @Transactional
    public void restaurar(Integer id, Integer idUsuarioLogado) {
        Portaria portaria = buscarOuFalhar(id);
        if (!portaria.isRevogado()) {
            throw new RuntimeException("A portaria já está em vigor.");
        }
        portaria.setInRevogado(Portaria.REVOGADO_NAO);
        repository.save(portaria);
        log.info("Portaria restaurada: id={}, número={}/{}", id, portaria.getNumero(), portaria.getAno());

        String textoLog = String.format(
                "Portaria restaurada: ID=%d, Número=%d/%d",
                id, portaria.getNumero(), portaria.getAno());
        logJetonService.registrarLog("portaria", idUsuarioLogado, textoLog);
    }

    @Transactional
    public void excluirFisicamente(Integer id, Integer idUsuarioLogado) {
        Portaria portaria = buscarOuFalhar(id);
        if (!portaria.isRevogado()) {
            throw new RuntimeException("Para excluir fisicamente, a portaria deve estar revogada primeiro.");
        }
        long countRegras = regrasRepository.countByPortariaIdPortaria(id);
        if (countRegras > 0) {
            throw new RuntimeException("Não é possível excluir a portaria pois existem " + countRegras +
                    " regra(s) vinculada(s). Revogue-as ou exclua-as antes.");
        }
        String numeroAno = portaria.getNumero() + "/" + portaria.getAno();
        repository.deleteById(id);
        log.info("Portaria excluída fisicamente: id={}, número={}", id, numeroAno);

        String textoLog = String.format(
                "Portaria excluída fisicamente: ID=%d, Número=%d/%d",
                id, portaria.getNumero(), portaria.getAno());
        logJetonService.registrarLog("portaria", idUsuarioLogado, textoLog);
    }

    // =========================================================================
    // OPERAÇÕES DE LEITURA
    // =========================================================================

    @Transactional(readOnly = true)
    public List<Portaria> listarTodos() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Portaria> buscarPorId(Integer id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public Portaria buscarOuFalhar(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Portaria não encontrada com ID: " + id));
    }

    @Transactional(readOnly = true)
    public Page<Portaria> listarComPaginacaoEPesquisa(String termo, String situacao, int page, int size,
            String sortField, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortField).descending() : Sort.by(sortField).ascending();
        Pageable pageable = (size == 0) ? Pageable.unpaged(sort) : PageRequest.of(page, size, sort);
        return repository.pesquisarPaginado(termo, situacao, pageable);
    }
}