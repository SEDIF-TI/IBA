package mx.gob.sedif.sea.core.certificado;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.gob.sedif.sea.core.auditoria.AccionAuditoria;
import mx.gob.sedif.sea.core.auditoria.AuditoriaService;
import mx.gob.sedif.sea.core.documental.CertificadoPdfService;
import mx.gob.sedif.sea.core.documental.CredencialPdfService;
import mx.gob.sedif.sea.core.documental.IdentidadGrafica;
import mx.gob.sedif.sea.core.documento.DocumentoService;
import mx.gob.sedif.sea.core.folio.FolioService;
import mx.gob.sedif.sea.core.solicitud.Solicitud;
import mx.gob.sedif.sea.core.solicitud.SolicitudRepository;
import mx.gob.sedif.sea.exception.AlmacenamientoException;
import mx.gob.sedif.sea.exception.MessageConstants;
import mx.gob.sedif.sea.exception.RecursoNoEncontradoException;
import mx.gob.sedif.sea.exception.ReglaNegocioException;
import mx.gob.sedif.sea.security.SecurityUtil;
import mx.gob.sedif.sea.security.Usuario;
import mx.gob.sedif.sea.security.UsuarioRepository;
import mx.gob.sedif.sea.util.enums.EstatusSolicitudEnum;

/**
 * Emision y entrega de los certificados.
 *
 * <p>El certificado se emite al aprobar el tramite y es lo que acredita el
 * empadronamiento. A partir de el se imprimen la credencial y las reposiciones.
 */
@Service
public class CertificadoService
        implements mx.gob.sedif.sea.core.solicitud.EmisorCertificado {

    private static final Logger log = LoggerFactory.getLogger(CertificadoService.class);

    private final CertificadoRepository certificadoRepository;
    private final SolicitudRepository solicitudRepository;
    private final UsuarioRepository usuarioRepository;
    private final FolioService folioService;
    private final CertificadoPdfService certificadoPdf;
    private final CredencialPdfService credencialPdf;
    private final DocumentoService documentoService;
    private final IdentidadGrafica identidad;
    private final AuditoriaService auditoria;

    public CertificadoService(CertificadoRepository certificadoRepository,
                              SolicitudRepository solicitudRepository,
                              UsuarioRepository usuarioRepository,
                              FolioService folioService,
                              CertificadoPdfService certificadoPdf,
                              CredencialPdfService credencialPdf,
                              DocumentoService documentoService,
                              IdentidadGrafica identidad,
                              AuditoriaService auditoria) {
        this.certificadoRepository = certificadoRepository;
        this.solicitudRepository = solicitudRepository;
        this.usuarioRepository = usuarioRepository;
        this.folioService = folioService;
        this.certificadoPdf = certificadoPdf;
        this.credencialPdf = credencialPdf;
        this.documentoService = documentoService;
        this.identidad = identidad;
        this.auditoria = auditoria;
    }

    /**
     * Emite el certificado de un tramite aprobado.
     *
     * <p>Solo procede sobre tramites aprobados: emitir uno sobre un tramite sin
     * resolver acreditaria algo que nadie autorizo.
     *
     * @param solicitudId tramite aprobado
     * @return certificado emitido
     */
    @Override
    @Transactional
    public void emitir(Long solicitudId) {
        emitirCertificado(solicitudId);
    }

    /**
     * Emite el certificado y lo devuelve.
     *
     * @param solicitudId tramite aprobado
     * @return certificado emitido
     */
    @Transactional
    public Certificado emitirCertificado(Long solicitudId) {
        Solicitud solicitud = solicitudRepository.findConDetallePorId(solicitudId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud", solicitudId));

        String estatus = solicitud.getEstatus().getClave();
        if (!EstatusSolicitudEnum.APROBADA.name().equals(estatus)) {
            throw new ReglaNegocioException(
                    "Solo se emite certificado de un tramite aprobado; este esta %s"
                            .formatted(solicitud.getEstatus().getNombre().toLowerCase()));
        }

        if (certificadoRepository.existsBySolicitudId(solicitudId)) {
            throw new ReglaNegocioException(
                    "El tramite %s ya tiene certificado emitido"
                            .formatted(solicitud.getFolioSolicitud()));
        }

        Usuario emisor = SecurityUtil.obtenerUsuarioId()
                .flatMap(usuarioRepository::findById)
                .orElseThrow(() -> new ReglaNegocioException(MessageConstants.NO_AUTORIZADO));

        Certificado certificado = new Certificado();
        certificado.setSolicitud(solicitud);
        certificado.setFolio(folioService.generarFolioCertificado(
                solicitud.getPersona().getMunicipio()));
        certificado.setCodigoVerificacion(UUID.randomUUID());
        certificado.setUsuarioEmisor(emisor);

        // La vigencia se toma de los ajustes; cero significa sin vencimiento.
        int meses = identidad.vigenciaMeses();
        if (meses > 0) {
            certificado.setFechaVigencia(LocalDate.now().plusMonths(meses));
        }

        Certificado guardado = certificadoRepository.saveAndFlush(certificado);

        // Se genera el documento y se guarda su huella: permite comprobar
        // despues que un PDF presentado es el que expidio el sistema.
        byte[] pdf = certificadoPdf.generar(
                guardado,
                solicitud.getPersona(),
                solicitud.getAnimal(),
                documentoService.obtenerFotografiaAnimal(solicitudId));

        guardado.setHashDocumento(calcularHash(pdf));
        certificadoRepository.save(guardado);

        auditoria.registrar(AccionAuditoria.EMITIR_CERTIFICADO, "certificado",
                guardado.getId(),
                "Certificado %s emitido para el tramite %s".formatted(
                        guardado.getFolio(), solicitud.getFolioSolicitud()));

        log.info("Certificado {} emitido para el tramite {}",
                guardado.getFolio(), solicitud.getFolioSolicitud());

        return guardado;
    }

    /**
     * Genera el PDF del certificado.
     *
     * <p>Se vuelve a componer en cada descarga en lugar de guardarlo: asi
     * cualquier correccion de los datos se refleja, y no se acumulan archivos
     * que hay que resguardar. La huella guardada permite detectar si el
     * contenido cambio desde la emision.
     */
    @Transactional(readOnly = true)
    public byte[] generarPdf(Long certificadoId) {
        Certificado certificado = obtener(certificadoId);
        Solicitud solicitud = certificado.getSolicitud();

        auditoria.registrar(AccionAuditoria.EMITIR_CERTIFICADO, "certificado",
                certificadoId,
                "Descarga del certificado " + certificado.getFolio());

        return certificadoPdf.generar(
                certificado,
                solicitud.getPersona(),
                solicitud.getAnimal(),
                documentoService.obtenerFotografiaAnimal(solicitud.getId()));
    }

    /** Genera la credencial de un certificado. */
    @Transactional(readOnly = true)
    public byte[] generarCredencial(Long certificadoId) {
        Certificado certificado = obtener(certificadoId);
        Solicitud solicitud = certificado.getSolicitud();

        auditoria.registrar(AccionAuditoria.EMITIR_CERTIFICADO, "certificado",
                certificadoId,
                "Descarga de la credencial " + certificado.getFolio());

        return credencialPdf.generar(
                certificado,
                solicitud.getPersona(),
                solicitud.getAnimal(),
                documentoService.obtenerFotografiaAnimal(solicitud.getId()));
    }

    /**
     * Genera varias credenciales en una sola impresion.
     *
     * <p>Es lo que se usa el dia de una jornada: se validan los tramites y se
     * imprime todo de una vez, tres por hoja.
     *
     * @param certificadoIds certificados a imprimir
     */
    @Transactional(readOnly = true)
    public byte[] generarCredencialesPorLote(List<Long> certificadoIds) {
        if (certificadoIds == null || certificadoIds.isEmpty()) {
            throw new ReglaNegocioException("No se indicaron credenciales que imprimir");
        }

        List<CredencialPdfService.DatosCredencial> credenciales = certificadoIds.stream()
                .map(this::obtener)
                .map(certificado -> new CredencialPdfService.DatosCredencial(
                        certificado,
                        certificado.getSolicitud().getPersona(),
                        certificado.getSolicitud().getAnimal(),
                        documentoService.obtenerFotografiaAnimal(
                                certificado.getSolicitud().getId())))
                .toList();

        auditoria.registrar(AccionAuditoria.EMITIR_CERTIFICADO, "certificado", null,
                "Impresion por lote de %d credenciales".formatted(credenciales.size()));

        log.info("Impresion por lote de {} credenciales", credenciales.size());
        return credencialPdf.generarLote(credenciales);
    }

    /**
     * Cancela un certificado.
     *
     * <p>Procede cuando se detecta un error o el empadronamiento deja de tener
     * efecto. El registro se conserva: el certificado circula y hay que poder
     * responder que fue cancelado, no que nunca existio.
     */
    @Transactional
    public Certificado cancelar(Long certificadoId, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaNegocioException(
                    "Debes indicar el motivo de la cancelacion");
        }

        Certificado certificado = obtener(certificadoId);

        if (Boolean.FALSE.equals(certificado.getVigente())) {
            throw new ReglaNegocioException("El certificado ya estaba cancelado");
        }

        Usuario responsable = SecurityUtil.obtenerUsuarioId()
                .flatMap(usuarioRepository::findById)
                .orElseThrow(() -> new ReglaNegocioException(MessageConstants.NO_AUTORIZADO));

        certificado.cancelar(motivo, responsable);
        Certificado guardado = certificadoRepository.save(certificado);

        auditoria.registrar(AccionAuditoria.CANCELAR_CERTIFICADO, "certificado",
                certificadoId,
                "Certificado %s cancelado: %s".formatted(certificado.getFolio(), motivo));

        log.info("Certificado {} cancelado", certificado.getFolio());
        return guardado;
    }

    /** Registra la entrega del documento con su sello. */
    @Transactional
    public Certificado registrarEntrega(Long certificadoId) {
        Certificado certificado = obtener(certificadoId);

        if (Boolean.TRUE.equals(certificado.getSelloFisicoEntregado())) {
            throw new ReglaNegocioException("La entrega ya estaba registrada");
        }

        certificado.registrarEntrega();
        Certificado guardado = certificadoRepository.save(certificado);

        auditoria.registrar(AccionAuditoria.MODIFICAR, "certificado", certificadoId,
                "Entrega registrada del certificado " + certificado.getFolio());

        return guardado;
    }

    /**
     * Certificado con sus relaciones ya cargadas.
     *
     * <p>Quien lo reciba va a componer un PDF o un DTO fuera de la
     * transaccion, y ahi una relacion sin cargar ya no se resuelve.
     */
    @Transactional(readOnly = true)
    public Certificado obtener(Long id) {
        return certificadoRepository.findConDetallePorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Certificado", id));
    }

    @Transactional(readOnly = true)
    public Optional<Certificado> buscarPorSolicitud(Long solicitudId) {
        return certificadoRepository.findBySolicitudId(solicitudId);
    }

    /**
     * Localiza un certificado por el codigo de su QR.
     *
     * <p>Es la consulta del verificador publico.
     */
    @Transactional(readOnly = true)
    public Optional<Certificado> buscarPorCodigoVerificacion(UUID codigo) {
        return certificadoRepository.findByCodigoVerificacion(codigo);
    }

    /** Huella del documento, para comprobar que no fue alterado. */
    private String calcularHash(byte[] contenido) {
        try {
            MessageDigest resumen = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(resumen.digest(contenido));
        } catch (NoSuchAlgorithmException ex) {
            throw new AlmacenamientoException("Algoritmo de resumen no disponible", ex);
        }
    }
}
