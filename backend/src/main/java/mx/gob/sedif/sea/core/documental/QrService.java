package mx.gob.sedif.sea.core.documental;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import mx.gob.sedif.sea.exception.AlmacenamientoException;

/**
 * Codigos QR de verificacion.
 *
 * <p>Cada documento lleva uno que apunta al verificador publico. Quien lo
 * escanea comprueba que el certificado es autentico y ve los datos del animal;
 * nunca informacion personal del tutor.
 */
@Service
public class QrService {

    /**
     * Nivel de correccion de errores alto.
     *
     * <p>Permite reconstruir el codigo aunque una parte quede ilegible. Importa
     * porque estos documentos se doblan, se guardan en la cartera y se mojan:
     * un QR que deja de leerse al primer maltrato no sirve de nada.
     */
    private static final ErrorCorrectionLevel CORRECCION = ErrorCorrectionLevel.H;

    /** Margen alrededor del codigo, en modulos. Sin el, los lectores fallan. */
    private static final int MARGEN = 1;

    private final String urlVerificacion;

    public QrService(@Value("${app.verificacion.url:http://localhost/verificar}")
                     String urlVerificacion) {
        this.urlVerificacion = urlVerificacion;
    }

    /**
     * Genera el QR de un certificado.
     *
     * @param codigoVerificacion codigo opaco del certificado
     * @param tamano             lado del cuadro en pixeles
     * @return imagen PNG del codigo
     */
    public byte[] generarParaCertificado(String codigoVerificacion, int tamano) {
        return generar(construirUrl(codigoVerificacion), tamano);
    }

    /** Direccion a la que apunta el codigo. */
    public String construirUrl(String codigoVerificacion) {
        String base = urlVerificacion.endsWith("/")
                ? urlVerificacion.substring(0, urlVerificacion.length() - 1)
                : urlVerificacion;
        return base + "/" + codigoVerificacion;
    }

    /**
     * Genera un codigo QR a partir de un texto.
     *
     * @param contenido texto a codificar
     * @param tamano    lado del cuadro en pixeles
     * @return imagen PNG
     */
    public byte[] generar(String contenido, int tamano) {
        if (contenido == null || contenido.isBlank()) {
            throw new IllegalArgumentException("El contenido del código no puede ir vacío");
        }

        try {
            Map<EncodeHintType, Object> opciones = Map.of(
                    EncodeHintType.ERROR_CORRECTION, CORRECCION,
                    EncodeHintType.CHARACTER_SET, "UTF-8",
                    EncodeHintType.MARGIN, MARGEN);

            BitMatrix matriz = new QRCodeWriter()
                    .encode(contenido, BarcodeFormat.QR_CODE, tamano, tamano, opciones);

            BufferedImage imagen = MatrixToImageWriter.toBufferedImage(matriz);

            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            ImageIO.write(imagen, "PNG", salida);
            return salida.toByteArray();

        } catch (WriterException | IOException ex) {
            throw new AlmacenamientoException("No se pudo generar el código QR", ex);
        }
    }
}
