package mx.gob.sedif.sea.core.documental;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfObject;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PRStream;

import mx.gob.sedif.sea.core.parametro.ParametroService;

/**
 * Comprueba que el codigo QR embebido en el documento se pueda leer.
 *
 * <p>Es la prueba que de verdad importa para el verificador publico: el codigo
 * viaja dentro de un PDF que la gente imprime y escanea con el telefono. Que se
 * genere una imagen no garantiza que un lector la entienda una vez incrustada.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QrEnPdfTest {

    private static final String CODIGO = "550e8400-e29b-41d4-a716-446655440000";

    @Mock
    private ParametroService parametros;

    private CertificadoPdfService certificadoPdf;

    @BeforeEach
    void prepararEscenario() {
        when(parametros.obtener(any(), any()))
                .thenAnswer(invocacion -> invocacion.getArgument(1));
        when(parametros.obtenerEntero(any(), anyInt()))
                .thenAnswer(invocacion -> invocacion.getArgument(1));

        certificadoPdf = new CertificadoPdfService(
                new QrService("https://sea.puebla.gob.mx/verificar"),
                new IdentidadGrafica(parametros),
                new FirmaServiceStub("Acude por tu sello."));
    }

    @Test
    @DisplayName("el codigo incrustado en el certificado se lee y lleva al verificador")
    void elCodigoDelCertificadoSeLee() throws Exception {
        byte[] pdf = certificadoPdf.generar(
                PdfGeneracionTest.TestFactoria.certificado(),
                PdfGeneracionTest.TestFactoria.persona(),
                PdfGeneracionTest.TestFactoria.animal(),
                java.util.Optional.empty());

        BufferedImage qr = extraerPrimeraImagen(pdf);
        assertThat(qr).as("el certificado debe llevar el codigo incrustado").isNotNull();

        String leido = decodificar(qr);
        assertThat(leido)
                .as("el codigo debe llevar al verificador publico del certificado")
                .isEqualTo("https://sea.puebla.gob.mx/verificar/" + CODIGO);
    }

    /**
     * Saca la primera imagen incrustada en el documento.
     *
     * <p>Se usa el lector de imagenes de la propia biblioteca: los bytes en
     * crudo del flujo vienen con la compresion del PDF y no son un PNG que se
     * pueda abrir directamente.
     */
    private BufferedImage extraerPrimeraImagen(byte[] pdf) throws Exception {
        PdfReader lector = new PdfReader(pdf);

        for (int numero = 1; numero <= lector.getNumberOfPages(); numero++) {
            PdfDictionary recursos = lector.getPageN(numero).getAsDict(PdfName.RESOURCES);
            if (recursos == null) {
                continue;
            }
            PdfDictionary objetos = recursos.getAsDict(PdfName.XOBJECT);
            if (objetos == null) {
                continue;
            }

            for (PdfName clave : objetos.getKeys()) {
                PdfObject objeto = objetos.getDirectObject(clave);
                if (!objeto.isStream()) {
                    continue;
                }
                PRStream flujo = (PRStream) objeto;
                if (!PdfName.IMAGE.equals(flujo.getAsName(PdfName.SUBTYPE))) {
                    continue;
                }

                // La biblioteca no guarda el PNG tal cual: lo convierte a
                // pixeles crudos comprimidos con Flate. Hay que descomprimir y
                // rearmar la imagen a partir de sus dimensiones.
                byte[] pixeles = PdfReader.getStreamBytes(flujo);
                int ancho = flujo.getAsNumber(PdfName.WIDTH).intValue();
                int alto = flujo.getAsNumber(PdfName.HEIGHT).intValue();

                BufferedImage imagen = rearmar(pixeles, ancho, alto);
                if (imagen != null) {
                    return imagen;
                }
            }
        }
        return null;
    }

    /**
     * Rearma la imagen a partir de los pixeles descomprimidos.
     *
     * <p>La biblioteca guarda tres bytes por pixel aunque el codigo sea en
     * blanco y negro; se deduce cuantos hay por el tamano del bloque.
     */
    private BufferedImage rearmar(byte[] pixeles, int ancho, int alto) {
        int porPixel = pixeles.length / (ancho * alto);
        if (porPixel < 1) {
            return null;
        }

        BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                int posicion = (y * ancho + x) * porPixel;
                if (posicion + porPixel > pixeles.length) {
                    return null;
                }
                int rojo = pixeles[posicion] & 0xFF;
                int verde = porPixel > 1 ? pixeles[posicion + 1] & 0xFF : rojo;
                int azul = porPixel > 2 ? pixeles[posicion + 2] & 0xFF : rojo;
                imagen.setRGB(x, y, (rojo << 16) | (verde << 8) | azul);
            }
        }
        return imagen;
    }

    /** Lee un codigo QR como lo haria un telefono. */
    private String decodificar(BufferedImage imagen) throws Exception {
        var fuente = new BufferedImageLuminanceSource(imagen);
        var mapa = new BinaryBitmap(new HybridBinarizer(fuente));
        return new QRCodeReader().decode(mapa).getText();
    }
}
