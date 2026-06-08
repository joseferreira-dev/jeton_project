package br.com.cremepe.jeton.servico;

import br.com.cremepe.jeton.annotation.Auditar;
import br.com.cremepe.jeton.domain.NivelAcesso;
import br.com.cremepe.jeton.repository.NivelAcessoRepository;
import br.com.cremepe.jeton.repository.UsuarioAcessoRepository;

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

    @Auditar(tabela = "nivel_acesso", acao = "CRIAR", descricao = "Criação de novo nível de acesso", dadosParametros = "{ 'idNivel': #nivel.idNivel, 'nomeNivel': #nivel.nomeNivel }", dadosRetorno = "#result", capturarEstadoAnterior = false, auditarExcecao = true)
    @Transactional
    public NivelAcesso criar(NivelAcesso nivel) {
        nivel.setIdNivel(null);
        return salvar(nivel, true);
    }

    @Auditar(tabela = "nivel_acesso", acao = "ATUALIZAR", descricao = "Atualização de nível de acesso existente", dadosParametros = "{ 'idNivel': #nivel.idNivel, 'nomeNivel': #nivel.nomeNivel }", dadosRetorno = "#result", capturarEstadoAnterior = true, auditarExcecao = true)
    @Transactional
    public NivelAcesso atualizar(NivelAcesso nivel) {
        if (nivel.getIdNivel() == null || nivel.getIdNivel().trim().isEmpty()) {
            throw new RuntimeException("ID do nível de acesso não informado para atualização.");
        }
        return salvar(nivel, false);
    }

    /**
     * Método privado que contém a lógica comum de persistência.
     * 
     * @param isNovo true para criação, false para atualização
     */
    private NivelAcesso salvar(NivelAcesso nivel, boolean isNovo) {
        // Normaliza e valida ID
        String idNormalizado = nivel.getIdNivel() != null ? nivel.getIdNivel().trim().toUpperCase() : null;
        if (isNovo) {
            if (idNormalizado == null || idNormalizado.isEmpty()) {
                throw new RuntimeException("O ID do nível de acesso é obrigatório (uma letra maiúscula de A a Z).");
            }
            // Verifica se já existe um nível com este ID
            if (repository.existsById(idNormalizado)) {
                throw new RuntimeException("Já existe um nível de acesso com o ID '" + idNormalizado + "'.");
            }
            nivel.setIdNivel(idNormalizado);
        } else {
            // Para atualização, carrega o existente para garantir que o ID não foi alterado
            NivelAcesso existente = repository.findById(nivel.getIdNivel())
                    .orElseThrow(() -> new RuntimeException("Nível de acesso não encontrado para atualização."));
            // Mantém o ID original
            nivel.setIdNivel(existente.getIdNivel());
        }

        // Valida nome único (ignorando o próprio registro)
        validarNomeUnico(nivel, isNovo ? null : nivel.getIdNivel());

        // Garante que o nome seja trimado
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

    // =========================================================================
    // EXCLUSÃO
    // =========================================================================

    @Auditar(tabela = "nivel_acesso", acao = "EXCLUIR", descricao = "Exclusão de nível de acesso (apenas se não houver usuários vinculados)", dadosParametros = "{ 'idNivel': #id }", capturarEstadoAnterior = false, auditarExcecao = true, incluirRetorno = false)
    @Transactional
    public void excluir(String id) {
        // Verifica se existem usuários com este nível de acesso
        boolean emUso = usuarioAcessoRepository.existsByIdIdNivel(id);
        if (emUso) {
            throw new RuntimeException(
                    "Não é possível excluir o nível de acesso '" + id +
                            "' pois ele está associado a um ou mais usuários.");
        }

        // Busca o nível para obter nome antes de excluir (para o log)
        NivelAcesso nivel = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Nível de acesso não encontrado: " + id));

        repository.deleteById(id);
        log.info("Nível de acesso excluído: id={}, nome={}", id, nivel.getNomeNivel());
    }

    // =========================================================================
    // OPERAÇÕES DE LEITURA (sem alterações)
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
}