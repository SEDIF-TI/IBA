package mx.gob.sedif.sea.exception;

/**
 * Fallo al guardar o recuperar un archivo del volumen protegido.
 *
 * <p>La causa tecnica se registra en la bitacora del servidor; a la persona
 * usuaria solo se le devuelve un mensaje generico.
 */
public class AlmacenamientoException extends RuntimeException {

    public AlmacenamientoException(String mensaje) {
        super(mensaje);
    }

    public AlmacenamientoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
