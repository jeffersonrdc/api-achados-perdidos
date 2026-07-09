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
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/portal/eventos/*/meus-claims").hasRole("PARTICIPANTE")
                        .requestMatchers(HttpMethod.GET, "/api/v1/portal/eventos", "/api/v1/portal/eventos/*/itens").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/portal/eventos/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/portal/eventos/*/claims", "/api/v1/portal/eventos/*/claims/item",
                                "/api/v1/portal/eventos/*/criancas", "/api/v1/portal/eventos/*/criancas/responsaveis",
                                "/api/v1/portal/auth/registro").permitAll()
                        .requestMatchers("/api/v1/portal/**").hasRole("PARTICIPANTE")
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/v1/dashboard/**").hasAnyRole("ADMIN", "OPERADOR", "ATENDENTE", "CONSULTA")
                        .requestMatchers(HttpMethod.GET, "/api/v1/status-itens", "/api/v1/status-itens/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/categorias", "/api/v1/categorias/**").authenticated()
                        .requestMatchers("/api/v1/usuarios", "/api/v1/usuarios/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/eventos", "/api/v1/eventos/**").hasAnyRole("ADMIN", "OPERADOR", "ATENDENTE")
                        .requestMatchers("/api/v1/itens", "/api/v1/itens/**").hasAnyRole("ADMIN", "OPERADOR", "ATENDENTE")
                        .requestMatchers("/api/v1/claims", "/api/v1/claims/**").hasAnyRole("ADMIN", "ATENDENTE")
                        .requestMatchers("/api/v1/depositos", "/api/v1/depositos/**").hasAnyRole("ADMIN", "OPERADOR", "ATENDENTE", "CONSULTA")
                        .requestMatchers("/api/v1/localizacoes", "/api/v1/localizacoes/**").hasAnyRole("ADMIN", "OPERADOR", "ATENDENTE", "CONSULTA")
                        .requestMatchers("/api/v1/devolucoes", "/api/v1/devolucoes/**").hasAnyRole("ADMIN", "OPERADOR", "ATENDENTE", "CONSULTA")
                        .requestMatchers(HttpMethod.POST, "/api/v1/criancas", "/api/v1/criancas/**").hasAnyRole("ADMIN", "OPERADOR", "ATENDENTE")
                        .requestMatchers("/api/v1/criancas", "/api/v1/criancas/**").hasAnyRole("ADMIN", "OPERADOR", "ATENDENTE", "CONSULTA")
                        .requestMatchers("/api/v1/arquivos", "/api/v1/arquivos/**").hasAnyRole("ADMIN", "OPERADOR", "ATENDENTE", "CONSULTA")
                        .requestMatchers("/api/v1/workflow", "/api/v1/workflow/**").hasAnyRole("ADMIN", "OPERADOR", "ATENDENTE", "CONSULTA")
                        .requestMatchers("/api/v1/sla", "/api/v1/sla/**").hasAnyRole("ADMIN", "OPERADOR", "ATENDENTE", "CONSULTA")
                        .requestMatchers("/api/v1/categorias/campos", "/api/v1/categorias/campos/**").hasAnyRole("ADMIN", "OPERADOR", "ATENDENTE", "CONSULTA")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/categorias/campos").hasRole("ADMIN")
                        .requestMatchers("/api/v1/itens/campos", "/api/v1/itens/campos/**").hasAnyRole("ADMIN", "OPERADOR", "ATENDENTE", "CONSULTA")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/itens/campos").hasAnyRole("ADMIN", "OPERADOR")
                        .requestMatchers("/api/v1/auditoria", "/api/v1/auditoria/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/claims/validacoes", "/api/v1/claims/validacoes/**").hasAnyRole("ADMIN", "ATENDENTE", "CONSULTA")
                        .requestMatchers(HttpMethod.POST, "/api/v1/claims/validacoes").hasAnyRole("ADMIN", "ATENDENTE")
                        .requestMatchers("/api/v1/contatos", "/api/v1/contatos/**").hasAnyRole("ADMIN", "ATENDENTE", "CONSULTA")
                        .requestMatchers("/api/v1/lacres", "/api/v1/lacres/**").hasAnyRole("ADMIN", "OPERADOR", "ATENDENTE", "CONSULTA")
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
