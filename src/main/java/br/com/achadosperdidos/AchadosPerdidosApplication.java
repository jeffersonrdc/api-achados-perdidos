package br.com.achadosperdidos;

import br.com.achadosperdidos.config.TimeConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class AchadosPerdidosApplication {

    public static void main(String[] args) {
        // Garante DATETIME/LocalDateTime em horário de Brasília (evita +3h com JVM em UTC).
        TimeZone.setDefault(TimeZone.getTimeZone(TimeConfig.ZONE_BRASILIA));
        SpringApplication.run(AchadosPerdidosApplication.class, args);
    }
}
