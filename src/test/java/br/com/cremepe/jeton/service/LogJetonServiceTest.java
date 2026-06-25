package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.annotation.AuditoriaUser;
import br.com.cremepe.jeton.domain.*;
import br.com.cremepe.jeton.repository.LogJetonRepository;
import br.com.cremepe.jeton.repository.UsuarioRepository;
import br.com.cremepe.jeton.util.JsonConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do serviço de Logs (LogJetonService)")
class LogJetonServiceTest {

    @Mock
    private LogJetonRepository logRepositoryMock;

    @Mock
    private UsuarioRepository usuarioRepositoryMock;

    @Mock
    private UsuarioLogadoService usuarioLogadoServiceMock;

    @Mock
    private JsonConverter jsonConverterMock;

    @Mock
    private AsyncLogWriter asyncLogWriterMock;

    @InjectMocks
    private LogJetonService service;

    private static final Integer USUARIO_ID = 1;
    private static final String NOME_USUARIO = "Usuário Teste";

    @BeforeEach
    void setUp() {
        // ✅ Todos os stubs usam lenient() para evitar UnnecessaryStubbingException
        AuditoriaUser auditoriaUser = new AuditoriaUser(USUARIO_ID, NOME_USUARIO);
        lenient().when(usuarioLogadoServiceMock.getUsuarioLogado()).thenReturn(auditoriaUser);
        lenient().when(usuarioLogadoServiceMock.getViewUserLogin())
                .thenReturn(Optional.of(criarViewUserLogin(USUARIO_ID, NOME_USUARIO)));

        Usuario usuarioMock = mock(Usuario.class);
        Pessoa pessoaMock = mock(Pessoa.class);
        lenient().when(pessoaMock.getNome()).thenReturn(NOME_USUARIO);
        lenient().when(usuarioMock.getPessoa()).thenReturn(pessoaMock);
        lenient().when(usuarioRepositoryMock.getReferenceById(USUARIO_ID)).thenReturn(usuarioMock);

        // ✅ Mock do JsonConverter: inclui erro se presente
        lenient().when(jsonConverterMock.toJson(anyMap())).thenAnswer(invocation -> {
            Map<String, Object> map = invocation.getArgument(0);
            String acao = (String) map.get("acao");
            String descricao = (String) map.get("descricao");
            String erro = (String) map.get("erro");
            if (erro == null) {
                erro = "";
            }
            return "{\"acao\":\"" + acao + "\",\"descricao\":\"" + descricao + "\",\"erro\":\"" + erro
                    + "\",\"sucesso\":true}";
        });
    }

    private ViewUserLogin criarViewUserLogin(Integer id, String nome) {
        ViewUserLogin view = new ViewUserLogin();
        view.setIdPessoa(id);
        view.setNome(nome);
        return view;
    }

    // ========== TESTES DE CONSULTA ==========

    @Test
    @DisplayName("deve listar logs com filtros")
    void deveListarLogsComFiltros() {
        // Dado
        String nomeTabela = "usuario";
        LocalDateTime inicio = LocalDateTime.now().minusDays(1);
        LocalDateTime fim = LocalDateTime.now();
        String termo = "teste";
        Pageable pageable = Pageable.unpaged();

        Page<LogJeton> paginaEsperada = new PageImpl<>(List.of(new LogJeton()));
        when(logRepositoryMock.findAllByFilters(nomeTabela, inicio, fim, termo, pageable))
                .thenReturn(paginaEsperada);

        // Quando
        Page<LogJeton> resultado = service.listarComFiltros(nomeTabela, inicio, fim, termo, pageable);

        // Então
        assertThat(resultado).isEqualTo(paginaEsperada);
        verify(logRepositoryMock).findAllByFilters(nomeTabela, inicio, fim, termo, pageable);
    }

    // ========== TESTES DE LOGIN/LOGOUT ==========

    @Test
    @DisplayName("deve logar login com sucesso")
    void deveLogarLogin() {
        service.logLogin(USUARIO_ID, NOME_USUARIO);
        verificarChamadaLog("login", "LOGIN", "Login bem-sucedido");
    }

    @Test
    @DisplayName("deve logar logout com sucesso")
    void deveLogarLogout() {
        service.logLogout(USUARIO_ID, NOME_USUARIO);
        verificarChamadaLog("login", "LOGOUT", "Logout do sistema");
    }

    // ========== TESTES DE LOG DE ATIVIDADE ==========

    @Test
    @DisplayName("deve logar criação de atividade")
    void deveLogarAtividadeCriada() {
        AtividadeConselhal atividade = criarAtividade(1);
        service.logAtividadeCriada(atividade);
        verificarChamadaLog("atividade_conselhal", "CRIAR", "Criação de nova atividade");
    }

    @Test
    @DisplayName("deve logar atualização de atividade")
    void deveLogarAtividadeAtualizada() {
        AtividadeConselhal antiga = criarAtividade(1);
        AtividadeConselhal nova = criarAtividade(2);
        service.logAtividadeAtualizada(antiga, nova);
        verificarChamadaLog("atividade_conselhal", "ATUALIZAR", "Atualização de atividade existente");
    }

    @Test
    @DisplayName("deve logar validação de atividade")
    void deveLogarAtividadeValidada() {
        service.logAtividadeValidada(10);
        verificarChamadaLog("atividade_conselhal", "VALIDAR", "Validação de atividade pendente");
    }

    @Test
    @DisplayName("deve logar desvalidação de atividade")
    void deveLogarAtividadeDesvalidada() {
        service.logAtividadeDesvalidada(10);
        verificarChamadaLog("atividade_conselhal", "DESVALIDAR", "Desvalidação de atividade");
    }

    @Test
    @DisplayName("deve logar exclusão de atividade")
    void deveLogarAtividadeExcluida() {
        AtividadeConselhal atividade = criarAtividade(1);
        service.logAtividadeExcluida(atividade);
        verificarChamadaLog("atividade_conselhal", "EXCLUIR", "Exclusão de atividade");
    }

    // ========== TESTES DE LOG DE LOTE ==========

    @Test
    @DisplayName("deve logar criação de lote")
    void deveLogarLoteCriado() {
        service.logLoteCriado(100, 1, 5, List.of(10, 20), LocalDateTime.now());
        verificarChamadaLog("atividade_conselhal", "CRIAR_LOTE",
                "Criação de múltiplas atividades com mesmo comprovante");
    }

    @Test
    @DisplayName("deve logar atualização de lote")
    void deveLogarLoteAtualizado() {
        service.logLoteAtualizado(100, List.of(10, 20), List.of(30, 40), 1, 5, LocalDateTime.now());
        verificarChamadaLog("atividade_conselhal", "EDITAR_LOTE",
                "Edição em massa de atividades que compartilham o mesmo comprovante");
    }

    // ========== TESTES DE LOG DE COMPROVANTE ==========

    @Test
    @DisplayName("deve logar criação de comprovante")
    void deveLogarComprovanteCriado() {
        Comprovante comprovante = criarComprovante(1);
        service.logComprovanteCriado(comprovante);
        verificarChamadaLog("comprovante", "CRIAR", "Criação de novo comprovante");
    }

    @Test
    @DisplayName("deve logar exclusão de comprovante")
    void deveLogarComprovanteExcluido() {
        Comprovante comprovante = criarComprovante(1);
        service.logComprovanteExcluido(comprovante);
        verificarChamadaLog("comprovante", "EXCLUIR", "Exclusão de comprovante");
    }

    // ========== TESTES DE LOG DE CONSELHEIRO ==========

    @Test
    @DisplayName("deve logar criação de conselheiro")
    void deveLogarConselheiroCriado() {
        Conselheiro conselheiro = criarConselheiro(1);
        service.logConselheiroCriado(conselheiro);
        verificarChamadaLog("conselheiro", "CRIAR", "Criação de novo conselheiro");
    }

    @Test
    @DisplayName("deve logar atualização de conselheiro")
    void deveLogarConselheiroAtualizado() {
        Conselheiro antigo = criarConselheiro(1);
        Conselheiro novo = criarConselheiro(2);
        service.logConselheiroAtualizado(antigo, novo);
        verificarChamadaLog("conselheiro", "ATUALIZAR", "Atualização de conselheiro existente");
    }

    @Test
    @DisplayName("deve logar exclusão de conselheiro")
    void deveLogarConselheiroExcluido() {
        Conselheiro conselheiro = criarConselheiro(1);
        service.logConselheiroExcluido(conselheiro);
        verificarChamadaLog("conselheiro", "EXCLUIR", "Exclusão física de conselheiro");
    }

    // ========== TESTES DE LOG DE FILE STORAGE ==========

    @Test
    @DisplayName("deve logar upload de arquivo")
    void deveLogarUploadArquivo() {
        service.logUploadArquivo("original.pdf", "uuid.pdf", 1024, 2026, 6, "application/pdf");
        verificarChamadaLog("file_storage", "UPLOAD", "Upload de arquivo para o servidor FTP");
    }

    @Test
    @DisplayName("deve logar exclusão de arquivo")
    void deveLogarExcluirArquivo() {
        service.logExcluirArquivo("arquivo.pdf", 2026, 6);
        verificarChamadaLog("file_storage", "EXCLUIR", "Remoção de arquivo do servidor FTP");
    }

    // ========== TESTES DE LOG DE VÍNCULO ==========

    @Test
    @DisplayName("deve logar criação de vínculo")
    void deveLogarVinculoCriado() {
        GestaoConselheiro vinculo = criarVinculo(1, 10);
        service.logVinculoCriado(vinculo);
        verificarChamadaLog("gestao_conselheiro", "CRIAR", "Criação de vínculo entre conselheiro e gestão");
    }

    @Test
    @DisplayName("deve logar atualização de vínculo")
    void deveLogarVinculoAtualizado() {
        GestaoConselheiro antigo = criarVinculo(1, 10);
        GestaoConselheiro novo = criarVinculo(1, 20);
        service.logVinculoAtualizado(antigo, novo);
        verificarChamadaLog("gestao_conselheiro", "ATUALIZAR",
                "Atualização de vínculo entre conselheiro e gestão (apenas situação)");
    }

    @Test
    @DisplayName("deve logar ativação de vínculo")
    void deveLogarVinculoAtivado() {
        service.logVinculoAtivado(1, 10);
        verificarChamadaLog("gestao_conselheiro", "ATIVAR", "Ativação de vínculo entre conselheiro e gestão");
    }

    @Test
    @DisplayName("deve logar inativação de vínculo")
    void deveLogarVinculoInativado() {
        service.logVinculoInativado(1, 10);
        verificarChamadaLog("gestao_conselheiro", "INATIVAR", "Inativação de vínculo entre conselheiro e gestão");
    }

    @Test
    @DisplayName("deve logar exclusão de vínculo")
    void deveLogarVinculoExcluido() {
        GestaoConselheiro vinculo = criarVinculo(1, 10);
        service.logVinculoExcluido(vinculo);
        verificarChamadaLog("gestao_conselheiro", "EXCLUIR", "Exclusão de vínculo");
    }

    @Test
    @DisplayName("deve logar atualização em massa de vínculos")
    void deveLogarVinculosAtualizadosEmMassa() {
        service.logVinculosAtualizadosEmMassa(1, List.of(10, 20), List.of(30, 40));
        verificarChamadaLog("gestao_conselheiro", "ATUALIZAR_EM_MASSA",
                "Atualização em massa de vínculos de conselheiros para uma gestão");
    }

    // ========== TESTES DE LOG DE GESTÃO ==========

    @Test
    @DisplayName("deve logar criação de gestão")
    void deveLogarGestaoCriada() {
        Gestao gestao = criarGestao(1);
        service.logGestaoCriada(gestao);
        verificarChamadaLog("gestao", "CRIAR", "Criação de nova gestão");
    }

    @Test
    @DisplayName("deve logar atualização de gestão")
    void deveLogarGestaoAtualizada() {
        Gestao antiga = criarGestao(1);
        Gestao nova = criarGestao(2);
        service.logGestaoAtualizada(antiga, nova);
        verificarChamadaLog("gestao", "ATUALIZAR", "Edição de gestão");
    }

    @Test
    @DisplayName("deve logar exclusão de gestão")
    void deveLogarGestaoExcluida() {
        Gestao gestao = criarGestao(1);
        service.logGestaoExcluida(gestao);
        verificarChamadaLog("gestao", "EXCLUIR", "Exclusão de gestão");
    }

    // ========== TESTES DE LOG DE JETON ==========

    @Test
    @DisplayName("deve logar processamento de folha")
    void deveLogarFolhaProcessada() {
        Gestao gestao = criarGestao(1);
        service.logFolhaProcessada(gestao, 6, 2026, 5, 10, BigDecimal.valueOf(1000));
        verificarChamadaLog("jeton", "PROCESSAR_FOLHA", "Processamento mensal de folha de jetons");
    }

    @Test
    @DisplayName("deve logar estorno de jeton")
    void deveLogarJetonEstornado() {
        service.logJetonEstornado(10, "Conselheiro", "Gestão", 6, 2026);
        verificarChamadaLog("jeton", "ESTORNAR_PONTUAL", "Estorno pontual de um Jeton (exclusão lógica)");
    }

    @Test
    @DisplayName("deve logar homologação de folha")
    void deveLogarFolhaHomologada() {
        Gestao gestao = criarGestao(1);
        service.logFolhaHomologada(gestao, 6, 2026, 8, 3);
        verificarChamadaLog("jeton", "HOMOLOGAR_FOLHA", "Homologação e fechamento definitivo da folha mensal");
    }

    @Test
    @DisplayName("deve logar exclusão de jeton")
    void deveLogarJetonExcluido() {
        service.logJetonExcluido(10, "Conselheiro", "Gestão", 6, 2026);
        verificarChamadaLog("jeton", "EXCLUIR", "Exclusão física de um registro de Jeton");
    }

    // ========== TESTES DE LOG DE NÍVEL DE ACESSO ==========

    @Test
    @DisplayName("deve logar criação de nível de acesso")
    void deveLogarNivelAcessoCriado() {
        NivelAcesso nivel = criarNivel("A");
        service.logNivelAcessoCriado(nivel);
        verificarChamadaLog("nivel_acesso", "CRIAR", "Criação de novo nível de acesso");
    }

    @Test
    @DisplayName("deve logar atualização de nível de acesso")
    void deveLogarNivelAcessoAtualizado() {
        NivelAcesso antigo = criarNivel("A");
        NivelAcesso novo = criarNivel("B");
        service.logNivelAcessoAtualizado(antigo, novo);
        verificarChamadaLog("nivel_acesso", "ATUALIZAR", "Atualização de nível de acesso existente");
    }

    @Test
    @DisplayName("deve logar exclusão de nível de acesso")
    void deveLogarNivelAcessoExcluido() {
        service.logNivelAcessoExcluido("A", "Atividades");
        verificarChamadaLog("nivel_acesso", "EXCLUIR",
                "Exclusão de nível de acesso (apenas se não houver usuários vinculados)");
    }

    // ========== TESTES DE LOG DE PARÂMETROS ==========

    @Test
    @DisplayName("deve logar alternância de bloqueio")
    void deveLogarBloqueioAlternado() {
        service.logBloqueioAlternado("N", "S");
        verificarChamadaLog("parametros", "ALTERAR_BLOQUEIO", "Alterna o bloqueio do sistema");
    }

    // ========== TESTES DE LOG DE PERMISSÃO ==========

    @Test
    @DisplayName("deve logar concessão de permissão")
    void deveLogarPermissaoConcedida() {
        Usuario usuario = criarUsuario(1);
        NivelAcesso nivel = criarNivel("A");
        service.logPermissaoConcedida(usuario, nivel);
        verificarChamadaLog("usuario_acesso", "CONCEDER", "Concessão de permissão (nível de acesso) a um usuário");
    }

    @Test
    @DisplayName("deve logar revogação de permissão")
    void deveLogarPermissaoRevogada() {
        Usuario usuario = criarUsuario(1);
        NivelAcesso nivel = criarNivel("A");
        service.logPermissaoRevogada(usuario, nivel);
        verificarChamadaLog("usuario_acesso", "REVOGAR", "Revogação de permissão (nível de acesso) de um usuário");
    }

    @Test
    @DisplayName("deve logar revogação de todas as permissões")
    void deveLogarTodasPermissoesRevogadas() {
        Usuario usuario = criarUsuario(1);
        service.logTodasPermissoesRevogadas(usuario);
        verificarChamadaLog("usuario_acesso", "REVOGAR_TODAS", "Revogação de todas as permissões de um usuário");
    }

    // ========== TESTES DE LOG DE PONTOS SALDO ==========

    @Test
    @DisplayName("deve logar criação de pontos saldo")
    void deveLogarPontosSaldoCriado() {
        PontosSaldo pontos = criarPontosSaldo(1);
        service.logPontosSaldoCriado(pontos);
        verificarChamadaLog("pontos_saldo", "CRIAR", "Criação de registro de saldo de pontos");
    }

    @Test
    @DisplayName("deve logar atualização de pontos saldo")
    void deveLogarPontosSaldoAtualizado() {
        PontosSaldo antigo = criarPontosSaldo(1);
        PontosSaldo novo = criarPontosSaldo(2);
        service.logPontosSaldoAtualizado(antigo, novo);
        verificarChamadaLog("pontos_saldo", "ATUALIZAR", "Atualização de registro de saldo de pontos");
    }

    @Test
    @DisplayName("deve logar exclusão de pontos saldo")
    void deveLogarPontosSaldoExcluido() {
        PontosSaldo pontos = criarPontosSaldo(1);
        service.logPontosSaldoExcluido(pontos);
        verificarChamadaLog("pontos_saldo", "EXCLUIR",
                "Exclusão de registro de saldo de pontos (apenas se não utilizado)");
    }

    // ========== TESTES DE LOG DE PORTARIA ==========

    @Test
    @DisplayName("deve logar criação de portaria")
    void deveLogarPortariaCriada() {
        Portaria portaria = criarPortaria(1);
        service.logPortariaCriada(portaria);
        verificarChamadaLog("portaria", "CRIAR", "Criação de nova portaria");
    }

    @Test
    @DisplayName("deve logar atualização de portaria")
    void deveLogarPortariaAtualizada() {
        Portaria antiga = criarPortaria(1);
        Portaria nova = criarPortaria(2);
        service.logPortariaAtualizada(antiga, nova);
        verificarChamadaLog("portaria", "ATUALIZAR", "Atualização de portaria existente");
    }

    @Test
    @DisplayName("deve logar revogação de portaria")
    void deveLogarPortariaRevogada() {
        Portaria portaria = criarPortaria(1);
        service.logPortariaRevogada(portaria);
        verificarChamadaLog("portaria", "REVOGAR", "Revogação de portaria");
    }

    @Test
    @DisplayName("deve logar restauração de portaria")
    void deveLogarPortariaRestaurada() {
        Portaria portaria = criarPortaria(1);
        service.logPortariaRestaurada(portaria);
        verificarChamadaLog("portaria", "RESTAURAR", "Restauração de portaria revogada (volta a ficar em vigor)");
    }

    @Test
    @DisplayName("deve logar exclusão de portaria")
    void deveLogarPortariaExcluida() {
        Portaria portaria = criarPortaria(1);
        service.logPortariaExcluida(portaria);
        verificarChamadaLog("portaria", "EXCLUIR", "Exclusão de portaria");
    }

    // ========== TESTES DE LOG DE REGRA ==========

    @Test
    @DisplayName("deve logar criação de regra")
    void deveLogarRegraCriada() {
        Regras regra = criarRegra(1);
        service.logRegraCriada(regra);
        verificarChamadaLog("regras", "CRIAR", "Criação de nova regra de pontuação");
    }

    @Test
    @DisplayName("deve logar atualização de regra")
    void deveLogarRegraAtualizada() {
        Regras antiga = criarRegra(1);
        Regras nova = criarRegra(2);
        service.logRegraAtualizada(antiga, nova);
        verificarChamadaLog("regras", "ATUALIZAR", "Atualização de regra de pontuação existente");
    }

    @Test
    @DisplayName("deve logar revogação de regra")
    void deveLogarRegraRevogada() {
        Regras regra = criarRegra(1);
        service.logRegraRevogada(regra);
        verificarChamadaLog("regras", "REVOGAR", "Revogação de regra de pontuação");
    }

    @Test
    @DisplayName("deve logar restauração de regra")
    void deveLogarRegraRestaurada() {
        Regras regra = criarRegra(1);
        service.logRegraRestaurada(regra);
        verificarChamadaLog("regras", "RESTAURAR", "Restauração de regra revogada (volta a ficar em vigor)");
    }

    @Test
    @DisplayName("deve logar exclusão de regra")
    void deveLogarRegraExcluida() {
        Regras regra = criarRegra(1);
        service.logRegraExcluida(regra);
        verificarChamadaLog("regras", "EXCLUIR", "Exclusão de regra");
    }

    // ========== TESTES DE LOG DE REGRA CONJUNTA ==========

    @Test
    @DisplayName("deve logar criação de regra conjunta")
    void deveLogarRegraConjuntaCriada() {
        RegrasConjuntas regra = criarRegraConjunta(1);
        service.logRegraConjuntaCriada(regra);
        verificarChamadaLog("regras_conjuntas", "CRIAR", "Criação de novo agrupamento de regras (regras conjuntas)");
    }

    @Test
    @DisplayName("deve logar atualização de regra conjunta")
    void deveLogarRegraConjuntaAtualizada() {
        RegrasConjuntas antiga = criarRegraConjunta(1);
        RegrasConjuntas nova = criarRegraConjunta(2);
        service.logRegraConjuntaAtualizada(antiga, nova);
        verificarChamadaLog("regras_conjuntas", "ATUALIZAR", "Atualização de agrupamento de regras existente");
    }

    @Test
    @DisplayName("deve logar exclusão de regra conjunta")
    void deveLogarRegraConjuntaExcluida() {
        RegrasConjuntas regra = criarRegraConjunta(1);
        service.logRegraConjuntaExcluida(regra, "Regra A, Regra B");
        verificarChamadaLog("regras_conjuntas", "EXCLUIR", "Exclusão de agrupamento de regras (e das associações)");
    }

    // ========== TESTES DE LOG DE RELATÓRIO ==========

    @Test
    @DisplayName("deve logar geração de relatório")
    void deveLogarRelatorioGerado() {
        service.logRelatorioGerado(1, 10, 5, LocalDate.now().minusDays(1), LocalDate.now(), 100);
        verificarChamadaLog("relatorio", "GERAR_RELATORIO_ATIVIDADES", "Geração de relatório agrupado de atividades");
    }

    // ========== TESTES DE LOG DE RESOLUÇÃO ==========

    @Test
    @DisplayName("deve logar criação de resolução")
    void deveLogarResolucaoCriada() {
        Resolucao resolucao = criarResolucao(1);
        service.logResolucaoCriada(resolucao);
        verificarChamadaLog("resolucao", "CRIAR", "Criação de nova resolução");
    }

    @Test
    @DisplayName("deve logar atualização de resolução")
    void deveLogarResolucaoAtualizada() {
        Resolucao antiga = criarResolucao(1);
        Resolucao nova = criarResolucao(2);
        service.logResolucaoAtualizada(antiga, nova);
        verificarChamadaLog("resolucao", "ATUALIZAR", "Atualização de resolução existente");
    }

    @Test
    @DisplayName("deve logar revogação de resolução")
    void deveLogarResolucaoRevogada() {
        Resolucao resolucao = criarResolucao(1);
        service.logResolucaoRevogada(resolucao);
        verificarChamadaLog("resolucao", "REVOGAR", "Revogação de resolução");
    }

    @Test
    @DisplayName("deve logar restauração de resolução")
    void deveLogarResolucaoRestaurada() {
        Resolucao resolucao = criarResolucao(1);
        service.logResolucaoRestaurada(resolucao);
        verificarChamadaLog("resolucao", "RESTAURAR", "Restauração de resolução revogada (volta a ficar em vigor)");
    }

    @Test
    @DisplayName("deve logar exclusão de resolução")
    void deveLogarResolucaoExcluida() {
        Resolucao resolucao = criarResolucao(1);
        service.logResolucaoExcluida(resolucao);
        verificarChamadaLog("resolucao", "EXCLUIR", "Exclusão de resolução");
    }

    // ========== TESTES DE LOG DE TIPO DE ANEXO ==========

    @Test
    @DisplayName("deve logar criação de tipo de anexo")
    void deveLogarTipoAnexoCriado() {
        TipoAnexo tipo = criarTipoAnexo(1);
        service.logTipoAnexoCriado(tipo);
        verificarChamadaLog("tipo_anexo", "CRIAR", "Criação de novo tipo de anexo");
    }

    @Test
    @DisplayName("deve logar atualização de tipo de anexo")
    void deveLogarTipoAnexoAtualizado() {
        TipoAnexo antigo = criarTipoAnexo(1);
        TipoAnexo novo = criarTipoAnexo(2);
        service.logTipoAnexoAtualizado(antigo, novo);
        verificarChamadaLog("tipo_anexo", "ATUALIZAR", "Atualização de tipo de anexo existente");
    }

    @Test
    @DisplayName("deve logar exclusão de tipo de anexo")
    void deveLogarTipoAnexoExcluido() {
        TipoAnexo tipo = criarTipoAnexo(1);
        service.logTipoAnexoExcluido(tipo);
        verificarChamadaLog("tipo_anexo", "EXCLUIR",
                "Exclusão de tipo de anexo (apenas se não houver comprovantes vinculados)");
    }

    // ========== TESTES DE LOG DE USUÁRIO ==========

    @Test
    @DisplayName("deve logar criação de usuário")
    void deveLogarUsuarioCriado() {
        Usuario usuario = criarUsuario(1);
        service.logUsuarioCriado(usuario);
        verificarChamadaLog("usuario", "CRIAR", "Criação de novo usuário");
    }

    @Test
    @DisplayName("deve logar atualização de usuário")
    void deveLogarUsuarioAtualizado() {
        Usuario antigo = criarUsuario(1);
        Usuario novo = criarUsuario(2);
        service.logUsuarioAtualizado(antigo, novo);
        verificarChamadaLog("usuario", "ATUALIZAR", "Atualização de usuário existente");
    }

    @Test
    @DisplayName("deve logar exclusão de usuário")
    void deveLogarUsuarioExcluido() {
        Usuario usuario = criarUsuario(1);
        service.logUsuarioExcluido(usuario);
        verificarChamadaLog("usuario", "EXCLUIR", "Exclusão de usuário");
    }

    // ========== MÉTODO AUXILIAR ==========

    private void verificarChamadaLog(String tabelaEsperada, String acaoEsperada, String descricaoEsperada) {
        ArgumentCaptor<String> tabelaCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> usuarioCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<String> textoCaptor = ArgumentCaptor.forClass(String.class);

        verify(asyncLogWriterMock).writeLog(tabelaCaptor.capture(), usuarioCaptor.capture(), textoCaptor.capture());

        assertThat(tabelaCaptor.getValue()).isEqualTo(tabelaEsperada);
        assertThat(usuarioCaptor.getValue()).isEqualTo(USUARIO_ID);
        assertThat(textoCaptor.getValue()).contains(acaoEsperada);
        assertThat(textoCaptor.getValue()).contains(descricaoEsperada);
        assertThat(textoCaptor.getValue()).contains("\"sucesso\":true");
    }

    // ========== HELPERS PARA CRIAÇÃO DE OBJETOS MOCK ==========

    private AtividadeConselhal criarAtividade(Integer id) {
        AtividadeConselhal a = new AtividadeConselhal();
        a.setIdAtividade(id);
        a.setGestao(criarGestao(1));
        a.setConselheiro(criarConselheiro(1));
        a.setRegra(criarRegra(1));
        a.setComprovante(criarComprovante(1));
        a.setDataHoraAtividade(LocalDateTime.now());
        a.setDataHoraRegistro(LocalDateTime.now());
        a.setInTurno(AtividadeConselhal.TURNO_MANHA);
        a.setInSituacao(AtividadeConselhal.SITUACAO_PENDENTE);
        return a;
    }

    private Comprovante criarComprovante(Integer id) {
        Comprovante c = new Comprovante();
        c.setIdComprovante(id);
        c.setTipoAnexo(criarTipoAnexo(1));
        c.setNomeComprovante("Comprovante " + id);
        c.setNomeArquivo("arquivo" + id + ".pdf");
        c.setContentType("application/pdf");
        c.setMes(6);
        c.setAno(2026);
        return c;
    }

    private Conselheiro criarConselheiro(Integer id) {
        Conselheiro c = new Conselheiro();
        c.setIdPessoa(id);
        Pessoa p = new Pessoa();
        p.setIdPessoa(id);
        p.setNome("Conselheiro " + id);
        p.setCpf("123456789" + id);
        p.setEmail("conselheiro" + id + "@email.com");
        c.setPessoa(p);
        c.setCrm(10000 + id);
        c.setInSituacao(Conselheiro.SITUACAO_ATIVO);
        return c;
    }

    private Gestao criarGestao(Integer id) {
        Gestao g = new Gestao();
        g.setIdGestao(id);
        g.setNomeGestao("Gestão " + id);
        g.setDtInicio(LocalDate.of(2026, 1, 1));
        g.setDtFim(LocalDate.of(2026, 12, 31));
        return g;
    }

    private GestaoConselheiro criarVinculo(Integer idGestao, Integer idPessoa) {
        GestaoConselheiro v = new GestaoConselheiro();
        v.setId(new GestaoConselheiroId(idGestao, idPessoa));
        v.setGestao(criarGestao(idGestao));
        v.setConselheiro(criarConselheiro(idPessoa));
        v.setInSituacao(GestaoConselheiro.SITUACAO_ATIVO);
        return v;
    }

    private NivelAcesso criarNivel(String id) {
        NivelAcesso n = new NivelAcesso();
        n.setIdNivel(id);
        n.setNomeNivel("Nível " + id);
        return n;
    }

    private PontosSaldo criarPontosSaldo(Integer id) {
        PontosSaldo ps = new PontosSaldo();
        ps.setIdPontosSaldo(id);
        ps.setConselheiro(criarConselheiro(1));
        ps.setGestao(criarGestao(1));
        ps.setResolucao(criarResolucao(1));
        ps.setDataHora(LocalDateTime.now());
        ps.setPontosTrabalhados(10);
        ps.setPontosUtilizados(0);
        ps.setPontosSobrando(10);
        ps.setInSituacao(PontosSaldo.SITUACAO_ATIVO);
        return ps;
    }

    private Portaria criarPortaria(Integer id) {
        Portaria p = new Portaria();
        p.setIdPortaria(id);
        p.setNumero(100 + id);
        p.setAno(2026);
        p.setDtInicioVigencia(LocalDate.of(2026, 1, 1));
        p.setDtFimVigencia(LocalDate.of(2026, 12, 31));
        p.setInRevogado(Portaria.REVOGADO_NAO);
        return p;
    }

    private Regras criarRegra(Integer id) {
        Regras r = new Regras();
        r.setIdRegra(id);
        r.setNomeRegra("Regra " + id);
        r.setDescricao("Descrição da regra " + id);
        r.setPontos(3);
        r.setInRevogado(Regras.REVOGADO_NAO);
        r.setInJudicante(Regras.JUDICANTE_NAO);
        r.setResolucao(criarResolucao(1));
        return r;
    }

    private RegrasConjuntas criarRegraConjunta(Integer id) {
        RegrasConjuntas rc = new RegrasConjuntas();
        rc.setIdRegraConjunta(id);
        rc.setNomeRegra("Agrupamento " + id);
        rc.setInTipoLimite(RegrasConjuntas.TIPO_LIMITE_DIARIO);
        rc.setPontosLimite(10);
        rc.setRegrasAgrupadas(List.of(criarRegra(1), criarRegra(2)));
        return rc;
    }

    private Resolucao criarResolucao(Integer id) {
        Resolucao r = new Resolucao();
        r.setIdResolucao(id);
        r.setNumero(1);
        r.setAno(2026);
        r.setDtInicioVigencia(LocalDate.of(2026, 1, 1));
        r.setDtFimVigencia(LocalDate.of(2026, 12, 31));
        r.setEmenta("Ementa da resolução " + id);
        r.setPontosPorJeton(3);
        r.setMaxJetonsDia(3);
        r.setMaxJetonsPeriodo(1);
        r.setMaxJetonsMes(22);
        r.setValorJeton(BigDecimal.valueOf(100.00));
        r.setInRevogado(Resolucao.REVOGADO_NAO);
        return r;
    }

    private TipoAnexo criarTipoAnexo(Integer id) {
        TipoAnexo t = new TipoAnexo();
        t.setIdTipo(id);
        t.setNome("Tipo " + id);
        t.setExigePublicacao(TipoAnexo.EXIGE_PUBLICACAO_NAO);
        return t;
    }

    private Usuario criarUsuario(Integer id) {
        Usuario u = new Usuario();
        u.setIdUsuarioPessoa(id);
        Pessoa p = new Pessoa();
        p.setIdPessoa(id);
        p.setNome("Usuário " + id);
        p.setCpf("123456789" + id);
        p.setEmail("usuario" + id + "@email.com");
        u.setPessoa(p);
        u.setInSituacao(Usuario.SITUACAO_ATIVO);
        return u;
    }
}