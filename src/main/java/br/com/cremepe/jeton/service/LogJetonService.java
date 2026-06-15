package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.domain.AtividadeConselhal;
import br.com.cremepe.jeton.domain.Comprovante;
import br.com.cremepe.jeton.domain.Conselheiro;
import br.com.cremepe.jeton.domain.Gestao;
import br.com.cremepe.jeton.domain.GestaoConselheiro;
import br.com.cremepe.jeton.domain.NivelAcesso;
import br.com.cremepe.jeton.domain.PontosSaldo;
import br.com.cremepe.jeton.domain.TipoAnexo;
import br.com.cremepe.jeton.domain.Usuario;
import br.com.cremepe.jeton.domain.ViewUserLogin;
import br.com.cremepe.jeton.repository.LogJetonRepository;
import br.com.cremepe.jeton.repository.UsuarioRepository;
import br.com.cremepe.jeton.util.JsonConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class LogJetonService {

    private static final Logger log = LoggerFactory.getLogger(LogJetonService.class);

    private final LogJetonRepository logRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioLogadoService usuarioLogadoService;
    private final JsonConverter jsonConverter;

    public LogJetonService(LogJetonRepository logRepository,
            UsuarioRepository usuarioRepository,
            UsuarioLogadoService usuarioLogadoService,
            JsonConverter jsonConverter) {
        this.logRepository = logRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioLogadoService = usuarioLogadoService;
        this.jsonConverter = jsonConverter;
    }

    // ========== Método genérico que centraliza a escrita do log ==========

    private void registrarLogComum(String nomeTabela, String acao, String descricao, boolean sucesso,
            Map<String, Object> dadosEspecificos) {
        try {
            Integer idUsuario = obterIdUsuarioLogado();
            if (idUsuario == null) {
                log.warn("Tentativa de registrar log sem usuário logado. Ação: {}, Tabela: {}", acao, nomeTabela);
                return;
            }

            var usuario = usuarioRepository.getReferenceById(idUsuario);
            var detalhesRequisicao = obterDetalhesRequisicao();

            Map<String, Object> logCompleto = new LinkedHashMap<>();
            logCompleto.put("idUnico", UUID.randomUUID().toString());
            logCompleto.put("timestamp", LocalDateTime.now().toString());
            logCompleto.put("usuarioId", idUsuario);
            logCompleto.put("usuarioNome", usuario.getPessoa().getNome());
            logCompleto.put("acao", acao);
            logCompleto.put("tabela", nomeTabela);
            logCompleto.put("descricao", descricao);
            logCompleto.put("sucesso", sucesso);
            logCompleto.putAll(detalhesRequisicao);
            if (dadosEspecificos != null && !dadosEspecificos.isEmpty()) {
                logCompleto.put("dados", dadosEspecificos);
            }

            String textoLog = jsonConverter.toJson(logCompleto);
            registrarLog(nomeTabela, idUsuario, textoLog);
        } catch (Exception e) {
            log.error("Falha ao registrar log de auditoria", e);
        }
    }

    // ========== Método original (mantido para compatibilidade) ==========

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarLog(String nomeTabela, Integer idUsuario, String textoLog) {
        if (idUsuario == null)
            return;
        var usuario = usuarioRepository.getReferenceById(idUsuario);
        var logJeton = new br.com.cremepe.jeton.domain.LogJeton();
        logJeton.setNomeTabela(nomeTabela);
        logJeton.setUsuario(usuario);
        logJeton.setDataHoraLog(LocalDateTime.now());
        logJeton.setTextoLog(textoLog);
        logRepository.save(logJeton);
        log.debug("Log registrado: tabela={}, usuario={}", nomeTabela, idUsuario);
    }

    // AtividadeConselhalService

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAtividadeCriada(AtividadeConselhal atividade) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("atividade", extrairDadosAtividade(atividade));
        registrarLogComum("atividade_conselhal", "CRIAR", "Criação de nova atividade", true, dados);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAtividadeAtualizada(AtividadeConselhal antiga, AtividadeConselhal atualizada) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("atualizada", extrairDadosAtividade(atualizada));
        dados.put("anterior", extrairDadosAtividade(antiga));
        registrarLogComum("atividade_conselhal", "ATUALIZAR", "Atualização de atividade existente", true, dados);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAtividadeValidada(Integer idAtividade) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("idAtividade", idAtividade);
        registrarLogComum("atividade_conselhal", "VALIDAR", "Validação de atividade pendente", true, dados);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAtividadeDesvalidada(Integer idAtividade) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("idAtividade", idAtividade);
        registrarLogComum("atividade_conselhal", "DESVALIDAR", "Desvalidação de atividade", true, dados);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAtividadeExcluida(AtividadeConselhal atividade) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("atividade", extrairDadosAtividade(atividade));
        registrarLogComum("atividade_conselhal", "EXCLUIR", "Exclusão de atividade", true, dados);
    }

    private Map<String, Object> extrairDadosAtividade(AtividadeConselhal atividade) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("id", atividade.getIdAtividade());
        dados.put("gestaoId", atividade.getGestao() != null ? atividade.getGestao().getIdGestao() : null);
        dados.put("conselheiroId",
                atividade.getConselheiro() != null ? atividade.getConselheiro().getIdPessoa() : null);
        dados.put("regraId", atividade.getRegra() != null ? atividade.getRegra().getIdRegra() : null);
        dados.put("comprovanteId",
                atividade.getComprovante() != null ? atividade.getComprovante().getIdComprovante() : null);
        dados.put("dataHoraAtividade",
                atividade.getDataHoraAtividade() != null ? atividade.getDataHoraAtividade().toString() : null);
        dados.put("dataHoraRegistro",
                atividade.getDataHoraRegistro() != null ? atividade.getDataHoraRegistro().toString() : null);
        dados.put("turno", atividade.getInTurno() != null && atividade.getInTurno() == "M" ? "Manhã"
                : atividade.getInTurno() == "T" ? "Tarde" : atividade.getInTurno() == "N" ? "Noite" : null);
        dados.put("situacao", atividade.getInSituacao() == "P" ? "Pendente"
                : atividade.getInSituacao() == "F" ? "Fechada" : atividade.getInSituacao() == "C" ? "Computada" : null);
        dados.put("computada",
                atividade.getInComputada() != null && atividade.getInComputada() == "S" ? "Sim"
                        : atividade.getInComputada() == "N" ? "Não" : null);
        return dados;
    }

    // AtividadeLoteService

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logLoteCriado(Integer idComprovante, Integer idGestao, Integer idRegra,
            List<Integer> idsConselheiros, LocalDateTime dataHoraAtividade) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("idComprovante", idComprovante);
        dados.put("idGestao", idGestao);
        dados.put("idRegra", idRegra);
        dados.put("idsConselheiros", idsConselheiros);
        dados.put("dataHoraAtividade", dataHoraAtividade != null ? dataHoraAtividade.toString() : null);
        registrarLogComum("atividade_conselhal", "CRIAR_LOTE",
                "Criação de múltiplas atividades com mesmo comprovante", true, dados);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logLoteAtualizado(Integer idComprovante, List<Integer> idsAntigos, List<Integer> idsNovos,
            Integer idGestao, Integer idRegra, LocalDateTime dataHoraAtividade) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("idComprovante", idComprovante);
        dados.put("idsAntigos", idsAntigos);
        dados.put("idsNovos", idsNovos);
        dados.put("idGestao", idGestao);
        dados.put("idRegra", idRegra);
        dados.put("dataHoraAtividade", dataHoraAtividade != null ? dataHoraAtividade.toString() : null);
        registrarLogComum("atividade_conselhal", "EDITAR_LOTE",
                "Edição em massa de atividades que compartilham o mesmo comprovante", true, dados);
    }

    // ComprovanteService

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logComprovanteCriado(Comprovante comprovante) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("comprovante", extrairDadosComprovante(comprovante));
        registrarLogComum("comprovante", "CRIAR", "Criação de novo comprovante", true, dados);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logComprovanteExcluido(Comprovante comprovante) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("comprovante", extrairDadosComprovante(comprovante));
        registrarLogComum("comprovante", "EXCLUIR", "Exclusão de comprovante", true, dados);
    }

    private Map<String, Object> extrairDadosComprovante(Comprovante comprovante) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("id", comprovante.getIdComprovante());
        dados.put("nomeComprovante", comprovante.getNomeComprovante());
        dados.put("nomeArquivo", comprovante.getNomeArquivo());
        dados.put("contentType", comprovante.getContentType());
        dados.put("mes", comprovante.getMes());
        dados.put("ano", comprovante.getAno());
        dados.put("idTipoAnexo", comprovante.getTipoAnexo() != null ? comprovante.getTipoAnexo().getIdTipo() : null);
        dados.put("tipoAnexoNome", comprovante.getTipoAnexo() != null ? comprovante.getTipoAnexo().getNome() : null);
        return dados;
    }

    // ConselheiroService

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logConselheiroCriado(Conselheiro conselheiro) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("conselheiro", extrairDadosConselheiro(conselheiro));
        registrarLogComum("conselheiro", "CRIAR", "Criação de novo conselheiro", true, dados);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logConselheiroAtualizado(Conselheiro antigo, Conselheiro atualizado) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("atualizado", extrairDadosConselheiro(atualizado));
        dados.put("anterior", extrairDadosConselheiro(antigo));
        registrarLogComum("conselheiro", "ATUALIZAR", "Atualização de conselheiro existente", true, dados);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logConselheiroExcluido(Conselheiro conselheiro) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("conselheiro", extrairDadosConselheiro(conselheiro));
        registrarLogComum("conselheiro", "EXCLUIR", "Exclusão física de conselheiro", true, dados);
    }

    private Map<String, Object> extrairDadosConselheiro(Conselheiro conselheiro) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("id", conselheiro.getIdPessoa());
        dados.put("nome", conselheiro.getPessoa() != null ? conselheiro.getPessoa().getNome() : null);
        dados.put("email", conselheiro.getPessoa() != null ? conselheiro.getPessoa().getEmail() : null);
        dados.put("cpf", conselheiro.getPessoa() != null ? conselheiro.getPessoa().getCpf() : null);
        dados.put("crm", conselheiro.getCrm());
        dados.put("situacao", conselheiro.getInSituacao() != null && conselheiro.getInSituacao() == "A" ? "Ativo"
                : conselheiro.getInSituacao() == "I" ? "Inativo" : null);
        return dados;
    }

    // FileStorageService

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logUploadArquivo(String nomeOriginal, String nomeGerado, long tamanho,
            Integer ano, Integer mes, String contentType) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("nomeOriginal", nomeOriginal);
        dados.put("nomeGerado", nomeGerado);
        dados.put("tamanhoBytes", tamanho);
        dados.put("ano", ano);
        dados.put("mes", mes);
        dados.put("contentType", contentType);
        registrarLogComum("file_storage", "UPLOAD", "Upload de arquivo para o servidor FTP", true, dados);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logExcluirArquivo(String fileName, Integer ano, Integer mes) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("fileName", fileName);
        dados.put("ano", ano);
        dados.put("mes", mes);
        registrarLogComum("file_storage", "EXCLUIR", "Remoção de arquivo do servidor FTP", true, dados);
    }

    // GestaoConselheiroService

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logVinculoCriado(GestaoConselheiro vinculo) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("vinculo", extrairDadosVinculo(vinculo));
        registrarLogComum("gestao_conselheiro", "CRIAR", "Criação de vínculo entre conselheiro e gestão", true, dados);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logVinculoAtualizado(GestaoConselheiro antigo, GestaoConselheiro novo) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("atualizado", extrairDadosVinculo(novo));
        dados.put("anterior", extrairDadosVinculo(antigo));
        registrarLogComum("gestao_conselheiro", "ATUALIZAR",
                "Atualização de vínculo entre conselheiro e gestão (apenas situação)", true, dados);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logVinculoAtivado(Integer idGestao, Integer idPessoa) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("idGestao", idGestao);
        dados.put("idPessoa", idPessoa);
        registrarLogComum("gestao_conselheiro", "ATIVAR", "Ativação de vínculo entre conselheiro e gestão", true,
                dados);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logVinculoInativado(Integer idGestao, Integer idPessoa) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("idGestao", idGestao);
        dados.put("idPessoa", idPessoa);
        registrarLogComum("gestao_conselheiro", "INATIVAR", "Inativação de vínculo entre conselheiro e gestão", true,
                dados);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logVinculoExcluido(GestaoConselheiro vinculo) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("vinculo", extrairDadosVinculo(vinculo));
        registrarLogComum("gestao_conselheiro", "EXCLUIR", "Exclusão de vínculo", true, dados);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logVinculosAtualizadosEmMassa(Integer idGestao, List<Integer> idsRemovidos,
            List<Integer> idsAdicionados) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("idGestao", idGestao);
        dados.put("idsRemovidos", idsRemovidos);
        dados.put("idsAdicionados", idsAdicionados);
        registrarLogComum("gestao_conselheiro", "ATUALIZAR_EM_MASSA",
                "Atualização em massa de vínculos de conselheiros para uma gestão", true, dados);
    }

    private Map<String, Object> extrairDadosVinculo(GestaoConselheiro vinculo) {
        Map<String, Object> dados = new LinkedHashMap<>();
        if (vinculo.getId() != null) {
            dados.put("idGestao", vinculo.getId().getIdGestao());
            dados.put("idPessoa", vinculo.getId().getIdPessoa());
        }
        dados.put("nomeGestao", vinculo.getGestao() != null ? vinculo.getGestao().getNomeGestao() : null);
        dados.put("nomeConselheiro", vinculo.getConselheiro() != null && vinculo.getConselheiro().getPessoa() != null
                ? vinculo.getConselheiro().getPessoa().getNome()
                : null);
        dados.put("situacao", vinculo.getInSituacao());
        return dados;
    }

    // GestaoService

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logGestaoCriada(Gestao gestao) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("gestao", extrairDadosGestao(gestao));
        registrarLogComum("gestao", "CRIAR", "Criação de nova gestão", true, dados);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logGestaoAtualizada(Gestao antiga, Gestao nova) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("atualizada", extrairDadosGestao(nova));
        dados.put("anterior", extrairDadosGestao(antiga));
        registrarLogComum("gestao", "ATUALIZAR", "Edição de gestão", true, dados);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logGestaoExcluida(Gestao gestao) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("gestao", extrairDadosGestao(gestao));
        registrarLogComum("gestao", "EXCLUIR", "Exclusão de gestão", true, dados);
    }

    private Map<String, Object> extrairDadosGestao(Gestao gestao) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("id", gestao.getIdGestao());
        dados.put("nome", gestao.getNomeGestao());
        dados.put("dataInicio", gestao.getDtInicio() != null ? gestao.getDtInicio().toString() : null);
        dados.put("dataFim", gestao.getDtFim() != null ? gestao.getDtFim().toString() : null);
        return dados;
    }

    // JetonService

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFolhaProcessada(Gestao gestao, Integer mes, Integer ano,
            Integer totalConselheiros, Integer totalJetons, BigDecimal totalValor) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("idGestao", gestao.getIdGestao());
        dados.put("nomeGestao", gestao.getNomeGestao());
        dados.put("mes", mes);
        dados.put("ano", ano);
        dados.put("totalConselheirosProcessados", totalConselheiros);
        dados.put("totalJetonsGerados", totalJetons);
        dados.put("totalValorPago", totalValor != null ? totalValor.toString() : "0");
        registrarLogComum("jeton", "PROCESSAR_FOLHA", "Processamento mensal de folha de jetons", true, dados);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logJetonEstornado(Integer idJeton, String nomeConselheiro, String nomeGestao,
            Integer mes, Integer ano) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("idJeton", idJeton);
        dados.put("nomeConselheiro", nomeConselheiro);
        dados.put("nomeGestao", nomeGestao);
        dados.put("mes", mes);
        dados.put("ano", ano);
        registrarLogComum("jeton", "ESTORNAR_PONTUAL", "Estorno pontual de um Jeton (exclusão lógica)", true, dados);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFolhaHomologada(Gestao gestao, Integer mes, Integer ano,
            Integer totalAtividadesFechadas, Integer totalJetonsAfetados) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("idGestao", gestao.getIdGestao());
        dados.put("nomeGestao", gestao.getNomeGestao());
        dados.put("mes", mes);
        dados.put("ano", ano);
        dados.put("totalAtividadesFechadas", totalAtividadesFechadas);
        dados.put("totalJetonsAfetados", totalJetonsAfetados);
        registrarLogComum("jeton", "HOMOLOGAR_FOLHA", "Homologação e fechamento definitivo da folha mensal", true,
                dados);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logJetonExcluido(Integer idJeton, String nomeConselheiro, String nomeGestao,
            Integer mes, Integer ano) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("idJeton", idJeton);
        dados.put("nomeConselheiro", nomeConselheiro);
        dados.put("nomeGestao", nomeGestao);
        dados.put("mes", mes);
        dados.put("ano", ano);
        registrarLogComum("jeton", "EXCLUIR", "Exclusão física de um registro de Jeton", true, dados);
    }

    // NivelAcessoService

    // ========== Métodos para NivelAcessoService ==========

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logNivelAcessoCriado(NivelAcesso nivel) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("nivelAcesso", extrairDadosNivelAcesso(nivel));
        registrarLogComum("nivel_acesso", "CRIAR", "Criação de novo nível de acesso", true, dados);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logNivelAcessoAtualizado(NivelAcesso antigo, NivelAcesso novo) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("atualizado", extrairDadosNivelAcesso(novo));
        dados.put("anterior", extrairDadosNivelAcesso(antigo));
        registrarLogComum("nivel_acesso", "ATUALIZAR", "Atualização de nível de acesso existente", true, dados);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logNivelAcessoExcluido(String idNivel, String nomeNivel) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("idNivel", idNivel);
        dados.put("nomeNivel", nomeNivel);
        registrarLogComum("nivel_acesso", "EXCLUIR",
                "Exclusão de nível de acesso (apenas se não houver usuários vinculados)", true, dados);
    }

    private Map<String, Object> extrairDadosNivelAcesso(NivelAcesso nivel) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("id", nivel.getIdNivel());
        dados.put("nome", nivel.getNomeNivel());
        return dados;
    }

    // ParametrosService

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logBloqueioAlternado(String statusAntigo, String statusNovo) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("statusAnterior", "S".equals(statusAntigo) ? "BLOQUEADO" : "LIBERADO");
        dados.put("statusNovo", "S".equals(statusNovo) ? "BLOQUEADO" : "LIBERADO");
        registrarLogComum("parametros", "ALTERAR_BLOQUEIO", "Alterna o bloqueio do sistema", true, dados);
    }

    // PermissaoService

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logPermissaoConcedida(Usuario usuario, NivelAcesso nivel) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("usuarioId", usuario.getIdUsuarioPessoa());
        dados.put("usuarioNome", usuario.getPessoa() != null ? usuario.getPessoa().getNome() : null);
        dados.put("nivelId", nivel.getIdNivel());
        registrarLogComum("usuario_acesso", "CONCEDER", "Concessão de permissão (nível de acesso) a um usuário", true,
                dados);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logPermissaoRevogada(Usuario usuario, NivelAcesso nivel) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("usuarioId", usuario.getIdUsuarioPessoa());
        dados.put("usuarioNome", usuario.getPessoa() != null ? usuario.getPessoa().getNome() : null);
        dados.put("nivelId", nivel.getIdNivel());
        registrarLogComum("usuario_acesso", "REVOGAR", "Revogação de permissão (nível de acesso) de um usuário", true,
                dados);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logTodasPermissoesRevogadas(Usuario usuario) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("usuarioId", usuario.getIdUsuarioPessoa());
        dados.put("usuarioNome", usuario.getPessoa() != null ? usuario.getPessoa().getNome() : null);
        registrarLogComum("usuario_acesso", "REVOGAR_TODAS", "Revogação de todas as permissões de um usuário", true,
                dados);
    }

    // PontosSaldoService

    // ========== Métodos para PontosSaldoService ==========

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logPontosSaldoCriado(PontosSaldo pontos) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("pontosSaldo", extrairDadosPontosSaldo(pontos));
        registrarLogComum("pontos_saldo", "CRIAR", "Criação de registro de saldo de pontos", true, dados);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logPontosSaldoAtualizado(PontosSaldo antigo, PontosSaldo novo) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("atualizado", extrairDadosPontosSaldo(novo));
        dados.put("anterior", extrairDadosPontosSaldo(antigo));
        registrarLogComum("pontos_saldo", "ATUALIZAR", "Atualização de registro de saldo de pontos", true, dados);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logPontosSaldoExcluido(PontosSaldo pontos) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("pontosSaldo", extrairDadosPontosSaldo(pontos));
        registrarLogComum("pontos_saldo", "EXCLUIR",
                "Exclusão de registro de saldo de pontos (apenas se não utilizado)", true, dados);
    }

    private Map<String, Object> extrairDadosPontosSaldo(PontosSaldo pontos) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("id", pontos.getIdPontosSaldo());
        dados.put("conselheiroId", pontos.getConselheiro() != null ? pontos.getConselheiro().getIdPessoa() : null);
        dados.put("gestaoId", pontos.getGestao() != null ? pontos.getGestao().getIdGestao() : null);
        dados.put("atividadeId", pontos.getAtividade() != null ? pontos.getAtividade().getIdAtividade() : null);
        dados.put("jetonId", pontos.getJeton() != null ? pontos.getJeton().getIdJeton() : null);
        dados.put("resolucaoId", pontos.getResolucao() != null ? pontos.getResolucao().getIdResolucao() : null);
        dados.put("dataHora", pontos.getDataHora() != null ? pontos.getDataHora().toString() : null);
        dados.put("pontosTrabalhados", pontos.getPontosTrabalhados());
        dados.put("pontosUtilizados", pontos.getPontosUtilizados());
        dados.put("pontosSobrando", pontos.getPontosSobrando());
        dados.put("situacao", pontos.getInSituacao());
        return dados;
    }

    // PortariaService

    // RegrasService

    // RegrasConjuntasService

    // TipoAnexoService

    // ========== Métodos para TipoAnexoService ==========

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logTipoAnexoCriado(TipoAnexo tipo) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("tipoAnexo", extrairDadosTipoAnexo(tipo));
        registrarLogComum("tipo_anexo", "CRIAR", "Criação de novo tipo de anexo", true, dados);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logTipoAnexoAtualizado(TipoAnexo antigo, TipoAnexo novo) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("atualizado", extrairDadosTipoAnexo(novo));
        dados.put("anterior", extrairDadosTipoAnexo(antigo));
        registrarLogComum("tipo_anexo", "ATUALIZAR", "Atualização de tipo de anexo existente", true, dados);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logTipoAnexoExcluido(TipoAnexo tipo) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("tipoAnexo", extrairDadosTipoAnexo(tipo));
        registrarLogComum("tipo_anexo", "EXCLUIR",
                "Exclusão de tipo de anexo (apenas se não houver comprovantes vinculados)", true, dados);
    }

    private Map<String, Object> extrairDadosTipoAnexo(TipoAnexo tipo) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("id", tipo.getIdTipo());
        dados.put("nome", tipo.getNome());
        dados.put("exigePublicacao", tipo.isExigePublicacao() ? "Sim" : "Não");
        return dados;
    }

    // UsuarioService

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logUsuarioCriado(Usuario usuario) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("usuario", extrairDadosUsuario(usuario));
        registrarLogComum("usuario", "CRIAR", "Criação de novo usuário", true, dados);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logUsuarioAtualizado(Usuario atualizado, Usuario antigo) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("atualizado", extrairDadosUsuario(atualizado));
        dados.put("anterior", extrairDadosUsuario(antigo));
        registrarLogComum("usuario", "ATUALIZAR", "Atualização de usuário existente", true, dados);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logUsuarioExcluido(Usuario usuario) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("usuario", extrairDadosUsuario(usuario));
        registrarLogComum("usuario", "EXCLUIR", "Exclusão de usuário", true, dados);
    }

    private Map<String, Object> extrairDadosUsuario(Usuario usuario) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("id", usuario.getIdUsuarioPessoa());
        dados.put("nome", usuario.getPessoa().getNome());
        dados.put("email", usuario.getPessoa().getEmail());
        dados.put("cpf", usuario.getPessoa().getCpf());
        dados.put("situacao", usuario.getInSituacao());
        dados.put("tipoPessoa", usuario.getPessoa().getInTipoPessoa());
        return dados;
    }

    // ServiceExceptionLoggingAspect

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logErro(String tabela, String acao, String descricao, Exception ex, Object... parametros) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("erro", ex.getMessage());
        if (parametros != null && parametros.length > 0) {
            dados.put("parametros", Arrays.toString(parametros));
        }
        registrarLogComum(tabela, acao, descricao, false, dados);
    }

    // ========== Utilitários privados ==========

    private Integer obterIdUsuarioLogado() {
        var usuarioOpt = usuarioLogadoService.getViewUserLogin();
        return usuarioOpt.map(ViewUserLogin::getIdPessoa).orElse(null);
    }

    private Map<String, Object> obterDetalhesRequisicao() {
        Map<String, Object> detalhes = new HashMap<>();
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                detalhes.put("ip", obterIpCliente(request));
                detalhes.put("url", request.getRequestURL().toString());
                detalhes.put("httpMethod", request.getMethod());
                detalhes.put("userAgent", request.getHeader("User-Agent"));
            }
        } catch (Exception e) {
            log.debug("Não foi possível obter detalhes da requisição para o log");
        }
        return detalhes;
    }

    private String obterIpCliente(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}