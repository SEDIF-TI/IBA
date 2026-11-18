package mx.gob.sedif.sea.core.jornada;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Jornada tal como se muestra en el panel.
 *
 * @param id             identificador
 * @param nombre         como se anuncia
 * @param descripcion    detalle
 * @param municipioId    municipio donde se celebra
 * @param municipio      nombre del municipio
 * @param domicilioSede  direccion del lugar
 * @param fechaInicio    primer dia
 * @param fechaFin       ultimo dia
 * @param horario        horario de atencion
 * @param cupo           limite de personas
 * @param publicada      si aparece en el portal
 * @param responsable    quien la coordina
 * @param enCurso        si se esta celebrando hoy
 * @param version  versión del registro para el bloqueo optimista
 */
public record JornadaRecord(
        Long id,
        String nombre,
        String descripcion,
        Integer municipioId,
        String municipio,
        String domicilioSede,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String horario,
        Integer cupo,
        Boolean publicada,
        OffsetDateTime fechaPublicacion,
        String responsable,
        boolean enCurso,
        Long version) {

    public static JornadaRecord de(Jornada jornada) {
        return new JornadaRecord(
                jornada.getId(),
                jornada.getNombre(),
                jornada.getDescripcion(),
                jornada.getMunicipio() == null ? null : jornada.getMunicipio().getId(),
                jornada.getMunicipio() == null ? null : jornada.getMunicipio().getNombre(),
                jornada.getDomicilioSede(),
                jornada.getFechaInicio(),
                jornada.getFechaFin(),
                jornada.getHorario(),
                jornada.getCupo(),
                jornada.getPublicada(),
                jornada.getFechaPublicacion(),
                jornada.getUsuarioResponsable() == null
                        ? null : jornada.getUsuarioResponsable().getNombreCompleto(),
                jornada.estaEnCurso(),
                jornada.getVersion());
    }
}
