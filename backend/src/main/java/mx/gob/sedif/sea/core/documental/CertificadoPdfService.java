package mx.gob.sedif.sea.core.documental;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
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
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfWriter;

import mx.gob.sedif.sea.core.animal.Animal;
import mx.gob.sedif.sea.core.certificado.Certificado;
import mx.gob.sedif.sea.core.persona.Persona;
import mx.gob.sedif.sea.exception.AlmacenamientoException;

/**
 * Certificado de empadronamiento, siguiendo el formato oficial del instituto.
 *
 * <p>Se dibuja con coordenadas y no con tablas porque hay que respetar una
 * maqueta concreta: franja institucional arriba, foto del animal a la izquierda,
 * los campos en su sitio, y al pie la fecha y la firma de quien autoriza.
 *
 * <p>Colores, textos y firma se leen de los ajustes. La paleta definitiva
 * todavia no esta resuelta, asi que ponerla en el codigo obligaria a
 * recompilar cada vez que el area cambie de opinion.
 */
@Service
public class CertificadoPdfService {

    private static final Logger log = LoggerFactory.getLogger(CertificadoPdfService.class);

    private static final Locale ESPANOL_MX = new Locale("es", "MX");
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("d");
    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MMMM", ESPANOL_MX);
    private static final DateTimeFormatter ANIO = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter FECHA_CORTA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Alto de la franja institucional superior. */
    private static final float ALTO_FRANJA = 62f;

    /** Recuadro de la fotografia del animal. */
    private static final float FOTO_ANCHO = 150f;
    private static final float FOTO_ALTO = 145f;

    private final QrService qrService;
    private final IdentidadGrafica identidad;
    private final FirmaService firmaService;

    public CertificadoPdfService(QrService qrService,
                                 IdentidadGrafica identidad,
                                 FirmaService firmaService) {
        this.qrService = qrService;
        this.identidad = identidad;
        this.firmaService = firmaService;
    }

    /**
     * Genera el certificado.
     *
     * @param certificado certificado emitido
     * @param persona     tutor responsable
     * @param animal      animal empadronado
     * @param fotografia  foto del animal, si la adjuntaron
     * @return contenido del PDF
     */
    public byte[] generar(Certificado certificado, Persona persona, Animal animal,
                          Optional<byte[]> fotografia) {

        Document documento = new Document(PageSize.LETTER, 0, 0, 0, 0);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();

        try {
            PdfWriter escritor = PdfWriter.getInstance(documento, salida);
            documento.addTitle("Certificado de empadronamiento " + certificado.getFolio());
            documento.addCreator(identidad.nombreArea());
            documento.open();

            PdfContentByte lienzo = escritor.getDirectContent();

            dibujarFondoHuellas(lienzo);
            dibujarFranjaInstitucional(lienzo);
            dibujarTitulo(lienzo);
            dibujarFotografia(lienzo, fotografia);
            dibujarDatos(lienzo, certificado, persona, animal);
            dibujarQr(lienzo, certificado);
            dibujarFechaYFirma(lienzo, certificado);

            // Reverso con la marca de agua, como el formato oficial.
            documento.newPage();
            dibujarReverso(escritor.getDirectContent());

            documento.close();

            byte[] pdf = salida.toByteArray();

            FirmaService.ResultadoFirma resultado = firmaService.firmar(
                    pdf,
                    new FirmaService.ContextoFirma(
                            certificado.getFolio(),
                            certificado.getHashDocumento(),
                            certificado.getUsuarioEmisor().getUsuario()));

            log.info("Certificado {} generado ({} bytes){}",
                    certificado.getFolio(), resultado.documentoFirmado().length,
                    resultado.firmado() ? " con firma electronica" : "");

            return resultado.documentoFirmado();

        } catch (RuntimeException ex) {
            throw new AlmacenamientoException(
                    "No se pudo generar el certificado " + certificado.getFolio(), ex);
        }
    }

    /** Fondo decorativo de huellas y huesos, muy tenue. */
    private void dibujarFondoHuellas(PdfContentByte lienzo) {
        Color color = identidad.colorFondoHuellas();
        float ancho = PageSize.LETTER.getWidth();
        float alto = PageSize.LETTER.getHeight();

        lienzo.saveState();
        PdfGState estado = new PdfGState();
        // Muy tenue: el texto va encima y debe leerse sin esfuerzo.
        estado.setFillOpacity(0.13f);
        lienzo.setGState(estado);
        lienzo.setColorFill(color);

        // Se reparten por los margenes para no invadir la zona de datos.
        float[][] huellas = {
                {60, 640}, {95, 690}, {520, 660}, {555, 620},
                {50, 470}, {75, 520}, {545, 480}, {510, 530},
                {60, 300}, {90, 250}, {540, 320}, {560, 270},
                {70, 140}, {520, 150},
        };
        for (float[] posicion : huellas) {
            dibujarHuella(lienzo, posicion[0], posicion[1], 1.1f);
        }

        float[][] huesos = {
                {150, 700}, {430, 690}, {40, 400}, {575, 400},
                {130, 180}, {470, 200}, {200, 620}, {400, 250},
        };
        for (float[] posicion : huesos) {
            dibujarHueso(lienzo, posicion[0], posicion[1], 1.0f);
        }

        lienzo.restoreState();
        // Silencia el aviso de variables no usadas si cambia el tamano.
        if (ancho < 0 || alto < 0) {
            log.trace("dimensiones invalidas");
        }
    }

    /** Franja superior con la identidad institucional. */
    private void dibujarFranjaInstitucional(PdfContentByte lienzo) {
        float ancho = PageSize.LETTER.getWidth();
        float alto = PageSize.LETTER.getHeight();
        Color color = identidad.colorFranja();

        lienzo.saveState();
        // Fondo muy claro derivado del color configurado.
        PdfGState suave = new PdfGState();
        suave.setFillOpacity(0.10f);
        lienzo.setGState(suave);
        lienzo.setColorFill(color);
        lienzo.rectangle(0, alto - ALTO_FRANJA, ancho, ALTO_FRANJA);
        lienzo.fill();
        lienzo.restoreState();

        // Filete inferior que cierra la franja.
        lienzo.saveState();
        lienzo.setColorFill(color);
        lienzo.rectangle(0, alto - ALTO_FRANJA - 3f, ancho, 3f);
        lienzo.fill();
        lienzo.restoreState();

        // Los logotipos institucionales son imagenes que el area debe
        // proporcionar; mientras tanto se rotulan sus nombres, que es lo que
        // permite identificar al emisor.
        float y = alto - 26f;
        texto(lienzo, "GOBIERNO DEL ESTADO", 60f, y, 9.5f, color, true, Element.ALIGN_LEFT);
        texto(lienzo, "2024 - 2030", 60f, y - 11f, 7f, color, false, Element.ALIGN_LEFT);

        texto(lienzo, "|", 185f, y - 4f, 12f, Color.LIGHT_GRAY, false, Element.ALIGN_CENTER);

        texto(lienzo, "Familias", 205f, y, 11f, color, true, Element.ALIGN_LEFT);
        texto(lienzo, identidad.nombreInstitucion(), 205f, y - 11f, 6.5f,
              Color.DARK_GRAY, false, Element.ALIGN_LEFT);

        texto(lienzo, "|", 320f, y - 4f, 12f, Color.LIGHT_GRAY, false, Element.ALIGN_CENTER);

        texto(lienzo, identidad.areaSiglas(), 340f, y, 13f, color, true, Element.ALIGN_LEFT);
        texto(lienzo, identidad.areaNombre(), 340f, y - 11f, 6.5f,
              Color.DARK_GRAY, false, Element.ALIGN_LEFT);

        texto(lienzo, "|", 455f, y - 4f, 12f, Color.LIGHT_GRAY, false, Element.ALIGN_CENTER);

        texto(lienzo, "POR AMOR A PUEBLA", 475f, y - 5f, 9f, color, true, Element.ALIGN_LEFT);
    }

    /** Titulo del certificado y cinta con el nombre del padron. */
    private void dibujarTitulo(PdfContentByte lienzo) {
        float centro = PageSize.LETTER.getWidth() / 2f;
        float y = PageSize.LETTER.getHeight() - 112f;

        texto(lienzo, "Certificado de Empadronamiento", centro, y, 26f,
              identidad.colorTitulo(), true, Element.ALIGN_CENTER);

        // Cinta con el nombre del padron.
        float anchoCinta = 250f;
        float altoCinta = 24f;
        float yCinta = y - 36f;

        lienzo.saveState();
        lienzo.setColorFill(identidad.colorCinta());
        lienzo.roundRectangle(centro - anchoCinta / 2f, yCinta, anchoCinta, altoCinta, 6f);
        lienzo.fill();
        lienzo.restoreState();

        texto(lienzo, "Padron Estatal de Perros y Gatos", centro, yCinta + 8f, 10f,
              Color.WHITE, true, Element.ALIGN_CENTER);
    }

    /** Recuadro con la fotografia del animal. */
    private void dibujarFotografia(PdfContentByte lienzo, Optional<byte[]> fotografia) {
        float x = 70f;
        float y = PageSize.LETTER.getHeight() - 330f;

        lienzo.saveState();
        PdfGState suave = new PdfGState();
        suave.setFillOpacity(0.12f);
        lienzo.setGState(suave);
        lienzo.setColorFill(identidad.colorTitulo());
        lienzo.roundRectangle(x, y, FOTO_ANCHO, FOTO_ALTO, 10f);
        lienzo.fill();
        lienzo.restoreState();

        fotografia.ifPresentOrElse(
                bytes -> {
                    try {
                        Image imagen = Image.getInstance(bytes);
                        // Se ajusta al recuadro conservando la proporcion, para
                        // que la foto no salga deformada.
                        imagen.scaleToFit(FOTO_ANCHO - 8f, FOTO_ALTO - 8f);
                        imagen.setAbsolutePosition(
                                x + (FOTO_ANCHO - imagen.getScaledWidth()) / 2f,
                                y + (FOTO_ALTO - imagen.getScaledHeight()) / 2f);
                        lienzo.addImage(imagen);
                    } catch (Exception ex) {
                        // El certificado se emite igual: negarlo por una imagen
                        // seria desproporcionado.
                        log.warn("No se pudo insertar la fotografia del animal", ex);
                    }
                },
                () -> texto(lienzo, "Sin fotografia",
                            x + FOTO_ANCHO / 2f, y + FOTO_ALTO / 2f, 8f,
                            Color.GRAY, false, Element.ALIGN_CENTER));
    }

    /** Datos del animal y de su tutor responsable. */
    private void dibujarDatos(PdfContentByte lienzo, Certificado certificado,
                              Persona persona, Animal animal) {
        float xDatos = 250f;
        float y = PageSize.LETTER.getHeight() - 200f;

        campo(lienzo, "Nombre:", animal.getNombre(), xDatos, y, 11f);
        y -= 34f;
        campo(lienzo, "Folio:", certificado.getFolio(), xDatos, y, 9f);

        // Fila de tres columnas, como el formato.
        y = PageSize.LETTER.getHeight() - 300f;
        campo(lienzo, "Edad:", animal.getEdadDescriptiva(), xDatos, y, 9f);
        campo(lienzo, "Especie:", nombreDe(animal.getEspecie()), xDatos + 130f, y, 9f);
        campo(lienzo, "Sexo:", nombreDe(animal.getSexo()), xDatos + 250f, y, 9f);

        // A partir de aqui el contenido ocupa el ancho completo.
        float xIzquierda = 70f;
        y = PageSize.LETTER.getHeight() - 400f;

        campo(lienzo, "Raza:", nombreDe(animal.getRaza()), xIzquierda, y, 9f);
        campo(lienzo, "Color:", nombreDe(animal.getColor()), xIzquierda + 180f, y, 9f);
        campo(lienzo, "Esterilizado:", siONo(animal.getEsterilizado()),
              xIzquierda + 330f, y, 9f);

        y -= 46f;
        campo(lienzo, "Caracteristicas fisicas:", animal.getCaracteristicas(),
              xIzquierda, y, 9f);

        y -= 46f;
        campo(lienzo, "Responsable:", persona.getNombreCompleto(), xIzquierda, y, 9f);

        y -= 34f;
        campo(lienzo, "Direccion:", domicilioDe(persona), xIzquierda, y, 9f);

        y -= 34f;
        campo(lienzo, "Telefono:", persona.getTelefono(), xIzquierda, y, 9f);

        y -= 34f;
        campo(lienzo, "Correo electronico:", persona.getCorreo(), xIzquierda, y, 9f);
    }

    /**
     * Codigo QR de verificacion.
     *
     * <p>No viene en el formato original, pero es lo que permite comprobar la
     * autenticidad del documento sin llamar a la oficina. Se coloca en el
     * margen para no estorbar la maqueta.
     */
    private void dibujarQr(PdfContentByte lienzo, Certificado certificado) {
        try {
            byte[] qr = qrService.generarParaCertificado(
                    certificado.getCodigoVerificacion().toString(), 160);

            Image imagen = Image.getInstance(qr);
            imagen.scaleToFit(72f, 72f);
            float x = PageSize.LETTER.getWidth() - 132f;
            float y = 178f;
            imagen.setAbsolutePosition(x, y);
            lienzo.addImage(imagen);

            texto(lienzo, "Verifica este documento", x + 36f, y - 11f, 6f,
                  Color.GRAY, false, Element.ALIGN_CENTER);

        } catch (Exception ex) {
            // Sin codigo el documento sigue siendo valido: lleva folio.
            log.warn("No se pudo insertar el codigo QR en el certificado {}",
                    certificado.getFolio(), ex);
        }
    }

    /** Fecha de expedicion y firma de quien autoriza. */
    private void dibujarFechaYFirma(PdfContentByte lienzo, Certificado certificado) {
        float centro = PageSize.LETTER.getWidth() / 2f;
        var fecha = certificado.getFechaEmision();

        String lineaFecha = "%s a %s de %s de %s".formatted(
                identidad.ciudadExpedicion(),
                fecha.format(DIA), fecha.format(MES), fecha.format(ANIO));

        texto(lienzo, lineaFecha, centro, 190f, 9.5f, Color.DARK_GRAY, false,
              Element.ALIGN_CENTER);

        // Firma.
        texto(lienzo, identidad.firmanteNombre(), centro, 118f, 12f,
              identidad.colorTitulo(), true, Element.ALIGN_CENTER);
        texto(lienzo, identidad.firmanteCargo(), centro, 105f, 8.5f,
              Color.DARK_GRAY, false, Element.ALIGN_CENTER);

        // Leyendas al pie.
        texto(lienzo, identidad.leyendaVerificacion(), centro, 68f, 7f,
              Color.GRAY, false, Element.ALIGN_CENTER);
        texto(lienzo, "Codigo: " + certificado.getCodigoVerificacion(), centro, 58f, 6.5f,
              Color.GRAY, false, Element.ALIGN_CENTER);

        if (certificado.getFechaVigencia() != null) {
            texto(lienzo,
                  "Vigencia: " + certificado.getFechaVigencia().format(FECHA_CORTA),
                  centro, 48f, 7f, Color.GRAY, false, Element.ALIGN_CENTER);
        }

        if (!firmaService.estaDisponible()) {
            texto(lienzo, identidad.leyendaSelloFisico(), centro, 36f, 6.5f,
                  Color.GRAY, false, Element.ALIGN_CENTER);
        }
    }

    /** Reverso con marca de agua repetida y el logotipo del area. */
    private void dibujarReverso(PdfContentByte lienzo) {
        float ancho = PageSize.LETTER.getWidth();
        float alto = PageSize.LETTER.getHeight();
        Color color = identidad.colorTitulo();

        // Franja superior, mas sobria que la del anverso.
        texto(lienzo, "GOBIERNO DEL ESTADO 2024 - 2030", ancho / 2f - 90f, alto - 50f,
              9f, color, true, Element.ALIGN_CENTER);
        texto(lienzo, "|", ancho / 2f + 10f, alto - 50f, 11f,
              Color.LIGHT_GRAY, false, Element.ALIGN_CENTER);
        texto(lienzo, "Familias", ancho / 2f + 60f, alto - 50f, 11f,
              color, true, Element.ALIGN_CENTER);

        // Marca de agua en diagonal.
        lienzo.saveState();
        PdfGState tenue = new PdfGState();
        tenue.setFillOpacity(0.16f);
        lienzo.setGState(tenue);

        var fuente = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15f, color);
        String leyenda = identidad.marcaAguaTexto();

        for (float y = 120f; y < alto - 90f; y += 78f) {
            for (float x = -60f; x < ancho + 120f; x += 235f) {
                ColumnText.showTextAligned(lienzo, Element.ALIGN_LEFT,
                        new Phrase(leyenda, fuente), x, y, 30f);
            }
        }
        lienzo.restoreState();

        // Identidad del area al centro.
        texto(lienzo, identidad.areaSiglas(), ancho / 2f, alto / 2f, 54f,
              color, true, Element.ALIGN_CENTER);
        texto(lienzo, identidad.areaNombre(), ancho / 2f, alto / 2f - 22f, 12f,
              color, true, Element.ALIGN_CENTER);

        texto(lienzo, "POR AMOR A PUEBLA", ancho / 2f, 80f, 13f,
              color, true, Element.ALIGN_CENTER);
    }

    // ---- Apoyo ----

    /**
     * Escribe una etiqueta con su valor debajo.
     *
     * <p>La etiqueta se dibuja siempre, aunque el valor falte: el formato
     * oficial las lleva impresas y dejar el hueco visible permite completarlo a
     * mano si hace falta.
     */
    private void campo(PdfContentByte lienzo, String etiqueta, String valor,
                       float x, float y, float tamanoValor) {
        texto(lienzo, etiqueta, x, y, 9.5f, identidad.colorEtiquetas(), true,
              Element.ALIGN_LEFT);

        if (valor != null && !valor.isBlank()) {
            texto(lienzo, valor, x, y - 14f, tamanoValor, Color.BLACK, false,
                  Element.ALIGN_LEFT);
        }
    }

    /** Escribe una linea de texto en una posicion exacta. */
    private void texto(PdfContentByte lienzo, String contenido, float x, float y,
                       float tamano, Color color, boolean negrita, int alineacion) {
        if (contenido == null || contenido.isBlank()) {
            return;
        }
        var fuente = FontFactory.getFont(
                negrita ? FontFactory.HELVETICA_BOLD : FontFactory.HELVETICA, tamano, color);
        ColumnText.showTextAligned(lienzo, alineacion, new Phrase(contenido, fuente), x, y, 0);
    }

    /** Arma el domicilio en una linea. */
    private String domicilioDe(Persona persona) {
        StringBuilder domicilio = new StringBuilder();

        if (persona.getCalle() != null && !persona.getCalle().isBlank()) {
            domicilio.append(persona.getCalle());
            if (persona.getNumeroExterior() != null && !persona.getNumeroExterior().isBlank()) {
                domicilio.append(' ').append(persona.getNumeroExterior());
            }
            if (persona.getNumeroInterior() != null && !persona.getNumeroInterior().isBlank()) {
                domicilio.append(" int. ").append(persona.getNumeroInterior());
            }
        }
        if (persona.getColonia() != null && !persona.getColonia().isBlank()) {
            if (domicilio.length() > 0) {
                domicilio.append(", ");
            }
            domicilio.append("Col. ").append(persona.getColonia());
        }
        if (persona.getMunicipio() != null) {
            if (domicilio.length() > 0) {
                domicilio.append(", ");
            }
            domicilio.append(persona.getMunicipio().getNombre());
        }
        if (persona.getCodigoPostal() != null && !persona.getCodigoPostal().isBlank()) {
            domicilio.append(", C.P. ").append(persona.getCodigoPostal());
        }

        return domicilio.toString();
    }

    /** Huella de cuatro dedos y almohadilla. */
    private void dibujarHuella(PdfContentByte lienzo, float x, float y, float escala) {
        elipse(lienzo, x, y, 11f * escala, 9f * escala);
        elipse(lienzo, x - 9f * escala, y + 11f * escala, 4f * escala, 5.5f * escala);
        elipse(lienzo, x - 3f * escala, y + 15f * escala, 4f * escala, 5.5f * escala);
        elipse(lienzo, x + 3f * escala, y + 15f * escala, 4f * escala, 5.5f * escala);
        elipse(lienzo, x + 9f * escala, y + 11f * escala, 4f * escala, 5.5f * escala);
    }

    /** Hueso decorativo: dos pares de lobulos unidos por una barra. */
    private void dibujarHueso(PdfContentByte lienzo, float x, float y, float escala) {
        float largo = 30f * escala;
        float radio = 6f * escala;

        lienzo.rectangle(x, y - radio / 2f, largo, radio);
        lienzo.fill();

        elipse(lienzo, x, y + radio * 0.7f, radio, radio);
        elipse(lienzo, x, y - radio * 0.7f, radio, radio);
        elipse(lienzo, x + largo, y + radio * 0.7f, radio, radio);
        elipse(lienzo, x + largo, y - radio * 0.7f, radio, radio);
    }

    /** Elipse aproximada con cuatro curvas de Bezier. */
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

    private String siONo(Boolean valor) {
        return Boolean.TRUE.equals(valor) ? "Si" : "No";
    }
}
