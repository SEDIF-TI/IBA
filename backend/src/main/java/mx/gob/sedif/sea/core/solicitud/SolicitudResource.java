package mx.gob.sedif.sea.core.solicitud;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mx.gob.sedif.sea.exception.ApiResponse;

import jakarta.validation.Valid;

/**
 * Bandeja de tramites del panel administrativo.
 *
 * <p>Cada resolucion exige su propio permiso: aprobar, observar y rechazar son
 * decisiones distintas y no todo el personal debe poder tomarlas todas.
 */
@RestController
@RequestMapping("/solicitudes")
public class SolicitudResource {

    private final SolicitudService solicitudService;
    private final SolicitudRepository solicitudRepository;

    public SolicitudResource(SolicitudService solicitudService,
                             SolicitudRepository solicitudRepository) {
        this.solicitudService = solicitudService;
        this.solicitudRepository = solicitudRepository;
    }

    /**
     * Bandeja de trabajo con los filtros que usa el personal.
     *
     * <p>Un filtro sin valor no restringe.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('solicitud:ver')")
    public ResponseEntity<ApiResponse<Page<SolicitudRecord>>> listar(
            @RequestParam(required = false) Integer estatusId,
            @RequestParam(required = false) Integer municipioId,
            @RequestParam(required = false) Long jornadaId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta,
            @PageableDefault(size = 20, sort = "fechaRegistro",
                             direction = Sort.Direction.DESC) Pageable pageable) {

        Page<SolicitudRecord> pagina = solicitudRepository
                .buscarBandeja(estatusId, municipioId, jornadaId, desde, hasta, pageable)
                .map(SolicitudRecord::de);

        return ResponseEntity.ok(ApiResponse.ok(pagina));
    }

    /** Detalle de un tramite. */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('solicitud:ver')")
    public ResponseEntity<ApiResponse<SolicitudRecord>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.ok(SolicitudRecord.de(solicitudService.obtener(id))));
    }

    /** Expediente completo: cada movimiento con su fecha y responsable. */
    @GetMapping("/{id}/historial")
    @PreAuthorize("hasAuthority('solicitud:ver')")
    public ResponseEntity<ApiResponse<List<HistorialRecord>>> historial(@PathVariable Long id) {
        List<HistorialRecord> historial = solicitudService.obtenerHistorial(id).stream()
                .map(HistorialRecord::de)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(historial));
    }

    /** Toma el tramite para revisarlo. */
    @PatchMapping("/{id}/revisar")
    @PreAuthorize("hasAuthority('solicitud:editar')")
    public ResponseEntity<ApiResponse<SolicitudRecord>> tomarParaRevision(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                SolicitudRecord.de(solicitudService.tomarParaRevision(id)),
                "El tramite quedo en revision"));
    }

    /** Aprueba el tramite. */
    @PatchMapping("/{id}/aprobar")
    @PreAuthorize("hasAuthority('solicitud:aprobar')")
    public ResponseEntity<ApiResponse<SolicitudRecord>> aprobar(
            @PathVariable Long id,
            @Valid @RequestBody ResolucionRequest peticion) {

        return ResponseEntity.ok(ApiResponse.ok(
                SolicitudRecord.de(solicitudService.aprobar(id, peticion.observacion())),
                "Tramite aprobado"));
    }

    /** Devuelve el tramite para que la persona corrija. */
    @PatchMapping("/{id}/observar")
    @PreAuthorize("hasAuthority('solicitud:observar')")
    public ResponseEntity<ApiResponse<SolicitudRecord>> observar(
            @PathVariable Long id,
            @Valid @RequestBody ResolucionRequest peticion) {

        return ResponseEntity.ok(ApiResponse.ok(
                SolicitudRecord.de(solicitudService.observar(
                        id, peticion.motivoId(), peticion.observacion())),
                "Se registraron las observaciones"));
    }

    /** Rechaza el tramite. */
    @PatchMapping("/{id}/rechazar")
    @PreAuthorize("hasAuthority('solicitud:rechazar')")
    public ResponseEntity<ApiResponse<SolicitudRecord>> rechazar(
            @PathVariable Long id,
            @Valid @RequestBody ResolucionRequest peticion) {

        return ResponseEntity.ok(ApiResponse.ok(
                SolicitudRecord.de(solicitudService.rechazar(
                        id, peticion.motivoId(), peticion.observacion())),
                "Tramite rechazado"));
    }

    /** Cancela el tramite. */
    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasAuthority('solicitud:cancelar')")
    public ResponseEntity<ApiResponse<SolicitudRecord>> cancelar(
            @PathVariable Long id,
            @Valid @RequestBody ResolucionRequest peticion) {

        return ResponseEntity.ok(ApiResponse.ok(
                SolicitudRecord.de(solicitudService.cancelar(
                        id, peticion.motivoId(), peticion.observacion())),
                "Tramite cancelado"));
    }

    /**
     * Un movimiento del expediente.
     *
     * @param estatusAnterior situacion previa; nula en el alta
     * @param estatusNuevo    situacion resultante
     * @param usuario         quien lo hizo; nulo si vino del portal publico
     */
    public record HistorialRecord(
            Long id,
            String estatusAnterior,
            String estatusNuevo,
            String usuario,
            String motivo,
            String observacion,
            OffsetDateTime fecha) {

        public static HistorialRecord de(SolicitudHistorial renglon) {
            return new HistorialRecord(
                    renglon.getId(),
                    renglon.getEstatusAnterior() == null
                            ? null : renglon.getEstatusAnterior().getNombre(),
                    renglon.getEstatusNuevo().getNombre(),
                    renglon.getUsuario() == null
                            ? "Portal publico" : renglon.getUsuario().getNombreCompleto(),
                    renglon.getMotivoRechazo() == null
                            ? null : renglon.getMotivoRechazo().getNombre(),
                    renglon.getObservacion(),
                    renglon.getFecha());
        }
    }
}
