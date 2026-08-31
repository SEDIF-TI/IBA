package mx.gob.sedif.sea.core.catalogo;

/**
 * Datos de un catalogo para la interfaz.
 *
 * <p>Sirve para los trece catalogos: todos comparten la misma forma. Los que
 * tienen campos propios (municipio, raza, sede) usan su propio registro.
 *
 * @param id          identificador
 * @param clave       referencia estable que usa el codigo
 * @param nombre      texto que ve la persona usuaria
 * @param descripcion detalle opcional
 * @param orden       posicion en la que se despliega
 * @param activo      si sigue disponible para elegirse
 */
public record CatalogoRecord(
        Integer id,
        String clave,
        String nombre,
        String descripcion,
        Integer orden,
        Boolean activo) {

    public static CatalogoRecord de(CatalogoBase catalogo) {
        return new CatalogoRecord(
                catalogo.getId(),
                catalogo.getClave(),
                catalogo.getNombre(),
                catalogo.getDescripcion(),
                catalogo.getOrden(),
                catalogo.getActivo());
    }
}
