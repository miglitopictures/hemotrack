package com.hemotrack.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration 
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RespostaNaoAutenticado respostaNaoAutanticado;
    private final RespostaAcessoNegado respostaAcessoNegado;
    private final InstituicaoAprovada instituicaoAprovada;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          RespostaNaoAutenticado respostaNaoAutanticado,
                          RespostaAcessoNegado respostaAcessoNegado,
                          InstituicaoAprovada instituicaoAprovada) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.respostaNaoAutanticado = respostaNaoAutanticado;
        this.respostaAcessoNegado = respostaAcessoNegado;
        this.instituicaoAprovada = instituicaoAprovada;
    }
    
    @Bean
    @Order(1)
    public SecurityFilterChain gerenciamento(HttpSecurity http,
            @Value("${management.server.port:8082}") int porta, Environment environment) throws Exception {
        // A rede Docker alcanca 8082; a porta publica da API nao libera o Actuator.
        return http.securityMatcher(request -> request.getLocalPort() == environment.getProperty("local.management.port", Integer.class, porta))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(regras -> regras
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().denyAll())
            .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean 
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable());
        http.cors(Customizer.withDefaults());
        http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        

        http.sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.exceptionHandling(erros -> {
            erros.authenticationEntryPoint(respostaNaoAutanticado);
            erros.accessDeniedHandler(respostaAcessoNegado);
        });

        http.authorizeHttpRequests(regras -> regras
            .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
            .requestMatchers(HttpMethod.POST, "/instituicoes").permitAll()
            .requestMatchers("/h2-console/**").permitAll()
            .requestMatchers("/health").permitAll()
            .requestMatchers("/actuator/**").denyAll()
            .requestMatchers("/error").permitAll()
            .requestMatchers(HttpMethod.PATCH, "/instituicoes/*/aprovar").hasRole("ADMIN_SISTEMA")
            .requestMatchers(HttpMethod.POST, "/instituicoes/*/usuarios").hasRole("ADMIN_INSTITUICAO")
            .requestMatchers(HttpMethod.PATCH, "/instituicoes/*/usuarios/*").hasRole("ADMIN_INSTITUICAO")
            .requestMatchers("/requisicoes/**").access(instituicaoAprovada)
            .anyRequest().authenticated());
        
        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
}
