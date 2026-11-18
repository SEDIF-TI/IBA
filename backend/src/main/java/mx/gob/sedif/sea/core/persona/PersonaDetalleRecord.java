package mx.gob.sedif.sea.core.persona;

import java.time.LocalDate;

/**
 * Ficha completa del tutor.
 *
 * <p>Aqui la CURP va completa: quien abre la ficha necesita cotejarla contra el
 * documento. Por eso abrirla queda registrado en bitacora.
 */
public record PersonaDetalleRecord(
        Long id,
        String nombre,
        String apellidoPaterno,
        String apellidoMaterno,
        String nombreCompleto,
        String curp,
        Boolean identidadValidada,
        LocalDate fechaNacimiento,
        String sexoCurp,
        String entidadNacimiento,
        String entidadNacimientoNombre,
        String correo,
        String telefono,
        String telefonoWhatsapp,
        Integer municipioId,
        String municipio,
        String calle,
        String numeroExterior,
        String numeroInterior,
        String colonia,
        String codigoPostal,
        Long animalesACargo,
        // Versión del registro (bloqueo optimista); el formulario la reenvía al guardar.
        Long version) {

    public static PersonaDetalleRecord de(Persona persona, long animalesACargo) {
        return new PersonaDetalleRecord(
                persona.getId(),
                persona.getNombre(),
                persona.getApellidoPaterno(),
                persona.getApellidoMaterno(),
                persona.getNombreCompleto(),
                persona.getCurp(),
                persona.getIdentidadValidada(),
                persona.getFechaNacimiento(),
                persona.getSexoCurp(),
                persona.getEntidadNacimiento(),
                persona.getEntidadNacimiento() == null
                        ? null
                        : CurpUtil.nombreEntidad(persona.getEntidadNacimiento()).orElse(null),
                persona.getCorreo(),
                persona.getTelefono(),
                persona.getTelefonoWhatsapp(),
                persona.getMunicipio() == null ? null : persona.getMunicipio().getId(),
                persona.getMunicipio() == null ? null : persona.getMunicipio().getNombre(),
                persona.getCalle(),
                persona.getNumeroExterior(),
                persona.getNumeroInterior(),
                persona.getColonia(),
                persona.getCodigoPostal(),
                animalesACargo,
                persona.getVersion());
    }
}
