package mx.gob.sedif.sea.core.publico;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import mx.gob.sedif.sea.core.certificado.Certificado;
import mx.gob.sedif.sea.core.certificado.CertificadoService;
import mx.gob.sedif.sea.core.certificado.VerificacionRecord;
import mx.gob.sedif.sea.core.documental.AcusePdfService;
import mx.gob.sedif.sea.core.jornada.JornadaPublicaRecord;
import mx.gob.sedif.sea.core.jornada.JornadaService;
import mx.gob.sedif.sea.core.solicitud.RegistroPublicoRequest;
import mx.gob.sedif.sea.core.solicitud.SeguimientoRecord;
import mx.gob.sedif.sea.core.solicitud.Solicitud;
import mx.gob.sedif.sea.core.solicitud.SolicitudService;
import mx.gob.sedif.sea.security.SecurityUtil;
import mx.gob.sedif.sea.util.enums.CanalSolicitud;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * Portal ciudadano, sin autenticacion.
 *
 * <p>La ciudadania no tiene cuenta a proposito: a la persona le interesa su
 * documentacion, no administrar un usuario mas. El sistema anterior obligaba a
 * crear cuenta y esperar hasta 24 horas a que alguien la validara, antes
 * siquiera de poder registrar al animal; buena parte de la gente abandonaba ahi.
 *
 * <p>Al estar abierto, estos destinos van con limite de peticiones por
 * direccion: un formulario publico sin freno se llena de basura y el personal
 * acabaria depurando en vez de resolviendo tramites.
 */
@RestController
@RequestMapping("/publico")
public class PublicoResource {

    private final SolicitudService solicitudService;
    private final CertificadoService certificadoService;
    private final AcusePdfService acusePdfService;
    private final JornadaService jornadaService;

    public PublicoResource(SolicitudService solicitudService,
                           CertificadoService certificadoService,
                           AcusePdfService acusePdfService,
                           JornadaService jornadaService) {
        this.solicitudService = solicitudService;
        this.certificadoService = certificadoService;
        this.acusePdfService = acusePdfService;
        this.jornadaService = jornadaService;
    }

    /**
     * Alta de un tramite en un solo envio.
     *
     * <p>Devuelve el folio de seguimiento, que es lo que la persona necesita
     * anotar para consultar despues.
     */
    @PostMapping("/solicitud")
    public ResponseEntity<mx.gob.sedif.sea.exception.ApiResponse<RegistroRespuesta>> registrar(
            @Valid @RequestBody RegistroPublicoRequest peticion,
            HttpServletRequest http) {

        // La direccion de origen es la evidencia del consentimiento: sin cuenta
        // de usuario, es la unica constancia de que la persona acepto.
        String ip = SecurityUtil.obtenerIpCliente(http);

        // Si la persona eligio una jornada, el tramite queda precargado para
        // ese evento y el canal lo refleja.
        boolean esPrecarga = peticion.jornadaId() != null;

        Solicitud solicitud = solicitudService.registrar(
                peticion,
                esPrecarga ? CanalSolicitud.JORNADA : CanalSolicitud.PUBLICO,
                ip);

        RegistroRespuesta respuesta = new RegistroRespuesta(
                solicitud.getFolioSolicitud(),
                solicitud.getTokenConsulta().toString(),
                solicitud.getEstatus().getNombre(),
                esPrecarga
                        ? "Guarda tu folio y preséntalo el dia de la jornada. "
                                + "Lleva tu identificación original."
                        : "Guarda tu folio: lo necesitaras para consultar el avance "
                                + "de tu trámite.");

        return ResponseEntity.ok(mx.gob.sedif.sea.exception.ApiResponse.ok(
                respuesta,
                esPrecarga
                        ? "Tu registro quedó adelantado para la jornada. El dia del "
                                + "evento solo tendras que presentarte."
                        : "Tu trámite quedó registrado. Recibiras el resultado por "
                                + "el medio de contacto que proporcionaste."));
    }

    /**
     * Consulta del avance del tramite.
     *
     * <p>Exige folio Y CURP. Nunca solo la CURP: quien conociera la de un
     * tercero podria ver sus datos.
     */
    @GetMapping("/consulta")
    public ResponseEntity<mx.gob.sedif.sea.exception.ApiResponse<SeguimientoRecord>> consultar(
            @RequestParam @NotBlank String folio,
            @RequestParam @NotBlank String curp) {

        Solicitud solicitud = solicitudService.consultarPublica(folio, curp);
        return ResponseEntity.ok(mx.gob.sedif.sea.exception.ApiResponse.ok(
                SeguimientoRecord.de(solicitud)));
    }

    /**
     * Calendario de jornadas publicadas.
     *
     * <p>Es lo que permite a la ciudadania saber cuando toca su municipio y
     * llegar con el tramite adelantado, en lugar de enterarse el mismo dia.
     */
    @GetMapping("/jornadas")
    public ResponseEntity<mx.gob.sedif.sea.exception.ApiResponse<
            java.util.List<JornadaPublicaRecord>>> jornadas() {

        java.util.List<JornadaPublicaRecord> jornadas =
                jornadaService.publicadasVigentes().stream()
                        .map(JornadaPublicaRecord::de)
                        .toList();

        return ResponseEntity.ok(mx.gob.sedif.sea.exception.ApiResponse.ok(
                jornadas,
                jornadas.isEmpty()
                        ? "Por ahora no hay jornadas programadas. Puedes registrar "
                                + "tu trámite en cualquier momento desde este portal."
                        : "Hay %d jornada(s) programada(s)".formatted(jornadas.size())));
    }

    /**
     * Verifica un certificado a partir del codigo de su QR.
     *
     * <p>Es el destino al que llega quien escanea el documento. Confirma que el
     * certificado es autentico y describe al animal, pero no revela nada del
     * tutor: quien escanea puede ser cualquiera, y no tiene por que enterarse
     * del nombre, domicilio o telefono de su responsable.
     */
    @GetMapping("/verificar/{codigo}")
    public ResponseEntity<mx.gob.sedif.sea.exception.ApiResponse<VerificacionRecord>> verificar(
            @PathVariable String codigo) {

        VerificacionRecord resultado;
        try {
            java.util.UUID identificador = java.util.UUID.fromString(codigo);

            resultado = certificadoService.buscarPorCodigoVerificacion(identificador)
                    .map(this::evaluarVigencia)
                    .orElseGet(VerificacionRecord::noEncontrado);

        } catch (IllegalArgumentException ex) {
            // El codigo ni siquiera tiene la forma de un identificador valido.
            resultado = VerificacionRecord.noEncontrado();
        }

        return ResponseEntity.ok(mx.gob.sedif.sea.exception.ApiResponse.ok(resultado));
    }

    /** Determina si el certificado sigue surtiendo efecto y por que no. */
    private VerificacionRecord evaluarVigencia(Certificado certificado) {
        if (Boolean.FALSE.equals(certificado.getVigente())) {
            return VerificacionRecord.noVigente(certificado,
                    "Este certificado fue cancelado: "
                            + certificado.getMotivoCancelacion());
        }

        if (certificado.getFechaVigencia() != null
                && certificado.getFechaVigencia().isBefore(java.time.LocalDate.now())) {
            return VerificacionRecord.noVigente(certificado,
                    "Este certificado vencio el "
                            + certificado.getFechaVigencia()
                            + ". Acude al Instituto de Bienestar Animal para renovarlo.");
        }

        return VerificacionRecord.valido(certificado);
    }

    /**
     * Descarga el acuse del tramite.
     *
     * <p>Exige folio y CURP, igual que la consulta: el acuse lleva datos
     * personales y no puede quedar al alcance de quien solo conozca el folio.
     */
    @GetMapping("/acuse")
    public ResponseEntity<byte[]> descargarAcuse(
            @RequestParam @NotBlank String folio,
            @RequestParam @NotBlank String curp) {

        Solicitud solicitud = solicitudService.consultarPublica(folio, curp);
        byte[] pdf = acusePdfService.generar(solicitud);

        org.springframework.http.ContentDisposition disposicion =
                org.springframework.http.ContentDisposition.attachment()
                        .filename("acuse-%s.pdf".formatted(solicitud.getFolioSolicitud()))
                        .build();

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        disposicion.toString())
                .header(org.springframework.http.HttpHeaders.CACHE_CONTROL,
                        "no-store, no-cache, must-revalidate, private")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }

    /**
     * Respuesta del alta.
     *
     * @param folio    folio de seguimiento
     * @param token    identificador opaco para la liga de correccion
     * @param estatus  situacion inicial
     * @param mensaje  indicacion para la persona
     */
    public record RegistroRespuesta(
            String folio,
            String token,
            String estatus,
            String mensaje) {
    }
}
