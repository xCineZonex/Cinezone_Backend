package com.cinezone.demo.config;

import com.cinezone.demo.security.JwtAuthenticationFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    // Token secreto que solo conoce Grafana Alloy — inyectado como variable de entorno
    @Value("${ACTUATOR_TOKEN:changeme}")
    private String actuatorToken;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        // 1. Rutas Públicas (Login, Registro y Documentación)
                        .requestMatchers(
                                "/actuator/health",   // Cloud Run health check (siempre público)
                                "/api/v1/auth/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/error"
                        ).permitAll()

                        // 2. Cartelera pública, sedes, funciones, catálogos y archivos estáticos
                        .requestMatchers("/api/v1/peliculas/**", "/api/v1/sedes/**", "/api/v1/funciones/**", "/api/v1/public/**", "/api/v1/uploads/**").permitAll()

                        // 3. Rutas de Administrador y Operaciones
                        .requestMatchers("/api/v1/admin/**").hasAnyRole("SUPER_ADMIN", "ADMIN_SEDE", "JEFE_SALA")
                        .requestMatchers("/api/v1/taquilla/**", "/api/v1/dulceria/**", "/api/v1/cocina/**", "/api/v1/portero/**", "/api/v1/operaciones/**").hasAnyRole("STAFF", "SUPER_ADMIN", "ADMIN_SEDE", "JEFE_SALA", "PORTERO", "TAQUILLA", "DULCERIA")

                        // 4. Todo lo demás requiere estar logueado
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                // Filtro de token para /actuator/prometheus — va ANTES del filtro JWT
                .addFilterBefore(actuatorTokenFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Filtro que protege /actuator/prometheus con un Bearer token estático.
     * Solo Grafana Alloy (que conoce ACTUATOR_TOKEN) puede acceder.
     * Cualquier otra petición sin el token recibe 401.
     */
    @Bean
    public OncePerRequestFilter actuatorTokenFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain) throws ServletException, IOException {
                if ("/actuator/prometheus".equals(request.getRequestURI())) {
                    String authHeader = request.getHeader("Authorization");
                    String expected   = "Bearer " + actuatorToken;
                    if (authHeader == null || !authHeader.equals(expected)) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.getWriter().write("Unauthorized");
                        return;
                    }
                }
                filterChain.doFilter(request, response);
            }
        };
    }

    @org.springframework.beans.factory.annotation.Value("${cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}