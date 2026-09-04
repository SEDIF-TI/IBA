package mx.gob.sedif.sea.exception;

/**
 * Una regla de negocio impide completar la operacion.
 *
 * <p>El mensaje SI se muestra a la persona usuaria, asi que debe estar
 * redactado para ella: "El trámite ya fue entregado", no un volcado tecnico.
 * Responde 409.
 */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
