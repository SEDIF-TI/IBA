package mx.gob.sedif.sea.core.documental;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import mx.gob.sedif.sea.core.animal.Animal;
import mx.gob.sedif.sea.core.certificado.Certificado;
import mx.gob.sedif.sea.core.persona.Persona;
import mx.gob.sedif.sea.exception.AlmacenamientoException;

/**
 * Certificado de empadronamiento, en tamano carta.
 *
 * <p>Es el documento que la persona guarda y presenta. Lleva los datos del
 * animal, los de su tutor, el folio oficial y el codigo QR de verificacion.
 */
@Service
public class CertificadoPdfService {

    private static final Logger log = LoggerFactory.getLogger(CertificadoPdfService.class);

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", new Locale("es", "MX"));

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
     * @return contenido del PDF
     */
    public byte[] generar(Certificado certificado, Persona persona, Animal animal) {
        Document documento = new Document(PageSize.LETTER, 48f, 48f, 54f, 48f);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();

        try {
            PdfWriter escritor = PdfWriter.getInstance(documento, salida);

            // El fondo se dibuja por debajo del contenido, en cada pagina.
            escritor.setPageEvent(new FondoHuellas(identidad.colorPrimario()));

            documento.addTitle("Certificado de empadronamiento " + certificado.getFolio());
            documento.addCreator(identidad.nombreArea());
            documento.open();

            escribirEncabezado(documento);
            escribirTitulo(documento, certificado);
            escribirDatosAnimal(documento, animal);
            escribirDatosTutor(documento, persona);
            escribirPieConQr(documento, certificado);

            documento.close();

            byte[] pdf = salida.toByteArray();

            // La firma se aplica al final, sobre el documento ya armado. Hoy la
            // implementacion vigente no firma y lo hace constar.
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

    /** Encabezado con la institucion y el area responsable. */
    private void escribirEncabezado(Document documento) {
        Color color = identidad.colorPrimario();

        Paragraph institucion = new Paragraph(
                identidad.nombreInstitucion().toUpperCase(),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, color));
        institucion.setAlignment(Element.ALIGN_CENTER);
        documento.add(institucion);

        Paragraph area = new Paragraph(
                identidad.nombreArea(),
                FontFactory.getFont(FontFactory.HELVETICA, 11, Color.DARK_GRAY));
        area.setAlignment(Element.ALIGN_CENTER);
        area.setSpacingAfter(4f);
        documento.add(area);

        Paragraph padron = new Paragraph(
                "Padron Estatal de Perros y Gatos",
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, Color.GRAY));
        padron.setAlignment(Element.ALIGN_CENTER);
        padron.setSpacingAfter(18f);
        documento.add(padron);
    }

    /** Titulo del documento y su folio oficial. */
    private void escribirTitulo(Document documento, Certificado certificado) {
        Color color = identidad.colorPrimario();

        Paragraph titulo = new Paragraph(
                "CERTIFICADO DE EMPADRONAMIENTO",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 19, color));
        titulo.setAlignment(Element.ALIGN_CENTER);
        titulo.setSpacingAfter(6f);
        documento.add(titulo);

        Paragraph folio = new Paragraph(
                "Folio: " + certificado.getFolio(),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.DARK_GRAY));
        folio.setAlignment(Element.ALIGN_CENTER);
        folio.setSpacingAfter(22f);
        documento.add(folio);
    }

    /** Datos del animal empadronado. */
    private void escribirDatosAnimal(Document documento, Animal animal) {
        documento.add(tituloSeccion("Datos del ser sintiente"));

        PdfPTable tabla = crearTablaDatos();
        agregarFila(tabla, "Nombre", animal.getNombre());
        agregarFila(tabla, "Especie", nombreDe(animal.getEspecie()));
        agregarFila(tabla, "Raza", nombreDe(animal.getRaza()));
        agregarFila(tabla, "Sexo", nombreDe(animal.getSexo()));
        agregarFila(tabla, "Color", nombreDe(animal.getColor()));
        agregarFila(tabla, "Tamano", nombreDe(animal.getTamano()));
        agregarFila(tabla, "Edad", animal.getEdadDescriptiva());
        agregarFila(tabla, "Vacunado", siONo(animal.getVacunado()));
        agregarFila(tabla, "Esterilizado", siONo(animal.getEsterilizado()));

        if (animal.getIdentificador() != null && !animal.getIdentificador().isBlank()) {
            agregarFila(tabla, nombreDe(animal.getMetodoIdentificacion()),
                    animal.getIdentificador());
        }
        agregarFila(tabla, "Senas particulares", animal.getCaracteristicas());
        agregarFila(tabla, "Identificador interno", animal.getClaveAnimal());

        documento.add(tabla);
    }

    /**
     * Datos del tutor responsable.
     *
     * <p>La CURP va enmascarada. El certificado circula, se fotografia y se
     * comparte; no hay motivo para que lleve el dato completo.
     */
    private void escribirDatosTutor(Document documento, Persona persona) {
        documento.add(tituloSeccion("Tutor responsable"));

        PdfPTable tabla = crearTablaDatos();
        agregarFila(tabla, "Nombre", persona.getNombreCompleto());
        agregarFila(tabla, "CURP", persona.getCurpEnmascarada());
        agregarFila(tabla, "Municipio",
                persona.getMunicipio() == null ? null : persona.getMunicipio().getNombre());

        documento.add(tabla);
    }

    /** Pie con el codigo de verificacion y las leyendas. */
    private void escribirPieConQr(Document documento, Certificado certificado) {
        PdfPTable pie = new PdfPTable(new float[]{1f, 2.6f});
        pie.setWidthPercentage(100);
        pie.setSpacingBefore(26f);

        byte[] qr = qrService.generarParaCertificado(
                certificado.getCodigoVerificacion().toString(), 180);

        PdfPCell celdaQr = new PdfPCell();
        celdaQr.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        celdaQr.setHorizontalAlignment(Element.ALIGN_CENTER);
        try {
            Image imagen = Image.getInstance(qr);
            imagen.scaleToFit(110f, 110f);
            celdaQr.addElement(imagen);
        } catch (Exception ex) {
            // Sin el codigo el documento sigue siendo valido: lleva folio y se
            // puede verificar escribiendo la direccion.
            log.warn("No se pudo insertar el codigo QR en el certificado {}",
                    certificado.getFolio(), ex);
        }
        pie.addCell(celdaQr);

        PdfPCell celdaTexto = new PdfPCell();
        celdaTexto.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        celdaTexto.setVerticalAlignment(Element.ALIGN_MIDDLE);
        celdaTexto.setPaddingLeft(10f);

        Font fuentePie = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, Color.DARK_GRAY);

        celdaTexto.addElement(new Paragraph(identidad.leyendaVerificacion(), fuentePie));
        celdaTexto.addElement(new Paragraph(
                "Codigo: " + certificado.getCodigoVerificacion(),
                FontFactory.getFont(FontFactory.COURIER, 7.5f, Color.GRAY)));

        celdaTexto.addElement(new Paragraph(" ", fuentePie));
        celdaTexto.addElement(new Paragraph(
                "Fecha de emision: " + certificado.getFechaEmision().format(FORMATO_FECHA),
                fuentePie));

        if (certificado.getFechaVigencia() != null) {
            celdaTexto.addElement(new Paragraph(
                    "Vigencia: " + certificado.getFechaVigencia().format(FORMATO_FECHA),
                    fuentePie));
        }

        // Mientras no haya firma avanzada, se dice donde acudir por el sello.
        if (!firmaService.estaDisponible()) {
            celdaTexto.addElement(new Paragraph(" ", fuentePie));
            celdaTexto.addElement(new Paragraph(
                    identidad.leyendaSelloFisico(),
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8f, Color.GRAY)));
        }

        pie.addCell(celdaTexto);
        documento.add(pie);
    }

    // ---- Apoyo ----

    private Paragraph tituloSeccion(String texto) {
        Paragraph titulo = new Paragraph(
                texto.toUpperCase(),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, identidad.colorPrimario()));
        titulo.setSpacingBefore(14f);
        titulo.setSpacingAfter(6f);
        return titulo;
    }

    private PdfPTable crearTablaDatos() {
        PdfPTable tabla = new PdfPTable(new float[]{1f, 2f});
        tabla.setWidthPercentage(100);
        return tabla;
    }

    /** Agrega una fila; omite las que no tienen valor para no dejar huecos. */
    private void agregarFila(PdfPTable tabla, String etiqueta, String valor) {
        if (valor == null || valor.isBlank()) {
            return;
        }

        PdfPCell celdaEtiqueta = new PdfPCell(new Phrase(
                etiqueta, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, Color.DARK_GRAY)));
        celdaEtiqueta.setBorder(com.lowagie.text.Rectangle.BOTTOM);
        celdaEtiqueta.setBorderColor(new Color(0xE2, 0xE8, 0xF0));
        celdaEtiqueta.setPadding(5f);
        tabla.addCell(celdaEtiqueta);

        PdfPCell celdaValor = new PdfPCell(new Phrase(
                valor, FontFactory.getFont(FontFactory.HELVETICA, 9.5f, Color.BLACK)));
        celdaValor.setBorder(com.lowagie.text.Rectangle.BOTTOM);
        celdaValor.setBorderColor(new Color(0xE2, 0xE8, 0xF0));
        celdaValor.setPadding(5f);
        tabla.addCell(celdaValor);
    }

    private String nombreDe(mx.gob.sedif.sea.core.catalogo.CatalogoBase catalogo) {
        return catalogo == null ? null : catalogo.getNombre();
    }

    private String siONo(Boolean valor) {
        return Boolean.TRUE.equals(valor) ? "Si" : "No";
    }
}
