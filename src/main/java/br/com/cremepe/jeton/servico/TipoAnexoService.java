package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.TipoAnexo;
import br.com.cremepe.jeton.repositorio.ComprovanteRepository;
import br.com.cremepe.jeton.repositorio.TipoAnexoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TipoAnexoService {

    private static final Logger log = LoggerFactory.getLogger(TipoAnexoService.class);

    @Autowired
    private TipoAnexoRepository repository;
    @Autowired
    private ComprovanteRepository comprovanteRepository;
    @Autowired
    private LogJetonService logJetonService;

    // =========================================================================
    // OPERAÇÕES DE ESCRITA
    // =========================================================================

    @Transactional
    public TipoAnexo salvar(TipoAnexo tipoAnexo, Integer idUsuarioLogado) {
        boolean isNovo = tipoAnexo.getIdTipo() == null;

        validarNomeUnico(tipoAnexo);

        // Garante que exigePublicacao tenha valor padrão se não informado
        if (tipoAnexo.getExigePublicacao() == null || tipoAnexo.getExigePublicacao().trim().isEmpty()) {
            tipoAnexo.setExigePublicacao(TipoAnexo.EXIGE_PUBLICACAO_NAO);
        }

        TipoAnexo salvo = repository.save(tipoAnexo);
        log.info("Tipo de anexo {}: id={}, nome={}, exigePublicacao={}",
                isNovo ? "criado" : "atualizado",
                salvo.getIdTipo(), salvo.getNome(), salvo.getExigePublicacao());

        String textoLog = String.format(
                "Tipo de anexo %s: ID=%d, Nome='%s', ExigePublicacao='%s'",
                isNovo ? "criado" : "atualizado",
                salvo.getIdTipo(), salvo.getNome(), salvo.getExigePublicacao());
        logJetonService.registrarLog("tipo_anexo", idUsuarioLogado, textoLog);

        return salvo;
    }

    private void validarNomeUnico(TipoAnexo tipoAnexo) {
        String nome = tipoAnexo.getNome();
        if (nome == null || nome.trim().isEmpty())
            return;

        Optional<TipoAnexo> existente = repository.findByNome(nome.trim());
        if (existente.isPresent() && !existente.get().getIdTipo().equals(tipoAnexo.getIdTipo())) {
            throw new RuntimeException("Já existe um tipo de anexo cadastrado com o nome '" + nome + "'.");
        }
    }

    @Transactional
    public void excluir(Integer id, Integer idUsuarioLogado) {
        // Busca o tipo antes de excluir para obter dados para o log
        Optional<TipoAnexo> tipoOpt = repository.findById(id);
        if (tipoOpt.isEmpty()) {
            log.warn("Tentativa de excluir tipo de anexo inexistente ID={}", id);
            throw new RuntimeException("Tipo de anexo não encontrado para exclusão.");
        }
        TipoAnexo tipo = tipoOpt.get();
        String nome = tipo.getNome();
        String exigePublicacao = tipo.getExigePublicacao();

        // Verifica se existem comprovantes usando este tipo
        long count = comprovanteRepository.findByTipoAnexoIdTipo(id).size();
        if (count > 0) {
            throw new RuntimeException("Não é possível excluir este tipo de anexo pois existem " + count +
                    " comprovante(s) vinculado(s) a ele.");
        }

        repository.deleteById(id);
        log.info("Tipo de anexo excluído: id={}, nome={}", id, nome);

        String textoLog = String.format(
                "Tipo de anexo excluído: ID=%d, Nome='%s', ExigePublicacao='%s'",
                id, nome, exigePublicacao);
        logJetonService.registrarLog("tipo_anexo", idUsuarioLogado, textoLog);
    }

    // =========================================================================
    // OPERAÇÕES DE LEITURA
    // =========================================================================

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
}