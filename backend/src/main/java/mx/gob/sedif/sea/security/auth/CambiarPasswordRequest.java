package mx.gob.sedif.sea.security.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cambio de contrasena de la persona en sesion.
 *
 * @param passwordActual contrasena vigente, para confirmar que es quien dice ser
 * @param passwordNueva  contrasena nueva
 */
public record CambiarPasswordRequest(
        @NotBlank(message = "La contraseña actual es obligatoria")
        String passwordActual,

        @NotBlank(message = "La contraseña nueva es obligatoria")
        @Size(min = 12, message = "La contraseña debe tener al menos 12 caracteres")
        @Size(max = 200)
        // Se exige variedad para que no baste con una palabra del diccionario.
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "La contraseña debe incluir mayusculas, minusculas y numeros")
        String passwordNueva) {
}
