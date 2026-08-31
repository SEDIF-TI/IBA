package mx.gob.sedif.sea.core.solicitud;

import jakarta.validation.constraints.Size;

/**
 * Resolucion del personal sobre un tramite.
 *
 * @param motivoId    motivo del catalogo; obligatorio al observar, rechazar o cancelar
 * @param observacion nota libre que se le hace llegar a la persona
 */
public record ResolucionRequest(
        Integer motivoId,

        @Size(max = 1000)
        String observacion) {
}
