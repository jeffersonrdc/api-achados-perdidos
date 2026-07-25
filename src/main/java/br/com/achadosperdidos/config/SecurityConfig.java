package br.com.achadosperdidos.config;

import br.com.achadosperdidos.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Map;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                // Headers de segurança (A05 / relatório XPTO M-01, M-02, M-05):
                //  - nosniff (M-04 defesa em profundidade)
                //  - anti-clickjacking via frame-ancestors 'none' + X-Frame-Options DENY (M-02)
                //  - HSTS 1 ano com includeSubDomains (M-05)
                //  - Content-Security-Policy (M-01): elimina o alerta ZAP 10038 e adiciona
                //    contenção de XSS. É uma API JSON, então em produção (Swagger desligado por
                //    SPRINGDOC_ENABLED=false) o navegador não deve carregar nenhum recurso a
                //    partir das respostas. As diretivas 'unsafe-inline' em script/style existem
                //    apenas para a UI do Swagger (ambiente de dev), que injeta assets inline.
                //  - Referrer-Policy estrita.
                .headers(headers -> headers
                        .contentTypeOptions(Customizer.withDefaults())
                        .frameOptions(frame -> frame.deny())
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; "
                                        + "base-uri 'self'; "
                                        + "form-action 'self'; "
                                        + "frame-ancestors 'none'; "
                                        + "object-src 'none'; "
                                        + "script-src 'self' 'unsafe-inline'; "
                                        + "style-src 'self' 'unsafe-inline'; "
                                        + "img-src 'self' data:; "
                                        + "font-src 'self' data:; "
                                        + "connect-src 'self'"))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        .referrerPolicy(ref -> ref.policy(
                                org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/portal/eventos/*/meus-claims").hasRole("PARTICIPANTE")
                        // IDs assinados contêm '.' — usar `/**` (PathPattern/`*` de segmento falha).
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/portal/eventos",
                                "/api/v1/portal/eventos/**",
                                "/api/v1/portal/status",
                                "/api/v1/portal/metricas",
                                "/api/v1/portal/contatos",
                                "/api/v1/portal/arquivos/**",
                                "/api/v1/portal/categorias",
                                "/api/v1/portal/categorias/**",
                                "/api/v1/portal/subcategorias/**",
                                "/api/v1/portal/marcas",
                                "/api/v1/portal/modelos",
                                "/api/v1/portal/respostas/**").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/portal/eventos/**",
                                "/api/v1/portal/contato",
                                "/api/v1/portal/auth/registro",
                                "/api/v1/portal/respostas/**").permitAll()
                        .requestMatchers("/api/v1/portal/**").hasRole("PARTICIPANTE")
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        // Health check do container/orquestrador. Apenas /actuator/health é
                        // liberado (sem detalhes — ver application.properties); nenhum outro
                        // endpoint de management é exposto.
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        // Autorizacao fina por permissao (modulo.acao) e feita nos controllers
                        // via @PreAuthorize("@authz.pode('...')"). Aqui basta exigir autenticacao.
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            Map<String, Object> body = Map.of(
                    "status", 401,
                    "error", "Não autorizado",
                    "message", "Token JWT ausente ou inválido.",
                    "path", request.getRequestURI()
            );
            new ObjectMapper().writeValue(response.getOutputStream(), body);
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            Map<String, Object> body = Map.of(
                    "status", 403,
                    "error", "Proibido",
                    "message", "Você não tem permissão para acessar este recurso.",
                    "path", request.getRequestURI()
            );
            new ObjectMapper().writeValue(response.getOutputStream(), body);
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
