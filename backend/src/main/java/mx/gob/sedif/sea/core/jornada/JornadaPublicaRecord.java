package mx.gob.sedif.sea.core.jornada;

import java.time.LocalDate;

/**
 * Jornada tal como la ve la ciudadania en el calendario del portal.
 *
 * <p>Sin el responsable ni datos internos: a quien va a asistir le interesa
 * donde, cuando y en que horario.
 *
 * @param id            identificador, para precargar el registro
 * @param nombre        como se anuncia
 * @param descripcion   detalle
 * @param municipio     municipio donde se celebra
 * @param domicilioSede direccion exacta del lugar
 * @param fechaInicio   primer dia
 * @param fechaFin      ultimo dia
 * @param horario       horario de atencion
 */
public record JornadaPublicaRecord(
        Long id,
        String nombre,
        String descripcion,
        String municipio,
        String domicilioSede,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String horario) {

    public static JornadaPublicaRecord de(Jornada jornada) {
        return new JornadaPublicaRecord(
                jornada.getId(),
                jornada.getNombre(),
                jornada.getDescripcion(),
                jornada.getMunicipio() == null ? null : jornada.getMunicipio().getNombre(),
                jornada.getDomicilioSede(),
                jornada.getFechaInicio(),
                jornada.getFechaFin(),
                jornada.getHorario());
    }
}
