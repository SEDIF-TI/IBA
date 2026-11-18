package mx.gob.sedif.sea.exception;

/**
 * Otra persona guardó cambios en el registro mientras este usuario lo tenía abierto.
 * Área: Núcleo compartido. Lógica: bloqueo optimista de los formularios de edición.
 *
 * <p>{@link GlobalExceptionHandler} la traduce a 409 con un mensaje que pide recargar: guardar de
 * todos modos borraría en silencio lo que hizo la otra persona.
 */
public class EdicionConcurrenteException extends RuntimeException {

    public EdicionConcurrenteException() {
        super(MessageConstants.EDICION_CONCURRENTE);
    }

    /**
     * Compara la versión que la persona tenía a la vista con la vigente.
     *
     * @param vigente  versión actual del registro en la base
     * @param recibida versión que envió el formulario
     * @throws ReglaNegocioException        si el formulario no envió versión
     * @throws EdicionConcurrenteException si las versiones no coinciden
     */
    public static void verificar(Long vigente, Long recibida) {
        if (recibida == null) {
            throw new ReglaNegocioException(MessageConstants.VERSION_FALTANTE);
        }
        if (!recibida.equals(vigente)) {
            throw new EdicionConcurrenteException();
        }
    }
}
