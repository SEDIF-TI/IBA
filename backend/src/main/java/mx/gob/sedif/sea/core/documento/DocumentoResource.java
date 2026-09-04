package mx.gob.sedif.sea.core.documento;

import java.util.List;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import mx.gob.sedif.sea.exception.ApiResponse;

import jakarta.validation.Valid;

/**
 * Documentos adjuntos a los tramites.
 *
 * <p>Todos los destinos exigen sesion y permiso. La descarga es el unico camino
 * por el que sale un documento personal: los archivos viven fuera del alcance
 * de cualquier servidor web y no tienen direccion propia.
 */
@RestController
@RequestMapping("/documentos")
public class DocumentoResource {

    private final DocumentoService documentoService;

    public DocumentoResource(DocumentoService documentoService) {
        this.documentoService = documentoService;
    }

    /** Documentos de un tramite. */
    @GetMapping("/solicitud/{solicitudId}")
    @PreAuthorize("hasAuthority('documento:ver')")
    public ResponseEntity<ApiResponse<List<DocumentoRecord>>> listar(
            @PathVariable Long solicitudId) {

        List<DocumentoRecord> documentos = documentoService.listarPorSolicitud(solicitudId)
                .stream()
                .map(DocumentoRecord::de)
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(documentos));
    }

    /**
     * Descarga un documento personal.
     *
     * <p>Cada descarga queda registrada en bitacora con el usuario, la fecha y
     * la direccion de origen.
     */
    @GetMapping("/{id}/descargar")
    @PreAuthorize("hasAuthority('documento:descargar')")
    public ResponseEntity<InputStreamResource> descargar(@PathVariable Long id) {
        DocumentoService.DocumentoDescarga descarga = documentoService.descargar(id);

        // "attachment" evita que el navegador intente mostrar el archivo, lo que
        // con ciertos formatos permitiria ejecutar contenido en el contexto del sitio.
        ContentDisposition disposicion = ContentDisposition.attachment()
                .filename(descarga.nombreOriginal())
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicion.toString())
                // Documento personal: que ningun intermediario lo conserve.
                .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, private")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .contentType(MediaType.parseMediaType(descarga.mime()))
                .contentLength(descarga.tamanoBytes())
                .body(new InputStreamResource(descarga.contenido()));
    }

    /** Adjunta un archivo a un tramite. */
    @PostMapping("/solicitud/{solicitudId}")
    @PreAuthorize("hasAuthority('documento:ver')")
    public ResponseEntity<ApiResponse<DocumentoRecord>> adjuntar(
            @PathVariable Long solicitudId,
            @RequestParam Integer tipoDocumentoId,
            @RequestParam("archivo") MultipartFile archivo) {

        Documento documento = documentoService.adjuntar(solicitudId, tipoDocumentoId, archivo);
        return ResponseEntity.ok(
                ApiResponse.ok(DocumentoRecord.de(documento), "Documento adjuntado correctamente"));
    }

    /** Marca un documento como legible o ilegible. */
    @PatchMapping("/{id}/revisar")
    @PreAuthorize("hasAuthority('documento:revisar')")
    public ResponseEntity<ApiResponse<DocumentoRecord>> revisar(
            @PathVariable Long id,
            @Valid @RequestBody RevisarDocumentoRequest peticion) {

        Documento documento = documentoService.revisar(
                id, peticion.legible(), peticion.observacion());

        return ResponseEntity.ok(
                ApiResponse.ok(DocumentoRecord.de(documento), "Revisión registrada"));
    }

    /** Retira un documento del tramite. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('documento:eliminar')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        documentoService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok("Documento retirado correctamente"));
    }
}
