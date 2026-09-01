package mx.gob.sedif.sea.core.documental;

import java.awt.Color;

import com.lowagie.text.Document;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PdfPageEventHelper;

/**
 * Fondo de huellas del certificado y la credencial.
 *
 * <p>Se dibuja con vectores en lugar de una imagen: pesa unos bytes, se ve
 * nitido a cualquier tamano de impresion y no depende de un archivo que alguien
 * pueda mover de sitio.
 *
 * <p>Va muy tenue y por debajo del contenido. Un fondo con mas presencia
 * competiria con el texto y estropearia su legibilidad, que en un documento
 * oficial importa mas que el adorno.
 */
public class FondoHuellas extends PdfPageEventHelper {

    /** Separacion entre huellas, en puntos. */
    private static final float SEPARACION = 70f;

    /** Tamano de cada huella. */
    private static final float ESCALA = 0.55f;

    /** Muy tenue a proposito: el texto va encima y debe leerse sin esfuerzo. */
    private static final float OPACIDAD = 0.045f;

    private final Color color;

    public FondoHuellas(Color color) {
        this.color = color;
    }

    @Override
    public void onEndPage(PdfWriter writer, Document documento) {
        PdfContentByte lienzo = writer.getDirectContentUnder();
        Rectangle pagina = documento.getPageSize();

        lienzo.saveState();

        com.lowagie.text.pdf.PdfGState estado = new com.lowagie.text.pdf.PdfGState();
        estado.setFillOpacity(OPACIDAD);
        lienzo.setGState(estado);
        lienzo.setColorFill(color);

        // Cuadricula al tresbolillo: las filas impares van desplazadas media
        // separacion, para que no se lea como una retícula rígida.
        int fila = 0;
        for (float y = 40f; y < pagina.getHeight() - 20f; y += SEPARACION) {
            float desplazamiento = (fila % 2 == 0) ? 0f : SEPARACION / 2f;
            for (float x = 30f + desplazamiento; x < pagina.getWidth() - 20f; x += SEPARACION) {
                dibujarHuella(lienzo, x, y, ESCALA);
            }
            fila++;
        }

        lienzo.restoreState();
    }

    /**
     * Dibuja una huella: cuatro dedos y la almohadilla.
     *
     * @param x escala horizontal de origen
     * @param y escala vertical de origen
     * @param escala tamano relativo
     */
    private void dibujarHuella(PdfContentByte lienzo, float x, float y, float escala) {
        // Almohadilla central.
        elipse(lienzo, x, y, 11f * escala, 9f * escala);

        // Los cuatro dedos, en arco sobre la almohadilla.
        elipse(lienzo, x - 9f * escala, y + 11f * escala, 4f * escala, 5.5f * escala);
        elipse(lienzo, x - 3f * escala, y + 15f * escala, 4f * escala, 5.5f * escala);
        elipse(lienzo, x + 3f * escala, y + 15f * escala, 4f * escala, 5.5f * escala);
        elipse(lienzo, x + 9f * escala, y + 11f * escala, 4f * escala, 5.5f * escala);
    }

    /**
     * Elipse aproximada con cuatro curvas.
     *
     * <p>La constante 0.5523 es la proporcion que hace que una curva de Bezier
     * se aproxime a un cuarto de circunferencia.
     */
    private void elipse(PdfContentByte lienzo, float cx, float cy, float rx, float ry) {
        final float k = 0.5523f;

        lienzo.moveTo(cx - rx, cy);
        lienzo.curveTo(cx - rx, cy + ry * k, cx - rx * k, cy + ry, cx, cy + ry);
        lienzo.curveTo(cx + rx * k, cy + ry, cx + rx, cy + ry * k, cx + rx, cy);
        lienzo.curveTo(cx + rx, cy - ry * k, cx + rx * k, cy - ry, cx, cy - ry);
        lienzo.curveTo(cx - rx * k, cy - ry, cx - rx, cy - ry * k, cx - rx, cy);
        lienzo.fill();
    }
}
