package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.domain.NivelAcesso;
import br.com.cremepe.jeton.repository.NivelAcessoRepository;
import br.com.cremepe.jeton.repository.UsuarioAcessoRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class NivelAcessoService {

    private static final Logger log = LoggerFactory.getLogger(NivelAcessoService.class);

    private final NivelAcessoRepository repository;
    private final UsuarioAcessoRepository usuarioAcessoRepository;
    private final LogJetonService logJetonService; // <-- INJETADO

    public NivelAcessoService(NivelAcessoRepository repository,
            UsuarioAcessoRepository usuarioAcessoRepository,
            LogJetonService logJetonService) {
        this.repository = repository;
        this.usuarioAcessoRepository = usuarioAcessoRepository;
        this.logJetonService = logJetonService;
    }

    @Transactional
    public NivelAcesso criar(NivelAcesso nivel) {
        nivel.setIdNivel(null);
        NivelAcesso salvo = salvar(nivel, true);
        logJetonService.logNivelAcessoCriado(salvo);
        return salvo;
    }

    @Transactional
    public NivelAcesso atualizar(NivelAcesso nivel) {
        if (nivel.getIdNivel() == null || nivel.getIdNivel().trim().isEmpty()) {
            throw new RuntimeException("ID do nível de acesso não informado para atualização.");
        }
        NivelAcesso antigo = repository.findById(nivel.getIdNivel())
                .orElseThrow(() -> new RuntimeException("Nível de acesso não encontrado para atualização."));
        NivelAcesso copia = copiarNivelAcesso(antigo);

        NivelAcesso atualizado = salvar(nivel, false);
        logJetonService.logNivelAcessoAtualizado(copia, atualizado);
        return atualizado;
    }

    private NivelAcesso salvar(NivelAcesso nivel, boolean isNovo) {
        // Normaliza e valida ID
        String idNormalizado = nivel.getIdNivel() != null ? nivel.getIdNivel().trim().toUpperCase() : null;
        if (isNovo) {
            if (idNormalizado == null || idNormalizado.isEmpty()) {
                throw new RuntimeException("O ID do nível de acesso é obrigatório (uma letra maiúscula de A a Z).");
            }
            if (repository.existsById(idNormalizado)) {
                throw new RuntimeException("Já existe um nível de acesso com o ID '" + idNormalizado + "'.");
            }
            nivel.setIdNivel(idNormalizado);
        } else {
            NivelAcesso existente = repository.findById(nivel.getIdNivel())
                    .orElseThrow(() -> new RuntimeException("Nível de acesso não encontrado para atualização."));
            nivel.setIdNivel(existente.getIdNivel());
        }

        validarNomeUnico(nivel, isNovo ? null : nivel.getIdNivel());

        if (nivel.getNomeNivel() != null) {
            nivel.setNomeNivel(nivel.getNomeNivel().trim());
        }

        NivelAcesso salvo = repository.save(nivel);
        log.info("Nível de acesso {}: id={}, nome={}",
                isNovo ? "criado" : "atualizado", salvo.getIdNivel(), salvo.getNomeNivel());

        return salvo;
    }

    private void validarNomeUnico(NivelAcesso nivel, String idAtual) {
        if (nivel.getNomeNivel() == null || nivel.getNomeNivel().trim().isEmpty())
            return;
        String nome = nivel.getNomeNivel().trim();
        Optional<NivelAcesso> existente = repository.findByNomeNivel(nome);
        if (existente.isPresent() && !existente.get().getIdNivel().equals(idAtual)) {
            throw new RuntimeException("Já existe um nível de acesso com o nome '" + nome + "'.");
        }
    }

    @Transactional
    public void excluir(String id) {
        boolean emUso = usuarioAcessoRepository.existsByIdIdNivel(id);
        if (emUso) {
            throw new RuntimeException(
                    "Não é possível excluir o nível de acesso '" + id +
                            "' pois ele está associado a um ou mais usuários.");
        }

        NivelAcesso nivel = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Nível de acesso não encontrado: " + id));

        repository.deleteById(id);

        logJetonService.logNivelAcessoExcluido(id, nivel.getNomeNivel());
        log.info("Nível de acesso excluído: id={}, nome={}", id, nivel.getNomeNivel());
    }

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

    private NivelAcesso copiarNivelAcesso(NivelAcesso original) {
        NivelAcesso copia = new NivelAcesso();
        copia.setIdNivel(original.getIdNivel());
        copia.setNomeNivel(original.getNomeNivel());
        return copia;
    }
}