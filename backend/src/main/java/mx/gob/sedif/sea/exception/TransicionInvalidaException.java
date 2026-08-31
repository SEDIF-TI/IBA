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

    public TransicionInvalidaException(EstatusSolicitudEnum origen, EstatusSolicitudEnum destino) {
        super("Un tramite en estado %s no puede pasar a %s"
                .formatted(origen.name(), destino.name()));
    }
}
