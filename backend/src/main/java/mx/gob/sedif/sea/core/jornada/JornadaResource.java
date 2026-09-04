package mx.gob.sedif.sea.core.jornada;

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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mx.gob.sedif.sea.core.solicitud.SolicitudRecord;
import mx.gob.sedif.sea.exception.ApiResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Jornadas municipales.
 *
 * <p>Incluye la vista de operacion en sitio: la busqueda que el personal usa el
 * dia del evento, con la gente formada enfrente.
 */
@RestController
@RequestMapping("/jornadas")
public class JornadaResource {

    private final JornadaService jornadaService;

    public JornadaResource(JornadaService jornadaService) {
        this.jornadaService = jornadaService;
    }

    /** Todas las jornadas. */
    @GetMapping
    @PreAuthorize("hasAuthority('jornada:ver')")
    public ResponseEntity<ApiResponse<List<JornadaRecord>>> listar() {
        List<JornadaRecord> jornadas = jornadaService.todas().stream()
                .map(JornadaRecord::de)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(jornadas));
    }

    /**
     * Jornadas que se estan celebrando hoy.
     *
     * <p>Es por donde entra el personal el dia del evento.
     */
    @GetMapping("/en-curso")
    @PreAuthorize("hasAuthority('jornada:ver')")
    public ResponseEntity<ApiResponse<List<JornadaRecord>>> enCurso() {
        List<JornadaRecord> jornadas = jornadaService.enCurso().stream()
                .map(JornadaRecord::de)
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(
                jornadas,
                jornadas.isEmpty()
                        ? "No hay ninguna jornada programada para hoy"
                        : "Hay %d jornada(s) en curso".formatted(jornadas.size())));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('jornada:ver')")
    public ResponseEntity<ApiResponse<JornadaRecord>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                JornadaRecord.de(jornadaService.obtener(id))));
    }

    /** Avance de la jornada, para seguirla durante el evento. */
    @GetMapping("/{id}/resumen")
    @PreAuthorize("hasAuthority('jornada:ver')")
    public ResponseEntity<ApiResponse<JornadaService.ResumenJornada>> resumen(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(jornadaService.resumen(id)));
    }

    /**
     * Busca a una persona entre las precargas de la jornada.
     *
     * <p>Admite CURP completa, apellidos o nombre, e ignora acentos: el
     * personal teclea rapido y con la gente esperando.
     */
    @GetMapping("/{id}/buscar")
    @PreAuthorize("hasAuthority('jornada:operar')")
    public ResponseEntity<ApiResponse<Page<SolicitudRecord>>> buscar(
            @PathVariable Long id,
            @RequestParam(required = false) String texto,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<SolicitudRecord> resultados = jornadaService
                .buscarEnJornada(id, texto, pageable)
                .map(SolicitudRecord::de);

        String mensaje = resultados.isEmpty()
                ? (texto == null || texto.isBlank()
                        ? "Esta jornada todavia no tiene registros precargados"
                        : "No hay ningún registro con \"%s\" en está jornada. "
                                .formatted(texto)
                                + "Verifica la CURP o el apellido, o registra a la "
                                + "persona en el momento.")
                : "Se encontraron %d registro(s)".formatted(resultados.getTotalElements());

        return ResponseEntity.ok(ApiResponse.ok(resultados, mensaje));
    }

    /** Da de alta una jornada. Nace sin publicar. */
    @PostMapping
    @PreAuthorize("hasAuthority('jornada:crear')")
    public ResponseEntity<ApiResponse<JornadaRecord>> crear(
            @Valid @RequestBody JornadaRequest peticion) {

        Jornada jornada = jornadaService.crear(peticion);
        return ResponseEntity.ok(ApiResponse.ok(
                JornadaRecord.de(jornada),
                "La jornada quedó registrada. Aun no aparece en el portal: "
                        + "publícala cuando la sede y las fechas esten confirmadas."));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('jornada:crear')")
    public ResponseEntity<ApiResponse<JornadaRecord>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody JornadaRequest peticion) {

        return ResponseEntity.ok(ApiResponse.ok(
                JornadaRecord.de(jornadaService.actualizar(id, peticion)),
                "Se guardaron los cambios de la jornada"));
    }

    /** Publica la jornada en el portal. */
    @PatchMapping("/{id}/publicar")
    @PreAuthorize("hasAuthority('jornada:publicar')")
    public ResponseEntity<ApiResponse<JornadaRecord>> publicar(@PathVariable Long id) {
        Jornada jornada = jornadaService.publicar(id);

        return ResponseEntity.ok(ApiResponse.ok(
                JornadaRecord.de(jornada),
                "La jornada ya aparece en el portal. La ciudadania puede verla y "
                        + "adelantar su registro."));
    }

    /** Retira la jornada del portal. */
    @PatchMapping("/{id}/despublicar")
    @PreAuthorize("hasAuthority('jornada:publicar')")
    public ResponseEntity<ApiResponse<JornadaRecord>> despublicar(
            @PathVariable Long id,
            @Valid @RequestBody DespublicarRequest peticion) {

        Jornada jornada = jornadaService.despublicar(id, peticion.motivo());

        return ResponseEntity.ok(ApiResponse.ok(
                JornadaRecord.de(jornada),
                "La jornada dejo de aparecer en el portal. Los registros que la "
                        + "gente ya adelanto se conservan."));
    }

    /**
     * Motivo por el que se retira una jornada del portal.
     *
     * @param motivo razon del retiro, que queda en bitacora
     */
    public record DespublicarRequest(
            @NotBlank(message = "Escribe por que se retira la jornada del portal")
            @Size(max = 500)
            String motivo) {
    }
}
