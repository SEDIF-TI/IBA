package mx.gob.sedif.sea.core.documento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import mx.gob.sedif.sea.exception.AlmacenamientoException;
import mx.gob.sedif.sea.exception.ReglaNegocioException;

/**
 * Verifica el almacen de documentos personales.
 *
 * <p>Cada caso rechazado representa una via por la que se podrian haber colado
 * archivos peligrosos o leido documentos de terceros.
 */
class DocumentoStorageServiceTest {

    @TempDir
    Path almacen;

    private DocumentoStorageService servicio;

    /** Cabecera de un PDF real: Tika lo reconoce por estos bytes. */
    private static final byte[] PDF_VALIDO =
            "%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\ntrailer\n%%EOF"
                    .getBytes(StandardCharsets.US_ASCII);

    /** Cabecera de un PNG real. */
    private static final byte[] PNG_VALIDO = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4,
            (byte) 0x89};

    @BeforeEach
    void prepararServicio() {
        servicio = new DocumentoStorageService(almacen.toString(), 10);
        servicio.prepararAlmacenamiento();
    }

    @Nested
    @DisplayName("Guardado de archivos validos")
    class GuardadoValido {

        @Test
        @DisplayName("guarda un PDF y devuelve sus datos")
        void guardaPdf() {
            var archivo = new MockMultipartFile(
                    "documento", "ine.pdf", "application/pdf", PDF_VALIDO);

            var guardado = servicio.guardar(archivo);

            assertThat(guardado.nombreOriginal()).isEqualTo("ine.pdf");
            assertThat(guardado.mime()).isEqualTo("application/pdf");
            assertThat(guardado.tamanoBytes()).isEqualTo(PDF_VALIDO.length);
            assertThat(guardado.hashSha256()).matches("[a-f0-9]{64}");
            assertThat(servicio.existe(guardado.rutaRelativa())).isTrue();
        }

        @Test
        @DisplayName("el nombre en disco no conserva el original")
        void nombreEnDiscoEsGenerado() {
            var archivo = new MockMultipartFile(
                    "documento", "INE-Juan-Perez-2026.pdf", "application/pdf", PDF_VALIDO);

            var guardado = servicio.guardar(archivo);

            assertThat(guardado.nombreAlmacenado())
                    .as("el nombre en disco no debe revelar de quien es el documento")
                    .doesNotContain("Juan")
                    .doesNotContain("Perez")
                    .doesNotContain("INE")
                    .matches("[0-9a-f-]{36}\\.pdf");
        }

        @Test
        @DisplayName("dos archivos identicos reciben nombres distintos")
        void nombresNoSeRepiten() {
            var primero = servicio.guardar(new MockMultipartFile(
                    "d", "a.pdf", "application/pdf", PDF_VALIDO));
            var segundo = servicio.guardar(new MockMultipartFile(
                    "d", "a.pdf", "application/pdf", PDF_VALIDO));

            assertThat(primero.nombreAlmacenado()).isNotEqualTo(segundo.nombreAlmacenado());
            // El mismo contenido si produce la misma huella: asi se detectan duplicados.
            assertThat(primero.hashSha256()).isEqualTo(segundo.hashSha256());
        }

        @Test
        @DisplayName("acepta imagenes PNG")
        void aceptaPng() {
            var archivo = new MockMultipartFile(
                    "documento", "foto.png", "image/png", PNG_VALIDO);

            assertThatCode(() -> servicio.guardar(archivo)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Rechazo de archivos peligrosos")
    class RechazoPeligrosos {

        @Test
        @DisplayName("rechaza un ejecutable renombrado como imagen")
        void rechazaEjecutableDisfrazado() {
            // Cabecera de ejecutable de Windows con extension .jpg: el caso
            // clasico de intentar colar un programa como si fuera una foto.
            byte[] ejecutable = new byte[]{0x4D, 0x5A, (byte) 0x90, 0x00, 0x03, 0x00};
            var archivo = new MockMultipartFile(
                    "documento", "foto.jpg", "image/jpeg", ejecutable);

            assertThatThrownBy(() -> servicio.guardar(archivo))
                    .isInstanceOf(ReglaNegocioException.class);
        }

        @Test
        @DisplayName("rechaza un guion de consola renombrado a PDF")
        void rechazaGuionDisfrazado() {
            byte[] guion = "#!/bin/bash\nrm -rf /\n".getBytes(StandardCharsets.UTF_8);
            var archivo = new MockMultipartFile(
                    "documento", "comprobante.pdf", "application/pdf", guion);

            assertThatThrownBy(() -> servicio.guardar(archivo))
                    .isInstanceOf(ReglaNegocioException.class);
        }

        @Test
        @DisplayName("rechaza extensiones no permitidas")
        void rechazaExtensionNoPermitida() {
            for (String nombre : new String[]{"virus.exe", "script.sh", "hoja.xlsx", "doc.docx"}) {
                assertThatThrownBy(() -> servicio.guardar(new MockMultipartFile(
                        "documento", nombre, "application/octet-stream", PDF_VALIDO)))
                        .as("debe rechazarse %s", nombre)
                        .isInstanceOf(ReglaNegocioException.class);
            }
        }

        @Test
        @DisplayName("rechaza un archivo sin extension")
        void rechazaSinExtension() {
            assertThatThrownBy(() -> servicio.guardar(new MockMultipartFile(
                    "documento", "archivo", "application/pdf", PDF_VALIDO)))
                    .isInstanceOf(ReglaNegocioException.class);
        }

        @Test
        @DisplayName("rechaza un archivo vacio")
        void rechazaVacio() {
            assertThatThrownBy(() -> servicio.guardar(new MockMultipartFile(
                    "documento", "vacio.pdf", "application/pdf", new byte[0])))
                    .isInstanceOf(ReglaNegocioException.class);
        }

        @Test
        @DisplayName("rechaza un archivo por encima del tamano maximo")
        void rechazaDemasiadoGrande() {
            DocumentoStorageService conLimiteBajo =
                    new DocumentoStorageService(almacen.toString(), 1);
            conLimiteBajo.prepararAlmacenamiento();

            byte[] grande = new byte[2 * 1024 * 1024];
            System.arraycopy(PDF_VALIDO, 0, grande, 0, PDF_VALIDO.length);

            assertThatThrownBy(() -> conLimiteBajo.guardar(new MockMultipartFile(
                    "documento", "grande.pdf", "application/pdf", grande)))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("maximo");
        }

        @Test
        @DisplayName("rechaza texto plano renombrado a PDF")
        void rechazaTextoDisfrazado() {
            var archivo = new MockMultipartFile("documento", "ine.pdf", "application/pdf",
                    "esto es solo texto, no un PDF".getBytes(StandardCharsets.UTF_8));

            assertThatThrownBy(() -> servicio.guardar(archivo))
                    .isInstanceOf(ReglaNegocioException.class);
        }
    }

    @Nested
    @DisplayName("Proteccion de rutas")
    class ProteccionRutas {

        @Test
        @DisplayName("un nombre con rutas relativas no escapa del almacen")
        void nombreConRutaNoEscapa() {
            var archivo = new MockMultipartFile(
                    "documento", "../../../etc/passwd.pdf", "application/pdf", PDF_VALIDO);

            var guardado = servicio.guardar(archivo);

            // Se conserva para mostrarlo, pero no influye en donde se escribe.
            assertThat(guardado.rutaRelativa()).doesNotContain("..");
            assertThat(almacen.resolve(guardado.rutaRelativa()).normalize())
                    .as("el archivo debe quedar dentro del almacen")
                    .startsWith(almacen);
        }

        @Test
        @DisplayName("no se puede leer fuera del almacen con rutas relativas")
        void noLeeFueraDelAlmacen() {
            for (String ruta : new String[]{
                    "../../../etc/passwd",
                    "../../.env",
                    "..\\..\\windows\\system32\\config\\sam"}) {
                assertThatThrownBy(() -> servicio.abrir(ruta))
                        .as("no debe permitirse leer %s", ruta)
                        .isInstanceOf(AlmacenamientoException.class);
            }
        }

        @Test
        @DisplayName("rechaza una ruta vacia")
        void rechazaRutaVacia() {
            assertThatThrownBy(() -> servicio.abrir(""))
                    .isInstanceOf(AlmacenamientoException.class);
            assertThatThrownBy(() -> servicio.abrir(null))
                    .isInstanceOf(AlmacenamientoException.class);
        }
    }

    @Nested
    @DisplayName("Lectura y borrado")
    class LecturaBorrado {

        @Test
        @DisplayName("devuelve el mismo contenido que se guardo")
        void recuperaContenidoIntacto() throws IOException {
            var guardado = servicio.guardar(new MockMultipartFile(
                    "documento", "ine.pdf", "application/pdf", PDF_VALIDO));

            try (InputStream flujo = servicio.abrir(guardado.rutaRelativa())) {
                assertThat(flujo.readAllBytes()).isEqualTo(PDF_VALIDO);
            }
        }

        @Test
        @DisplayName("informa el tamano del archivo guardado")
        void informaTamano() {
            var guardado = servicio.guardar(new MockMultipartFile(
                    "documento", "ine.pdf", "application/pdf", PDF_VALIDO));

            assertThat(servicio.tamanoDe(guardado.rutaRelativa()))
                    .isEqualTo(PDF_VALIDO.length);
        }

        @Test
        @DisplayName("elimina el archivo del almacen")
        void eliminaArchivo() {
            var guardado = servicio.guardar(new MockMultipartFile(
                    "documento", "ine.pdf", "application/pdf", PDF_VALIDO));

            servicio.eliminar(guardado.rutaRelativa());

            assertThat(servicio.existe(guardado.rutaRelativa())).isFalse();
        }

        @Test
        @DisplayName("falla al abrir un archivo inexistente")
        void fallaSiNoExiste() {
            assertThatThrownBy(() -> servicio.abrir("2026/01/inexistente.pdf"))
                    .isInstanceOf(AlmacenamientoException.class);
        }
    }

    @Nested
    @DisplayName("Organizacion en disco")
    class Organizacion {

        @Test
        @DisplayName("reparte los archivos por ano y mes")
        void repartePorFecha() {
            var guardado = servicio.guardar(new MockMultipartFile(
                    "documento", "ine.pdf", "application/pdf", PDF_VALIDO));

            assertThat(guardado.rutaRelativa())
                    .matches("\\d{4}/\\d{2}/[0-9a-f-]{36}\\.pdf");
        }

        @Test
        @DisplayName("crea el directorio raiz si no existe")
        void creaRaiz() throws IOException {
            Path nueva = almacen.resolve("otra/carpeta/nueva");
            DocumentoStorageService otro =
                    new DocumentoStorageService(nueva.toString(), 10);

            otro.prepararAlmacenamiento();

            assertThat(Files.isDirectory(nueva)).isTrue();
        }
    }
}
