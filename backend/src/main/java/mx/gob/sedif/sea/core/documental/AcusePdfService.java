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
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import mx.gob.sedif.sea.core.solicitud.Solicitud;
import mx.gob.sedif.sea.exception.AlmacenamientoException;

/**
 * Acuse de recepcion del tramite.
 *
 * <p>Se entrega al registrar, antes de cualquier resolucion. Su valor esta en
 * el folio: es lo que la persona necesita para consultar despues en que va su
 * tramite.
 */
@Service
public class AcusePdfService {

    private static final Logger log = LoggerFactory.getLogger(AcusePdfService.class);

    private static final DateTimeFormatter FORMATO_FECHA_HORA =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy, HH:mm 'horas'",
                    new Locale("es", "MX"));

    private final IdentidadGrafica identidad;

    public AcusePdfService(IdentidadGrafica identidad) {
        this.identidad = identidad;
    }

    /**
     * Genera el acuse de un tramite.
     *
     * @param solicitud tramite recien registrado
     * @return contenido del PDF
     */
    public byte[] generar(Solicitud solicitud) {
        Document documento = new Document(PageSize.LETTER, 56f, 56f, 56f, 48f);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();

        try {
            PdfWriter escritor = PdfWriter.getInstance(documento, salida);
            escritor.setPageEvent(new FondoHuellas(identidad.colorPrimario()));

            documento.addTitle("Acuse de trámite " + solicitud.getFolioSolicitud());
            documento.addCreator(identidad.nombreArea());
            documento.open();

            Color color = identidad.colorPrimario();

            Paragraph institucion = new Paragraph(
                    identidad.nombreInstitucion().toUpperCase(),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, color));
            institucion.setAlignment(Element.ALIGN_CENTER);
            documento.add(institucion);

            Paragraph area = new Paragraph(
                    identidad.nombreArea(),
                    FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY));
            area.setAlignment(Element.ALIGN_CENTER);
            area.setSpacingAfter(22f);
            documento.add(area);

            Paragraph titulo = new Paragraph(
                    "ACUSE DE RECEPCIÓN DE TRÁMITE",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, color));
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(20f);
            documento.add(titulo);

            // El folio va destacado: es el dato que la persona debe conservar.
            PdfPTable recuadro = new PdfPTable(1);
            recuadro.setWidthPercentage(70);
            recuadro.setHorizontalAlignment(Element.ALIGN_CENTER);

            PdfPCell celda = new PdfPCell();
            celda.setBackgroundColor(new Color(0xFD, 0xF2, 0xF5));
            celda.setBorderColor(color);
            celda.setBorderWidth(1f);
            celda.setPadding(14f);
            celda.setHorizontalAlignment(Element.ALIGN_CENTER);

            Paragraph etiqueta = new Paragraph(
                    "FOLIO DE SEGUIMIENTO",
                    FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY));
            etiqueta.setAlignment(Element.ALIGN_CENTER);
            celda.addElement(etiqueta);

            Paragraph folio = new Paragraph(
                    solicitud.getFolioSolicitud(),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 17, color));
            folio.setAlignment(Element.ALIGN_CENTER);
            celda.addElement(folio);

            recuadro.addCell(celda);
            recuadro.setSpacingAfter(22f);
            documento.add(recuadro);

            PdfPTable datos = new PdfPTable(new float[]{1f, 2f});
            datos.setWidthPercentage(100);
            agregarFila(datos, "Fecha de registro",
                    solicitud.getFechaRegistro().format(FORMATO_FECHA_HORA));
            agregarFila(datos, "Tutor responsable",
                    solicitud.getPersona().getNombreCompleto());
            agregarFila(datos, "CURP", solicitud.getPersona().getCurpEnmascarada());
            agregarFila(datos, "Animal", solicitud.getAnimal().getNombre());
            agregarFila(datos, "Especie",
                    solicitud.getAnimal().getEspecie() == null
                            ? null : solicitud.getAnimal().getEspecie().getNombre());
            agregarFila(datos, "Situación", solicitud.getEstatus().getNombre());
            documento.add(datos);

            Paragraph indicaciones = new Paragraph(
                    "Conserva este acuse. Con tu folio y tu CURP puedes consultar en "
                            + "cualquier momento el avance de tu trámite.",
                    FontFactory.getFont(FontFactory.HELVETICA, 9.5f, Color.DARK_GRAY));
            indicaciones.setSpacingBefore(26f);
            indicaciones.setAlignment(Element.ALIGN_JUSTIFIED);
            documento.add(indicaciones);

            Paragraph aviso = new Paragraph(
                    "Este acuse hace constar unicamente la recepcion del trámite. "
                            + "No acredita el empadronamiento, que se hace constar en el "
                            + "certificado correspondiente una vez aprobada la solicitud.",
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8.5f, Color.GRAY));
            aviso.setSpacingBefore(10f);
            aviso.setAlignment(Element.ALIGN_JUSTIFIED);
            documento.add(aviso);

            documento.close();

            byte[] pdf = salida.toByteArray();
            log.debug("Acuse generado para el tramite {}", solicitud.getFolioSolicitud());
            return pdf;

        } catch (RuntimeException ex) {
            throw new AlmacenamientoException(
                    "No se pudo generar el acuse del trámite "
                            + solicitud.getFolioSolicitud(), ex);
        }
    }

    /** Agrega una fila; omite las que no tienen valor. */
    private void agregarFila(PdfPTable tabla, String etiqueta, String valor) {
        if (valor == null || valor.isBlank()) {
            return;
        }

        PdfPCell celdaEtiqueta = new PdfPCell(new Phrase(
                etiqueta,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, Color.DARK_GRAY)));
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
}
