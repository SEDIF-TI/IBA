package mx.gob.sedif.sea.core.documento;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import mx.gob.sedif.sea.core.auditoria.AccionAuditoria;
import mx.gob.sedif.sea.core.auditoria.AuditoriaService;
import mx.gob.sedif.sea.core.catalogo.TipoDocumento;
import mx.gob.sedif.sea.core.catalogo.TipoDocumentoRepository;
import mx.gob.sedif.sea.core.solicitud.Solicitud;
import mx.gob.sedif.sea.core.solicitud.SolicitudRepository;
import mx.gob.sedif.sea.exception.MessageConstants;
import mx.gob.sedif.sea.exception.RecursoNoEncontradoException;
import mx.gob.sedif.sea.exception.ReglaNegocioException;
import mx.gob.sedif.sea.security.SecurityUtil;
import mx.gob.sedif.sea.security.Usuario;
import mx.gob.sedif.sea.security.UsuarioRepository;

/**
 * Gestion de los documentos adjuntos a un tramite.
 *
 * <p>Coordina el almacen de archivos con sus metadatos, y se asegura de que
 * toda descarga de un documento personal quede registrada en bitacora.
 */
@Service
public class DocumentoService {

    private static final Logger log = LoggerFactory.getLogger(DocumentoService.class);

    /** Clave del tipo de documento que corresponde a la foto del animal. */
    private static final String CLAVE_FOTOGRAFIA = "FOTOGRAFIA_ANIMAL";

    private final DocumentoRepository documentoRepository;
    private final SolicitudRepository solicitudRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final DocumentoStorageService almacen;
    private final AuditoriaService auditoria;

    public DocumentoService(DocumentoRepository documentoRepository,
                            SolicitudRepository solicitudRepository,
                            TipoDocumentoRepository tipoDocumentoRepository,
                            UsuarioRepository usuarioRepository,
                            DocumentoStorageService almacen,
                            AuditoriaService auditoria) {
        this.documentoRepository = documentoRepository;
        this.solicitudRepository = solicitudRepository;
        this.tipoDocumentoRepository = tipoDocumentoRepository;
        this.usuarioRepository = usuarioRepository;
        this.almacen = almacen;
        this.auditoria = auditoria;
    }

    /**
     * Adjunta un archivo a un tramite.
     *
     * @param solicitudId      tramite al que pertenece
     * @param tipoDocumentoId  que documento es
     * @param archivo          archivo recibido
     * @return metadatos del documento guardado
     */
    /**
     * Tipos que la ciudadania puede adjuntar desde el portal.
     *
     * <p>Se limita a proposito: el resto del expediente lo integra el personal
     * al cotejar contra el documento fisico. Aqui solo va lo que la persona
     * tiene en la mano y sirve para armar su certificado.
     */
    private static final java.util.Set<String> TIPOS_CIUDADANO = java.util.Set.of(
            "FOTOGRAFIA_ANIMAL", "INE", "CURP", "COMPROBANTE_DOMICILIO",
            "CARTILLA_VACUNACION", "COMPROBANTE_ANTIRRABICO");

    /** Tipos vigentes que la ciudadania puede adjuntar desde el portal. */
    @Transactional(readOnly = true)
    public List<TipoDocumento> tiposParaCiudadania() {
        return tipoDocumentoRepository.findByActivoTrueOrderByOrdenAscNombreAsc().stream()
                .filter(t -> TIPOS_CIUDADANO.contains(t.getClave()))
                .toList();
    }

    /**
     * Adjunta un archivo desde el portal ciudadano, identificado por el token
     * del tramite.
     *
     * <p>El token es la credencial de la persona sobre su propio tramite: no
     * hay cuenta, asi que quien lo tiene demuestra que hizo ese registro. Por
     * eso nunca se acepta el id de la solicitud desde fuera: seria adivinable.
     *
     * <p>Solo se aceptan documentos mientras el tramite sigue vivo. Cerrado el
     * expediente, un archivo nuevo ya no cambiaria la resolucion y solo
     * confundiria a quien lo revisa.
     */
    @Transactional
    public Documento adjuntarPorToken(java.util.UUID token, Integer tipoDocumentoId,
                                      MultipartFile archivo) {

        Solicitud solicitud = solicitudRepository.findByTokenConsulta(token)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No encontramos tu tramite. Verifica la liga; si la copiaste, "
                        + "asegurate de haberla copiado completa."));

        String estatus = solicitud.getEstatus().getClave();
        if (mx.gob.sedif.sea.util.enums.EstatusSolicitudEnum.valueOf(estatus).esTerminal()) {
            throw new ReglaNegocioException(
                    "Tu tramite ya esta cerrado (%s), por lo que ya no admite documentos nuevos. "
                    .formatted(solicitud.getEstatus().getNombre())
                    + "Si necesitas corregir algo, comunicate con el IBA.");
        }

        TipoDocumento tipo = tipoDocumentoRepository.findById(tipoDocumentoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Tipo de documento", tipoDocumentoId));

        if (!TIPOS_CIUDADANO.contains(tipo.getClave())) {
            throw new ReglaNegocioException(
                    "El documento '%s' no se carga desde el portal: lo integra el personal "
                    .formatted(tipo.getNombre())
                    + "al revisar tu tramite.");
        }

        return adjuntar(solicitud.getId(), tipoDocumentoId, archivo);
    }

    @Transactional
    public Documento adjuntar(Long solicitudId, Integer tipoDocumentoId, MultipartFile archivo) {
        Solicitud solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud", solicitudId));

        TipoDocumento tipo = tipoDocumentoRepository.findById(tipoDocumentoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Tipo de documento", tipoDocumentoId));

        if (Boolean.FALSE.equals(tipo.getActivo())) {
            throw new ReglaNegocioException(
                    "El tipo de documento '%s' ya no esta disponible".formatted(tipo.getNombre()));
        }

        // Se valida y guarda primero el archivo: si no supera las
        // comprobaciones, no se crea ningun registro huerfano.
        DocumentoStorageService.ArchivoGuardado guardado = almacen.guardar(archivo);

        Documento documento = new Documento();
        documento.setSolicitud(solicitud);
        documento.setTipoDocumento(tipo);
        documento.setNombreOriginal(guardado.nombreOriginal());
        documento.setNombreAlmacenado(guardado.nombreAlmacenado());
        documento.setRutaAlmacenamiento(guardado.rutaRelativa());
        documento.setMime(guardado.mime());
        documento.setTamanoBytes(guardado.tamanoBytes());
        documento.setHashSha256(guardado.hashSha256());

        Documento persistido = documentoRepository.saveAndFlush(documento);

        auditoria.registrar(AccionAuditoria.SUBIR_DOCUMENTO, "documento", persistido.getId(),
                "Se adjunto %s al trámite %s".formatted(
                        tipo.getNombre(), solicitud.getFolioSolicitud()));

        log.info("Documento {} adjuntado al tramite {}",
                tipo.getClave(), solicitud.getFolioSolicitud());

        return documentoRepository.findConDetallePorId(persistido.getId())
                .orElse(persistido);
    }

    /**
     * Abre un documento para descargarlo y deja constancia de quien lo hizo.
     *
     * <p>Este es el unico camino por el que sale un documento personal. Los
     * archivos no son alcanzables por ninguna direccion directa.
     *
     * @param documentoId documento solicitado
     * @return contenido y metadatos para servir la respuesta
     */
    @Transactional(readOnly = true)
    public DocumentoDescarga descargar(Long documentoId) {
        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        MessageConstants.DOCUMENTO_NO_ENCONTRADO));

        if (Boolean.FALSE.equals(documento.getActivo())) {
            throw new RecursoNoEncontradoException(MessageConstants.DOCUMENTO_NO_ENCONTRADO);
        }

        // Constancia obligatoria: queda dicho quien accedio a este dato personal.
        auditoria.registrarDescargaDocumento(
                documento.getId(),
                documento.getTipoDocumento().getNombre(),
                documento.getSolicitud().getId());

        InputStream contenido = almacen.abrir(documento.getRutaAlmacenamiento());

        return new DocumentoDescarga(
                contenido,
                documento.getNombreOriginal(),
                documento.getMime(),
                documento.getTamanoBytes());
    }

    /** Documentos vigentes de un tramite. */
    @Transactional(readOnly = true)
    public List<Documento> listarPorSolicitud(Long solicitudId) {
        return documentoRepository.findBySolicitudIdAndActivoTrue(solicitudId);
    }

    /**
     * Recupera la fotografia del animal para imprimirla en sus documentos.
     *
     * <p>Devuelve vacio si no la adjuntaron o si el archivo ya no esta: el
     * documento se emite igual, con el recuadro sin foto. Negarse a expedir un
     * certificado por una imagen faltante seria desproporcionado.
     *
     * <p>A diferencia de la descarga, esto no pasa por bitacora: no es una
     * persona accediendo a un dato personal, es el propio sistema armando el
     * documento que va a entregar.
     *
     * @param solicitudId tramite del que se toma la foto
     * @return contenido de la imagen, si esta disponible
     */
    @Transactional(readOnly = true)
    public Optional<byte[]> obtenerFotografiaAnimal(Long solicitudId) {
        return documentoRepository.findBySolicitudIdAndActivoTrue(solicitudId).stream()
                .filter(documento -> CLAVE_FOTOGRAFIA.equals(
                        documento.getTipoDocumento().getClave()))
                // Si hay varias, la mas reciente.
                .max(java.util.Comparator.comparing(Documento::getId))
                .flatMap(documento -> {
                    try (InputStream flujo = almacen.abrir(documento.getRutaAlmacenamiento())) {
                        return Optional.of(flujo.readAllBytes());
                    } catch (Exception ex) {
                        log.warn("No se pudo leer la fotografia del tramite {}",
                                solicitudId, ex);
                        return Optional.empty();
                    }
                });
    }

    /**
     * Marca un documento como legible o ilegible.
     *
     * <p>El INE ilegible es el motivo de observacion mas frecuente; dejarlo
     * anotado permite pedir el reenvio con precision en vez de rechazar el
     * tramite entero.
     */
    @Transactional
    public Documento revisar(Long documentoId, boolean legible, String observacion) {
        Documento documento = documentoRepository.findConDetallePorId(documentoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        MessageConstants.DOCUMENTO_NO_ENCONTRADO));

        Usuario revisor = SecurityUtil.obtenerUsuarioId()
                .flatMap(usuarioRepository::findById)
                .orElseThrow(() -> new ReglaNegocioException(MessageConstants.NO_AUTORIZADO));

        documento.revisar(legible, observacion, revisor);
        Documento guardado = documentoRepository.saveAndFlush(documento);

        auditoria.registrar(AccionAuditoria.REVISAR_DOCUMENTO, "documento", documentoId,
                "Documento marcado como %s".formatted(legible ? "legible" : "ilegible"));

        return documentoRepository.findConDetallePorId(documentoId).orElse(guardado);
    }

    /**
     * Da de baja un documento.
     *
     * <p>El archivo se borra del almacen, pero los metadatos permanecen: asi
     * queda constancia de que existio y de que se retiro.
     */
    @Transactional
    public void eliminar(Long documentoId) {
        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        MessageConstants.DOCUMENTO_NO_ENCONTRADO));

        documento.setActivo(false);
        documentoRepository.save(documento);

        if (almacen.existe(documento.getRutaAlmacenamiento())) {
            almacen.eliminar(documento.getRutaAlmacenamiento());
        }

        auditoria.registrar(AccionAuditoria.ELIMINAR_DOCUMENTO, "documento", documentoId,
                "Se retiro %s del trámite %s".formatted(
                        documento.getTipoDocumento().getNombre(),
                        documento.getSolicitud().getFolioSolicitud()));
    }

    /**
     * Contenido y metadatos para servir una descarga.
     *
     * @param contenido      flujo del archivo
     * @param nombreOriginal nombre con el que se descarga
     * @param mime           tipo de contenido
     * @param tamanoBytes    tamano del archivo
     */
    public record DocumentoDescarga(
            InputStream contenido,
            String nombreOriginal,
            String mime,
            long tamanoBytes) {
    }
}
