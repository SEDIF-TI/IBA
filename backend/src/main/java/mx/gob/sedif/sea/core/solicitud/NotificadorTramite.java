package mx.gob.sedif.sea.core.solicitud;

/**
 * Aviso a la persona sobre su tramite, visto desde el tramite.
 *
 * <p>Existe para romper el ciclo entre los dos servicios, igual que
 * {@link EmisorCertificado}: el tramite depende de una operacion, no del
 * servicio de notificaciones completo.
 */
public interface NotificadorTramite {

    /**
     * Avisa el resultado de un movimiento del tramite.
     *
     * <p>Si la persona dejo correo, el aviso sale solo; si no, queda en la
     * bandeja de contacto para que alguien le llame.
     *
     * @param solicitudId tramite del que se avisa
     * @param claveAviso  plantilla a usar
     */
    void avisar(Long solicitudId, String claveAviso);
}
