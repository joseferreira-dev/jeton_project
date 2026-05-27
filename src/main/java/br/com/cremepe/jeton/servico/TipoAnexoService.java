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

    // =========================================================================
    // OPERAÇÕES DE ESCRITA
    // =========================================================================

    @Transactional
    public TipoAnexo salvar(TipoAnexo tipoAnexo) {
        validarNomeUnico(tipoAnexo);

        // Garante que exigePublicacao tenha valor padrão se não informado
        if (tipoAnexo.getExigePublicacao() == null || tipoAnexo.getExigePublicacao().trim().isEmpty()) {
            tipoAnexo.setExigePublicacao(TipoAnexo.EXIGE_PUBLICACAO_NAO);
        }

        TipoAnexo salvo = repository.save(tipoAnexo);
        log.info("Tipo de anexo salvo: id={}, nome={}", salvo.getIdTipo(), salvo.getNome());
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
    public void excluir(Integer id) {
        // Verifica se existem comprovantes usando este tipo
        long count = comprovanteRepository.findByTipoAnexoIdTipo(id).size();
        if (count > 0) {
            throw new RuntimeException("Não é possível excluir este tipo de anexo pois existem " + count +
                    " comprovante(s) vinculado(s) a ele.");
        }
        repository.deleteById(id);
        log.info("Tipo de anexo excluído: id={}", id);
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