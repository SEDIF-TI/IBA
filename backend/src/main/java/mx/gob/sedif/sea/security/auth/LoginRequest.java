package mx.gob.sedif.sea.security.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Credenciales de acceso al panel administrativo.
 *
 * @param usuario  nombre de usuario
 * @param password contrasena en claro; solo existe durante la peticion y nunca
 *                 se registra en bitacora
 */
public record LoginRequest(
        @NotBlank(message = "El usuario es obligatorio")
        @Size(max = 80)
        String usuario,

        @NotBlank(message = "La contrasena es obligatoria")
        @Size(max = 200)
        String password) {
}
