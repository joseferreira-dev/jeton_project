package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.dominio.NivelAcesso;
import br.com.cremepe.jeton.repositorio.NivelAcessoRepository;
import br.com.cremepe.jeton.repositorio.UsuarioAcessoRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class NivelAcessoService {

    private static final Logger log = LoggerFactory.getLogger(NivelAcessoService.class);

    @Autowired
    private NivelAcessoRepository repository;
    @Autowired
    private UsuarioAcessoRepository usuarioAcessoRepository;

    // =========================================================================
    // OPERAÇÕES DE ESCRITA
    // =========================================================================
    @Transactional
    public NivelAcesso salvar(NivelAcesso nivel) {
        validarNomeUnico(nivel);
        NivelAcesso salvo = repository.save(nivel);
        log.info("Nível de acesso salvo: id={}, nome={}", salvo.getIdNivel(), salvo.getNomeNivel());
        return salvo;
    }

    private void validarNomeUnico(NivelAcesso nivel) {
        if (nivel.getNomeNivel() == null || nivel.getNomeNivel().trim().isEmpty())
            return;
        Optional<NivelAcesso> existente = repository.findByNomeNivel(nivel.getNomeNivel().trim());
        if (existente.isPresent() && !existente.get().getIdNivel().equals(nivel.getIdNivel())) {
            throw new RuntimeException("Já existe um nível de acesso com o nome '" + nivel.getNomeNivel() + "'.");
        }
    }

    // =========================================================================
    // OPERAÇÕES DE LEITURA
    // =========================================================================
    @Transactional(readOnly = true)
    public List<NivelAcesso> listarTodos() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<NivelAcesso> buscarPorId(String id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public NivelAcesso buscarOuFalhar(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Nível de acesso não encontrado: " + id));
    }

    // =========================================================================
    // EXCLUSÃO
    // =========================================================================
    @Transactional
    public void excluir(String id) {
        // Verifica se existem usuários com este nível de acesso
        boolean emUso = usuarioAcessoRepository.existsByIdIdNivel(id);
        if (emUso) {
            throw new RuntimeException(
                    "Não é possível excluir o nível de acesso '" + id
                            + "' pois ele está associado a um ou mais usuários.");
        }
        repository.deleteById(id);
        log.info("Nível de acesso excluído: id={}", id);
    }
}