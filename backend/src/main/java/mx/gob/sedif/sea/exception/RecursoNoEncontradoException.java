package mx.gob.sedif.sea.exception;

/** El registro solicitado no existe o fue dado de baja. Responde 404. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }

    /**
     * Construye el mensaje a partir del tipo y el identificador buscado.
     *
     * @param entidad     nombre legible del recurso, por ejemplo "Solicitud"
     * @param identificador valor que se busco
     */
    public RecursoNoEncontradoException(String entidad, Object identificador) {
        super("No se encontro %s con identificador %s".formatted(entidad, identificador));
    }
}
