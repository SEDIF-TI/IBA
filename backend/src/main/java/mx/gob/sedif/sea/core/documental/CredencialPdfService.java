package mx.gob.sedif.sea.core.documental;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfWriter;

import mx.gob.sedif.sea.core.animal.Animal;
import mx.gob.sedif.sea.core.certificado.Certificado;
import mx.gob.sedif.sea.core.persona.Persona;
import mx.gob.sedif.sea.exception.AlmacenamientoException;

/**
 * Credencial Peluda, siguiendo el formato oficial del instituto.
 *
 * <p>Son dos caras que se imprimen lado a lado y se recortan juntas:
 * <ul>
 *   <li><b>Frente</b>: foto, nombre del animal y sus rasgos.</li>
 *   <li><b>Reverso</b>: datos del tutor responsable y la firma.</li>
 * </ul>
 *
 * <p>Van tres pares por hoja. No es un numero arbitrario: asi las imprime hoy
 * el personal en opalina de 125 gramos, y cambiarlo desperdiciaria material en
 * cada tirada.
 */
@Service
public class CredencialPdfService {

    private static final Logger log = LoggerFactory.getLogger(CredencialPdfService.class);

    private static final DateTimeFormatter FECHA_CORTA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Medidas de cada cara, en puntos. Proporcion de tarjeta de bolsillo. */
    private static final float ANCHO = 250f;
    private static final float ALTO = 158f;

    /** Separacion entre el frente y el reverso de la misma credencial. */
    private static final float SEPARACION_CARAS = 14f;

    /** Separacion vertical entre credenciales, para poder recortar. */
    private static final float SEPARACION_FILAS = 24f;

    private static final float MARGEN_SUPERIOR = 56f;

    private final QrService qrService;
    private final IdentidadGrafica identidad;

    public CredencialPdfService(QrService qrService, IdentidadGrafica identidad) {
        this.qrService = qrService;
        this.identidad = identidad;
    }

    /** Genera una sola credencial. */
    public byte[] generar(Certificado certificado, Persona persona, Animal animal,
                          Optional<byte[]> fotografia) {
        return generarLote(List.of(
                new DatosCredencial(certificado, persona, animal, fotografia)));
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

            float anchoPar = ANCHO * 2 + SEPARACION_CARAS;
            float xInicio = (PageSize.LETTER.getWidth() - anchoPar) / 2f;

            for (int i = 0; i < credenciales.size(); i++) {
                int posicionEnHoja = i % porHoja;

                if (i > 0 && posicionEnHoja == 0) {
                    documento.newPage();
                    lienzo = escritor.getDirectContent();
                }

                float y = PageSize.LETTER.getHeight() - MARGEN_SUPERIOR
                        - (posicionEnHoja + 1) * ALTO
                        - posicionEnHoja * SEPARACION_FILAS;

                DatosCredencial datos = credenciales.get(i);
                dibujarFrente(lienzo, datos, xInicio, y);
                dibujarReverso(lienzo, datos, xInicio + ANCHO + SEPARACION_CARAS, y);

                marcasCorte(lienzo, xInicio, y, ANCHO);
                marcasCorte(lienzo, xInicio + ANCHO + SEPARACION_CARAS, y, ANCHO);
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

    /** Frente: foto del animal y sus datos. */
    private void dibujarFrente(PdfContentByte lienzo, DatosCredencial datos,
                               float x, float y) {
        Color colorTitulo = identidad.colorTitulo();
        Color colorEtiqueta = identidad.colorEtiquetas();

        marco(lienzo, x, y, colorTitulo);
        fondoHuellas(lienzo, x, y);

        // Recuadro de la fotografia.
        float fotoAncho = 62f;
        float fotoAlto = 68f;
        float fotoX = x + 12f;
        float fotoY = y + ALTO - fotoAlto - 36f;

        lienzo.saveState();
        PdfGState suave = new PdfGState();
        suave.setFillOpacity(0.12f);
        lienzo.setGState(suave);
        lienzo.setColorFill(colorTitulo);
        lienzo.roundRectangle(fotoX, fotoY, fotoAncho, fotoAlto, 6f);
        lienzo.fill();
        lienzo.restoreState();

        datos.fotografia().ifPresent(bytes -> {
            try {
                Image imagen = Image.getInstance(bytes);
                imagen.scaleToFit(fotoAncho - 4f, fotoAlto - 4f);
                imagen.setAbsolutePosition(
                        fotoX + (fotoAncho - imagen.getScaledWidth()) / 2f,
                        fotoY + (fotoAlto - imagen.getScaledHeight()) / 2f);
                lienzo.addImage(imagen);
            } catch (Exception ex) {
                log.warn("No se pudo insertar la fotografia en la credencial", ex);
            }
        });

        // Titulo y cinta.
        float xTexto = fotoX + fotoAncho + 12f;
        float yTitulo = y + ALTO - 26f;

        texto(lienzo, "Credencial Peluda", xTexto, yTitulo, 14f, colorTitulo, true,
              Element.ALIGN_LEFT);

        float anchoCinta = 150f;
        lienzo.saveState();
        lienzo.setColorFill(identidad.colorCinta());
        lienzo.roundRectangle(xTexto, yTitulo - 17f, anchoCinta, 13f, 4f);
        lienzo.fill();
        lienzo.restoreState();

        texto(lienzo, "Padrón Estatal de Perros y Gatos", xTexto + anchoCinta / 2f,
              yTitulo - 13.5f, 6f, Color.WHITE, true, Element.ALIGN_CENTER);

        // Nombre y folio, junto a la foto.
        float yDato = yTitulo - 34f;
        campoLinea(lienzo, "Nombre:", datos.animal().getNombre(),
                   xTexto, yDato, colorEtiqueta, 7f, 8.5f, 42f);
        yDato -= 15f;
        campoLinea(lienzo, "Folio:", datos.certificado().getFolio(),
                   xTexto, yDato, colorEtiqueta, 7f, 6.5f, 32f);

        // Rasgos del animal, en dos columnas bajo la foto.
        // Dos columnas. Los valores se recortan a lo que cabe en su columna:
        // sin eso, un dato largo se monta sobre la etiqueta de al lado.
        float yRasgos = fotoY - 14f;
        float col1 = x + 12f;
        float col2 = x + 124f;

        campoLinea(lienzo, "Edad:", recortar(datos.animal().getEdadDescriptiva(), 14),
                   col1, yRasgos, colorEtiqueta, 6.5f, 6.5f, 26f);
        campoLinea(lienzo, "Raza:", recortar(nombreDe(datos.animal().getRaza()), 16),
                   col2, yRasgos, colorEtiqueta, 6.5f, 6.5f, 26f);
        yRasgos -= 13f;

        campoLinea(lienzo, "Color:", recortar(nombreDe(datos.animal().getColor()), 14),
                   col1, yRasgos, colorEtiqueta, 6.5f, 6.5f, 28f);
        campoLinea(lienzo, "Sexo:", recortar(nombreDe(datos.animal().getSexo()), 16),
                   col2, yRasgos, colorEtiqueta, 6.5f, 6.5f, 26f);
        yRasgos -= 13f;

        campoLinea(lienzo, "Especie:", recortar(nombreDe(datos.animal().getEspecie()), 12),
                   col1, yRasgos, colorEtiqueta, 6.5f, 6.5f, 32f);
        yRasgos -= 13f;

        // Las senas particulares ocupan el ancho completo: es el campo mas largo.
        campoLinea(lienzo, "Características:",
                   recortar(datos.animal().getCaracteristicas(), 34),
                   col1, yRasgos, colorEtiqueta, 6.5f, 6.5f, 52f);

        // Franja institucional al pie.
        franjaInstitucional(lienzo, x, y, ANCHO);
    }

    /** Reverso: datos del tutor responsable, codigo QR y firma. */
    private void dibujarReverso(PdfContentByte lienzo, DatosCredencial datos,
                                float x, float y) {
        Color colorTitulo = identidad.colorTitulo();
        Color colorEtiqueta = identidad.colorEtiquetas();

        marco(lienzo, x, y, colorTitulo);
        fondoHuellas(lienzo, x, y);

        // Franja institucional arriba, como en el formato.
        franjaInstitucional(lienzo, x, y + ALTO - 26f, ANCHO);

        float xTexto = x + 14f;
        float yDato = y + ALTO - 48f;

        campoLinea(lienzo, "Responsable:", datos.persona().getNombreCompleto(),
                   xTexto, yDato, colorEtiqueta, 6.5f, 7.5f, 52f);
        yDato -= 17f;

        campoLinea(lienzo, "Dirección:", recortar(domicilioDe(datos.persona()), 44),
                   xTexto, yDato, colorEtiqueta, 6.5f, 6.5f, 44f);
        yDato -= 17f;

        campoLinea(lienzo, "Teléfono:", datos.persona().getTelefono(),
                   xTexto, yDato, colorEtiqueta, 6.5f, 7f, 42f);
        yDato -= 17f;

        campoLinea(lienzo, "Correo electrónico:", recortar(datos.persona().getCorreo(), 32),
                   xTexto, yDato, colorEtiqueta, 6.5f, 6.5f, 68f);
        yDato -= 17f;

        campoLinea(lienzo, "No. de ID oficial:", datos.animal().getClaveAnimal(),
                   xTexto, yDato, colorEtiqueta, 6.5f, 7f, 62f);

        // Codigo QR de verificacion, en la esquina.
        try {
            byte[] qr = qrService.generarParaCertificado(
                    datos.certificado().getCodigoVerificacion().toString(), 140);
            Image imagen = Image.getInstance(qr);
            imagen.scaleToFit(44f, 44f);
            imagen.setAbsolutePosition(x + ANCHO - 56f, y + 40f);
            lienzo.addImage(imagen);
        } catch (Exception ex) {
            log.warn("No se pudo insertar el codigo QR en la credencial {}",
                    datos.certificado().getFolio(), ex);
        }

        if (datos.certificado().getFechaVigencia() != null) {
            texto(lienzo,
                  "Vigencia: " + datos.certificado().getFechaVigencia().format(FECHA_CORTA),
                  xTexto, y + 34f, 6f, Color.DARK_GRAY, false, Element.ALIGN_LEFT);
        }

        // Firma de quien autoriza.
        texto(lienzo, identidad.firmanteNombre(), xTexto, y + 20f, 6.5f,
              colorTitulo, true, Element.ALIGN_LEFT);
        texto(lienzo, identidad.firmanteCargo(), xTexto, y + 12f, 5f,
              Color.DARK_GRAY, false, Element.ALIGN_LEFT);
    }

    // ---- Piezas comunes ----

    /** Marco redondeado de una cara. */
    private void marco(PdfContentByte lienzo, float x, float y, Color color) {
        lienzo.saveState();
        lienzo.setColorStroke(color);
        lienzo.setLineWidth(1f);
        lienzo.roundRectangle(x, y, ANCHO, ALTO, 8f);
        lienzo.stroke();
        lienzo.restoreState();
    }

    /** Franja con la identidad institucional. */
    private void franjaInstitucional(PdfContentByte lienzo, float x, float y, float ancho) {
        Color color = identidad.colorFranja();

        lienzo.saveState();
        PdfGState suave = new PdfGState();
        suave.setFillOpacity(0.10f);
        lienzo.setGState(suave);
        lienzo.setColorFill(color);
        lienzo.rectangle(x + 1f, y + 1f, ancho - 2f, 24f);
        lienzo.fill();
        lienzo.restoreState();

        float yTexto = y + 10f;
        texto(lienzo, "GOBIERNO DEL ESTADO", x + 10f, yTexto, 5f, color, true,
              Element.ALIGN_LEFT);
        texto(lienzo, "Familias", x + 92f, yTexto, 5.5f, color, true, Element.ALIGN_LEFT);
        texto(lienzo, identidad.areaSiglas(), x + 140f, yTexto, 6.5f, color, true,
              Element.ALIGN_LEFT);
        texto(lienzo, "POR AMOR A PUEBLA", x + ancho - 12f, yTexto, 5f, color, true,
              Element.ALIGN_RIGHT);
    }

    /** Huellas decorativas en las esquinas de la credencial. */
    private void fondoHuellas(PdfContentByte lienzo, float x, float y) {
        lienzo.saveState();
        PdfGState tenue = new PdfGState();
        tenue.setFillOpacity(0.13f);
        lienzo.setGState(tenue);
        lienzo.setColorFill(identidad.colorFondoHuellas());

        float[][] posiciones = {
                {x + 16f, y + ALTO - 22f}, {x + ANCHO - 24f, y + ALTO - 30f},
                {x + 12f, y + 46f}, {x + ANCHO - 18f, y + 60f},
        };
        for (float[] posicion : posiciones) {
            huella(lienzo, posicion[0], posicion[1], 0.45f);
        }

        lienzo.restoreState();
    }

    /** Marcas de corte en las esquinas. */
    private void marcasCorte(PdfContentByte lienzo, float x, float y, float ancho) {
        final float largo = 7f;

        lienzo.saveState();
        lienzo.setColorStroke(new Color(0xCB, 0xD5, 0xE1));
        lienzo.setLineWidth(0.35f);

        float[][] esquinas = {
                {x, y}, {x + ancho, y}, {x, y + ALTO}, {x + ancho, y + ALTO},
        };
        for (float[] esquina : esquinas) {
            lienzo.moveTo(esquina[0] - largo, esquina[1]);
            lienzo.lineTo(esquina[0] + largo, esquina[1]);
            lienzo.moveTo(esquina[0], esquina[1] - largo);
            lienzo.lineTo(esquina[0], esquina[1] + largo);
        }

        lienzo.stroke();
        lienzo.restoreState();
    }

    /**
     * Etiqueta y valor en la misma linea.
     *
     * <p>La etiqueta se dibuja siempre, aunque falte el valor: el formato
     * oficial las lleva impresas.
     */
    private void campoLinea(PdfContentByte lienzo, String etiqueta, String valor,
                            float x, float y, Color colorEtiqueta,
                            float tamanoEtiqueta, float tamanoValor, float sangria) {
        texto(lienzo, etiqueta, x, y, tamanoEtiqueta, colorEtiqueta, true,
              Element.ALIGN_LEFT);
        texto(lienzo, valor, x + sangria, y, tamanoValor, Color.BLACK, false,
              Element.ALIGN_LEFT);
    }

    private void texto(PdfContentByte lienzo, String contenido, float x, float y,
                       float tamano, Color color, boolean negrita, int alineacion) {
        if (contenido == null || contenido.isBlank()) {
            return;
        }
        var fuente = FontFactory.getFont(
                negrita ? FontFactory.HELVETICA_BOLD : FontFactory.HELVETICA, tamano, color);
        ColumnText.showTextAligned(lienzo, alineacion, new Phrase(contenido, fuente), x, y, 0);
    }

    /** Recorta el texto que no cabe en el espacio de la credencial. */
    private String recortar(String texto, int maximo) {
        if (texto == null || texto.length() <= maximo) {
            return texto;
        }
        return texto.substring(0, maximo - 1) + "...";
    }

    private String domicilioDe(Persona persona) {
        StringBuilder domicilio = new StringBuilder();

        if (persona.getCalle() != null && !persona.getCalle().isBlank()) {
            domicilio.append(persona.getCalle());
            if (persona.getNumeroExterior() != null && !persona.getNumeroExterior().isBlank()) {
                domicilio.append(' ').append(persona.getNumeroExterior());
            }
        }
        if (persona.getColonia() != null && !persona.getColonia().isBlank()) {
            if (domicilio.length() > 0) {
                domicilio.append(", ");
            }
            domicilio.append(persona.getColonia());
        }
        if (persona.getMunicipio() != null) {
            if (domicilio.length() > 0) {
                domicilio.append(", ");
            }
            domicilio.append(persona.getMunicipio().getNombre());
        }
        return domicilio.toString();
    }

    private void huella(PdfContentByte lienzo, float x, float y, float escala) {
        elipse(lienzo, x, y, 11f * escala, 9f * escala);
        elipse(lienzo, x - 9f * escala, y + 11f * escala, 4f * escala, 5.5f * escala);
        elipse(lienzo, x - 3f * escala, y + 15f * escala, 4f * escala, 5.5f * escala);
        elipse(lienzo, x + 3f * escala, y + 15f * escala, 4f * escala, 5.5f * escala);
        elipse(lienzo, x + 9f * escala, y + 11f * escala, 4f * escala, 5.5f * escala);
    }

    private void elipse(PdfContentByte lienzo, float cx, float cy, float rx, float ry) {
        final float k = 0.5523f;
        lienzo.moveTo(cx - rx, cy);
        lienzo.curveTo(cx - rx, cy + ry * k, cx - rx * k, cy + ry, cx, cy + ry);
        lienzo.curveTo(cx + rx * k, cy + ry, cx + rx, cy + ry * k, cx + rx, cy);
        lienzo.curveTo(cx + rx, cy - ry * k, cx + rx * k, cy - ry, cx, cy - ry);
        lienzo.curveTo(cx - rx * k, cy - ry, cx - rx, cy - ry * k, cx - rx, cy);
        lienzo.fill();
    }

    private String nombreDe(mx.gob.sedif.sea.core.catalogo.CatalogoBase catalogo) {
        return catalogo == null ? null : catalogo.getNombre();
    }

    /**
     * Datos de una credencial.
     *
     * @param certificado certificado emitido
     * @param persona     tutor responsable
     * @param animal      animal empadronado
     * @param fotografia  foto del animal, si la adjuntaron
     */
    public record DatosCredencial(
            Certificado certificado,
            Persona persona,
            Animal animal,
            Optional<byte[]> fotografia) {
    }
}
