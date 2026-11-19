package mx.gob.sedif.sea.core.persona;


/**
 * Tutor tal como se muestra en el listado.
 *
 * <p>La CURP va enmascarada en el listado; el correo y el teléfono se muestran completos porque el
 * personal los coteja contra la documentación (decisión del responsable, 18-11-2026). La ficha del
 * tutor muestra todo y su consulta queda en la bitácora.
 * Área: Núcleo compartido (padrón). Lógica: listado del padrón de tutores.
 *
 * @param id                identificador del tutor
 * @param nombreCompleto    nombre para reconocerlo en la lista
 * @param curpEnmascarada   CURP con el centro oculto; nula si no la tiene
 * @param identidadValidada si el personal ya cotejó la CURP con el documento
 * @param correo            correo completo; nulo si no tiene
 * @param telefono          teléfono completo; nulo si no tiene
 * @param municipio         municipio del domicilio; nulo si no se capturó
 * @param animalesACargo    número de animales con titularidad vigente
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
