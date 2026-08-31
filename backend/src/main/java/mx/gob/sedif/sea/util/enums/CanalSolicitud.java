package mx.gob.sedif.sea.util.enums;

/**
 * Via por la que entro el tramite.
 *
 * <p>Se persiste como texto, no como numero ordinal: si alguien reordena las
 * constantes, los registros existentes seguirian significando lo mismo.
 */
public enum CanalSolicitud {

    /** Formulario del portal, sin autenticacion. */
    PUBLICO,

    /** Precarga o captura durante una jornada municipal. */
    JORNADA,

    /** Captura directa del personal en oficina. */
    VENTANILLA
}
