package br.com.achadosperdidos.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableConfigurationProperties(AppCorsProperties.class)
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(AppCorsProperties props) {
        CorsConfiguration cors = new CorsConfiguration();
        if (props.isEnabled()) {
            List<String> origins = clean(props.getAllowedOrigins());
            List<String> patterns = clean(props.getAllowedOriginPatterns());
            if (!origins.isEmpty()) {
                cors.setAllowedOrigins(origins);
            }
            if (!patterns.isEmpty()) {
                cors.setAllowedOriginPatterns(patterns);
            }
            cors.setAllowedMethods(clean(props.getAllowedMethods()));
            List<String> headers = clean(props.getAllowedHeaders());
            cors.setAllowedHeaders(headers.isEmpty() ? List.of("*") : headers);
            List<String> exposed = clean(props.getExposedHeaders());
            if (!exposed.isEmpty()) {
                cors.setExposedHeaders(exposed);
            }
            cors.setAllowCredentials(props.isAllowCredentials());
            cors.setMaxAge(props.getMaxAgeSeconds());
        }
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(props.getPathPattern(), cors);
        return source;
    }

    private static List<String> clean(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
    }
}
