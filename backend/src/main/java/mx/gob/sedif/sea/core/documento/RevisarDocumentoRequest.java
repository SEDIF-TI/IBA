package mx.gob.sedif.sea.core.documento;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Resultado de la revision de un documento.
 *
 * @param legible     si el documento se puede leer con claridad
 * @param observacion nota del revisor; util sobre todo cuando no es legible
 */
public record RevisarDocumentoRequest(
        @NotNull(message = "Indica si el documento es legible")
        Boolean legible,

        @Size(max = 500)
        String observacion) {
}
