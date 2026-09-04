package mx.gob.sedif.sea.core.persona;

import java.time.LocalDate;

/**
 * Tutor tal como se muestra en el listado.
 *
 * <p>La CURP va enmascarada: en un listado no hay motivo para verla completa.
 */
public record PersonaRecord(
        Long id,
        String nombreCompleto,
        String curpEnmascarada,
        Boolean identidadValidada,
        String correo,
        String telefono,
        String municipio,
        Long animalesACargo) {

    public static PersonaRecord de(Persona persona, long animalesACargo) {
        return new PersonaRecord(
                persona.getId(),
                persona.getNombreCompleto(),
                persona.getCurpEnmascarada(),
                persona.getIdentidadValidada(),
                persona.getCorreo(),
                persona.getTelefono(),
                persona.getMunicipio() == null ? null : persona.getMunicipio().getNombre(),
                animalesACargo);
    }
}
