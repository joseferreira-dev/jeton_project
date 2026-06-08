package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.annotation.Auditar;
import br.com.cremepe.jeton.domain.TipoAnexo;
import br.com.cremepe.jeton.repository.ComprovanteRepository;
import br.com.cremepe.jeton.repository.TipoAnexoRepository;

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

    @Auditar(tabela = "tipo_anexo", acao = "CRIAR", descricao = "Criação de novo tipo de anexo", dadosParametros = "{ 'nome': #tipoAnexo.nome, 'exigePublicacao': #tipoAnexo.exigePublicacao }", dadosRetorno = "#result", capturarEstadoAnterior = false, auditarExcecao = true)
    @Transactional
    public TipoAnexo criar(TipoAnexo tipoAnexo) {
        tipoAnexo.setIdTipo(null); // força criação
        return salvarTipoAnexo(tipoAnexo, true);
    }

    @Auditar(tabela = "tipo_anexo", acao = "ATUALIZAR", descricao = "Atualização de tipo de anexo existente", dadosParametros = "{ 'id': #tipoAnexo.idTipo, 'nome': #tipoAnexo.nome, 'exigePublicacao': #tipoAnexo.exigePublicacao }", dadosRetorno = "#result", capturarEstadoAnterior = true, auditarExcecao = true)
    @Transactional
    public TipoAnexo atualizar(TipoAnexo tipoAnexo) {
        if (tipoAnexo.getIdTipo() == null) {
            throw new RuntimeException("ID do tipo de anexo não informado para atualização.");
        }
        // Verifica se existe
        if (!repository.existsById(tipoAnexo.getIdTipo())) {
            throw new RuntimeException("Tipo de anexo não encontrado para atualização.");
        }
        return salvarTipoAnexo(tipoAnexo, false);
    }

    /**
     * Método privado que contém a lógica comum de persistência.
     * 
     * @param isNovo true para criação, false para atualização
     */
    private TipoAnexo salvarTipoAnexo(TipoAnexo tipoAnexo, boolean isNovo) {
        // Normaliza nome
        if (tipoAnexo.getNome() != null) {
            tipoAnexo.setNome(tipoAnexo.getNome().trim());
        }
        // Se for criação, valida nome único; se for atualização, valida ignorando o
        // próprio ID
        validarNomeUnico(tipoAnexo, isNovo ? null : tipoAnexo.getIdTipo());

        // Garante que exigePublicacao tenha valor padrão se não informado
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

    private void validarNomeUnico(TipoAnexo tipoAnexo, Integer idAtual) {
        String nome = tipoAnexo.getNome();
        if (nome == null || nome.trim().isEmpty())
            return;

        Optional<TipoAnexo> existente = repository.findByNome(nome.trim());
        if (existente.isPresent() && !existente.get().getIdTipo().equals(idAtual)) {
            throw new RuntimeException("Já existe um tipo de anexo cadastrado com o nome '" + nome + "'.");
        }
    }

    // =========================================================================
    // EXCLUSÃO
    // =========================================================================

    @Auditar(tabela = "tipo_anexo", acao = "EXCLUIR", descricao = "Exclusão de tipo de anexo (apenas se não houver comprovantes vinculados)", dadosParametros = "{ 'id': #id }", capturarEstadoAnterior = false, auditarExcecao = true, incluirRetorno = false)
    @Transactional
    public void excluir(Integer id) {
        // Busca o tipo antes de excluir para obter dados (usados no log.info)
        Optional<TipoAnexo> tipoOpt = repository.findById(id);
        if (tipoOpt.isEmpty()) {
            log.warn("Tentativa de excluir tipo de anexo inexistente ID={}", id);
            throw new RuntimeException("Tipo de anexo não encontrado para exclusão.");
        }
        TipoAnexo tipo = tipoOpt.get();

        // Verifica se existem comprovantes usando este tipo
        long count = comprovanteRepository.findByTipoAnexoIdTipo(id).size();
        if (count > 0) {
            throw new RuntimeException("Não é possível excluir este tipo de anexo pois existem " + count +
                    " comprovante(s) vinculado(s) a ele.");
        }

        repository.deleteById(id);
        log.info("Tipo de anexo excluído: id={}, nome={}", id, tipo.getNome());
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