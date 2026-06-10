package br.com.cremepe.jeton.security;

import br.com.cremepe.jeton.domain.NivelAcesso;
import br.com.cremepe.jeton.domain.ViewUserLogin;
import br.com.cremepe.jeton.service.AutorizacaoService;
import br.com.cremepe.jeton.service.ParametrosService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

@Component
public class AutorizacaoInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AutorizacaoInterceptor.class);

    // Mapa de padrões de URI (prefixos) e permissões necessárias
    private static final Map<String, String> URI_PERMISSOES = Map.ofEntries(
            Map.entry("/atividades", NivelAcesso.NIVEL_ATIVIDADE_CONSELHAL),
            Map.entry("/comprovantes", NivelAcesso.NIVEL_COMPROVANTES),
            Map.entry("/jeton", NivelAcesso.NIVEL_JETONS),
            Map.entry("/gestoes", NivelAcesso.NIVEL_GESTAO_CONSELHEIROS),
            Map.entry("/gestao-conselheiros", NivelAcesso.NIVEL_GESTAO_CONSELHEIROS),
            Map.entry("/conselheiros", NivelAcesso.NIVEL_GESTAO_CONSELHEIROS),
            Map.entry("/portarias", NivelAcesso.NIVEL_PORTARIAS_RESOLUCOES),
            Map.entry("/resolucoes", NivelAcesso.NIVEL_PORTARIAS_RESOLUCOES),
            Map.entry("/regras", NivelAcesso.NIVEL_PORTARIAS_RESOLUCOES),
            Map.entry("/usuarios", NivelAcesso.NIVEL_USUARIOS),
            Map.entry("/logs", NivelAcesso.NIVEL_NIVEIS_ACESSO));

    private static final String[] ROTAS_PUBLICAS = {
            "/login", "/autenticar", "/sair", "/css", "/js", "/images", "/error"
    };

    private static final String[] ROTAS_BLOQUEIO_PERMITIDAS = {
            "/bloqueio", "/sair", "/login", "/autenticar", "/css", "/js", "/images", "/error"
    };

    @Autowired
    private AutorizacaoService autorizacaoService;
    @Autowired
    private ParametrosService parametrosService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String uri = request.getRequestURI();
        String method = request.getMethod();

        // Ignora favicon para evitar logs indesejados
        if (uri.endsWith("/favicon.ico") || uri.endsWith("/favicon.png")) {
            return true;
        }

        // 1. Rotas públicas (acesso livre)
        if (isRotaPublica(uri)) {
            return true;
        }

        HttpSession session = request.getSession();
        ViewUserLogin usuarioLogado = (ViewUserLogin) session.getAttribute("usuarioLogado");

        // 2. Verifica autenticação
        if (!verificarAutenticacao(usuarioLogado, request, response)) {
            return false;
        }

        // 3. Verifica bloqueio do sistema
        if (!verificarBloqueioSistema(usuarioLogado, uri, response)) {
            return false;
        }

        // 4. Verifica permissões de acesso (exceto portal do conselheiro)
        if (!verificarPermissoes(usuarioLogado, uri, method, response, request)) {
            return false;
        }

        return true;
    }

    private boolean isRotaPublica(String uri) {
        for (String publica : ROTAS_PUBLICAS) {
            if (uri.startsWith(publica)) {
                return true;
            }
        }
        return false;
    }

    private boolean verificarAutenticacao(ViewUserLogin usuarioLogado, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        if (usuarioLogado == null) {
            log.warn("Acesso não autenticado à URI: {} {}", request.getMethod(), request.getRequestURI());
            response.sendRedirect("/login");
            return false;
        }
        return true;
    }

    private boolean verificarBloqueioSistema(ViewUserLogin usuarioLogado, String uri,
            HttpServletResponse response) throws Exception {
        boolean sistemaBloqueado = parametrosService.isSistemaBloqueado();
        if (sistemaBloqueado && !isRotaPermitidaDuranteBloqueio(uri)) {
            boolean isSuperAdmin = usuarioLogado.hasPermissao(NivelAcesso.NIVEL_SUPER_ADMIN);
            boolean isBloqueio = usuarioLogado.hasPermissao(NivelAcesso.NIVEL_BLOQUEIO_SISTEMA);
            if (!isSuperAdmin && !isBloqueio) {
                log.warn("Acesso negado: sistema bloqueado. Usuário {} (ID={}) tentou acessar {}",
                        usuarioLogado.getNome(), usuarioLogado.getIdPessoa(), uri);
                response.sendRedirect("/bloqueio");
                return false;
            }
        }
        return true;
    }

    private boolean isRotaPermitidaDuranteBloqueio(String uri) {
        for (String permitida : ROTAS_BLOQUEIO_PERMITIDAS) {
            if (uri.startsWith(permitida)) {
                return true;
            }
        }
        return false;
    }

    private boolean verificarPermissoes(ViewUserLogin usuarioLogado, String uri, String method,
            HttpServletResponse response, HttpServletRequest request) throws Exception {

        // 1. Super Admin tem acesso total
        if (usuarioLogado.hasPermissao(NivelAcesso.NIVEL_SUPER_ADMIN)) {
            return true;
        }

        // 2. Portal do Conselheiro (rota exclusiva)
        if (uri.startsWith("/conselheiro")) {
            boolean isConselheiro = "C".equals(usuarioLogado.getInTipoPessoa());
            if (!isConselheiro) {
                registrarAcessoNegado(usuarioLogado, method, uri, "ACESSO_RESTRITO_CONSELHEIRO");
                response.sendRedirect("/index?erro=acesso_negado");
                return false;
            }
            return true;
        }

        // 3. Verifica se a rota exige alguma permissão específica
        String permissaoNecessaria = obterPermissaoPorUri(uri);
        if (permissaoNecessaria != null) {
            // Se o usuário tem a permissão, permite o acesso
            if (usuarioLogado.hasPermissao(permissaoNecessaria)) {
                return true;
            }
            // Caso contrário, registra e nega
            registrarAcessoNegado(usuarioLogado, method, uri, permissaoNecessaria);
            // Redireciona conforme o tipo de usuário
            if ("C".equals(usuarioLogado.getInTipoPessoa())) {
                response.sendRedirect("/conselheiro/dashboard");
            } else {
                response.sendRedirect("/index?erro=acesso_negado");
            }
            return false;
        }

        // 4. Rota sem permissão definida: permite para qualquer usuário autenticado
        return true;
    }

    private void registrarAcessoNegado(ViewUserLogin usuario, String method, String uri, String permissaoFaltante) {
        autorizacaoService.registrarAcessoNegado(
                usuario.getIdPessoa(),
                usuario.getNome(),
                method,
                uri,
                permissaoFaltante);
        log.warn("Acesso negado: usuário {} tentou acessar {} sem permissão {}",
                usuario.getNome(), uri, permissaoFaltante);
    }

    private String obterPermissaoPorUri(String uri) {
        for (Map.Entry<String, String> entry : URI_PERMISSOES.entrySet()) {
            if (uri.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }
}