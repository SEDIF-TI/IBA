package mx.gob.sedif.sea.core.documento;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import mx.gob.sedif.sea.exception.AlmacenamientoException;
import mx.gob.sedif.sea.exception.MessageConstants;
import mx.gob.sedif.sea.exception.ReglaNegocioException;

import jakarta.annotation.PostConstruct;

/**
 * Guarda y recupera los archivos adjuntos a los tramites.
 *
 * <p>Esta clase existe para cerrar la falla mas grave del sistema anterior: los
 * documentos personales se guardaban bajo el directorio publicado por el
 * servidor web, con direccion predecible. Cualquiera podia descargar el INE de
 * un tercero adivinando la URL.
 *
 * <p>Aqui:
 * <ul>
 *   <li>Los archivos viven en un volumen que ningun servidor web publica.</li>
 *   <li>El nombre en disco es un identificador generado, sin relacion con el
 *       nombre original ni con la persona.</li>
 *   <li>El nombre que subio la persona jamas se usa para armar la ruta, para
 *       que no pueda escapar del directorio previsto.</li>
 *   <li>El tipo se determina leyendo el contenido, no la extension.</li>
 *   <li>La unica salida es el endpoint autenticado, que ademas deja constancia
 *       en bitacora.</li>
 * </ul>
 */
@Service
public class DocumentoStorageService {

    private static final Logger log = LoggerFactory.getLogger(DocumentoStorageService.class);

    /**
     * Tipos admitidos, determinados por contenido.
     *
     * <p>Es una lista blanca a proposito: enumerar lo permitido deja fuera lo
     * desconocido, mientras que enumerar lo prohibido siempre olvida algo.
     */
    private static final Set<String> MIMES_PERMITIDOS = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png");

    /** Extensiones que corresponden a cada tipo admitido. */
    private static final Set<String> EXTENSIONES_PERMITIDAS = Set.of(
            "pdf", "jpg", "jpeg", "png");

    /**
     * Firmas que delatan contenido ejecutable.
     *
     * <p>Se revisan aunque el tipo detectado sea aceptable, por si algun dia
     * apareciera una forma de que Tika devuelva un tipo inocuo para un archivo
     * que no lo es.
     */
    private static final List<byte[]> FIRMAS_EJECUTABLES = List.of(
            new byte[]{0x4D, 0x5A},                          // ejecutable de Windows
            new byte[]{0x7F, 0x45, 0x4C, 0x46},              // ejecutable de Linux
            new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE}, // clase Java
            new byte[]{0x23, 0x21});                         // guion con interprete

    private static final DateTimeFormatter FORMATO_CARPETA =
            DateTimeFormatter.ofPattern("yyyy/MM");

    private final Tika tika = new Tika();
    private final Path raizAlmacenamiento;
    private final long tamanoMaximoBytes;

    public DocumentoStorageService(
            @Value("${app.storage.path}") String rutaAlmacenamiento,
            @Value("${app.storage.max-file-mb:10}") int tamanoMaximoMb) {
        this.raizAlmacenamiento = Paths.get(rutaAlmacenamiento).toAbsolutePath().normalize();
        this.tamanoMaximoBytes = (long) tamanoMaximoMb * 1024 * 1024;
    }

    /** Prepara el directorio raiz al arrancar. */
    @PostConstruct
    public void prepararAlmacenamiento() {
        try {
            Files.createDirectories(raizAlmacenamiento);
            log.info("Almacen de documentos listo en {}", raizAlmacenamiento);
        } catch (IOException ex) {
            throw new AlmacenamientoException(
                    "No se pudo preparar el almacen de documentos en " + raizAlmacenamiento, ex);
        }
    }

    /**
     * Valida y guarda un archivo.
     *
     * @param archivo archivo recibido
     * @return datos del archivo ya guardado
     * @throws ReglaNegocioException   si el archivo no supera las validaciones
     * @throws AlmacenamientoException si falla la escritura
     */
    public ArchivoGuardado guardar(MultipartFile archivo) {
        validar(archivo);

        String extension = extensionDe(archivo.getOriginalFilename());
        // Identificador generado: no revela contenido, titular ni orden de alta.
        String nombreGenerado = UUID.randomUUID() + "." + extension;

        // Se reparte por ano y mes para que ningun directorio acumule cientos de
        // miles de archivos, lo que degrada el sistema de archivos.
        String subcarpeta = LocalDate.now().format(FORMATO_CARPETA);
        Path directorio = raizAlmacenamiento.resolve(subcarpeta).normalize();

        // Comprobacion de seguridad: el destino debe quedar dentro del almacen.
        if (!directorio.startsWith(raizAlmacenamiento)) {
            throw new AlmacenamientoException("Ruta de destino fuera del almacen");
        }

        try {
            Files.createDirectories(directorio);
            Path destino = directorio.resolve(nombreGenerado);

            String hash;
            try (InputStream entrada = archivo.getInputStream()) {
                Files.copy(entrada, destino, StandardCopyOption.REPLACE_EXISTING);
            }
            hash = calcularHash(destino);

            String mime = tika.detect(destino);

            log.debug("Archivo guardado: {} ({} bytes, {})",
                    nombreGenerado, archivo.getSize(), mime);

            return new ArchivoGuardado(
                    archivo.getOriginalFilename(),
                    nombreGenerado,
                    subcarpeta + "/" + nombreGenerado,
                    mime,
                    archivo.getSize(),
                    hash);

        } catch (IOException ex) {
            throw new AlmacenamientoException(MessageConstants.ERROR_ALMACENAR_ARCHIVO, ex);
        }
    }

    /**
     * Abre un archivo guardado para servirlo.
     *
     * <p>Solo debe invocarse desde el endpoint autenticado, que valida permisos
     * y deja constancia en bitacora.
     *
     * @param rutaRelativa ruta guardada en los metadatos del documento
     * @return contenido del archivo
     */
    public InputStream abrir(String rutaRelativa) {
        Path archivo = resolverRuta(rutaRelativa);
        try {
            if (!Files.exists(archivo)) {
                throw new AlmacenamientoException(
                        "El archivo ya no está en el almacen: " + rutaRelativa);
            }
            return Files.newInputStream(archivo);
        } catch (IOException ex) {
            throw new AlmacenamientoException(
                    "No se pudo leer el archivo " + rutaRelativa, ex);
        }
    }

    /** Tamano en disco del archivo. */
    public long tamanoDe(String rutaRelativa) {
        try {
            return Files.size(resolverRuta(rutaRelativa));
        } catch (IOException ex) {
            throw new AlmacenamientoException(
                    "No se pudo consultar el archivo " + rutaRelativa, ex);
        }
    }

    /** Verdadero si el archivo sigue estando en el almacen. */
    public boolean existe(String rutaRelativa) {
        return Files.exists(resolverRuta(rutaRelativa));
    }

    /**
     * Borra un archivo del almacen.
     *
     * <p>Se usa al reemplazar un documento corregido. Los metadatos conservan
     * el registro historico aunque el archivo desaparezca.
     */
    public void eliminar(String rutaRelativa) {
        try {
            Files.deleteIfExists(resolverRuta(rutaRelativa));
            log.info("Archivo eliminado del almacen: {}", rutaRelativa);
        } catch (IOException ex) {
            throw new AlmacenamientoException(
                    "No se pudo eliminar el archivo " + rutaRelativa, ex);
        }
    }

    /**
     * Convierte una ruta relativa en ruta absoluta dentro del almacen.
     *
     * <p>Comprueba que el resultado siga estando dentro: una ruta con
     * "../.." apuntaria a archivos del sistema fuera del almacen.
     */
    private Path resolverRuta(String rutaRelativa) {
        if (rutaRelativa == null || rutaRelativa.isBlank()) {
            throw new AlmacenamientoException("Ruta de archivo vacía");
        }
        Path resuelta = raizAlmacenamiento.resolve(rutaRelativa).normalize();
        if (!resuelta.startsWith(raizAlmacenamiento)) {
            log.warn("Intento de acceso fuera del almacen: {}", rutaRelativa);
            throw new AlmacenamientoException("Ruta de archivo invalida");
        }
        return resuelta;
    }

    /** Comprueba tamano, extension, tipo real y contenido. */
    private void validar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ReglaNegocioException(MessageConstants.ARCHIVO_VACIO);
        }

        if (archivo.getSize() > tamanoMaximoBytes) {
            // Se dice cuanto pesa y cuanto se admite: sin esos dos numeros la
            // persona no sabe cuanto tiene que reducirlo.
            throw new ReglaNegocioException(
                    "El archivo pesa %.1f MB y el maximo permitido es %d MB. "
                            .formatted(archivo.getSize() / 1024.0 / 1024.0,
                                    tamanoMaximoBytes / 1024 / 1024)
                            + "Reduce su tamaño o toma la fotografia con menor resolución.");
        }

        String extension = extensionDe(archivo.getOriginalFilename());
        if (!EXTENSIONES_PERMITIDAS.contains(extension)) {
            // Se nombra la extension que llego y las que si se aceptan.
            throw new ReglaNegocioException(
                    extension.isBlank()
                            ? "El archivo no tiene extension. Solo se aceptan archivos "
                                    + "PDF, JPG y PNG."
                            : "No se aceptan archivos .%s. Solo se admiten PDF, JPG y PNG."
                                    .formatted(extension));
        }

        byte[] inicio;
        String mimeReal;
        try (InputStream entrada = archivo.getInputStream()) {
            inicio = entrada.readNBytes(512);
            mimeReal = tika.detect(inicio, archivo.getOriginalFilename());
        } catch (IOException ex) {
            throw new AlmacenamientoException("No se pudo leer el archivo recibido", ex);
        }

        // Determinante: el tipo se decide por el contenido, no por la extension.
        // Renombrar un ejecutable a .jpg no basta para colarlo.
        if (!MIMES_PERMITIDOS.contains(mimeReal)) {
            log.warn("Archivo rechazado: se declaro '{}' pero el contenido es '{}'",
                    archivo.getOriginalFilename(), mimeReal);
            throw new ReglaNegocioException(
                    MessageConstants.ARCHIVO_CONTENIDO_NO_CORRESPONDE);
        }

        if (pareceEjecutable(inicio)) {
            log.warn("Archivo con firma de ejecutable rechazado: {}",
                    archivo.getOriginalFilename());
            throw new ReglaNegocioException(
                    "El archivo contiene un programa ejecutable y no se puede subir. "
                            + "Adjunta el documento en PDF, JPG o PNG.");
        }
    }

    /** Compara el inicio del archivo con firmas conocidas de ejecutables. */
    private boolean pareceEjecutable(byte[] inicio) {
        for (byte[] firma : FIRMAS_EJECUTABLES) {
            if (inicio.length >= firma.length) {
                boolean coincide = true;
                for (int i = 0; i < firma.length; i++) {
                    if (inicio[i] != firma[i]) {
                        coincide = false;
                        break;
                    }
                }
                if (coincide) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Extension en minusculas, sin punto. */
    private String extensionDe(String nombreArchivo) {
        if (nombreArchivo == null) {
            return "";
        }
        // Se toma solo el nombre final: un valor como "../../algo.pdf" no debe
        // aportar nada a la ruta.
        String soloNombre = Paths.get(nombreArchivo).getFileName().toString();
        int punto = soloNombre.lastIndexOf('.');
        if (punto < 0 || punto == soloNombre.length() - 1) {
            return "";
        }
        return soloNombre.substring(punto + 1).toLowerCase();
    }

    /** Huella SHA-256 del archivo, para detectar duplicados y alteraciones. */
    private String calcularHash(Path archivo) throws IOException {
        try (InputStream entrada = Files.newInputStream(archivo)) {
            MessageDigest resumen = MessageDigest.getInstance("SHA-256");
            byte[] bloque = new byte[8192];
            int leidos;
            while ((leidos = entrada.read(bloque)) != -1) {
                resumen.update(bloque, 0, leidos);
            }
            return HexFormat.of().formatHex(resumen.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new AlmacenamientoException("Algoritmo de resumen no disponible", ex);
        }
    }

    /**
     * Datos de un archivo ya guardado.
     *
     * @param nombreOriginal   nombre con el que llego; solo para mostrarlo
     * @param nombreAlmacenado nombre generado en disco
     * @param rutaRelativa     ubicacion dentro del almacen
     * @param mime             tipo determinado por contenido
     * @param tamanoBytes      tamano del archivo
     * @param hashSha256       huella del contenido
     */
    public record ArchivoGuardado(
            String nombreOriginal,
            String nombreAlmacenado,
            String rutaRelativa,
            String mime,
            long tamanoBytes,
            String hashSha256) {
    }
}
