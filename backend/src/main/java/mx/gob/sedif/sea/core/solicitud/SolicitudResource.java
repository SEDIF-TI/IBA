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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mx.gob.sedif.sea.exception.ApiResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

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

    /**
     * Captura un tramite en ventanilla.
     *
     * <p>Es para cuando la persona llega sin haberse registrado: no tiene
     * internet, o llego a la jornada sin precargar. El personal captura por
     * ella y el tramite queda con canal VENTANILLA.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('solicitud:crear')")
    public ResponseEntity<ApiResponse<SolicitudRecord>> capturarEnVentanilla(
            @Valid @RequestBody mx.gob.sedif.sea.core.solicitud.RegistroPublicoRequest peticion,
            jakarta.servlet.http.HttpServletRequest http) {

        String ip = mx.gob.sedif.sea.security.SecurityUtil.obtenerIpCliente(http);

        mx.gob.sedif.sea.core.solicitud.Solicitud solicitud = solicitudService.registrar(
                peticion,
                peticion.jornadaId() != null
                        ? mx.gob.sedif.sea.util.enums.CanalSolicitud.JORNADA
                        : mx.gob.sedif.sea.util.enums.CanalSolicitud.VENTANILLA,
                ip);

        return ResponseEntity.ok(ApiResponse.ok(
                SolicitudRecord.de(solicitud),
                "Tramite capturado con folio %s. Ya puedes tomarlo para revision."
                        .formatted(solicitud.getFolioSolicitud())));
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

    /**
     * Registra que la persona recogio su documentacion.
     *
     * <p>Cierra el tramite: es el ultimo paso del flujo.
     */
    @PatchMapping("/{id}/entregar")
    @PreAuthorize("hasAuthority('certificado:emitir')")
    public ResponseEntity<ApiResponse<SolicitudRecord>> registrarEntrega(
            @PathVariable Long id,
            @Valid @RequestBody ResolucionRequest peticion) {

        return ResponseEntity.ok(ApiResponse.ok(
                SolicitudRecord.de(solicitudService.registrarEntrega(
                        id, peticion.observacion())),
                "Se registro la entrega. El tramite queda concluido."));
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
     * Da de baja un registro que nunca fue real.
     *
     * <p>Es para depurar el padron: gente que registro por probar, o datos
     * evidentemente falsos. La baja es logica y se puede deshacer.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('solicitud:depurar')")
    public ResponseEntity<ApiResponse<Void>> darDeBaja(
            @PathVariable Long id,
            @Valid @RequestBody BajaRequest peticion) {

        solicitudService.darDeBaja(id, peticion.motivo(),
                Boolean.TRUE.equals(peticion.incluirPersona()));

        return ResponseEntity.ok(ApiResponse.ok(
                Boolean.TRUE.equals(peticion.incluirPersona())
                        ? "El tramite, el animal y el tutor dejaron de aparecer en el "
                                + "padron. Si fue un error, puedes reactivarlos."
                        : "El tramite dejo de aparecer en la bandeja. El tutor y su "
                                + "animal siguen en el padron."));
    }

    /** Registros dados de baja, para revisarlos o reactivarlos. */
    @GetMapping("/dados-de-baja")
    @PreAuthorize("hasAuthority('solicitud:depurar')")
    public ResponseEntity<ApiResponse<Page<SolicitudRecord>>> dadosDeBaja(
            @PageableDefault(size = 20) Pageable pageable) {

        Page<SolicitudRecord> pagina = solicitudRepository.findDadosDeBaja(pageable)
                .map(SolicitudRecord::de);

        return ResponseEntity.ok(ApiResponse.ok(
                pagina,
                pagina.isEmpty()
                        ? "No hay registros dados de baja"
                        : "%d registro(s) dados de baja".formatted(pagina.getTotalElements())));
    }

    /** Reactiva un registro dado de baja por error. */
    @PatchMapping("/{id}/reactivar")
    @PreAuthorize("hasAuthority('solicitud:depurar')")
    public ResponseEntity<ApiResponse<SolicitudRecord>> reactivar(
            @PathVariable Long id,
            @RequestBody(required = false) BajaRequest peticion) {

        return ResponseEntity.ok(ApiResponse.ok(
                SolicitudRecord.de(solicitudService.reactivar(
                        id, peticion == null ? null : peticion.motivo())),
                "El registro vuelve a aparecer en el padron"));
    }

    /**
     * Motivo de la baja.
     *
     * @param motivo         por que se depura este registro
     * @param incluirPersona si tambien se da de baja al tutor y su animal
     */
    public record BajaRequest(
            @NotBlank(message = "Escribe por que se da de baja este registro")
            @Size(max = 500)
            String motivo,

            Boolean incluirPersona) {
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
