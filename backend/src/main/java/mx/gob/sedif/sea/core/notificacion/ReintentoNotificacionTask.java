package mx.gob.sedif.sea.core.notificacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Reintento periodico de los avisos que no salieron.
 *
 * <p>Solo tiene sentido cuando el envio de correo esta configurado: sin
 * servidor, reintentar seria escribir en la bitacora cada cinco minutos sin
 * lograr nada.
 */
@Component
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class ReintentoNotificacionTask {

    private static final Logger log =
            LoggerFactory.getLogger(ReintentoNotificacionTask.class);

    private final NotificacionService notificacionService;

    public ReintentoNotificacionTask(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    /**
     * Reintenta cada cinco minutos.
     *
     * <p>El intervalo es un equilibrio: lo bastante seguido para que una caida
     * breve del servidor no retrase los avisos, y lo bastante espaciado para no
     * insistir sobre un servidor que esta teniendo problemas.
     */
    @Scheduled(fixedDelayString = "${app.notificacion.intervalo-reintento-ms:300000}")
    public void reintentar() {
        try {
            int enviados = notificacionService.reintentarPendientes();
            if (enviados > 0) {
                log.info("Reintento programado: se enviaron {} avisos pendientes",
                        enviados);
            }
        } catch (Exception ex) {
            // Un fallo aqui no debe detener la tarea: se reintenta al siguiente ciclo.
            log.error("Fallo el reintento programado de avisos", ex);
        }
    }
}
