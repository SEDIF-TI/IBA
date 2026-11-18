package mx.gob.sedif.sea.core.persona;


/**
 * Tutor tal como se muestra en el listado.
 *
 * <p>La CURP, el teléfono y el correo van enmascarados: en un listado basta con reconocer a la
 * persona; los datos completos están en la ficha del tutor, cuya consulta queda en la bitácora.
 * Área: Núcleo compartido (padrón). Lógica: listado del padrón de tutores.
 *
 * @param id                identificador del tutor
 * @param nombreCompleto    nombre para reconocerlo en la lista
 * @param curpEnmascarada   CURP con el centro oculto; nula si no la tiene
 * @param identidadValidada si el personal ya cotejó la CURP con el documento
 * @param correo            correo enmascarado (inicial y dominio); nulo si no tiene
 * @param telefono          teléfono enmascarado (últimos 4 dígitos); nulo si no tiene
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
                enmascararCorreo(persona.getCorreo()),
                enmascararTelefono(persona.getTelefono()),
                persona.getMunicipio() == null ? null : persona.getMunicipio().getNombre(),
                animalesACargo);
    }

    /**
     * Deja ver solo los últimos cuatro dígitos del teléfono.
     *
     * @param telefono teléfono completo; puede ser nulo
     * @return por ejemplo "••••••4567"; nulo si no hay teléfono
     */
    static String enmascararTelefono(String telefono) {
        if (telefono == null || telefono.isBlank()) {
            return null;
        }
        String limpio = telefono.trim();
        return limpio.length() <= 4 ? "••••" : "•".repeat(limpio.length() - 4) + limpio.substring(limpio.length() - 4);
    }

    /**
     * Deja ver la primera letra del usuario y el dominio completo del correo.
     *
     * @param correo correo completo; puede ser nulo
     * @return por ejemplo "j•••@gmail.com"; nulo si no hay correo
     */
    static String enmascararCorreo(String correo) {
        if (correo == null || correo.isBlank()) {
            return null;
        }
        int arroba = correo.indexOf('@');
        if (arroba <= 0) {
            return "•••";
        }
        return correo.charAt(0) + "•••" + correo.substring(arroba);
    }
}
