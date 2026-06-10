package br.com.cremepe.jeton.security;

import br.com.cremepe.jeton.domain.ViewUserLogin;
import br.com.cremepe.jeton.service.RegrasConjuntasService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private static final Logger log = LoggerFactory.getLogger(RegrasConjuntasService.class);

    private final ViewUserLogin viewUserLogin;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(ViewUserLogin viewUserLogin) {
        this.viewUserLogin = viewUserLogin;
        this.authorities = extractAuthorities(viewUserLogin);
    }

    private Collection<? extends GrantedAuthority> extractAuthorities(ViewUserLogin user) {
        List<GrantedAuthority> list = new ArrayList<>();
        // Adiciona as permissões padrão (letras)
        if (user.getPermissoes() != null && !user.getPermissoes().isBlank()) {
            for (String perm : user.getPermissoes().split(",")) {
                list.add(new SimpleGrantedAuthority(perm.trim()));
            }
        }
        // Adiciona papel implícito de conselheiro se for do tipo C
        if ("C".equals(user.getInTipoPessoa())) {
            list.add(new SimpleGrantedAuthority("ROLE_CONSELHEIRO"));
        }
        // Adiciona papel de funcionário (útil para regras gerais)
        if ("F".equals(user.getInTipoPessoa())) {
            list.add(new SimpleGrantedAuthority("ROLE_FUNCIONARIO"));
        }
        return list;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return viewUserLogin.getSenha();
    }

    @Override
    public String getUsername() {
        return viewUserLogin.getCpf();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        // A view já filtra apenas usuários com inSituacao = 'A'
        return true;
    }

    public ViewUserLogin getViewUserLogin() {
        return viewUserLogin;
    }
}