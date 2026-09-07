package mx.gob.sedif.sea.core.publico;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import mx.gob.sedif.sea.core.catalogo.CatalogoRecord;
import mx.gob.sedif.sea.core.catalogo.ColorService;
import mx.gob.sedif.sea.core.catalogo.EspecieService;
import mx.gob.sedif.sea.core.catalogo.MunicipioRecord;
import mx.gob.sedif.sea.core.catalogo.MunicipioService;
import mx.gob.sedif.sea.core.catalogo.SexoService;
import mx.gob.sedif.sea.core.catalogo.TamanoService;
import mx.gob.sedif.sea.core.certificado.Certificado;
import mx.gob.sedif.sea.core.documento.Documento;
import mx.gob.sedif.sea.core.documento.DocumentoRecord;
import mx.gob.sedif.sea.core.documento.DocumentoService;
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

import java.util.List;

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
    private final MunicipioService municipioService;
    private final EspecieService especieService;
    private final ColorService colorService;
    private final SexoService sexoService;
    private final TamanoService tamanoService;
    private final DocumentoService documentoService;

    public PublicoResource(SolicitudService solicitudService,
                           CertificadoService certificadoService,
                           AcusePdfService acusePdfService,
                           JornadaService jornadaService,
                           MunicipioService municipioService,
                           EspecieService especieService,
                           ColorService colorService,
                           SexoService sexoService,
                           TamanoService tamanoService,
                           DocumentoService documentoService) {
        this.solicitudService = solicitudService;
        this.certificadoService = certificadoService;
        this.acusePdfService = acusePdfService;
        this.jornadaService = jornadaService;
        this.municipioService = municipioService;
        this.especieService = especieService;
        this.colorService = colorService;
        this.sexoService = sexoService;
        this.tamanoService = tamanoService;
        this.documentoService = documentoService;
    }

    /**
     * Listas para los desplegables del formulario publico.
     *
     * <p>Van aqui y no en /catalogos porque el formulario ciudadano se llena sin
     * sesion: pedir permiso de catalogo obligaria a la persona a iniciar sesion
     * para poder elegir su municipio, que es justo lo que este portal evita.
     *
     * <p>Solo lectura y solo lo vigente. La administracion de catalogos sigue
     * siendo del personal.
     */
    @GetMapping("/catalogos")
    public ResponseEntity<mx.gob.sedif.sea.exception.ApiResponse<CatalogosPublicos>> catalogos() {
        CatalogosPublicos listas = new CatalogosPublicos(
                municipioService.listarVigentes().stream().map(MunicipioRecord::de).toList(),
                especieService.listarVigentes().stream().map(CatalogoRecord::de).toList(),
                colorService.listarVigentes().stream().map(CatalogoRecord::de).toList(),
                sexoService.listarVigentes().stream().map(CatalogoRecord::de).toList(),
                tamanoService.listarVigentes().stream().map(CatalogoRecord::de).toList());

        return ResponseEntity.ok(mx.gob.sedif.sea.exception.ApiResponse.ok(listas));
    }

    /** Lo que el formulario publico necesita para armar sus desplegables. */
    public record CatalogosPublicos(List<MunicipioRecord> municipios,
                                    List<CatalogoRecord> especies,
                                    List<CatalogoRecord> colores,
                                    List<CatalogoRecord> sexos,
                                    List<CatalogoRecord> tamanos) {
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
            @RequestParam @NotBlank(message = "Escribe tu folio de seguimiento, "
                    + "el que aparece en tu acuse (por ejemplo SOL-20260101-00001).")
            String folio,
            @RequestParam @NotBlank(message = "Escribe la CURP con la que se hizo el registro: "
                    + "se piden ambos datos para que nadie más pueda consultar tu trámite.")
            String curp) {

        Solicitud solicitud = solicitudService.consultarPublica(folio, curp);
        return ResponseEntity.ok(mx.gob.sedif.sea.exception.ApiResponse.ok(
                SeguimientoRecord.de(solicitud)));
    }

    /**
     * Carga de un documento desde el portal, identificado por el token.
     *
     * <p>Se pide el token y no el folio: el folio va impreso en el acuse y
     * podria verlo cualquiera, mientras que el token solo lo tiene quien hizo
     * el registro.
     */
    @PostMapping("/documento")
    public ResponseEntity<mx.gob.sedif.sea.exception.ApiResponse<DocumentoRecord>> adjuntar(
            @RequestParam @NotBlank(message = "Falta la liga de tu tramite. "
                    + "Usa la que aparece al terminar tu registro.") String token,
            @RequestParam Integer tipoDocumentoId,
            @RequestParam("archivo") MultipartFile archivo) {

        java.util.UUID identificador;
        try {
            identificador = java.util.UUID.fromString(token);
        } catch (IllegalArgumentException ex) {
            throw new mx.gob.sedif.sea.exception.ReglaNegocioException(
                    "La liga de tu tramite no es valida. Copiala completa desde tu acuse.");
        }

        Documento documento = documentoService.adjuntarPorToken(
                identificador, tipoDocumentoId, archivo);

        return ResponseEntity.ok(mx.gob.sedif.sea.exception.ApiResponse.ok(
                DocumentoRecord.de(documento),
                "Se adjunto %s correctamente.".formatted(documento.getNombreOriginal())));
    }

    /**
     * Tipos de documento que la ciudadania puede adjuntar.
     */
    @GetMapping("/tipos-documento")
    public ResponseEntity<mx.gob.sedif.sea.exception.ApiResponse<List<CatalogoRecord>>>
            tiposDocumento() {

        List<CatalogoRecord> tipos = documentoService.tiposParaCiudadania().stream()
                .map(CatalogoRecord::de)
                .toList();

        return ResponseEntity.ok(mx.gob.sedif.sea.exception.ApiResponse.ok(tipos));
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
            @RequestParam @NotBlank(message = "Escribe tu folio de seguimiento, "
                    + "el que aparece en tu acuse (por ejemplo SOL-20260101-00001).")
            String folio,
            @RequestParam @NotBlank(message = "Escribe la CURP con la que se hizo el registro: "
                    + "se piden ambos datos para que nadie más pueda consultar tu trámite.")
            String curp) {

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
