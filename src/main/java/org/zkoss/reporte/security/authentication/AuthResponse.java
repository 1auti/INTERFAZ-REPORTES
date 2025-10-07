package org.zkoss.reporte.security.authentication;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para las respuestas de autenticación
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    /**
     * Token JWT generado para el usuario autenticado
     */
    private String token;

    /**
     * Nombre de usuario autenticado
     */
    private String username;

    /**
     * Mensaje de respuesta (opcional)
     */
    private String message;

    /**
     * Constructor para crear una respuesta con solo el token
     * @param token Token JWT
     */
    public AuthResponse(String token) {
        this.token = token;
    }

    /**
     * Constructor para crear una respuesta con token y username
     * @param token Token JWT
     * @param username Nombre de usuario
     */
    public AuthResponse(String token, String username) {
        this.token = token;
        this.username = username;
    }
}