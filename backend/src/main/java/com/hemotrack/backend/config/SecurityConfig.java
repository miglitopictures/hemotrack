package com.hemotrack.backend.config;

import org.springframework.context.annotation.Bean;
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

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, RespostaNaoAutenticado respostaNaoAutanticado, RespostaAcessoNegado respostaAcessoNegado) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.respostaNaoAutanticado = respostaNaoAutanticado;
        this.respostaAcessoNegado = respostaAcessoNegado;
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
            .requestMatchers("/actuator/**").permitAll()
            .requestMatchers("/error").permitAll()
            .requestMatchers(HttpMethod.PATCH, "/instituicoes/*/aprovar").hasRole("ADMIN_SISTEMA")
            .anyRequest().authenticated());
        
        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
}
