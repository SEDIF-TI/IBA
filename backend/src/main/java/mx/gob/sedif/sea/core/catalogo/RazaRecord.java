package mx.gob.sedif.sea.core.catalogo;

/**
 * Raza con la especie a la que pertenece.
 *
 * @param id            identificador
 * @param clave         referencia estable
 * @param nombre        nombre de la raza
 * @param especieId     identificador de la especie
 * @param especieNombre nombre de la especie, para mostrar
 * @param orden         posicion de despliegue
 * @param activo        si sigue disponible
 */
public record RazaRecord(
        Integer id,
        String clave,
        String nombre,
        Integer especieId,
        String especieNombre,
        Integer orden,
        Boolean activo) {

    public static RazaRecord de(Raza raza) {
        return new RazaRecord(
                raza.getId(),
                raza.getClave(),
                raza.getNombre(),
                raza.getEspecie() == null ? null : raza.getEspecie().getId(),
                raza.getEspecie() == null ? null : raza.getEspecie().getNombre(),
                raza.getOrden(),
                raza.getActivo());
    }
}
