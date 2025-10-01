package org.zkoss.dominial.security.authentication;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.zkoss.dominial.security.config.JwtProperties;
import org.zkoss.dominial.security.token.TokenException;
import org.zkoss.dominial.security.refresh_token.RefreshToken;
import org.zkoss.dominial.security.refresh_token.RefreshTokenRequest;
import org.zkoss.dominial.security.refresh_token.RefreshTokenResponse;
import org.zkoss.dominial.security.refresh_token.RefreshTokenService;


import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authenticationService;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticate(@Valid @RequestBody AuthRequest request) {
        AuthResponse authResponse = authenticationService.authenticate(request);

        // Crear refresh token
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(request.getUsername());

        // Construir respuesta con access token y refresh token
        RefreshTokenResponse response = RefreshTokenResponse.builder()
                .accessToken(authResponse.getToken())
                .refreshToken(refreshToken.getToken())
                .username(authResponse.getUsername())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith(jwtProperties.getTokenPrefix())) {
            String jwt = authHeader.substring(jwtProperties.getTokenPrefix().length());
            authenticationService.logout(jwt);
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            return refreshTokenService.findByToken(request.getRefreshToken())
                    .map(refreshTokenService::verifyExpiration)
                    .map(RefreshToken::getUsuario)
                    .map(usuario -> {
                        // Generar nuevo access token
                        String newAccessToken = authenticationService.generateTokenForUser(usuario);

                        logger.info("Token refrescado para el usuario: {}", usuario.getUsername());

                        return ResponseEntity.ok(RefreshTokenResponse.builder()
                                .accessToken(newAccessToken)
                                .refreshToken(request.getRefreshToken())
                                .username(usuario.getUsername())
                                .build());
                    })
                    .orElseThrow(() -> new TokenException("Refresh token no encontrado"));
        } catch (TokenException ex) {
            logger.error("Error al refrescar token: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAllSessions(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails != null) {
            authenticationService.revocarTodosLosTokens(userDetails.getUsername());
            logger.info("Todas las sesiones cerradas para el usuario: {}", userDetails.getUsername());
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/registro")
    public ResponseEntity<?> registro(@Valid @RequestBody RegistroRequest request) {
        logger.info("Recibida solicitud de registro para usuario: {}", request.getUsername());

        // Llamar al servicio de autenticación para registrar el usuario
        AuthResponse authResponse = authenticationService.registrarUsuario(request);

        // Verificar si el registro fue exitoso (si tiene token)
        if (authResponse.getToken() != null) {
            logger.info("Registro exitoso para el usuario: {}", request.getUsername());

            // Crear refresh token para el nuevo usuario
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(request.getUsername());

            // Construir respuesta con token de acceso y refresh token
            RefreshTokenResponse response = RefreshTokenResponse.builder()
                    .accessToken(authResponse.getToken())
                    .refreshToken(refreshToken.getToken())
                    .username(authResponse.getUsername())
                    .build();

            return ResponseEntity.ok(response);
        } else {
            // Si no hay token, el registro falló
            logger.warn("Registro fallido para el usuario: {}. Motivo: {}",
                    request.getUsername(), authResponse.getMessage());

            return ResponseEntity.badRequest().body(authResponse);
        }
    }
}