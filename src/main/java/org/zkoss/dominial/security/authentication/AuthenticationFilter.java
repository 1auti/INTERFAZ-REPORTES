package org.zkoss.dominial.security.authentication;

import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.zkoss.dominial.security.config.JwtProperties;
import org.zkoss.dominial.security.config.JwtService;
import org.zkoss.dominial.security.token.TokenService;

// Usamos javax.servlet en lugar de jakarta.servlet para compatibilidad con Java 8
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AuthenticationFilter extends OncePerRequestFilter {
    // Intercepta todas las peticiones para validar el token JWT
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final JwtProperties jwtProperties;
    private final TokenService tokenService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        // Extrae el header de autorización
        final String authHeader = request.getHeader(jwtProperties.getHeaderName());
        final String jwt;
        final String username;

        // Si no hay token o no tiene el formato correcto, continúa la cadena de filtros
        if (authHeader == null || !authHeader.startsWith(jwtProperties.getTokenPrefix())) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extrae el token JWT
        jwt = authHeader.substring(jwtProperties.getTokenPrefix().length());
        username = jwtService.extractUsername(jwt);

        // Si hay un usuario y no está autenticado, verifica el token
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            // Verificación doble: el token debe ser válido (firma/exp) y estar en la BD como no revocado
            boolean isTokenValid = tokenService.findByToken(jwt)
                    .map(token -> !token.isRevocado() && !token.isExpirado())
                    .orElse(false);

            if (jwtService.isTokenValid(jwt, userDetails) && isTokenValid) {
                // Crea un token de autenticación y lo establece en el contexto de seguridad
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        // Continúa con la cadena de filtros
        filterChain.doFilter(request, response);
    }
}