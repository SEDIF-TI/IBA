package mx.gob.sedif.sea.core.jornada;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Datos para dar de alta o editar una jornada.
 *
 * @param nombre               como se anuncia la jornada
 * @param descripcion          detalle opcional para el portal
 * @param municipioId          municipio donde se celebra
 * @param domicilioSede        direccion exacta del lugar
 * @param fechaInicio          primer dia de atencion
 * @param fechaFin             ultimo dia de atencion
 * @param horario              horario de atencion
 * @param cupo                 limite de personas; nulo si no lo tiene
 * @param usuarioResponsableId quien coordina la jornada
 */
public record JornadaRequest(
        @NotBlank(message = "Escribe el nombre de la jornada")
        @Size(max = 200)
        String nombre,

        @Size(max = 1000)
        String descripcion,

        @NotNull(message = "Elige el municipio donde se realizara la jornada")
        Integer municipioId,

        @NotBlank(message = "Escribe la dirección exacta de la sede: es a donde "
                + "va a llegar la gente")
        @Size(max = 400)
        String domicilioSede,

        @NotNull(message = "Indica el dia en que inicia la jornada")
        LocalDate fechaInicio,

        @NotNull(message = "Indica el dia en que termina la jornada")
        LocalDate fechaFin,

        @Size(max = 120)
        String horario,

        @Positive(message = "El cupo debe ser mayor que cero. Dejalo vacío si no hay limite.")
        Integer cupo,

        Long usuarioResponsableId) {
}
