package mx.gob.sedif.sea.core.solicitud;

import java.time.OffsetDateTime;

/**
 * Situacion del tramite para la consulta publica.
 *
 * <p>Deliberadamente escueto: muestra en que va el tramite sin exponer datos
 * personales. Quien consulta ya sabe de quien es, porque tuvo que dar el folio
 * y la CURP para llegar aqui.
 */
public record SeguimientoRecord(
        String folio,
        String estatusClave,
        String estatusNombre,
        String estatusDescripcion,
        String estatusColor,
        String animalNombre,
        Boolean requiereCorreccion,
        String motivo,
        String observacion,
        OffsetDateTime fechaRegistro,
        OffsetDateTime fechaResolucion) {

    public static SeguimientoRecord de(Solicitud solicitud) {
        boolean requiereCorreccion = "OBSERVADA".equals(solicitud.getEstatus().getClave());

        return new SeguimientoRecord(
                solicitud.getFolioSolicitud(),
                solicitud.getEstatus().getClave(),
                solicitud.getEstatus().getNombre(),
                solicitud.getEstatus().getDescripcion(),
                solicitud.getEstatus().getColorUi(),
                solicitud.getAnimal().getNombre(),
                requiereCorreccion,
                solicitud.getMotivoRechazo() == null
                        ? null : solicitud.getMotivoRechazo().getNombre(),
                solicitud.getObservacionResolucion(),
                solicitud.getFechaRegistro(),
                solicitud.getFechaResolucion());
    }
}
