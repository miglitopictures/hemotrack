package com.hemotrack.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sem isso, o navegador bloqueia as chamadas que vêm do front (que roda numa
 * porta diferente da do back), mesmo que o back esteja funcionando
 * perfeitamente. Isso se chama CORS (Cross-Origin Resource Sharing) e é uma
 * proteção do próprio navegador, não um bug.
 *
 * As origens liberadas vêm da propriedade hemotrack.cors.origens
 * (variável de ambiente CORS_ALLOWED_ORIGINS, separada por vírgula).
 * Sem a variável, valem as portas de desenvolvimento local (8081, 5173, 3000).
 * Em produção, a variável recebe a URL do front publicado.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String[] origensPermitidas;

    public WebConfig(@Value("${hemotrack.cors.origens}") String[] origensPermitidas) {
        this.origensPermitidas = origensPermitidas;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(origensPermitidas)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH")
                .allowedHeaders("*");
    }
}
