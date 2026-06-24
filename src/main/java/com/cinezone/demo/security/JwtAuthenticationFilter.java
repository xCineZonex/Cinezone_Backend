package com.cinezone.demo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // 1. Si no hay token o no empieza con "Bearer ", lo dejamos pasar
        // (Spring Security lo bloqueará más adelante si la ruta es privada)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Extraemos el token quitando la palabra "Bearer " (7 caracteres)
        jwt = authHeader.substring(7);

        try {
            // 3. Extraemos el correo usando nuestra clase JwtService
            userEmail = jwtService.extractUsername(jwt);

            // 4. Si encontramos un correo y el usuario no está ya autenticado en este hilo
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Buscamos al usuario en la BD
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                com.cinezone.demo.model.entity.User dbUser = (com.cinezone.demo.model.entity.User) userDetails;

                // 5. Validamos que el token sea correcto y no haya expirado
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    
                    // Single Session Validation (Concurrency Check)
                    String tokenSession = jwtService.extractClaim(jwt, claims -> claims.get("sessionToken", String.class));
                    if (dbUser.getSessionToken() != null && !dbUser.getSessionToken().equals(tokenSession)) {
                        // El token de sesión no coincide (alguien más inició sesión en otra parte)
                        filterChain.doFilter(request, response);
                        return; // Lo tratamos como token inválido
                    }

                    // 6. Creamos el objeto de autenticación con sus roles
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 7. Guardamos la sesión en el contexto de Spring
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Si el token expira o es inválido, simplemente no autenticamos
            // (El usuario seguirá como "anónimo" y podrá acceder a las rutas públicas)
        }

        // Continuamos con el ciclo de la petición
        filterChain.doFilter(request, response);
    }
}