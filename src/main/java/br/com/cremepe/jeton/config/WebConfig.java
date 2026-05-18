package br.com.cremepe.jeton.config;

import br.com.cremepe.jeton.seguranca.AutorizacaoInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AutorizacaoInterceptor autorizacaoInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Aplica o interceptor a todas as rotas do sistema
        registry.addInterceptor(autorizacaoInterceptor).addPathPatterns("/**");
    }
}