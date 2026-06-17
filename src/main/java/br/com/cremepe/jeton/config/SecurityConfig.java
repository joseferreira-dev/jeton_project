package br.com.cremepe.jeton.config;

import br.com.cremepe.jeton.security.CustomAuthenticationFailureHandler;
import br.com.cremepe.jeton.security.CustomAuthenticationSuccessHandler;
import br.com.cremepe.jeton.security.CustomLogoutSuccessHandler;
import br.com.cremepe.jeton.security.CustomUserDetailsService;
import br.com.cremepe.jeton.security.SistemaBloqueioFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final CustomAuthenticationSuccessHandler authenticationSuccessHandler;
    private final CustomAuthenticationFailureHandler authenticationFailureHandler;
    private final CustomLogoutSuccessHandler logoutSuccessHandler;
    private final SistemaBloqueioFilter sistemaBloqueioFilter;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService,
            CustomAuthenticationSuccessHandler authenticationSuccessHandler,
            CustomAuthenticationFailureHandler authenticationFailureHandler,
            CustomLogoutSuccessHandler logoutSuccessHandler,
            SistemaBloqueioFilter sistemaBloqueioFilter) {
        this.customUserDetailsService = customUserDetailsService;
        this.authenticationSuccessHandler = authenticationSuccessHandler;
        this.authenticationFailureHandler = authenticationFailureHandler;
        this.logoutSuccessHandler = logoutSuccessHandler;
        this.sistemaBloqueioFilter = sistemaBloqueioFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.png", "/favicon.ico").permitAll()
                        .requestMatchers("/login", "/autenticar", "/sair", "/bloqueio", "/bloqueio/status").permitAll()
                        .requestMatchers("/conselheiro/**").hasRole("CONSELHEIRO")
                        .requestMatchers("/", "/index", "/conselheiro/**").authenticated()
                        .anyRequest().authenticated())
                .addFilterBefore(sistemaBloqueioFilter, UsernamePasswordAuthenticationFilter.class)
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/autenticar")
                        .usernameParameter("cpf")
                        .passwordParameter("senha")
                        .successHandler(authenticationSuccessHandler)
                        .failureHandler(authenticationFailureHandler)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/sair")
                        .logoutSuccessHandler(logoutSuccessHandler)
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll())
                .sessionManagement(session -> session
                        .sessionFixation().migrateSession()
                        .maximumSessions(1)
                        .expiredUrl("/login?expired"));

        return http.build();
    }
}