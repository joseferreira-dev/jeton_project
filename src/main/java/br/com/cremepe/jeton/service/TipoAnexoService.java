package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.domain.TipoAnexo;
import br.com.cremepe.jeton.repository.TipoAnexoRepository;
import br.com.cremepe.jeton.util.ArquivoValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TipoAnexoService {

    private static final Logger log = LoggerFactory.getLogger(TipoAnexoService.class);

    private final TipoAnexoRepository repository;
    private final LogJetonService logJetonService;
    private final ArquivoValidator arquivoValidator;

    public TipoAnexoService(TipoAnexoRepository repository,
            LogJetonService logJetonService,
            ArquivoValidator arquivoValidator) {
        this.repository = repository;
        this.logJetonService = logJetonService;
        this.arquivoValidator = arquivoValidator;
    }

    @Transactional
    public TipoAnexo criar(TipoAnexo tipoAnexo) {
        tipoAnexo.setIdTipo(null);
        TipoAnexo salvo = salvar(tipoAnexo, true);
        logJetonService.logTipoAnexoCriado(salvo);
        return salvo;
    }

    @Transactional
    public TipoAnexo atualizar(TipoAnexo tipoAnexo) {
        if (tipoAnexo.getIdTipo() == null) {
            throw new RuntimeException("ID do tipo de anexo não informado para atualização.");
        }
        if (!repository.existsById(tipoAnexo.getIdTipo())) {
            throw new RuntimeException("Tipo de anexo não encontrado para atualização.");
        }

        TipoAnexo antigo = repository.findById(tipoAnexo.getIdTipo())
                .orElseThrow(() -> new RuntimeException("Tipo de anexo não encontrado."));
        TipoAnexo copia = copiarTipoAnexo(antigo);

        TipoAnexo atualizado = salvar(tipoAnexo, false);
        logJetonService.logTipoAnexoAtualizado(copia, atualizado);
        return atualizado;
    }

    private TipoAnexo salvar(TipoAnexo tipoAnexo, boolean isNovo) {
        if (tipoAnexo.getNome() != null) {
            tipoAnexo.setNome(tipoAnexo.getNome().trim());
        }

        arquivoValidator.validarNomeUnicoTipoAnexo(tipoAnexo, isNovo);

        if (tipoAnexo.getExigePublicacao() == null || tipoAnexo.getExigePublicacao().trim().isEmpty()) {
            tipoAnexo.setExigePublicacao(TipoAnexo.EXIGE_PUBLICACAO_NAO);
        } else {
            tipoAnexo.setExigePublicacao(tipoAnexo.getExigePublicacao().toUpperCase());
        }

        TipoAnexo salvo = repository.save(tipoAnexo);
        log.info("Tipo de anexo {}: id={}, nome={}, exigePublicacao={}",
                isNovo ? "criado" : "atualizado",
                salvo.getIdTipo(), salvo.getNome(), salvo.getExigePublicacao());
        return salvo;
    }

    @Transactional
    public void excluir(Integer id) {
        Optional<TipoAnexo> tipoOpt = repository.findById(id);
        if (tipoOpt.isEmpty()) {
            log.warn("Tentativa de excluir tipo de anexo inexistente ID={}", id);
            throw new RuntimeException("Tipo de anexo não encontrado para exclusão.");
        }
        TipoAnexo tipo = tipoOpt.get();
        TipoAnexo copia = copiarTipoAnexo(tipo);

        // Valida se pode excluir
        arquivoValidator.validarExclusaoTipoAnexo(id);

        repository.deleteById(id);
        log.info("Tipo de anexo excluído: id={}, nome={}", id, tipo.getNome());
        logJetonService.logTipoAnexoExcluido(copia);
    }

    @Transactional(readOnly = true)
    public List<TipoAnexo> listarTodos() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<TipoAnexo> buscarPorId(Integer id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public TipoAnexo buscarOuFalhar(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tipo de anexo não encontrado com ID: " + id));
    }

    private TipoAnexo copiarTipoAnexo(TipoAnexo original) {
        TipoAnexo copia = new TipoAnexo();
        copia.setIdTipo(original.getIdTipo());
        copia.setNome(original.getNome());
        copia.setExigePublicacao(original.getExigePublicacao());
        return copia;
    }
}