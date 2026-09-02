package mx.gob.sedif.sea.core.certificado;

import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Certificados de empadronamiento.
 *
 * <p>Los documentos se componen al momento de descargarlos y no se guardan como
 * archivo: asi cualquier correccion de los datos se refleja, y no queda un
 * acervo de PDF que resguardar.
 */
@RestController
@RequestMapping("/certificados")
public class CertificadoResource {

    private final CertificadoService certificadoService;

    public CertificadoResource(CertificadoService certificadoService) {
        this.certificadoService = certificadoService;
    }

    /** Detalle de un certificado. */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('certificado:ver')")
    public ResponseEntity<ApiResponse<CertificadoRecord>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                CertificadoRecord.de(certificadoService.obtener(id))));
    }

    /** Certificado de un tramite, si ya se emitio. */
    @GetMapping("/solicitud/{solicitudId}")
    @PreAuthorize("hasAuthority('certificado:ver')")
    public ResponseEntity<ApiResponse<CertificadoRecord>> porSolicitud(
            @PathVariable Long solicitudId) {

        return certificadoService.buscarPorSolicitud(solicitudId)
                .map(certificado -> ResponseEntity.ok(
                        ApiResponse.ok(CertificadoRecord.de(certificado))))
                .orElseGet(() -> ResponseEntity.ok(
                        ApiResponse.ok(null, "El tramite aun no tiene certificado")));
    }

    /**
     * Emite el certificado de un tramite aprobado.
     *
     * <p>Normalmente se emite solo al aprobar; este destino sirve para los
     * casos en que hubo que emitirlo aparte.
     */
    @PostMapping("/solicitud/{solicitudId}")
    @PreAuthorize("hasAuthority('certificado:emitir')")
    public ResponseEntity<ApiResponse<CertificadoRecord>> emitir(
            @PathVariable Long solicitudId) {

        Certificado certificado = certificadoService.emitirCertificado(solicitudId);
        return ResponseEntity.ok(ApiResponse.ok(
                CertificadoRecord.de(certificado), "Certificado emitido correctamente"));
    }

    /** Descarga el certificado en PDF. */
    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAuthority('certificado:imprimir')")
    public ResponseEntity<byte[]> descargarCertificado(@PathVariable Long id) {
        Certificado certificado = certificadoService.obtener(id);
        byte[] pdf = certificadoService.generarPdf(id);

        return respuestaPdf(pdf, "certificado-%s.pdf".formatted(
                certificado.getFolio().replace('/', '-')));
    }

    /** Descarga la credencial en PDF. */
    @GetMapping("/{id}/credencial")
    @PreAuthorize("hasAuthority('certificado:imprimir')")
    public ResponseEntity<byte[]> descargarCredencial(@PathVariable Long id) {
        Certificado certificado = certificadoService.obtener(id);
        byte[] pdf = certificadoService.generarCredencial(id);

        return respuestaPdf(pdf, "credencial-%s.pdf".formatted(
                certificado.getFolio().replace('/', '-')));
    }

    /**
     * Imprime varias credenciales de una vez, tres por hoja.
     *
     * <p>Es lo que se usa al cierre de una jornada municipal.
     */
    @PostMapping("/credenciales/lote")
    @PreAuthorize("hasAuthority('certificado:imprimir')")
    public ResponseEntity<byte[]> imprimirLote(
            @Valid @RequestBody ImpresionLoteRequest peticion) {

        byte[] pdf = certificadoService.generarCredencialesPorLote(peticion.certificadoIds());
        return respuestaPdf(pdf, "credenciales-lote.pdf");
    }

    /** Cancela un certificado. */
    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasAuthority('certificado:cancelar')")
    public ResponseEntity<ApiResponse<CertificadoRecord>> cancelar(
            @PathVariable Long id,
            @Valid @RequestBody CancelacionRequest peticion) {

        Certificado certificado = certificadoService.cancelar(id, peticion.motivo());
        return ResponseEntity.ok(ApiResponse.ok(
                CertificadoRecord.de(certificado), "Certificado cancelado"));
    }

    /** Registra que el documento se entrego con su sello. */
    @PatchMapping("/{id}/entregar")
    @PreAuthorize("hasAuthority('certificado:emitir')")
    public ResponseEntity<ApiResponse<CertificadoRecord>> registrarEntrega(
            @PathVariable Long id) {

        Certificado certificado = certificadoService.registrarEntrega(id);
        return ResponseEntity.ok(ApiResponse.ok(
                CertificadoRecord.de(certificado), "Entrega registrada"));
    }

    /**
     * Arma la respuesta con el documento.
     *
     * <p>Se marca para que el navegador lo descargue y no lo muestre, y para
     * que ningun intermediario lo conserve: lleva datos personales.
     */
    private ResponseEntity<byte[]> respuestaPdf(byte[] pdf, String nombreArchivo) {
        ContentDisposition disposicion = ContentDisposition.attachment()
                .filename(nombreArchivo)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicion.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, private")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }

    /**
     * Credenciales a imprimir de una vez.
     *
     * @param certificadoIds certificados incluidos en la impresion
     */
    public record ImpresionLoteRequest(
            @NotEmpty(message = "Indica al menos una credencial")
            @Size(max = 200, message = "No se pueden imprimir mas de 200 credenciales de una vez")
            List<Long> certificadoIds) {
    }

    /**
     * Motivo de la cancelacion.
     *
     * @param motivo razon por la que el certificado deja de surtir efecto
     */
    public record CancelacionRequest(
            @NotBlank(message = "Indica el motivo de la cancelacion")
            @Size(max = 500)
            String motivo) {
    }
}
