package mx.gob.sedif.sea.core.documental;

/**
 * Firma electronica de los documentos que expide el instituto.
 *
 * <p>La firma electronica avanzada sigue pendiente de resolucion juridica, asi
 * que esta interfaz existe para que la decision no obligue a rehacer nada: hoy
 * opera la implementacion que no firma, y cuando el area juridica resuelva se
 * conecta otra sin tocar el resto del sistema.
 *
 * <p>Mientras tanto los documentos salen con codigo QR de verificacion y la
 * leyenda que indica donde acudir por el sello institucional.
 */
public interface FirmaService {

    /**
     * Firma el documento.
     *
     * @param documento contenido del PDF
     * @param contexto  datos del certificado que se esta firmando
     * @return resultado de la operacion
     */
    ResultadoFirma firmar(byte[] documento, ContextoFirma contexto);

    /**
     * Indica si la firma esta disponible.
     *
     * <p>La interfaz la consulta para saber si mostrar la leyenda del sello
     * fisico o la constancia de firma.
     */
    boolean estaDisponible();

    /**
     * Datos del documento que se firma.
     *
     * @param folioCertificado folio oficial
     * @param hashDocumento    huella del PDF
     * @param usuarioEmisor    quien lo expide
     */
    record ContextoFirma(
            String folioCertificado,
            String hashDocumento,
            String usuarioEmisor) {
    }

    /**
     * Resultado de firmar.
     *
     * @param firmado          si el documento quedo firmado
     * @param documentoFirmado contenido resultante; el original si no se firmo
     * @param sello            constancia de la firma, nulo si no se firmo
     * @param mensaje          explicacion, util cuando no se firmo
     */
    record ResultadoFirma(
            boolean firmado,
            byte[] documentoFirmado,
            String sello,
            String mensaje) {

        /** Resultado cuando la firma no esta disponible. */
        public static ResultadoFirma sinFirmar(byte[] documento, String motivo) {
            return new ResultadoFirma(false, documento, null, motivo);
        }

        /** Resultado de una firma aplicada. */
        public static ResultadoFirma firmado(byte[] documento, String sello) {
            return new ResultadoFirma(true, documento, sello, "Documento firmado");
        }
    }
}
