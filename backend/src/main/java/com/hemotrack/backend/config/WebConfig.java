package com.hemotrack.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sem isso, o navegador bloqueia as chamadas que vêm do front (que roda numa
 * porta diferente da do back), mesmo que o back esteja funcionando
 * perfeitamente. Isso se chama CORS (Cross-Origin Resource Sharing) e é uma
 * proteção do próprio navegador, não um bug.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // Ajuste aqui se o seu front rodar em outra porta. Depois de
                // rodar "npm run dev", veja no terminal qual endereço
                // aparece (geralmente localhost:5173 ou localhost:3000) e
                // deixe as duas portas na lista, para não precisar mexer
                // aqui de novo.
                .allowedOrigins("http://localhost:8081", "http://localhost:5173", "http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH")
                .allowedHeaders("*");
    }
}