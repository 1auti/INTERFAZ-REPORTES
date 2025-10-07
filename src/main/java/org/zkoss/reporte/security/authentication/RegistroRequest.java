package org.zkoss.reporte.security.authentication;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * DTO para solicitudes de registro de usuario
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegistroRequest {

    /**
     * Nombre de usuario para el registro
     * - No puede estar vacío
     * - Debe tener entre 4 y 50 caracteres
     * - Solo puede contener letras, números, puntos, guiones y guiones bajos
     */
    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(min = 4, max = 50, message = "El nombre de usuario debe tener entre 4 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El nombre de usuario solo puede contener letras, números, puntos, guiones y guiones bajos")
    private String username;

    /**
     * Contraseña para el registro
     * - No puede estar vacía
     * - Debe tener entre 6 y 100 caracteres
     */
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, max = 100, message = "La contraseña debe tener entre 6 y 100 caracteres")
    private String password;

    /**
     * Correo electrónico del usuario
     * - No puede estar vacío
     * - Debe ser un correo electrónico válido
     */
    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato del correo electrónico no es válido")
    private String email;

    private String rolSolicitado; // "PROYECTOS", "DATOS", "ADMIN"

}