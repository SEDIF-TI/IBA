package mx.gob.sedif.sea.security.admin;

import java.util.List;

import mx.gob.sedif.sea.util.validacion.Correo;
import mx.gob.sedif.sea.util.validacion.Telefono;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos para dar de alta o modificar una cuenta del personal.
 *
 * <p>No lleva contrasena a proposito: quien crea la cuenta no la elige. El
 * sistema genera una temporal y obliga a cambiarla en el primer acceso, para
 * que nadie mas que la propia persona conozca la definitiva.
 */
public record GuardarUsuarioRequest(

        @NotBlank(message = "Escribe el nombre de usuario con el que entrara al sistema")
        @Size(min = 4, max = 80, message = "El usuario debe tener entre 4 y 80 caracteres")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$",
                message = "El usuario solo admite letras, numeros, punto, guion y guion bajo. "
                        + "Sin espacios ni acentos.")
        String usuario,

        @NotBlank(message = "Escribe el nombre de la persona")
        @Size(max = 120, message = "El nombre no puede pasar de 120 caracteres")
        String nombre,

        @NotBlank(message = "Escribe el apellido paterno")
        @Size(max = 120, message = "El apellido paterno no puede pasar de 120 caracteres")
        String apellidoPaterno,

        @Size(max = 120, message = "El apellido materno no puede pasar de 120 caracteres")
        String apellidoMaterno,

        @NotBlank(message = "Escribe el correo institucional: es por donde se avisa "
                + "cualquier asunto de la cuenta")
        @Correo(message = "El correo no tiene un formato válido. Revisa que lleve arroba "
                + "y un dominio con terminación, por ejemplo nombre@puebla.gob.mx")
        String correo,

        @Telefono
        String telefono,

        Integer sedeId,

        @NotEmpty(message = "Asigna al menos un rol: sin rol la cuenta no puede hacer "
                + "nada dentro del sistema")
        List<Integer> rolesIds,

        // Versión que la persona tenía a la vista; obligatoria al modificar (bloqueo optimista).
        Long version) {
}
