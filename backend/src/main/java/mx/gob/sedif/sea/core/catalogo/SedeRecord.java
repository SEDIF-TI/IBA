package mx.gob.sedif.sea.core.catalogo;

/**
 * Punto de entrega con sus datos de contacto.
 *
 * <p>Estos datos se insertan en el correo de aprobacion, de modo que cambiar de
 * domicilio no obliga a tocar las plantillas ni el codigo.
 *
 * @param id              identificador
 * @param clave           referencia estable
 * @param nombre          nombre de la sede
 * @param domicilio       direccion completa
 * @param municipioId     identificador del municipio
 * @param municipioNombre nombre del municipio
 * @param horario         horario de atencion
 * @param telefono        telefono de contacto
 * @param correo          correo de contacto
 * @param activo          si sigue disponible
 * @param version  versión del registro para el bloqueo optimista
 */
public record SedeRecord(
        Integer id,
        String clave,
        String nombre,
        String domicilio,
        Integer municipioId,
        String municipioNombre,
        String horario,
        String telefono,
        String correo,
        Boolean activo,
        Long version) {

    public static SedeRecord de(Sede sede) {
        return new SedeRecord(
                sede.getId(),
                sede.getClave(),
                sede.getNombre(),
                sede.getDomicilio(),
                sede.getMunicipio() == null ? null : sede.getMunicipio().getId(),
                sede.getMunicipio() == null ? null : sede.getMunicipio().getNombre(),
                sede.getHorario(),
                sede.getTelefono(),
                sede.getCorreo(),
                sede.getActivo(),
                sede.getVersion());
    }
}
