package br.com.cremepe.jeton.service;

import br.com.cremepe.jeton.annotation.AuditoriaUser;
import br.com.cremepe.jeton.domain.ViewUserLogin;
import br.com.cremepe.jeton.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do serviço de Usuário Logado (UsuarioLogadoService)")
class UsuarioLogadoServiceTest {

    @InjectMocks
    private UsuarioLogadoService service;

    @Mock
    private HttpServletRequest requestMock;

    @Mock
    private HttpSession sessionMock;

    // ========== TESTES PARA getUsuarioLogado() ==========

    @Test
    @DisplayName("deve retornar AuditoriaUser do SecurityContext quando autenticado com CustomUserDetails")
    void deveRetornarAuditoriaUserDoSecurityContextQuandoAutenticado() {
        // Dado
        Integer idUsuario = 42;
        String nomeUsuario = "Dr. Teste";

        ViewUserLogin viewUser = new ViewUserLogin();
        viewUser.setIdPessoa(idUsuario);
        viewUser.setNome(nomeUsuario);

        CustomUserDetails userDetails = new CustomUserDetails(viewUser);

        Authentication authMock = mock(Authentication.class);
        when(authMock.isAuthenticated()).thenReturn(true);
        when(authMock.getPrincipal()).thenReturn(userDetails);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authMock);

        try (MockedStatic<SecurityContextHolder> securityHolderMock = mockStatic(SecurityContextHolder.class)) {
            securityHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            // Quando
            AuditoriaUser resultado = service.getUsuarioLogado();

            // Então
            assertThat(resultado).isNotNull();
            assertThat(resultado.id()).isEqualTo(idUsuario);
            assertThat(resultado.nome()).isEqualTo(nomeUsuario);
        }
    }

    @Test
    @DisplayName("deve retornar AuditoriaUser da sessão quando SecurityContext não tem usuário (fallback)")
    void deveRetornarAuditoriaUserDaSessaoQuandoSecurityContextNaoTemUsuario() {
        // Dado: SecurityContext sem autenticação ou com principal diferente de
        // CustomUserDetails
        Authentication authMock = mock(Authentication.class);
        when(authMock.isAuthenticated()).thenReturn(true);
        when(authMock.getPrincipal()).thenReturn("anonymousUser"); // não é CustomUserDetails

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authMock);

        // Dado: sessão com usuário logado
        Integer idUsuario = 99;
        String nomeUsuario = "Usuário Sessão";
        ViewUserLogin viewUser = new ViewUserLogin();
        viewUser.setIdPessoa(idUsuario);
        viewUser.setNome(nomeUsuario);

        when(requestMock.getSession()).thenReturn(sessionMock);
        when(sessionMock.getAttribute("usuarioLogado")).thenReturn(viewUser);

        ServletRequestAttributes attributes = new ServletRequestAttributes(requestMock);

        try (MockedStatic<SecurityContextHolder> securityHolderMock = mockStatic(SecurityContextHolder.class);
                MockedStatic<RequestContextHolder> requestHolderMock = mockStatic(RequestContextHolder.class)) {

            securityHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            requestHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(attributes);

            // Quando
            AuditoriaUser resultado = service.getUsuarioLogado();

            // Então
            assertThat(resultado).isNotNull();
            assertThat(resultado.id()).isEqualTo(idUsuario);
            assertThat(resultado.nome()).isEqualTo(nomeUsuario);
        }
    }

    @Test
    @DisplayName("deve retornar null quando não há usuário logado em lugar nenhum")
    void deveRetornarNullQuandoNaoHaUsuarioLogado() {
        // Dado: SecurityContext sem autenticação
        Authentication authMock = mock(Authentication.class);
        when(authMock.isAuthenticated()).thenReturn(false);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authMock);

        // Dado: RequestContextHolder sem atributos ou sem sessão
        try (MockedStatic<SecurityContextHolder> securityHolderMock = mockStatic(SecurityContextHolder.class);
                MockedStatic<RequestContextHolder> requestHolderMock = mockStatic(RequestContextHolder.class)) {

            securityHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            requestHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(null);

            // Quando
            AuditoriaUser resultado = service.getUsuarioLogado();

            // Então
            assertThat(resultado).isNull();
        }
    }

    @Test
    @DisplayName("deve retornar null quando autenticado mas principal não é CustomUserDetails e não há sessão")
    void deveRetornarNullQuandoPrincipalNaoEhCustomUserDetailsESessaoVazia() {
        // Dado: SecurityContext com autenticação, mas principal é String
        Authentication authMock = mock(Authentication.class);
        when(authMock.isAuthenticated()).thenReturn(true);
        when(authMock.getPrincipal()).thenReturn("anonymousUser");

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authMock);

        // Dado: RequestContextHolder com requisição, mas sem atributo na sessão
        when(requestMock.getSession()).thenReturn(sessionMock);
        when(sessionMock.getAttribute("usuarioLogado")).thenReturn(null);

        ServletRequestAttributes attributes = new ServletRequestAttributes(requestMock);

        try (MockedStatic<SecurityContextHolder> securityHolderMock = mockStatic(SecurityContextHolder.class);
                MockedStatic<RequestContextHolder> requestHolderMock = mockStatic(RequestContextHolder.class)) {

            securityHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            requestHolderMock.when(RequestContextHolder::getRequestAttributes).thenReturn(attributes);

            // Quando
            AuditoriaUser resultado = service.getUsuarioLogado();

            // Então
            assertThat(resultado).isNull();
        }
    }

    // ========== TESTES PARA getViewUserLogin() ==========

    @Test
    @DisplayName("deve retornar ViewUserLogin do SecurityContext quando autenticado com CustomUserDetails")
    void deveRetornarViewUserLoginDoSecurityContextQuandoAutenticado() {
        // Dado
        ViewUserLogin viewUser = new ViewUserLogin();
        viewUser.setIdPessoa(100);
        viewUser.setNome("Dr. View");

        CustomUserDetails userDetails = new CustomUserDetails(viewUser);

        Authentication authMock = mock(Authentication.class);
        when(authMock.isAuthenticated()).thenReturn(true);
        when(authMock.getPrincipal()).thenReturn(userDetails);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authMock);

        try (MockedStatic<SecurityContextHolder> securityHolderMock = mockStatic(SecurityContextHolder.class)) {
            securityHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            // Quando
            Optional<ViewUserLogin> resultado = service.getViewUserLogin();

            // Então
            assertThat(resultado).isPresent();
            assertThat(resultado.get()).isSameAs(viewUser);
        }
    }

    @Test
    @DisplayName("deve retornar Optional vazio quando autenticado mas principal não é CustomUserDetails")
    void deveRetornarOptionalVazioQuandoPrincipalNaoEhCustomUserDetails() {
        // Dado
        Authentication authMock = mock(Authentication.class);
        when(authMock.isAuthenticated()).thenReturn(true);
        when(authMock.getPrincipal()).thenReturn("anonymousUser");

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authMock);

        try (MockedStatic<SecurityContextHolder> securityHolderMock = mockStatic(SecurityContextHolder.class)) {
            securityHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            // Quando
            Optional<ViewUserLogin> resultado = service.getViewUserLogin();

            // Então
            assertThat(resultado).isEmpty();
        }
    }

    @Test
    @DisplayName("deve retornar Optional vazio quando não autenticado")
    void deveRetornarOptionalVazioQuandoNaoAutenticado() {
        // Dado
        Authentication authMock = mock(Authentication.class);
        when(authMock.isAuthenticated()).thenReturn(false);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authMock);

        try (MockedStatic<SecurityContextHolder> securityHolderMock = mockStatic(SecurityContextHolder.class)) {
            securityHolderMock.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            // Quando
            Optional<ViewUserLogin> resultado = service.getViewUserLogin();

            // Então
            assertThat(resultado).isEmpty();
        }
    }
}