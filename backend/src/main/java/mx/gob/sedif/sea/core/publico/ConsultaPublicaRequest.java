package mx.gob.sedif.sea.core.publico;

import mx.gob.sedif.sea.util.validacion.CurpValida;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos con los que la ciudadanía consulta su trámite o descarga su acuse.
 * Área: Núcleo compartido (portal público). Lógica: seguimiento del trámite sin cuenta.
 *
 * <p>Viajan en el cuerpo de un POST y no en la dirección: una CURP en la URL queda en los registros
 * de los proxies, en el historial del navegador y en el encabezado Referer.
 *
 * @param folio folio de seguimiento impreso en el acuse, por ejemplo SOL-20260101-00001
 * @param curp  CURP con la que se hizo el registro; se exige junto con el folio para que nadie
 *              consulte el trámite de otra persona conociendo solo uno de los dos datos
 */
public record ConsultaPublicaRequest(
        @NotBlank(message = "Escribe tu folio de seguimiento, el que aparece en tu acuse "
                + "(por ejemplo SOL-20260101-00001).")
        @Size(max = 40, message = "El folio no puede pasar de 40 caracteres.")
        String folio,

        @NotBlank(message = "Escribe la CURP con la que se hizo el registro: se piden ambos datos "
                + "para que nadie más pueda consultar tu trámite.")
        @CurpValida
        String curp) {
}
