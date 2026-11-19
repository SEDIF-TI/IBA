package mx.gob.sedif.sea.core.persona;

import mx.gob.sedif.sea.util.validacion.CodigoPostal;
import mx.gob.sedif.sea.util.validacion.Correo;
import mx.gob.sedif.sea.util.validacion.Telefono;

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
 * @param version  versión del registro para el bloqueo optimista; obligatoria al modificar y
 *                 debe ser la que la persona tenía a la vista
 */
public record ActualizarPersonaRequest(
        @Correo
        String correo,

        @Telefono
        String telefono,

        @Telefono(message = "El número de WhatsApp debe tener exactamente 10 dígitos")
        String telefonoWhatsapp,

        Integer municipioId,

        @Size(max = 200) String calle,
        @Size(max = 20) String numeroExterior,
        @Size(max = 20) String numeroInterior,
        @Size(max = 150) String colonia,

        @CodigoPostal
        String codigoPostal,

        // Versión que la persona tenía a la vista; obligatoria al modificar (bloqueo optimista).
        Long version) {
}
