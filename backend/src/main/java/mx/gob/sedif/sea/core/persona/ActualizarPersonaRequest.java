package mx.gob.sedif.sea.core.persona;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos de contacto y domicilio del tutor.
 *
 * <p>El nombre no se edita aqui: cambiarlo equivaldria a decir que el tramite
 * es de otra persona. Para eso se corrige el tramite completo.
 *
 * @param correo           correo de contacto
 * @param telefono         telefono de contacto
 * @param telefonoWhatsapp numero de WhatsApp
 * @param municipioId      municipio de domicilio
 * @param calle            calle
 * @param numeroExterior   numero exterior
 * @param numeroInterior   numero interior
 * @param colonia          colonia
 * @param codigoPostal     codigo postal
 */
public record ActualizarPersonaRequest(
        @Email(message = "El correo no tiene un formato valido. Revisa que incluya "
                + "la arroba y el dominio.")
        @Size(max = 150)
        String correo,

        @Pattern(regexp = "^$|^[0-9]{10}$",
                 message = "El telefono debe tener exactamente 10 digitos, sin espacios "
                         + "ni guiones")
        String telefono,

        @Pattern(regexp = "^$|^[0-9]{10}$",
                 message = "El numero de WhatsApp debe tener exactamente 10 digitos")
        String telefonoWhatsapp,

        Integer municipioId,

        @Size(max = 200) String calle,
        @Size(max = 20) String numeroExterior,
        @Size(max = 20) String numeroInterior,
        @Size(max = 150) String colonia,

        @Pattern(regexp = "^$|^[0-9]{5}$",
                 message = "El codigo postal debe tener 5 digitos")
        String codigoPostal) {
}
