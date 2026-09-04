package mx.gob.sedif.sea.core.animal;

import java.time.LocalDate;

import mx.gob.sedif.sea.core.titularidad.Titularidad;

/**
 * Un tramo del historial de tutores de un animal.
 *
 * @param id          identificador
 * @param tutorId     tutor de este tramo
 * @param tutorNombre nombre del tutor
 * @param fechaInicio desde cuando responde por el animal
 * @param fechaFin    hasta cuando; nulo si sigue vigente
 * @param motivoBaja  por que dejo de responder
 * @param vigente     si es el tutor actual
 */
public record TitularidadRecord(
        Long id,
        Long tutorId,
        String tutorNombre,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String motivoBaja,
        Boolean vigente) {

    public static TitularidadRecord de(Titularidad titularidad) {
        return new TitularidadRecord(
                titularidad.getId(),
                titularidad.getPersona().getId(),
                titularidad.getPersona().getNombreCompleto(),
                titularidad.getFechaInicio(),
                titularidad.getFechaFin(),
                titularidad.getMotivoBaja(),
                titularidad.getVigente());
    }
}
