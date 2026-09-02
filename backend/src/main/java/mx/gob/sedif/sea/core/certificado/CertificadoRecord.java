package mx.gob.sedif.sea.core.certificado;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Certificado tal como se muestra en el panel.
 *
 * @param id                identificador
 * @param folio             folio oficial
 * @param codigoVerificacion codigo del QR
 * @param folioSolicitud    folio del tramite de origen
 * @param animalNombre      animal empadronado
 * @param tutorNombre       tutor responsable
 * @param fechaEmision      cuando se emitio
 * @param fechaVigencia     hasta cuando surte efecto
 * @param vigente           si sigue surtiendo efecto
 * @param motivoCancelacion por que se cancelo, si aplica
 * @param entregado         si ya se entrego con su sello
 * @param firmado           si lleva firma electronica
 */
public record CertificadoRecord(
        Long id,
        String folio,
        String codigoVerificacion,
        String folioSolicitud,
        String animalNombre,
        String tutorNombre,
        OffsetDateTime fechaEmision,
        LocalDate fechaVigencia,
        Boolean vigente,
        String motivoCancelacion,
        Boolean entregado,
        Boolean firmado) {

    public static CertificadoRecord de(Certificado certificado) {
        var solicitud = certificado.getSolicitud();

        return new CertificadoRecord(
                certificado.getId(),
                certificado.getFolio(),
                certificado.getCodigoVerificacion().toString(),
                solicitud.getFolioSolicitud(),
                solicitud.getAnimal().getNombre(),
                solicitud.getPersona().getNombreCompleto(),
                certificado.getFechaEmision(),
                certificado.getFechaVigencia(),
                certificado.getVigente(),
                certificado.getMotivoCancelacion(),
                certificado.getSelloFisicoEntregado(),
                certificado.getFirmadoElectronicamente());
    }
}
