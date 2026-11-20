package com.hemotrack.backend.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Relogio da aplicacao. Os services recebem o Clock em vez de chamar Instant.now() direto,
 * para os testes poderem usar um horario fixo (Clock.fixed).
 */
@Configuration
public class RelogioConfig {

    @Bean
    public Clock relogio() {
        return Clock.systemUTC();
    }
}
