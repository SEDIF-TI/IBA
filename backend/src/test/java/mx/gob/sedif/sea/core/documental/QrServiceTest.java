package mx.gob.sedif.sea.core.documental;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

/**
 * Verifica los codigos QR de los documentos.
 *
 * <p>Lo que importa no es que se produzca una imagen, sino que un lector real
 * pueda leerla: el QR va impreso en un documento que la gente escanea con su
 * telefono. Por eso cada caso decodifica el resultado.
 */
class QrServiceTest {

    private static final String URL_BASE = "https://sea.puebla.gob.mx/verificar";

    private QrService servicio;

    @BeforeEach
    void prepararServicio() {
        servicio = new QrService(URL_BASE);
    }

    @Test
    @DisplayName("el codigo generado se puede leer y contiene la direccion")
    void generaCodigoLegible() throws Exception {
        String codigo = "550e8400-e29b-41d4-a716-446655440000";

        byte[] png = servicio.generarParaCertificado(codigo, 300);
        String leido = decodificar(png);

        assertThat(leido)
                .as("el lector debe recuperar la direccion completa")
                .isEqualTo(URL_BASE + "/" + codigo);
    }

    @Test
    @DisplayName("produce una imagen PNG del tamano pedido")
    void produceImagenDelTamanoPedido() throws Exception {
        byte[] png = servicio.generar("prueba", 250);

        BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(png));
        assertThat(imagen).isNotNull();
        assertThat(imagen.getWidth()).isEqualTo(250);
        assertThat(imagen.getHeight()).isEqualTo(250);
    }

    @Test
    @DisplayName("se lee tambien en tamano pequeno, como va en la credencial")
    void seLeeEnTamanoPequeno() throws Exception {
        String codigo = "550e8400-e29b-41d4-a716-446655440000";

        // La credencial es de bolsillo: el codigo va chico y aun asi debe leerse.
        byte[] png = servicio.generarParaCertificado(codigo, 120);

        assertThat(decodificar(png)).contains(codigo);
    }

    @Test
    @DisplayName("arma la direccion sin duplicar la diagonal")
    void armaDireccionCorrecta() {
        QrService conDiagonal = new QrService("https://sea.puebla.gob.mx/verificar/");

        assertThat(conDiagonal.construirUrl("abc123"))
                .isEqualTo("https://sea.puebla.gob.mx/verificar/abc123")
                .doesNotContain("//verificar");
    }

    @Test
    @DisplayName("rechaza contenido vacio")
    void rechazaContenidoVacio() {
        assertThatThrownBy(() -> servicio.generar("", 200))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> servicio.generar(null, 200))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** Lee un codigo QR como lo haria un telefono. */
    private String decodificar(byte[] png) throws Exception {
        BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(png));
        var fuente = new BufferedImageLuminanceSource(imagen);
        var mapa = new BinaryBitmap(new HybridBinarizer(fuente));
        return new QRCodeReader().decode(mapa).getText();
    }
}
