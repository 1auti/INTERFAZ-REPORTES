package org.zkoss.reporte.security.refresh_token;



import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.zkoss.reporte.security.token.TokenException;
import org.zkoss.reporte.security.user.Usuario;
import org.zkoss.reporte.security.user.UsuarioRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final UsuarioRepository usuarioRepository;

    @Value("${app.jwt.refresh-token.expiration}")
    private Long refreshTokenDurationMs;

    /**
     * Crea un nuevo refresh token para un usuario
     */
    public RefreshToken createRefreshToken(String username) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con username: " + username));

        RefreshToken refreshToken = RefreshToken.builder()
                .usuario(usuario)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                .build();

        refreshToken = refreshTokenRepository.save(refreshToken);
        logger.info("Refresh token creado para el usuario: {}", username);

        return refreshToken;
    }

    /**
     * Verifica un refresh token
     */
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isRevocado()) {
            logger.warn("Intento de usar un refresh token revocado: {}", token.getToken());
            refreshTokenRepository.delete(token);
            throw new TokenException.TokenRevokedException();
        }

        if (token.getExpiryDate().isBefore(Instant.now())) {
            logger.warn("Intento de usar un refresh token expirado: {}", token.getToken());
            refreshTokenRepository.delete(token);
            throw new TokenException.TokenExpiredException();
        }

        return token;
    }

    /**
     * Busca un refresh token por su valor
     */
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    /**
     * Revoca un refresh token específico
     */
    @Transactional
    public void revocarToken(RefreshToken token) {
        logger.info("Revocando refresh token: {}", token.getToken());
        token.setRevocado(true);
        refreshTokenRepository.save(token);
    }

    /**
     * Revoca todos los refresh tokens de un usuario
     */
    @Transactional
    public void revocarTodosLosTokens(Usuario usuario) {
        logger.info("Revocando todos los refresh tokens para el usuario: {}", usuario.getUsername());
        refreshTokenRepository.revokeAllByUsuarioId(usuario.getId());
    }

    /**
     * Limpia tokens expirados (puede ejecutarse periódicamente)
     */
    @Transactional
    public void limpiarTokensExpirados() {
        logger.info("Ejecutando limpieza de refresh tokens expirados");
        refreshTokenRepository.deleteAllExpiredTokens();
    }

    /**
     * Obtiene todos los refresh tokens activos de un usuario
     */
    public List<RefreshToken> getActiveTokensByUsuario(Usuario usuario) {
        return refreshTokenRepository.findByUsuarioAndRevocadoFalse(usuario);
    }
}
