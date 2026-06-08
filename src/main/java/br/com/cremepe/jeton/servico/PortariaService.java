package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.anotacao.Auditar;
import br.com.cremepe.jeton.dominio.Portaria;
import br.com.cremepe.jeton.repository.PortariaRepository;
import br.com.cremepe.jeton.repository.RegrasRepository;

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

    // =========================================================================
    // OPERAÇÕES DE ESCRITA
    // =========================================================================

    @Auditar(tabela = "portaria", acao = "CRIAR", descricao = "Criação de nova portaria", dadosParametros = "{ 'numero': #portaria.numero, 'ano': #portaria.ano, 'dtInicioVigencia': #portaria.dtInicioVigencia, 'dtFimVigencia': #portaria.dtFimVigencia, 'linkPublicado': #portaria.linkPublicado }", dadosRetorno = "#result", capturarEstadoAnterior = false, auditarExcecao = true)
    @Transactional
    public Portaria criar(Portaria portaria) {
        portaria.setIdPortaria(null); // força criação
        return salvarPortaria(portaria, true);
    }

    @Auditar(tabela = "portaria", acao = "ATUALIZAR", descricao = "Atualização de portaria existente", dadosParametros = "{ 'id': #portaria.idPortaria, 'numero': #portaria.numero, 'ano': #portaria.ano, 'dtInicioVigencia': #portaria.dtInicioVigencia, 'dtFimVigencia': #portaria.dtFimVigencia, 'linkPublicado': #portaria.linkPublicado }", dadosRetorno = "#result", capturarEstadoAnterior = true, auditarExcecao = true)
    @Transactional
    public Portaria atualizar(Portaria portaria) {
        if (portaria.getIdPortaria() == null) {
            throw new RuntimeException("ID da portaria não informado para atualização.");
        }
        if (!repository.existsById(portaria.getIdPortaria())) {
            throw new RuntimeException("Portaria não encontrada para atualização.");
        }
        return salvarPortaria(portaria, false);
    }

    /**
     * Método privado com a lógica comum de persistência.
     * 
     * @param isNovo true para criação, false para atualização
     */
    private Portaria salvarPortaria(Portaria portaria, boolean isNovo) {
        validarUnicidade(portaria);
        validarSobreposicao(portaria);
        normalizarFlags(portaria);

        Portaria salva = repository.save(portaria);
        log.info("Portaria {}: id={}, número={}/{}, vigência={} até {}, revogado={}",
                isNovo ? "criada" : "atualizada",
                salva.getIdPortaria(), salva.getNumero(), salva.getAno(),
                salva.getDtInicioVigencia(), salva.getDtFimVigencia(), salva.getInRevogado());
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
        if (inicio == null)
            return;
        LocalDate fimValidacao = (fim != null) ? fim : LocalDate.of(9999, 12, 31);
        Integer idPortaria = (portaria.getIdPortaria() != null) ? portaria.getIdPortaria() : 0;
        boolean sobrepoe = repository.existePeriodoSobreposto(idPortaria, inicio, fimValidacao);
        if (sobrepoe) {
            throw new RuntimeException(
                    "Já existe uma portaria cadastrada cujo período de vigência coincide com o informado. Verifique as datas.");
        }
    }

    private void normalizarFlags(Portaria portaria) {
        if (portaria.getInRevogado() == null || portaria.getInRevogado().trim().isEmpty()) {
            portaria.setInRevogado(Portaria.REVOGADO_NAO);
        } else {
            portaria.setInRevogado(portaria.getInRevogado().toUpperCase());
        }
        if (portaria.getLinkPublicado() != null) {
            portaria.setLinkPublicado(portaria.getLinkPublicado().trim());
        }
    }

    // =========================================================================
    // REVOGAÇÃO
    // =========================================================================

    @Auditar(tabela = "portaria", acao = "REVOGAR", descricao = "Revogação de portaria", dadosParametros = "{ 'id': #id }", capturarEstadoAnterior = true, auditarExcecao = true, incluirRetorno = false)
    @Transactional
    public void revogar(Integer id) {
        Portaria portaria = buscarOuFalhar(id);
        if (portaria.isRevogado()) {
            throw new RuntimeException("A portaria já está revogada.");
        }
        portaria.setInRevogado(Portaria.REVOGADO_SIM);
        repository.save(portaria);
        // Revoga todas as regras vinculadas a esta portaria
        regrasRepository.revogarRegrasPorPortaria(id);
        log.info("Portaria revogada: id={}, número={}/{}", id, portaria.getNumero(), portaria.getAno());
    }

    // =========================================================================
    // RESTAURAÇÃO
    // =========================================================================

    @Auditar(tabela = "portaria", acao = "RESTAURAR", descricao = "Restauração de portaria revogada (volta a ficar em vigor)", dadosParametros = "{ 'id': #id }", capturarEstadoAnterior = true, auditarExcecao = true, incluirRetorno = false)
    @Transactional
    public void restaurar(Integer id) {
        Portaria portaria = buscarOuFalhar(id);
        if (!portaria.isRevogado()) {
            throw new RuntimeException("A portaria já está em vigor.");
        }
        portaria.setInRevogado(Portaria.REVOGADO_NAO);
        repository.save(portaria);
        log.info("Portaria restaurada: id={}, número={}/{}", id, portaria.getNumero(), portaria.getAno());
    }

    // =========================================================================
    // EXCLUSÃO
    // =========================================================================

    @Auditar(tabela = "portaria", acao = "EXCLUIR", descricao = "Exclusão permanente de portaria", dadosParametros = "{ 'id': #id }", capturarEstadoAnterior = true, auditarExcecao = true, incluirRetorno = false)
    @Transactional
    public void excluir(Integer id) {
        Portaria portaria = buscarOuFalhar(id);
        if (!portaria.isRevogado()) {
            throw new RuntimeException("Para excluir, a portaria deve estar revogada primeiro.");
        }
        long countRegras = regrasRepository.countByPortariaIdPortaria(id);
        if (countRegras > 0) {
            throw new RuntimeException("Não é possível excluir a portaria pois existem " + countRegras +
                    " regra(s) vinculada(s). Revogue-as ou exclua-as antes.");
        }
        repository.deleteById(id);
        log.info("Portaria excluída fisicamente: id={}, número={}/{}", id, portaria.getNumero(), portaria.getAno());
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