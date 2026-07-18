package br.com.achadosperdidos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Fuso horário oficial da aplicação: Brasília (America/Sao_Paulo, UTC−3). */
@Configuration
public class TimeConfig {
    public static final ZoneId ZONE_BRASILIA = ZoneId.of("America/Sao_Paulo");

    /** Agora em horário de Brasília (wall-clock para colunas DATETIME). */
    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE_BRASILIA);
    }

    @Bean
    public ZoneId appZoneId() {
        return ZONE_BRASILIA;
    }

    @Bean
    @Primary
    public Clock appClock(ZoneId appZoneId) {
        return Clock.system(appZoneId);
    }
}
