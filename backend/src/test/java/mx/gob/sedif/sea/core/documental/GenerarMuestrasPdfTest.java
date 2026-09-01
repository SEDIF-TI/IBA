package mx.gob.sedif.sea.core.documental;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import mx.gob.sedif.sea.core.parametro.ParametroService;

/**
 * Escribe muestras de los tres documentos para revisarlos a la vista.
 *
 * <p>No se ejecuta en la bateria normal: solo cuando se pide expresamente, para
 * revisar como quedaron. La comprobacion de contenido vive en
 * {@link PdfGeneracionTest}.
 *
 * <pre>./mvnw test -Dtest=GenerarMuestrasPdfTest -Dmuestras.pdf=/ruta/destino</pre>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@EnabledIfSystemProperty(named = "muestras.pdf", matches = ".+")
class GenerarMuestrasPdfTest {

    @Mock
    private ParametroService parametros;

    private CertificadoPdfService certificadoPdf;
    private CredencialPdfService credencialPdf;
    private AcusePdfService acusePdf;

    @BeforeEach
    void prepararEscenario() {
        when(parametros.obtener(any(), any()))
                .thenAnswer(invocacion -> invocacion.getArgument(1));
        when(parametros.obtenerEntero(any(), anyInt()))
                .thenAnswer(invocacion -> invocacion.getArgument(1));

        QrService qr = new QrService("https://sea.puebla.gob.mx/verificar");
        IdentidadGrafica identidad = new IdentidadGrafica(parametros);

        certificadoPdf = new CertificadoPdfService(
                qr, identidad, new FirmaServiceStub("Acude por tu sello institucional."));
        credencialPdf = new CredencialPdfService(qr, identidad);
        acusePdf = new AcusePdfService(identidad);
    }

    @Test
    @DisplayName("escribe las muestras de los tres documentos")
    void escribirMuestras() throws Exception {
        Path destino = Path.of(System.getProperty("muestras.pdf"));
        Files.createDirectories(destino);

        var certificado = PdfGeneracionTest.TestFactoria.certificado();
        var persona = PdfGeneracionTest.TestFactoria.persona();
        var animal = PdfGeneracionTest.TestFactoria.animal();

        // Si se indica una foto, se incluye: sirve para revisar como queda el
        // recuadro con una imagen real.
        String rutaFoto = System.getProperty("muestras.foto");
        java.util.Optional<byte[]> fotografia = rutaFoto == null
                ? java.util.Optional.empty()
                : java.util.Optional.of(Files.readAllBytes(Path.of(rutaFoto)));

        Files.write(destino.resolve("certificado.pdf"),
                certificadoPdf.generar(certificado, persona, animal, fotografia));

        List<CredencialPdfService.DatosCredencial> lote = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            lote.add(new CredencialPdfService.DatosCredencial(
                    certificado, persona, animal, fotografia));
        }
        Files.write(destino.resolve("credenciales-3-por-hoja.pdf"),
                credencialPdf.generarLote(lote));

        Files.write(destino.resolve("acuse.pdf"),
                acusePdf.generar(PdfGeneracionTest.TestFactoria.solicitud()));

        assertThat(destino.resolve("certificado.pdf")).exists();
        assertThat(destino.resolve("credenciales-3-por-hoja.pdf")).exists();
        assertThat(destino.resolve("acuse.pdf")).exists();
    }
}
