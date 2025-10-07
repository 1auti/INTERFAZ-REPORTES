package org.zkoss.reporte.security.config;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Service;
import org.zkoss.reporte.security.token.TokenService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Service
@RequiredArgsConstructor
public class LogoutService implements LogoutHandler {

    private final TokenService tokenService;
    private final JwtProperties jwtProperties;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        // Desactivamos la funcionalidad de logout no haciendo nada
        // Los tokens permanecerán válidos hasta que expiren naturalmente

        // Código original comentado:
        /*
        final String authHeader = request.getHeader(jwtProperties.getHeaderName());
        final String jwt;

        if (authHeader == null || !authHeader.startsWith(jwtProperties.getTokenPrefix())) {
            return;
        }

        jwt = authHeader.substring(jwtProperties.getTokenPrefix().length());

        Optional<Token> token = tokenService.findByToken(jwt);

        if (token.isPresent()) {
            tokenService.marcarTokenComoRevocado(token.get());
            tokenService.marcarTokenComoExpirado(token.get());
        }
        */
    }
}