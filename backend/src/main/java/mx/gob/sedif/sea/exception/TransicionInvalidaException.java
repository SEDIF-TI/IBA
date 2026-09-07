package mx.gob.sedif.sea.exception;

import mx.gob.sedif.sea.util.enums.EstatusSolicitudEnum;

/**
 * Se intento mover un tramite a un estado al que no puede llegar desde su
 * situacion actual.
 *
 * <p>Protege la integridad del expediente: sin esto se podria entregar un
 * tramite que nunca se aprobo.
 */
public class TransicionInvalidaException extends ReglaNegocioException {

    /**
     * @param origen  situacion actual del tramite
     * @param destino situacion a la que se intento pasar
     */
    public TransicionInvalidaException(EstatusSolicitudEnum origen, EstatusSolicitudEnum destino) {
        // Se explica en terminos del tramite, no con las claves internas, y se
        // dice que si se puede hacer desde donde esta.
        super("No se puede %s un trámite que esta %s. %s"
                .formatted(accion(destino), legible(origen), sugerencia(origen)));
    }

    /** Verbo de la accion que se intento. */
    private static String accion(EstatusSolicitudEnum destino) {
        return switch (destino) {
            case EN_REVISION -> "poner en revisión";
            case APROBADA -> "aprobar";
            case OBSERVADA -> "devolver para corrección";
            case RECHAZADA -> "rechazar";
            case CANCELADA -> "cancelar";
            case CERTIFICADO_EMITIDO -> "emitir el certificado de";
            case ENTREGADA -> "marcar como entregado";
            case REGISTRADA -> "registrar";
        };
    }

    /** Situacion actual, en los terminos que usa el personal. */
    private static String legible(EstatusSolicitudEnum origen) {
        return switch (origen) {
            case REGISTRADA -> "recién registrado y aún no se toma para revisión";
            case EN_REVISION -> "en revision";
            case OBSERVADA -> "devuelto para corrección";
            case APROBADA -> "aprobado";
            case CERTIFICADO_EMITIDO -> "con su certificado ya emitido";
            case ENTREGADA -> "entregado";
            case RECHAZADA -> "rechazado";
            case CANCELADA -> "cancelado";
        };
    }

    /** Que si se puede hacer desde la situacion actual. */
    private static String sugerencia(EstatusSolicitudEnum origen) {
        return switch (origen) {
            case REGISTRADA -> "Tómalo primero para revisión.";
            case EN_REVISION -> "Desde aquí puedes aprobarlo, devolverlo para "
                    + "corrección o rechazarlo.";
            case OBSERVADA -> "Espera a que la persona corrija, o vuelve a "
                    + "ponerlo en revisión.";
            case APROBADA -> "Desde aquí solo procede emitir su certificado.";
            case CERTIFICADO_EMITIDO -> "Desde aquí solo procede registrar la entrega.";
            case ENTREGADA, RECHAZADA, CANCELADA ->
                    "Este trámite ya concluyó y no admite más cambios.";
        };
    }
}
