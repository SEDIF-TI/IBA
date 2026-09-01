package mx.gob.sedif.sea.core.documental;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;

import mx.gob.sedif.sea.core.animal.Animal;
import mx.gob.sedif.sea.core.certificado.Certificado;
import mx.gob.sedif.sea.core.persona.Persona;
import mx.gob.sedif.sea.exception.AlmacenamientoException;

/**
 * Credencial de bolsillo, la "Credencial Peluda".
 *
 * <p>Van tres por hoja carta. No es un numero elegido al azar: asi las imprime
 * hoy el personal en opalina de 125 gramos, y cambiarlo desperdiciaria material
 * en cada tirada.
 *
 * <p>Se dibuja con coordenadas y no con tablas porque hay que colocar cada
 * credencial en una posicion exacta de la hoja, con sus marcas de corte.
 */
@Service
public class CredencialPdfService {

    private static final Logger log = LoggerFactory.getLogger(CredencialPdfService.class);

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Medidas de la credencial, en puntos (85.6 x 54 mm, como una tarjeta). */
    private static final float ANCHO = 242f;
    private static final float ALTO = 153f;

    /** Margen superior de la hoja. */
    private static final float MARGEN_SUPERIOR = 60f;

    /** Separacion entre credenciales, para poder cortar sin estropearlas. */
    private static final float SEPARACION = 26f;

    private final QrService qrService;
    private final IdentidadGrafica identidad;

    public CredencialPdfService(QrService qrService, IdentidadGrafica identidad) {
        this.qrService = qrService;
        this.identidad = identidad;
    }

    /** Genera una sola credencial en su hoja. */
    public byte[] generar(Certificado certificado, Persona persona, Animal animal) {
        return generarLote(List.of(new DatosCredencial(certificado, persona, animal)));
    }

    /**
     * Genera varias credenciales, tres por hoja.
     *
     * <p>Es lo que se usa el dia de una jornada: se validan los tramites y se
     * imprime todo de una vez.
     *
     * @param credenciales datos de cada credencial
     * @return contenido del PDF
     */
    public byte[] generarLote(List<DatosCredencial> credenciales) {
        if (credenciales.isEmpty()) {
            throw new IllegalArgumentException("No hay credenciales que imprimir");
        }

        Document documento = new Document(PageSize.LETTER, 0, 0, 0, 0);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();

        try {
            PdfWriter escritor = PdfWriter.getInstance(documento, salida);
            documento.addTitle("Credenciales de empadronamiento");
            documento.addCreator(identidad.nombreArea());
            documento.open();

            PdfContentByte lienzo = escritor.getDirectContent();
            int porHoja = identidad.credencialesPorHoja();
            float centroX = (PageSize.LETTER.getWidth() - ANCHO) / 2f;

            for (int i = 0; i < credenciales.size(); i++) {
                int posicionEnHoja = i % porHoja;

                if (i > 0 && posicionEnHoja == 0) {
                    documento.newPage();
                    lienzo = escritor.getDirectContent();
                }

                // Se cuenta desde arriba: la primera credencial va mas alta.
                float y = PageSize.LETTER.getHeight() - MARGEN_SUPERIOR
                        - (posicionEnHoja + 1) * ALTO
                        - posicionEnHoja * SEPARACION;

                dibujarCredencial(lienzo, credenciales.get(i), centroX, y);
                dibujarMarcasCorte(lienzo, centroX, y);
            }

            documento.close();

            byte[] pdf = salida.toByteArray();
            log.info("Se generaron {} credenciales en {} hoja(s)",
                    credenciales.size(),
                    (int) Math.ceil((double) credenciales.size() / porHoja));

            return pdf;

        } catch (RuntimeException ex) {
            throw new AlmacenamientoException("No se pudieron generar las credenciales", ex);
        }
    }

    /** Dibuja una credencial completa en la posicion indicada. */
    private void dibujarCredencial(PdfContentByte lienzo, DatosCredencial datos,
                                   float x, float y) {
        Color color = identidad.colorPrimario();

        // Marco de la credencial.
        lienzo.saveState();
        lienzo.setColorStroke(color);
        lienzo.setLineWidth(1.2f);
        lienzo.roundRectangle(x, y, ANCHO, ALTO, 8f);
        lienzo.stroke();

        // Franja superior con el nombre del padron.
        lienzo.setColorFill(color);
        lienzo.roundRectangle(x, y + ALTO - 30f, ANCHO, 30f, 8f);
        lienzo.fill();
        lienzo.restoreState();

        texto(lienzo, "PADRON ESTATAL DE PERROS Y GATOS",
              x + ANCHO / 2f, y + ALTO - 14f, 7.5f, Color.WHITE, true, Element.ALIGN_CENTER);
        texto(lienzo, identidad.siglas() + " - " + identidad.nombreInstitucion(),
              x + ANCHO / 2f, y + ALTO - 23f, 5.5f, Color.WHITE, false, Element.ALIGN_CENTER);

        // Datos del animal.
        float yTexto = y + ALTO - 48f;

        texto(lienzo, datos.animal().getNombre(),
              x + 12f, yTexto, 13f, Color.BLACK, true, Element.ALIGN_LEFT);
        yTexto -= 14f;

        String especieRaza = nombreDe(datos.animal().getEspecie());
        if (datos.animal().getRaza() != null) {
            especieRaza += " - " + datos.animal().getRaza().getNombre();
        }
        texto(lienzo, especieRaza, x + 12f, yTexto, 7.5f, Color.DARK_GRAY, false,
              Element.ALIGN_LEFT);
        yTexto -= 11f;

        String rasgos = nombreDe(datos.animal().getSexo());
        if (datos.animal().getColor() != null) {
            rasgos += " - " + datos.animal().getColor().getNombre();
        }
        if (datos.animal().getEdadDescriptiva() != null) {
            rasgos += " - " + datos.animal().getEdadDescriptiva();
        }
        texto(lienzo, rasgos, x + 12f, yTexto, 7f, Color.GRAY, false, Element.ALIGN_LEFT);
        yTexto -= 15f;

        texto(lienzo, "TUTOR RESPONSABLE",
              x + 12f, yTexto, 5.5f, color, true, Element.ALIGN_LEFT);
        yTexto -= 10f;
        texto(lienzo, datos.persona().getNombreCompleto(),
              x + 12f, yTexto, 8f, Color.BLACK, false, Element.ALIGN_LEFT);
        yTexto -= 14f;

        texto(lienzo, "Folio: " + datos.certificado().getFolio(),
              x + 12f, yTexto, 6f, Color.DARK_GRAY, false, Element.ALIGN_LEFT);
        yTexto -= 9f;

        if (datos.certificado().getFechaVigencia() != null) {
            texto(lienzo,
                  "Vigencia: " + datos.certificado().getFechaVigencia().format(FORMATO_FECHA),
                  x + 12f, yTexto, 6f, Color.DARK_GRAY, false, Element.ALIGN_LEFT);
        }

        // Codigo QR a la derecha.
        try {
            byte[] qr = qrService.generarParaCertificado(
                    datos.certificado().getCodigoVerificacion().toString(), 150);
            Image imagen = Image.getInstance(qr);
            imagen.scaleToFit(52f, 52f);
            imagen.setAbsolutePosition(x + ANCHO - 64f, y + 14f);
            lienzo.addImage(imagen);
        } catch (Exception ex) {
            log.warn("No se pudo insertar el codigo QR en la credencial {}",
                    datos.certificado().getFolio(), ex);
        }
    }

    /**
     * Marcas de corte en las esquinas.
     *
     * <p>Sirven para recortar derecho con guillotina o tijeras.
     */
    private void dibujarMarcasCorte(PdfContentByte lienzo, float x, float y) {
        final float largo = 8f;

        lienzo.saveState();
        lienzo.setColorStroke(new Color(0xCB, 0xD5, 0xE1));
        lienzo.setLineWidth(0.4f);

        float[][] esquinas = {
                {x, y}, {x + ANCHO, y}, {x, y + ALTO}, {x + ANCHO, y + ALTO},
        };

        for (float[] esquina : esquinas) {
            float ex = esquina[0];
            float ey = esquina[1];
            // Marca horizontal.
            lienzo.moveTo(ex - largo, ey);
            lienzo.lineTo(ex + largo, ey);
            // Marca vertical.
            lienzo.moveTo(ex, ey - largo);
            lienzo.lineTo(ex, ey + largo);
        }

        lienzo.stroke();
        lienzo.restoreState();
    }

    /** Escribe una linea de texto en una posicion exacta. */
    private void texto(PdfContentByte lienzo, String contenido, float x, float y,
                       float tamano, Color color, boolean negrita, int alineacion) {
        if (contenido == null || contenido.isBlank()) {
            return;
        }
        var fuente = FontFactory.getFont(
                negrita ? FontFactory.HELVETICA_BOLD : FontFactory.HELVETICA,
                tamano, color);

        com.lowagie.text.pdf.ColumnText.showTextAligned(
                lienzo, alineacion, new Phrase(contenido, fuente), x, y, 0);
    }

    private String nombreDe(mx.gob.sedif.sea.core.catalogo.CatalogoBase catalogo) {
        return catalogo == null ? "" : catalogo.getNombre();
    }

    /**
     * Datos de una credencial.
     *
     * @param certificado certificado emitido
     * @param persona     tutor responsable
     * @param animal      animal empadronado
     */
    public record DatosCredencial(
            Certificado certificado,
            Persona persona,
            Animal animal) {
    }
}
