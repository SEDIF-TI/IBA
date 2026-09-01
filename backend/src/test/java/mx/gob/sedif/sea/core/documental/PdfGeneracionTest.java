package mx.gob.sedif.sea.core.documental;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;

import mx.gob.sedif.sea.core.animal.Animal;
import mx.gob.sedif.sea.core.catalogo.Especie;
import mx.gob.sedif.sea.core.catalogo.Municipio;
import mx.gob.sedif.sea.core.catalogo.Sexo;
import mx.gob.sedif.sea.core.certificado.Certificado;
import mx.gob.sedif.sea.core.parametro.ParametroService;
import mx.gob.sedif.sea.core.persona.Persona;
import mx.gob.sedif.sea.security.Usuario;

/**
 * Verifica los documentos que expide el instituto.
 *
 * <p>No basta con que se produzca un archivo: cada caso abre el PDF resultante
 * y comprueba que diga lo que debe decir. Un certificado sin folio, o con la
 * CURP completa a la vista, seria un problema real aunque el archivo se generara
 * sin error.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PdfGeneracionTest {

    @Mock
    private ParametroService parametros;

    private QrService qrService;
    private IdentidadGrafica identidad;
    private FirmaService firmaService;

    private CertificadoPdfService certificadoPdf;
    private CredencialPdfService credencialPdf;
    private AcusePdfService acusePdf;

    @BeforeEach
    void prepararEscenario() {
        // Los ajustes devuelven su valor por omision, como en una instalacion nueva.
        when(parametros.obtener(any(), any()))
                .thenAnswer(invocacion -> invocacion.getArgument(1));
        when(parametros.obtenerEntero(any(), org.mockito.ArgumentMatchers.anyInt()))
                .thenAnswer(invocacion -> invocacion.getArgument(1));

        qrService = new QrService("https://sea.puebla.gob.mx/verificar");
        identidad = new IdentidadGrafica(parametros);
        firmaService = new FirmaServiceStub("Acude por tu sello institucional.");

        certificadoPdf = new CertificadoPdfService(qrService, identidad, firmaService);
        credencialPdf = new CredencialPdfService(qrService, identidad);
        acusePdf = new AcusePdfService(identidad);
    }

    @Nested
    @DisplayName("Certificado de empadronamiento")
    class CertificadoDeEmpadronamiento {

        @Test
        @DisplayName("produce un PDF de una hoja con folio y datos del animal")
        void produceCertificadoCompleto() throws Exception {
            byte[] pdf = certificadoPdf.generar(certificado(), persona(), animal());

            assertThat(pdf).isNotEmpty();
            assertThat(new String(pdf, 0, 5)).startsWith("%PDF-");

            String texto = extraerTexto(pdf);
            assertThat(texto)
                    .contains("CERTIFICADO DE EMPADRONAMIENTO")
                    .contains("SEDIF/IBA/EMP-21114/00001/2026")
                    .contains("Firulais")
                    .contains("Canino")
                    .contains("Juan Perez Gomez");
        }

        @Test
        @DisplayName("muestra la CURP enmascarada, nunca completa")
        void enmascaraLaCurp() throws Exception {
            byte[] pdf = certificadoPdf.generar(certificado(), persona(), animal());
            String texto = extraerTexto(pdf);

            assertThat(texto)
                    .as("el certificado circula y se fotografia: la CURP no va completa")
                    .doesNotContain("PEGJ850315HPLRRN03");
            assertThat(texto).contains("PEGJ**********03");
        }

        @Test
        @DisplayName("incluye la leyenda del sello mientras no haya firma electronica")
        void incluyeLeyendaDelSello() throws Exception {
            byte[] pdf = certificadoPdf.generar(certificado(), persona(), animal());

            assertThat(extraerTexto(pdf)).contains("sello institucional");
        }

        @Test
        @DisplayName("convierte la edad en meses a texto legible")
        void muestraLaEdadLegible() throws Exception {
            Animal animal = animal();
            animal.setEdadMeses(30);

            assertThat(extraerTexto(certificadoPdf.generar(certificado(), persona(), animal)))
                    .contains("2 anos 6 meses");
        }
    }

    @Nested
    @DisplayName("Credencial de bolsillo")
    class CredencialDeBolsillo {

        @Test
        @DisplayName("genera una credencial con el nombre del animal y su tutor")
        void generaCredencial() throws Exception {
            byte[] pdf = credencialPdf.generar(certificado(), persona(), animal());

            assertThat(new PdfReader(pdf).getNumberOfPages()).isEqualTo(1);
            assertThat(extraerTexto(pdf))
                    .contains("Firulais")
                    .contains("Juan Perez Gomez")
                    .contains("PADRON ESTATAL DE PERROS Y GATOS");
        }

        @Test
        @DisplayName("acomoda tres credenciales en una sola hoja")
        void acomodaTresPorHoja() throws Exception {
            List<CredencialPdfService.DatosCredencial> lote = crearLote(3);

            PdfReader lector = new PdfReader(credencialPdf.generarLote(lote));

            assertThat(lector.getNumberOfPages())
                    .as("tres credenciales caben en una hoja de opalina")
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("pasa a la hoja siguiente a partir de la cuarta")
        void pasaDeHojaALaCuarta() throws Exception {
            assertThat(new PdfReader(credencialPdf.generarLote(crearLote(4))).getNumberOfPages())
                    .isEqualTo(2);
            assertThat(new PdfReader(credencialPdf.generarLote(crearLote(7))).getNumberOfPages())
                    .as("siete credenciales ocupan tres hojas")
                    .isEqualTo(3);
        }

        @Test
        @DisplayName("imprime por lote las credenciales de una jornada")
        void imprimeLoteDeJornada() throws Exception {
            // Una jornada municipal deja decenas de tramites listos de una vez.
            byte[] pdf = credencialPdf.generarLote(crearLote(25));

            assertThat(new PdfReader(pdf).getNumberOfPages())
                    .as("25 credenciales, de tres en tres, son 9 hojas")
                    .isEqualTo(9);
        }
    }

    @Nested
    @DisplayName("Acuse de tramite")
    class AcuseDeTramite {

        @Test
        @DisplayName("destaca el folio de seguimiento")
        void destacaElFolio() throws Exception {
            byte[] pdf = acusePdf.generar(TestFactoria.solicitud());

            String texto = extraerTexto(pdf);
            assertThat(texto)
                    .contains("ACUSE DE RECEPCION DE TRAMITE")
                    .contains("FOLIO DE SEGUIMIENTO")
                    .contains("SOL-20260901-00001");
        }

        @Test
        @DisplayName("aclara que no acredita el empadronamiento")
        void aclaraQueNoAcredita() throws Exception {
            byte[] pdf = acusePdf.generar(TestFactoria.solicitud());

            assertThat(extraerTexto(pdf))
                    .as("un acuse no puede confundirse con el certificado")
                    .contains("No acredita el empadronamiento");
        }
    }

    // ---- Apoyo ----

    private List<CredencialPdfService.DatosCredencial> crearLote(int cantidad) {
        List<CredencialPdfService.DatosCredencial> lote = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            lote.add(new CredencialPdfService.DatosCredencial(
                    certificado(), persona(), animal()));
        }
        return lote;
    }

    private String extraerTexto(byte[] pdf) throws Exception {
        PdfReader lector = new PdfReader(new ByteArrayInputStream(pdf));
        StringBuilder texto = new StringBuilder();
        PdfTextExtractor extractor = new PdfTextExtractor(lector);
        for (int pagina = 1; pagina <= lector.getNumberOfPages(); pagina++) {
            texto.append(extractor.getTextFromPage(pagina)).append('\n');
        }
        return texto.toString();
    }

    private Certificado certificado() {
        return TestFactoria.certificado();
    }

    private Persona persona() {
        return TestFactoria.persona();
    }

    private Animal animal() {
        return TestFactoria.animal();
    }

    /** Arma los objetos de prueba con datos realistas. */
    static final class TestFactoria {

        static Certificado certificado() {
            Usuario emisor = new Usuario();
            emisor.setId(1L);
            emisor.setUsuario("validador");
            emisor.setNombre("Ana");
            emisor.setApellidoPaterno("Lopez");

            Certificado certificado = new Certificado();
            certificado.setId(1L);
            certificado.setFolio("SEDIF/IBA/EMP-21114/00001/2026");
            certificado.setCodigoVerificacion(
                    UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
            certificado.setFechaEmision(OffsetDateTime.now());
            certificado.setFechaVigencia(LocalDate.now().plusYears(1));
            certificado.setUsuarioEmisor(emisor);
            certificado.setHashDocumento("a".repeat(64));
            return certificado;
        }

        static Persona persona() {
            Municipio municipio = new Municipio();
            municipio.setId(114);
            municipio.setClave("PUEBLA");
            municipio.setNombre("Puebla");
            municipio.setClaveInegi("21114");

            Persona persona = new Persona();
            persona.setId(1L);
            persona.setCurp("PEGJ850315HPLRRN03");
            persona.setNombre("Juan");
            persona.setApellidoPaterno("Perez");
            persona.setApellidoMaterno("Gomez");
            persona.setMunicipio(municipio);
            return persona;
        }

        static Animal animal() {
            Especie especie = new Especie();
            especie.setId(1);
            especie.setClave("CANINO");
            especie.setNombre("Canino");

            Sexo sexo = new Sexo();
            sexo.setId(1);
            sexo.setClave("MACHO");
            sexo.setNombre("Macho");

            Animal animal = new Animal();
            animal.setId(1L);
            animal.setNombre("Firulais");
            animal.setEspecie(especie);
            animal.setSexo(sexo);
            animal.setEdadMeses(30);
            animal.setVacunado(true);
            animal.setEsterilizado(false);
            animal.setClaveAnimal("PJFIRCM260831");
            animal.setCaracteristicas("Mancha blanca en el pecho");
            return animal;
        }

        static mx.gob.sedif.sea.core.solicitud.Solicitud solicitud() {
            var estatus = new mx.gob.sedif.sea.core.catalogo.EstatusSolicitud();
            estatus.setId(1);
            estatus.setClave("REGISTRADA");
            estatus.setNombre("Registrada");

            var solicitud = new mx.gob.sedif.sea.core.solicitud.Solicitud();
            solicitud.setId(1L);
            solicitud.setFolioSolicitud("SOL-20260901-00001");
            solicitud.setTokenConsulta(UUID.randomUUID());
            solicitud.setPersona(persona());
            solicitud.setAnimal(animal());
            solicitud.setEstatus(estatus);
            solicitud.setFechaRegistro(OffsetDateTime.now());
            return solicitud;
        }
    }
}
