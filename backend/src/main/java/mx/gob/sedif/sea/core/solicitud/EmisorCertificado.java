package mx.gob.sedif.sea.core.solicitud;

/**
 * Emision del certificado, vista desde el tramite.
 *
 * <p>Existe para romper el ciclo entre los dos servicios: el tramite necesita
 * emitir al aprobar, y la emision necesita consultar el tramite. Con esta
 * interfaz el tramite depende de una operacion, no del otro servicio completo.
 */
public interface EmisorCertificado {

    /**
     * Emite el certificado de un tramite aprobado.
     *
     * @param solicitudId tramite aprobado
     */
    void emitir(Long solicitudId);
}
