package mx.gob.sedif.sea.core.solicitud;

import java.time.OffsetDateTime;

/**
 * Tramite tal como se muestra en el panel.
 *
 * <p>La CURP va enmascarada: en un listado no hay motivo para mostrarla
 * completa.
 */
public record SolicitudRecord(
        Long id,
        String folio,
        String estatusClave,
        String estatusNombre,
        String estatusColor,
        String canal,
        String tutorNombre,
        String tutorCurpEnmascarada,
        String tutorMunicipio,
        String animalNombre,
        String animalEspecie,
        Boolean revisionReforzada,
        String motivoRevisionReforzada,
        OffsetDateTime fechaRegistro,
        OffsetDateTime fechaResolucion) {

    public static SolicitudRecord de(Solicitud solicitud) {
        return new SolicitudRecord(
                solicitud.getId(),
                solicitud.getFolioSolicitud(),
                solicitud.getEstatus().getClave(),
                solicitud.getEstatus().getNombre(),
                solicitud.getEstatus().getColorUi(),
                solicitud.getCanal().name(),
                solicitud.getPersona().getNombreCompleto(),
                solicitud.getPersona().getCurpEnmascarada(),
                solicitud.getPersona().getMunicipio() == null
                        ? null : solicitud.getPersona().getMunicipio().getNombre(),
                solicitud.getAnimal().getNombre(),
                solicitud.getAnimal().getEspecie().getNombre(),
                solicitud.getRevisionReforzada(),
                solicitud.getMotivoRevisionReforzada(),
                solicitud.getFechaRegistro(),
                solicitud.getFechaResolucion());
    }
}
