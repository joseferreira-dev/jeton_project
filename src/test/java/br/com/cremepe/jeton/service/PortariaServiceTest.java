package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.domain.Portaria;
import br.com.cremepe.jeton.repository.PortariaRepository;
import br.com.cremepe.jeton.repository.RegrasRepository;
import br.com.cremepe.jeton.util.NormativaValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do serviço de Portarias (PortariaService)")
class PortariaServiceTest {

    @Mock
    private PortariaRepository repositoryMock;

    @Mock
    private RegrasRepository regrasRepositoryMock;

    @Mock
    private LogJetonService logJetonServiceMock;

    @Mock
    private NormativaValidator normativaValidatorMock;

    @InjectMocks
    private PortariaService service;

    // ========== HELPERS ==========

    private Portaria criarPortaria(Integer id, Integer numero, Integer ano, LocalDate inicio, LocalDate fim,
            String revogado) {
        Portaria p = new Portaria();
        p.setIdPortaria(id);
        p.setNumero(numero);
        p.setAno(ano);
        p.setDtInicioVigencia(inicio);
        p.setDtFimVigencia(fim);
        p.setInRevogado(revogado);
        return p;
    }

    private Portaria criarPortariaValida(Integer id) {
        return criarPortaria(id, 100, 2026,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                Portaria.REVOGADO_NAO);
    }

    // ========== TESTES DE CRIAÇÃO ==========

    @Test
    @DisplayName("deve criar portaria com sucesso")
    void deveCriarPortariaComSucesso() {
        // Dado
        Integer numero = 100;
        Integer ano = 2026;
        LocalDate inicio = LocalDate.of(2026, 1, 1);
        LocalDate fim = LocalDate.of(2026, 12, 31);

        Portaria portaria = criarPortaria(null, numero, ano, inicio, fim, Portaria.REVOGADO_NAO);

        // Mock do validator
        doNothing().when(normativaValidatorMock)
                .validarPortaria(numero, ano, inicio, fim, null);

        // Mock do save
        when(repositoryMock.save(any(Portaria.class)))
                .thenAnswer(inv -> {
                    Portaria p = inv.getArgument(0);
                    p.setIdPortaria(999);
                    return p;
                });

        // Quando
        Portaria salva = service.criar(portaria);

        // Então
        assertThat(salva).isNotNull();
        assertThat(salva.getIdPortaria()).isEqualTo(999);
        assertThat(salva.getNumero()).isEqualTo(numero);
        assertThat(salva.getAno()).isEqualTo(ano);
        assertThat(salva.getInRevogado()).isEqualTo(Portaria.REVOGADO_NAO);

        verify(normativaValidatorMock).validarPortaria(numero, ano, inicio, fim, null);
        verify(repositoryMock).save(portaria);
        verify(logJetonServiceMock).logPortariaCriada(salva);
    }

    @Test
    @DisplayName("deve normalizar inRevogado para N ao criar portaria sem valor")
    void deveNormalizarInRevogadoParaNAoCriar() {
        // Dado
        Portaria portaria = criarPortaria(null, 100, 2026,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                null);

        doNothing().when(normativaValidatorMock).validarPortaria(anyInt(), anyInt(), any(), any(), isNull());

        when(repositoryMock.save(any(Portaria.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Quando
        Portaria salva = service.criar(portaria);

        // Então
        assertThat(salva.getInRevogado()).isEqualTo(Portaria.REVOGADO_NAO);
    }

    @Test
    @DisplayName("deve lançar exceção ao criar portaria com número duplicado")
    void deveLancarExcecaoCriarPortariaNumeroDuplicado() {
        // Dado
        Portaria portaria = criarPortariaValida(null);

        doThrow(new RuntimeException("Já existe uma portaria cadastrada com o número 100/2026"))
                .when(normativaValidatorMock).validarPortaria(anyInt(), anyInt(), any(), any(), isNull());

        // Quando / Então
        assertThatThrownBy(() -> service.criar(portaria))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Já existe uma portaria cadastrada com o número 100/2026");

        verify(repositoryMock, never()).save(any());
    }

    // ========== TESTES DE ATUALIZAÇÃO ==========

    @Test
    @DisplayName("deve atualizar portaria com sucesso")
    void deveAtualizarPortariaComSucesso() {
        // Dado
        Integer id = 50;
        Integer novoNumero = 200;
        Integer novoAno = 2027;
        LocalDate novoInicio = LocalDate.of(2027, 1, 1);
        LocalDate novoFim = LocalDate.of(2027, 12, 31);

        Portaria existente = criarPortariaValida(id);
        Portaria atualizada = criarPortaria(id, novoNumero, novoAno, novoInicio, novoFim, Portaria.REVOGADO_NAO);

        when(repositoryMock.existsById(id)).thenReturn(true);
        when(repositoryMock.findById(id)).thenReturn(Optional.of(existente));
        doNothing().when(normativaValidatorMock)
                .validarPortaria(novoNumero, novoAno, novoInicio, novoFim, id);

        when(repositoryMock.save(any(Portaria.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Quando
        Portaria salva = service.atualizar(atualizada);

        // Então
        assertThat(salva.getNumero()).isEqualTo(novoNumero);
        assertThat(salva.getAno()).isEqualTo(novoAno);
        assertThat(salva.getDtInicioVigencia()).isEqualTo(novoInicio);
        assertThat(salva.getDtFimVigencia()).isEqualTo(novoFim);

        verify(repositoryMock).save(existente);
        verify(logJetonServiceMock).logPortariaAtualizada(any(Portaria.class), eq(salva));
    }

    @Test
    @DisplayName("deve lançar exceção ao atualizar portaria inexistente")
    void deveLancarExcecaoAtualizarPortariaInexistente() {
        // Dado
        Integer id = 999;
        Portaria portaria = criarPortariaValida(id);

        when(repositoryMock.existsById(id)).thenReturn(false);

        // Quando / Então
        assertThatThrownBy(() -> service.atualizar(portaria))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Portaria não encontrada para atualização.");

        verify(repositoryMock, never()).save(any());
    }

    @Test
    @DisplayName("deve lançar exceção ao atualizar portaria sem ID")
    void deveLancarExcecaoAtualizarPortariaSemId() {
        // Dado
        Portaria portaria = criarPortariaValida(null);

        // Quando / Então
        assertThatThrownBy(() -> service.atualizar(portaria))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("ID da portaria não informado para atualização.");

        verify(repositoryMock, never()).existsById(any());
        verify(repositoryMock, never()).save(any());
    }

    // ========== TESTES DE REVOGAÇÃO ==========

    @Test
    @DisplayName("deve revogar portaria com sucesso e revogar regras vinculadas")
    void deveRevogarPortariaComSucesso() {
        // Dado
        Integer id = 50;
        Portaria portaria = criarPortariaValida(id);

        when(repositoryMock.findById(id)).thenReturn(Optional.of(portaria));

        when(repositoryMock.save(any(Portaria.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Quando
        service.revogar(id);

        // Então
        assertThat(portaria.getInRevogado()).isEqualTo(Portaria.REVOGADO_SIM);
        verify(repositoryMock).save(portaria);
        verify(regrasRepositoryMock).revogarRegrasPorPortaria(id);
        verify(logJetonServiceMock).logPortariaRevogada(portaria);
    }

    @Test
    @DisplayName("deve lançar exceção ao revogar portaria já revogada")
    void deveLancarExcecaoRevogarPortariaJaRevogada() {
        // Dado
        Integer id = 50;
        Portaria portaria = criarPortaria(id, 100, 2026,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                Portaria.REVOGADO_SIM);

        when(repositoryMock.findById(id)).thenReturn(Optional.of(portaria));

        // Quando / Então
        assertThatThrownBy(() -> service.revogar(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("A portaria já está revogada.");

        verify(repositoryMock, never()).save(any());
        verify(regrasRepositoryMock, never()).revogarRegrasPorPortaria(anyInt());
    }

    // ========== TESTES DE RESTAURAÇÃO ==========

    @Test
    @DisplayName("deve restaurar portaria com sucesso")
    void deveRestaurarPortariaComSucesso() {
        // Dado
        Integer id = 50;
        Portaria portaria = criarPortaria(id, 100, 2026,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                Portaria.REVOGADO_SIM);

        when(repositoryMock.findById(id)).thenReturn(Optional.of(portaria));

        when(repositoryMock.save(any(Portaria.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Quando
        service.restaurar(id);

        // Então
        assertThat(portaria.getInRevogado()).isEqualTo(Portaria.REVOGADO_NAO);
        verify(repositoryMock).save(portaria);
        verify(logJetonServiceMock).logPortariaRestaurada(portaria);
    }

    @Test
    @DisplayName("deve lançar exceção ao restaurar portaria já em vigor")
    void deveLancarExcecaoRestaurarPortariaJaEmVigor() {
        // Dado
        Integer id = 50;
        Portaria portaria = criarPortariaValida(id);

        when(repositoryMock.findById(id)).thenReturn(Optional.of(portaria));

        // Quando / Então
        assertThatThrownBy(() -> service.restaurar(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("A portaria já está em vigor.");

        verify(repositoryMock, never()).save(any());
    }

    // ========== TESTES DE EXCLUSÃO ==========

    @Test
    @DisplayName("deve excluir portaria com sucesso quando revogada e sem regras vinculadas")
    void deveExcluirPortariaComSucesso() {
        // Dado
        Integer id = 50;
        Portaria portaria = criarPortaria(id, 100, 2026,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                Portaria.REVOGADO_SIM);

        when(repositoryMock.findById(id)).thenReturn(Optional.of(portaria));
        when(regrasRepositoryMock.countByPortariaIdPortaria(id)).thenReturn(0L);

        // Quando
        service.excluir(id);

        // Então
        verify(repositoryMock).deleteById(id);
        verify(logJetonServiceMock).logPortariaExcluida(portaria);
    }

    @Test
    @DisplayName("deve lançar exceção ao excluir portaria em vigor (não revogada)")
    void deveLancarExcecaoExcluirPortariaEmVigor() {
        // Dado
        Integer id = 50;
        Portaria portaria = criarPortariaValida(id);

        when(repositoryMock.findById(id)).thenReturn(Optional.of(portaria));

        // Quando / Então
        assertThatThrownBy(() -> service.excluir(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Para excluir, a portaria deve estar revogada primeiro.");

        verify(repositoryMock, never()).deleteById(any());
    }

    @Test
    @DisplayName("deve lançar exceção ao excluir portaria com regras vinculadas")
    void deveLancarExcecaoExcluirPortariaComRegras() {
        // Dado
        Integer id = 50;
        Portaria portaria = criarPortaria(id, 100, 2026,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                Portaria.REVOGADO_SIM);

        when(repositoryMock.findById(id)).thenReturn(Optional.of(portaria));
        when(regrasRepositoryMock.countByPortariaIdPortaria(id)).thenReturn(3L);

        // Quando / Então
        assertThatThrownBy(() -> service.excluir(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessage(
                        "Não é possível excluir a portaria pois existem 3 regra(s) vinculada(s). Revogue-as ou exclua-as antes.");

        verify(repositoryMock, never()).deleteById(any());
    }

    // ========== TESTES DE CONSULTA ==========

    @Test
    @DisplayName("deve listar todas as portarias")
    void deveListarTodasPortarias() {
        // Dado
        List<Portaria> lista = List.of(
                criarPortariaValida(1),
                criarPortariaValida(2));
        when(repositoryMock.findAll()).thenReturn(lista);

        // Quando
        List<Portaria> resultado = service.listarTodos();

        // Então
        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getIdPortaria()).isEqualTo(1);
        assertThat(resultado.get(1).getIdPortaria()).isEqualTo(2);
        verify(repositoryMock).findAll();
    }

    @Test
    @DisplayName("deve buscar portaria por ID com sucesso")
    void deveBuscarPortariaPorIdComSucesso() {
        // Dado
        Integer id = 10;
        Portaria portaria = criarPortariaValida(id);
        when(repositoryMock.findById(id)).thenReturn(Optional.of(portaria));

        // Quando
        Optional<Portaria> resultado = service.buscarPorId(id);

        // Então
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getIdPortaria()).isEqualTo(id);
        verify(repositoryMock).findById(id);
    }

    @Test
    @DisplayName("deve retornar Optional vazio ao buscar portaria inexistente")
    void deveRetornarOptionalVazioBuscarInexistente() {
        // Dado
        Integer id = 999;
        when(repositoryMock.findById(id)).thenReturn(Optional.empty());

        // Quando
        Optional<Portaria> resultado = service.buscarPorId(id);

        // Então
        assertThat(resultado).isEmpty();
        verify(repositoryMock).findById(id);
    }

    @Test
    @DisplayName("deve buscar portaria ou falhar com sucesso quando existe")
    void deveBuscarOuFalharComSucesso() {
        // Dado
        Integer id = 10;
        Portaria portaria = criarPortariaValida(id);
        when(repositoryMock.findById(id)).thenReturn(Optional.of(portaria));

        // Quando
        Portaria resultado = service.buscarOuFalhar(id);

        // Então
        assertThat(resultado).isNotNull();
        assertThat(resultado.getIdPortaria()).isEqualTo(id);
    }

    @Test
    @DisplayName("deve lançar exceção ao buscar ou falhar com ID inexistente")
    void deveLancarExcecaoBuscarOuFalharInexistente() {
        // Dado
        Integer id = 999;
        when(repositoryMock.findById(id)).thenReturn(Optional.empty());

        // Quando / Então
        assertThatThrownBy(() -> service.buscarOuFalhar(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Portaria não encontrada com ID: " + id);
    }

    @Test
    @DisplayName("deve listar portarias paginadas com filtros")
    void deveListarPortariasPaginadasComFiltros() {
        // Dado
        String termo = "100";
        String situacao = "N";
        int page = 0, size = 10;
        String sortField = "ano", sortDir = "desc";

        Page<Portaria> paginaEsperada = new PageImpl<>(List.of(criarPortariaValida(1)));

        when(repositoryMock.findAllByFilters(eq(termo), eq(situacao), any(Pageable.class)))
                .thenReturn(paginaEsperada);

        // Quando
        Page<Portaria> resultado = service.listarComPaginacaoEPesquisa(termo, situacao, page, size, sortField, sortDir);

        // Então
        assertThat(resultado).isNotNull();
        assertThat(resultado.getContent()).hasSize(1);
        verify(repositoryMock).findAllByFilters(eq(termo), eq(situacao), any(Pageable.class));
    }

    @Test
    @DisplayName("deve listar portarias paginadas sem ordenação (size = 0)")
    void deveListarPortariasPaginadasSemOrdenacao() {
        // Dado
        String termo = "";
        String situacao = "";
        int page = 0, size = 0;

        Page<Portaria> paginaEsperada = new PageImpl<>(List.of(criarPortariaValida(1)));

        when(repositoryMock.findAllByFilters(eq(termo), eq(situacao), any(Pageable.class)))
                .thenReturn(paginaEsperada);

        // Quando
        Page<Portaria> resultado = service.listarComPaginacaoEPesquisa(termo, situacao, page, size, "ano", "asc");

        // Então
        assertThat(resultado).isNotNull();
        verify(repositoryMock).findAllByFilters(eq(termo), eq(situacao), any(Pageable.class));
    }
}