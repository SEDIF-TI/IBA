package mx.gob.sedif.sea.core.documento;

import java.time.OffsetDateTime;

/**
 * Datos de un documento para la interfaz.
 *
 * <p>Contiene metadatos, nunca el contenido ni la ruta en disco: revelar la
 * ubicacion no serviria para acceder al archivo, pero tampoco hay motivo para
 * exponerla.
 *
 * @param id             identificador
 * @param tipoClave      clave del tipo de documento
 * @param tipoNombre     nombre del tipo, para mostrar
 * @param nombreOriginal nombre con el que se subio
 * @param mime           tipo de contenido
 * @param tamanoBytes    tamano del archivo
 * @param legible        resultado de la revision; nulo si no se ha revisado
 * @param observacion    nota del revisor
 * @param fechaCarga     cuando se adjunto
 */
public record DocumentoRecord(
        Long id,
        String tipoClave,
        String tipoNombre,
        String nombreOriginal,
        String mime,
        Long tamanoBytes,
        Boolean legible,
        String observacion,
        OffsetDateTime fechaCarga) {

    public static DocumentoRecord de(Documento documento) {
        return new DocumentoRecord(
                documento.getId(),
                documento.getTipoDocumento().getClave(),
                documento.getTipoDocumento().getNombre(),
                documento.getNombreOriginal(),
                documento.getMime(),
                documento.getTamanoBytes(),
                documento.getLegible(),
                documento.getObservacionRevision(),
                documento.getFechaCreacion());
    }
}
