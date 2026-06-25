package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.domain.*;
import br.com.cremepe.jeton.dto.LoteAtividadeDTO;
import br.com.cremepe.jeton.repository.AtividadeConselhalRepository;
import br.com.cremepe.jeton.repository.ComprovanteRepository;
import br.com.cremepe.jeton.repository.GestaoConselheiroRepository;
import br.com.cremepe.jeton.repository.GestaoRepository;
import br.com.cremepe.jeton.util.AtividadeValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do serviço de Atividade em Lote")
class AtividadeLoteServiceTest {

    @Mock
    private AtividadeConselhalRepository atividadeRepositoryMock;

    @Mock
    private ComprovanteRepository comprovanteRepositoryMock;

    @Mock
    private GestaoRepository gestaoRepositoryMock;

    @Mock
    private GestaoConselheiroRepository gestaoConselheiroRepositoryMock;

    @Mock
    private ComprovanteService comprovanteServiceMock;

    @Mock
    private ConselheiroService conselheiroServiceMock;

    @Mock
    private RegrasService regrasServiceMock;

    @Mock
    private LogJetonService logJetonServiceMock;

    @Mock
    private AtividadeValidator atividadeValidatorMock;

    @InjectMocks
    private AtividadeLoteService service;

    // ========== HELPERS ==========

    private Gestao criarGestao(Integer id, LocalDate inicio, LocalDate fim) {
        Gestao g = new Gestao();
        g.setIdGestao(id);
        g.setDtInicio(inicio);
        g.setDtFim(fim);
        g.setNomeGestao("Gestão Teste");
        return g;
    }

    private Conselheiro criarConselheiro(Integer id, String nome) {
        Pessoa p = new Pessoa();
        p.setIdPessoa(id);
        p.setNome(nome);
        Conselheiro c = new Conselheiro();
        c.setIdPessoa(id);
        c.setPessoa(p);
        c.setInSituacao(Conselheiro.SITUACAO_ATIVO);
        return c;
    }

    private Regras criarRegra(Integer id) {
        Regras r = new Regras();
        r.setIdRegra(id);
        r.setNomeRegra("Regra Teste");
        r.setPontos(2);
        return r;
    }

    private Comprovante criarComprovante(Integer id) {
        Comprovante c = new Comprovante();
        c.setIdComprovante(id);
        c.setNomeComprovante("Comprovante Lote");
        c.setNomeArquivo("arquivo.pdf");
        c.setMes(6);
        c.setAno(2026);
        return c;
    }

    private AtividadeConselhal criarAtividade(Integer id, Gestao gestao, Conselheiro conselheiro, Regras regra,
            Comprovante comprovante, String situacao) {
        AtividadeConselhal a = new AtividadeConselhal();
        a.setIdAtividade(id);
        a.setGestao(gestao);
        a.setConselheiro(conselheiro);
        a.setRegra(regra);
        a.setComprovante(comprovante);
        a.setQtdAtividade(1);
        a.setDataHoraAtividade(LocalDateTime.now());
        a.setDataHoraRegistro(LocalDateTime.now());
        a.setInTurno(AtividadeConselhal.TURNO_MANHA);
        a.setInSituacao(situacao);
        a.setInComputada(AtividadeConselhal.COMPUTADA_NAO);
        return a;
    }

    private LoteAtividadeDTO criarLoteDto(Integer idGestao, Integer idRegra, LocalDateTime dataHora,
            List<Integer> idsConselheiros, MultipartFile file, Integer idTipoAnexo,
            String nomeComprovante) {
        LoteAtividadeDTO dto = new LoteAtividadeDTO();
        dto.setIdGestao(idGestao);
        dto.setDataHoraAtividade(dataHora);
        dto.setIdRegra(idRegra);
        dto.setIdsConselheiros(idsConselheiros);
        dto.setFile(file);
        dto.setIdTipoAnexo(idTipoAnexo);
        dto.setNomeComprovanteUsuario(nomeComprovante);
        return dto;
    }

    // ========== TESTES DE CRIAÇÃO ==========

    @Test
    @DisplayName("deve criar lote de atividades sem comprovante com sucesso")
    void deveCriarLoteSemComprovante() {
        Integer idGestao = 1;
        Integer idRegra = 10;
        LocalDateTime dataHora = LocalDateTime.now();
        List<Integer> idsConselheiros = List.of(100, 101, 102);

        Gestao gestao = criarGestao(idGestao, LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1));
        Regras regra = criarRegra(idRegra);

        Conselheiro conselheiro1 = criarConselheiro(100, "Médico A");
        Conselheiro conselheiro2 = criarConselheiro(101, "Médico B");
        Conselheiro conselheiro3 = criarConselheiro(102, "Médico C");

        LoteAtividadeDTO dto = criarLoteDto(idGestao, idRegra, dataHora, idsConselheiros, null, null, null);

        doNothing().when(atividadeValidatorMock).validarDataHoraObrigatoria(dataHora);
        when(atividadeValidatorMock.validarGestaoExistente(idGestao)).thenReturn(gestao);
        doNothing().when(atividadeValidatorMock).validarDataDentroDoMandato(dataHora.toLocalDate(), idGestao);
        when(regrasServiceMock.buscarOuFalhar(idRegra)).thenReturn(regra);

        when(conselheiroServiceMock.buscarPorId(100)).thenReturn(Optional.of(conselheiro1));
        when(conselheiroServiceMock.buscarPorId(101)).thenReturn(Optional.of(conselheiro2));
        when(conselheiroServiceMock.buscarPorId(102)).thenReturn(Optional.of(conselheiro3));

        doNothing().when(atividadeValidatorMock).validarVinculoConselheiroGestao(anyInt(), eq(idGestao));

        when(atividadeRepositoryMock.save(any(AtividadeConselhal.class)))
                .thenAnswer(inv -> {
                    AtividadeConselhal a = inv.getArgument(0);
                    a.setIdAtividade(200);
                    return a;
                });

        List<AtividadeConselhal> criadas = service.criar(dto);

        assertThat(criadas).hasSize(3);
        for (AtividadeConselhal a : criadas) {
            assertThat(a.getGestao()).isEqualTo(gestao);
            assertThat(a.getRegra()).isEqualTo(regra);
            assertThat(a.getDataHoraAtividade()).isEqualTo(dataHora);
            assertThat(a.getInSituacao()).isEqualTo(AtividadeConselhal.SITUACAO_PENDENTE);
            assertThat(a.getComprovante()).isNull();
            assertThat(a.getQtdAtividade()).isEqualTo(1);
        }

        verify(atividadeRepositoryMock, times(3)).save(any(AtividadeConselhal.class));
        verify(comprovanteServiceMock, never()).criar(any(), any(), any());
        verify(logJetonServiceMock).logLoteCriado(any(), eq(idGestao), eq(idRegra), eq(idsConselheiros), eq(dataHora));
    }

    @Test
    @DisplayName("deve criar lote de atividades com comprovante com sucesso")
    void deveCriarLoteComComprovante() throws Exception {
        Integer idGestao = 1;
        Integer idRegra = 10;
        LocalDateTime dataHora = LocalDateTime.now();
        List<Integer> idsConselheiros = List.of(200, 201);
        Integer idTipoAnexo = 5;
        String nomeComprovante = "Ata da Reunião em Lote";
        MultipartFile fileMock = mock(MultipartFile.class);
        when(fileMock.isEmpty()).thenReturn(false);

        Gestao gestao = criarGestao(idGestao, LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1));
        Regras regra = criarRegra(idRegra);
        Comprovante comprovante = criarComprovante(999);

        Conselheiro conselheiro1 = criarConselheiro(200, "Médico X");
        Conselheiro conselheiro2 = criarConselheiro(201, "Médico Y");

        LoteAtividadeDTO dto = criarLoteDto(idGestao, idRegra, dataHora, idsConselheiros, fileMock, idTipoAnexo,
                nomeComprovante);

        doNothing().when(atividadeValidatorMock).validarDataHoraObrigatoria(dataHora);
        when(atividadeValidatorMock.validarGestaoExistente(idGestao)).thenReturn(gestao);
        doNothing().when(atividadeValidatorMock).validarDataDentroDoMandato(dataHora.toLocalDate(), idGestao);
        when(regrasServiceMock.buscarOuFalhar(idRegra)).thenReturn(regra);
        when(comprovanteServiceMock.criar(fileMock, idTipoAnexo, nomeComprovante)).thenReturn(comprovante);

        when(conselheiroServiceMock.buscarPorId(200)).thenReturn(Optional.of(conselheiro1));
        when(conselheiroServiceMock.buscarPorId(201)).thenReturn(Optional.of(conselheiro2));

        doNothing().when(atividadeValidatorMock).validarVinculoConselheiroGestao(anyInt(), eq(idGestao));

        when(atividadeRepositoryMock.save(any(AtividadeConselhal.class)))
                .thenAnswer(inv -> {
                    AtividadeConselhal a = inv.getArgument(0);
                    a.setIdAtividade(300);
                    return a;
                });

        List<AtividadeConselhal> criadas = service.criar(dto);

        assertThat(criadas).hasSize(2);
        for (AtividadeConselhal a : criadas) {
            assertThat(a.getComprovante()).isEqualTo(comprovante);
        }

        verify(comprovanteServiceMock).criar(fileMock, idTipoAnexo, nomeComprovante);
        verify(logJetonServiceMock).logLoteCriado(eq(comprovante.getIdComprovante()), eq(idGestao), eq(idRegra),
                eq(idsConselheiros), eq(dataHora));
    }

    @Test
    @DisplayName("deve lançar exceção ao criar lote com data/hora nula")
    void deveLancarExcecaoCriarLoteDataHoraNula() {
        LoteAtividadeDTO dto = new LoteAtividadeDTO();
        dto.setDataHoraAtividade(null);

        doThrow(new RuntimeException("Data/hora obrigatória"))
                .when(atividadeValidatorMock).validarDataHoraObrigatoria(null);

        assertThatThrownBy(() -> service.criar(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Data/hora obrigatória");

        verify(atividadeRepositoryMock, never()).save(any());
    }

    @Test
    @DisplayName("deve lançar exceção ao criar lote com gestão inválida")
    void deveLancarExcecaoCriarLoteGestaoInvalida() {
        Integer idGestao = 999;
        LocalDateTime dataHora = LocalDateTime.now();
        LoteAtividadeDTO dto = new LoteAtividadeDTO();
        dto.setIdGestao(idGestao);
        dto.setDataHoraAtividade(dataHora);

        doNothing().when(atividadeValidatorMock).validarDataHoraObrigatoria(dataHora);
        when(atividadeValidatorMock.validarGestaoExistente(idGestao))
                .thenThrow(new RuntimeException("Gestão não encontrada"));

        assertThatThrownBy(() -> service.criar(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Gestão não encontrada");
    }

    @Test
    @DisplayName("deve lançar exceção ao criar lote com conselheiro não vinculado à gestão")
    void deveLancarExcecaoCriarLoteConselheiroNaoVinculado() {
        Integer idGestao = 1;
        Integer idRegra = 10;
        LocalDateTime dataHora = LocalDateTime.now();
        List<Integer> idsConselheiros = List.of(50);

        Gestao gestao = criarGestao(idGestao, LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1));
        Regras regra = criarRegra(idRegra);
        Conselheiro conselheiro = criarConselheiro(50, "Médico Z");

        LoteAtividadeDTO dto = criarLoteDto(idGestao, idRegra, dataHora, idsConselheiros, null, null, null);

        doNothing().when(atividadeValidatorMock).validarDataHoraObrigatoria(dataHora);
        when(atividadeValidatorMock.validarGestaoExistente(idGestao)).thenReturn(gestao);
        doNothing().when(atividadeValidatorMock).validarDataDentroDoMandato(dataHora.toLocalDate(), idGestao);
        when(regrasServiceMock.buscarOuFalhar(idRegra)).thenReturn(regra);
        when(conselheiroServiceMock.buscarPorId(50)).thenReturn(Optional.of(conselheiro));

        doThrow(new RuntimeException("Conselheiro não vinculado à gestão"))
                .when(atividadeValidatorMock).validarVinculoConselheiroGestao(50, idGestao);

        assertThatThrownBy(() -> service.criar(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Conselheiro não vinculado à gestão");

        verify(atividadeRepositoryMock, never()).save(any());
    }

    // ========== TESTES DE ATUALIZAÇÃO ==========

    @Test
    @DisplayName("deve atualizar lote mantendo o mesmo comprovante e atualizando dados comuns")
    void deveAtualizarLoteMantendoComprovante() {
        Integer idComprovante = 1000;
        Integer idGestao = 1;
        Integer idRegra = 10;
        LocalDateTime novaData = LocalDateTime.now().plusDays(2);
        List<Integer> idsConselheirosNovos = List.of(300, 302);

        Gestao gestao = criarGestao(idGestao, LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1));
        Regras regra = criarRegra(idRegra);
        Comprovante comprovante = criarComprovante(idComprovante);
        Conselheiro conselheiro1 = criarConselheiro(300, "Médico A");
        Conselheiro conselheiro2 = criarConselheiro(301, "Médico B");
        Conselheiro conselheiro3 = criarConselheiro(302, "Médico C");

        AtividadeConselhal atv1 = criarAtividade(10, gestao, conselheiro1, regra, comprovante,
                AtividadeConselhal.SITUACAO_PENDENTE);
        AtividadeConselhal atv2 = criarAtividade(11, gestao, conselheiro2, regra, comprovante,
                AtividadeConselhal.SITUACAO_PENDENTE);

        when(atividadeRepositoryMock.findByComprovanteIdComprovante(idComprovante))
                .thenReturn(List.of(atv1, atv2));

        when(comprovanteRepositoryMock.findById(idComprovante))
                .thenReturn(Optional.of(comprovante));

        LoteAtividadeDTO dto = criarLoteDto(idGestao, idRegra, novaData, idsConselheirosNovos, null, null, null);

        doNothing().when(atividadeValidatorMock).validarDataHoraObrigatoria(novaData);
        when(atividadeValidatorMock.validarGestaoExistente(idGestao)).thenReturn(gestao);
        doNothing().when(atividadeValidatorMock).validarDataDentroDoMandato(novaData.toLocalDate(), idGestao);
        when(regrasServiceMock.buscarOuFalhar(idRegra)).thenReturn(regra);

        // Apenas o novo conselheiro (302) precisa ser buscado
        when(conselheiroServiceMock.buscarPorId(302)).thenReturn(Optional.of(conselheiro3));

        doNothing().when(atividadeValidatorMock).validarAtividadeNaoFechada(any(AtividadeConselhal.class));
        doNothing().when(atividadeValidatorMock).validarVinculoConselheiroGestao(302, idGestao);

        when(atividadeRepositoryMock.save(any(AtividadeConselhal.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.atualizar(idComprovante, dto);

        // Verifica dados atualizados na atividade que permaneceu
        assertThat(atv1.getDataHoraAtividade()).isEqualTo(novaData);
        assertThat(atv1.getRegra()).isEqualTo(regra);
        assertThat(atv1.getGestao()).isEqualTo(gestao);

        // Verifica exclusão do conselheiro 301
        verify(atividadeRepositoryMock).delete(atv2);

        // Verifica criação da nova atividade para 302
        ArgumentCaptor<AtividadeConselhal> captor = ArgumentCaptor.forClass(AtividadeConselhal.class);
        verify(atividadeRepositoryMock, atLeastOnce()).save(captor.capture());
        boolean novaCriada = captor.getAllValues().stream()
                .anyMatch(a -> a.getConselheiro().getIdPessoa().equals(302));
        assertThat(novaCriada).isTrue();

        verify(logJetonServiceMock).logLoteAtualizado(eq(idComprovante), eq(List.of(300, 301)),
                eq(List.of(300, 302)), eq(idGestao), eq(idRegra), eq(novaData));
    }

    @Test
    @DisplayName("deve atualizar lote substituindo o comprovante e excluir o antigo se não houver mais vínculos")
    void deveAtualizarLoteSubstituindoComprovante() throws Exception {
        Integer idComprovanteAntigo = 1000;
        Integer idGestao = 1;
        Integer idRegra = 10;
        LocalDateTime data = LocalDateTime.now();
        List<Integer> idsConselheiros = List.of(400, 401);
        Integer idTipoAnexo = 7;
        String novoNomeComprovante = "Novo Comprovante";
        MultipartFile fileMock = mock(MultipartFile.class);
        when(fileMock.isEmpty()).thenReturn(false);

        Gestao gestao = criarGestao(idGestao, LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1));
        Regras regra = criarRegra(idRegra);
        Comprovante comprovanteAntigo = criarComprovante(idComprovanteAntigo);
        Comprovante novoComprovante = criarComprovante(2000);

        Conselheiro conselheiro1 = criarConselheiro(400, "Médico A");
        Conselheiro conselheiro2 = criarConselheiro(401, "Médico B");

        AtividadeConselhal atv1 = criarAtividade(20, gestao, conselheiro1, regra, comprovanteAntigo,
                AtividadeConselhal.SITUACAO_PENDENTE);
        AtividadeConselhal atv2 = criarAtividade(21, gestao, conselheiro2, regra, comprovanteAntigo,
                AtividadeConselhal.SITUACAO_PENDENTE);

        when(atividadeRepositoryMock.findByComprovanteIdComprovante(idComprovanteAntigo))
                .thenReturn(List.of(atv1, atv2));

        when(comprovanteRepositoryMock.findById(idComprovanteAntigo))
                .thenReturn(Optional.of(comprovanteAntigo));

        LoteAtividadeDTO dto = criarLoteDto(idGestao, idRegra, data, idsConselheiros, fileMock, idTipoAnexo,
                novoNomeComprovante);

        doNothing().when(atividadeValidatorMock).validarDataHoraObrigatoria(data);
        when(atividadeValidatorMock.validarGestaoExistente(idGestao)).thenReturn(gestao);
        doNothing().when(atividadeValidatorMock).validarDataDentroDoMandato(data.toLocalDate(), idGestao);
        when(regrasServiceMock.buscarOuFalhar(idRegra)).thenReturn(regra);

        doNothing().when(atividadeValidatorMock).validarAtividadeNaoFechada(any(AtividadeConselhal.class));

        when(comprovanteServiceMock.criar(fileMock, idTipoAnexo, novoNomeComprovante)).thenReturn(novoComprovante);
        when(atividadeRepositoryMock.countByComprovanteIdComprovante(idComprovanteAntigo)).thenReturn(0L);

        when(atividadeRepositoryMock.save(any(AtividadeConselhal.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.atualizar(idComprovanteAntigo, dto);

        // Verifica que as atividades foram atualizadas com o novo comprovante
        assertThat(atv1.getComprovante()).isEqualTo(novoComprovante);
        assertThat(atv2.getComprovante()).isEqualTo(novoComprovante);

        verify(comprovanteServiceMock).excluir(idComprovanteAntigo);
        verify(logJetonServiceMock).logLoteAtualizado(eq(idComprovanteAntigo), any(), any(), eq(idGestao),
                eq(idRegra), eq(data));
    }

    @Test
    @DisplayName("deve lançar exceção ao atualizar lote com atividade já fechada")
    void deveLancarExcecaoAtualizarLoteAtividadeFechada() {
        Integer idComprovante = 500;
        Integer idGestao = 1;
        Integer idRegra = 10;
        LocalDateTime data = LocalDateTime.now();

        Gestao gestao = criarGestao(idGestao, LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1));
        Regras regra = criarRegra(idRegra);
        Comprovante comprovante = criarComprovante(idComprovante);
        Conselheiro conselheiro = criarConselheiro(900, "Médico X");

        AtividadeConselhal atividadeFechada = criarAtividade(100, gestao, conselheiro, regra, comprovante,
                AtividadeConselhal.SITUACAO_FECHADA);

        when(atividadeRepositoryMock.findByComprovanteIdComprovante(idComprovante))
                .thenReturn(List.of(atividadeFechada));

        when(comprovanteRepositoryMock.findById(idComprovante))
                .thenReturn(Optional.of(comprovante));

        LoteAtividadeDTO dto = criarLoteDto(idGestao, idRegra, data, List.of(900), null, null, null);

        doNothing().when(atividadeValidatorMock).validarDataHoraObrigatoria(data);
        when(atividadeValidatorMock.validarGestaoExistente(idGestao)).thenReturn(gestao);
        doNothing().when(atividadeValidatorMock).validarDataDentroDoMandato(data.toLocalDate(), idGestao);
        when(regrasServiceMock.buscarOuFalhar(idRegra)).thenReturn(regra);

        doThrow(new RuntimeException("Atividade já fechada"))
                .when(atividadeValidatorMock).validarAtividadeNaoFechada(atividadeFechada);

        assertThatThrownBy(() -> service.atualizar(idComprovante, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Atividade já fechada");

        verify(atividadeRepositoryMock, never()).save(any());
    }

    @Test
    @DisplayName("deve lançar exceção ao atualizar lote com comprovante inexistente")
    void deveLancarExcecaoAtualizarLoteComprovanteInexistente() {
        Integer idComprovante = 9999;
        LoteAtividadeDTO dto = new LoteAtividadeDTO();

        when(atividadeRepositoryMock.findByComprovanteIdComprovante(idComprovante))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.atualizar(idComprovante, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Nenhuma atividade encontrada para o comprovante informado.");
    }

    // ========== TESTES DE CONSULTA ==========

    @Test
    @DisplayName("deve listar atividades por comprovante com sucesso")
    void deveListarAtividadesPorComprovante() {
        Integer idComprovante = 777;
        Comprovante comprovante = criarComprovante(idComprovante);
        Gestao gestao = criarGestao(1, LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(1));
        Regras regra = criarRegra(1);
        Conselheiro conselheiro = criarConselheiro(1, "Médico");
        AtividadeConselhal atividade = criarAtividade(1, gestao, conselheiro, regra, comprovante, "P");
        List<AtividadeConselhal> listaEsperada = List.of(atividade);

        when(atividadeRepositoryMock.findByComprovanteIdComprovante(idComprovante))
                .thenReturn(listaEsperada);

        List<AtividadeConselhal> resultado = service.listarPorComprovante(idComprovante);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getComprovante().getIdComprovante()).isEqualTo(idComprovante);
    }

    @Test
    @DisplayName("deve contar atividades por comprovante com sucesso")
    void deveContarAtividadesPorComprovante() {
        Integer idComprovante = 888;
        long expectedCount = 5L;

        when(atividadeRepositoryMock.countByComprovanteIdComprovante(idComprovante))
                .thenReturn(expectedCount);

        long resultado = service.contarAtividadesPorComprovante(idComprovante);

        assertThat(resultado).isEqualTo(expectedCount);
    }
}