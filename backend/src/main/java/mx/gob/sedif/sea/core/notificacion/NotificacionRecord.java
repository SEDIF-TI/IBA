package mx.gob.sedif.sea.core.notificacion;

import java.time.OffsetDateTime;

/**
 * Aviso tal como se muestra en la bandeja de contacto.
 *
 * @param id             identificador
 * @param folioSolicitud tramite del que se avisa
 * @param tutorNombre    a quien hay que contactar
 * @param destinatario   telefono o correo
 * @param canal          si sale solo o lo tiene que dar una persona
 * @param asunto         asunto del aviso
 * @param cuerpo         texto que se le debe comunicar
 * @param estatus        situacion del envio
 * @param intentos       cuantas veces se intento
 * @param error          por que fallo, si aplica
 * @param observacion    resultado del contacto telefonico
 * @param atendioUsuario quien hizo la llamada
 * @param fechaCreacion  cuando se genero
 * @param fechaContacto  cuando se atendio
 */
public record NotificacionRecord(
        Long id,
        String folioSolicitud,
        String tutorNombre,
        String destinatario,
        String canal,
        String asunto,
        String cuerpo,
        String estatus,
        Integer intentos,
        String error,
        String observacion,
        String atendioUsuario,
        OffsetDateTime fechaCreacion,
        OffsetDateTime fechaContacto) {

    public static NotificacionRecord de(Notificacion aviso) {
        return new NotificacionRecord(
                aviso.getId(),
                aviso.getSolicitud().getFolioSolicitud(),
                aviso.getSolicitud().getPersona().getNombreCompleto(),
                aviso.getDestinatario(),
                aviso.getCanal().name(),
                aviso.getAsunto(),
                aviso.getCuerpo(),
                aviso.getEstatusEnvio().name(),
                aviso.getIntentos(),
                aviso.getError(),
                aviso.getObservacionContacto(),
                aviso.getUsuarioContacto() == null
                        ? null : aviso.getUsuarioContacto().getNombreCompleto(),
                aviso.getFechaCreacion(),
                aviso.getFechaContacto());
    }
}
