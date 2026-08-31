package mx.gob.sedif.sea.core.catalogo;

/**
 * Municipio con su clave geoestadistica.
 *
 * @param id          identificador
 * @param clave       referencia estable
 * @param nombre      nombre oficial, con su ortografia
 * @param claveInegi  clave INEGI de cinco digitos
 * @param orden       posicion de despliegue
 * @param activo      si sigue disponible
 */
public record MunicipioRecord(
        Integer id,
        String clave,
        String nombre,
        String claveInegi,
        Integer orden,
        Boolean activo) {

    public static MunicipioRecord de(Municipio municipio) {
        return new MunicipioRecord(
                municipio.getId(),
                municipio.getClave(),
                municipio.getNombre(),
                municipio.getClaveInegi(),
                municipio.getOrden(),
                municipio.getActivo());
    }
}
