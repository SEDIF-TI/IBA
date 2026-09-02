package mx.gob.sedif.sea.core.notificacion;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import mx.gob.sedif.sea.exception.ApiResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Bandeja de contacto y avisos del tramite.
 *
 * <p>La bandeja reune a quienes hay que llamar: las personas que solo dejaron
 * telefono. Sin esta pantalla esos avisos no ocurririan y nadie se enteraria de
 * que hay gente esperando respuesta.
 */
@RestController
@RequestMapping("/notificaciones")
public class NotificacionResource {

    private final NotificacionService notificacionService;

    public NotificacionResource(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    /** Contactos pendientes de atender, del mas antiguo al mas reciente. */
    @GetMapping("/bandeja")
    @PreAuthorize("hasAuthority('notificacion:contactar')")
    public ResponseEntity<ApiResponse<Page<NotificacionRecord>>> bandeja(
            @PageableDefault(size = 20) Pageable pageable) {

        Page<NotificacionRecord> pagina = notificacionService.bandejaContacto(pageable)
                .map(NotificacionRecord::de);

        return ResponseEntity.ok(ApiResponse.ok(pagina));
    }

    /** Cuantos contactos quedan por atender, para el indicador del panel. */
    @GetMapping("/bandeja/pendientes")
    @PreAuthorize("hasAuthority('notificacion:ver')")
    public ResponseEntity<ApiResponse<ResumenBandeja>> pendientes() {
        return ResponseEntity.ok(ApiResponse.ok(new ResumenBandeja(
                notificacionService.pendientesDeContacto(),
                notificacionService.envioHabilitado())));
    }

    /** Avisos de un tramite. */
    @GetMapping("/solicitud/{solicitudId}")
    @PreAuthorize("hasAuthority('notificacion:ver')")
    public ResponseEntity<ApiResponse<List<NotificacionRecord>>> porSolicitud(
            @PathVariable Long solicitudId) {

        List<NotificacionRecord> avisos = notificacionService.porSolicitud(solicitudId)
                .stream()
                .map(NotificacionRecord::de)
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(avisos));
    }

    /**
     * Registra el resultado de una llamada.
     *
     * <p>La observacion es obligatoria: sin ella, quien abra el expediente
     * despues no sabra en que quedo el contacto.
     */
    @PatchMapping("/{id}/contacto")
    @PreAuthorize("hasAuthority('notificacion:contactar')")
    public ResponseEntity<ApiResponse<NotificacionRecord>> registrarContacto(
            @PathVariable Long id,
            @Valid @RequestBody ContactoRequest peticion) {

        Notificacion aviso = notificacionService.registrarContacto(
                id, peticion.logrado(), peticion.observacion());

        return ResponseEntity.ok(ApiResponse.ok(
                NotificacionRecord.de(aviso),
                peticion.logrado()
                        ? "Se registro el contacto. El tramite queda atendido."
                        : "Se registro el intento. El contacto sigue pendiente "
                                + "para volver a intentarlo."));
    }

    /**
     * Reintenta los avisos por correo que fallaron.
     *
     * <p>Se ejecuta solo de forma periodica; este destino permite forzarlo
     * cuando se acaba de corregir la configuracion del servidor.
     */
    @PostMapping("/reintentar")
    @PreAuthorize("hasAuthority('notificacion:contactar')")
    public ResponseEntity<ApiResponse<Integer>> reintentar() {
        int enviados = notificacionService.reintentarPendientes();

        return ResponseEntity.ok(ApiResponse.ok(
                enviados,
                enviados == 0
                        ? "No habia avisos pendientes de reintento, o ninguno se "
                                + "logro enviar. Revisa la bandeja de contacto."
                        : "Se enviaron %d avisos que estaban pendientes".formatted(enviados)));
    }

    /**
     * Situacion de la bandeja.
     *
     * @param pendientes      contactos por atender
     * @param envioHabilitado si el correo sale solo
     */
    public record ResumenBandeja(long pendientes, boolean envioHabilitado) {
    }

    /**
     * Resultado de una llamada.
     *
     * @param logrado     si se logro hablar con la persona
     * @param observacion que se le dijo o por que no se pudo
     */
    public record ContactoRequest(
            @NotNull(message = "Indica si lograste comunicarte con la persona")
            Boolean logrado,

            @Size(max = 1000)
            String observacion) {
    }
}
