package mx.gob.sedif.sea.core.notificacion;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.gob.sedif.sea.core.auditoria.AccionAuditoria;
import mx.gob.sedif.sea.core.auditoria.AuditoriaService;
import mx.gob.sedif.sea.core.certificado.Certificado;
import mx.gob.sedif.sea.core.cita.Cita;
import mx.gob.sedif.sea.core.persona.Persona;
import mx.gob.sedif.sea.core.solicitud.Solicitud;
import mx.gob.sedif.sea.exception.MessageConstants;
import mx.gob.sedif.sea.exception.RecursoNoEncontradoException;
import mx.gob.sedif.sea.exception.ReglaNegocioException;
import mx.gob.sedif.sea.security.SecurityUtil;
import mx.gob.sedif.sea.security.Usuario;
import mx.gob.sedif.sea.security.UsuarioRepository;
import mx.gob.sedif.sea.util.enums.CanalNotificacion;
import mx.gob.sedif.sea.util.enums.EstatusEnvio;

/**
 * Avisos del tramite.
 *
 * <p>Regla central: si la persona dejo correo, el aviso sale solo; si no, se
 * crea igual pero como contacto manual, para que alguien le llame. Lo que no
 * puede pasar es que no se cree nada, porque entonces nadie se entera de que
 * hay gente esperando respuesta. Eso era justo lo que ocurria antes.
 */
@Service
public class NotificacionService
        implements mx.gob.sedif.sea.core.solicitud.NotificadorTramite {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    /** Claves de las plantillas sembradas. */
    public static final String PLANTILLA_RECIBIDA = "SOLICITUD_RECIBIDA";
    public static final String PLANTILLA_APROBADA = "SOLICITUD_APROBADA";
    public static final String PLANTILLA_OBSERVADA = "SOLICITUD_OBSERVADA";
    public static final String PLANTILLA_RECHAZADA = "SOLICITUD_RECHAZADA";

    private final NotificacionRepository notificacionRepository;
    private final PlantillaNotificacionRepository plantillaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PlantillaResolver resolver;
    private final EnvioCorreoService envioCorreo;
    private final AuditoriaService auditoria;
    private final mx.gob.sedif.sea.core.parametro.ParametroService parametroService;
    private final mx.gob.sedif.sea.core.solicitud.SolicitudRepository solicitudRepository;
    private final mx.gob.sedif.sea.core.certificado.CertificadoRepository certificadoRepository;
    private final String urlConsulta;
    private final int maxIntentos;

    public NotificacionService(NotificacionRepository notificacionRepository,
                               PlantillaNotificacionRepository plantillaRepository,
                               UsuarioRepository usuarioRepository,
                               PlantillaResolver resolver,
                               EnvioCorreoService envioCorreo,
                               AuditoriaService auditoria,
                               mx.gob.sedif.sea.core.parametro.ParametroService parametroService,
                               mx.gob.sedif.sea.core.solicitud.SolicitudRepository solicitudRepository,
                               mx.gob.sedif.sea.core.certificado.CertificadoRepository certificadoRepository,
                               @Value("${app.portal.url-consulta:http://localhost/consulta}")
                               String urlConsulta,
                               @Value("${app.notificacion.max-intentos:3}") int maxIntentos) {
        this.notificacionRepository = notificacionRepository;
        this.plantillaRepository = plantillaRepository;
        this.usuarioRepository = usuarioRepository;
        this.resolver = resolver;
        this.envioCorreo = envioCorreo;
        this.auditoria = auditoria;
        this.parametroService = parametroService;
        this.solicitudRepository = solicitudRepository;
        this.certificadoRepository = certificadoRepository;
        this.urlConsulta = urlConsulta;
        this.maxIntentos = maxIntentos;
    }

    /**
     * Avisa sobre un tramite, localizandolo por su identificador.
     *
     * <p>Es la via que usa el servicio de tramites, que no conoce el
     * certificado ni la cita.
     */
    @Override
    @Transactional
    public void avisar(Long solicitudId, String claveAviso) {
        solicitudRepository.findConDetallePorId(solicitudId).ifPresentOrElse(
                solicitud -> avisar(solicitud, claveAviso,
                        certificadoRepository.findBySolicitudId(solicitudId).orElse(null),
                        null),
                () -> log.error("No se pudo avisar del tramite {}: no se encontro",
                        solicitudId));
    }

    /**
     * Crea y despacha el aviso de un movimiento del tramite.
     *
     * <p>Un fallo aqui nunca interrumpe la resolucion del tramite: el aviso
     * queda pendiente y visible, pero la aprobacion o el rechazo se conservan.
     *
     * @param solicitud    tramite del que se avisa
     * @param claveAviso   plantilla a usar
     * @param certificado  certificado emitido, si aplica
     * @param cita         cita asignada, si aplica
     */
    @Transactional
    public void avisar(Solicitud solicitud, String claveAviso,
                       Certificado certificado, Cita cita) {
        try {
            PlantillaNotificacion plantilla = plantillaRepository
                    .findByClaveAndActivoTrue(claveAviso)
                    .orElse(null);

            if (plantilla == null) {
                log.error("No existe la plantilla '{}' o esta dada de baja; "
                        + "el tramite {} se queda sin aviso",
                        claveAviso, solicitud.getFolioSolicitud());
                return;
            }

            Map<String, String> datos = resolver.datosDe(
                    solicitud, certificado, cita, urlConsulta,
                    parametroService.obtenerEntero(
                            "portal.dias_vigencia_token_correccion", 15));

            Notificacion aviso = new Notificacion();
            aviso.setSolicitud(solicitud);
            aviso.setPlantilla(plantilla);
            aviso.setAsunto(resolver.resolver(plantilla.getAsunto(), datos));
            aviso.setCuerpo(resolver.resolver(plantilla.getCuerpo(), datos));

            Persona persona = solicitud.getPersona();
            boolean tieneCorreo = persona.getCorreo() != null
                    && !persona.getCorreo().isBlank();

            if (tieneCorreo) {
                aviso.setCanal(CanalNotificacion.AUTOMATICO);
                aviso.setDestinatario(persona.getCorreo());
            } else {
                // Sin correo el aviso lo tiene que dar una persona. Se crea
                // igual para que aparezca en la bandeja de contacto.
                aviso.setCanal(CanalNotificacion.MANUAL);
                aviso.setDestinatario(telefonoDe(persona));
            }

            Notificacion guardado = notificacionRepository.save(aviso);

            if (tieneCorreo) {
                despachar(guardado);
            } else {
                log.info("El tramite {} no tiene correo; su aviso queda en la "
                                + "bandeja de contacto para llamar al {}",
                        solicitud.getFolioSolicitud(), guardado.getDestinatario());
            }

        } catch (Exception ex) {
            // Nunca dejar que un fallo del aviso tumbe la resolucion del tramite.
            log.error("No se pudo generar el aviso '{}' del tramite {}",
                    claveAviso, solicitud.getFolioSolicitud(), ex);
        }
    }

    /**
     * Intenta enviar un aviso automatico.
     *
     * <p>Si falla, queda registrado con la causa concreta para que el personal
     * sepa si el problema fue la direccion, el servidor o la red.
     */
    @Transactional
    public void despachar(Notificacion aviso) {
        EnvioCorreoService.ResultadoEnvio resultado = envioCorreo.enviar(
                aviso.getDestinatario(), aviso.getAsunto(), aviso.getCuerpo());

        if (resultado.enviado()) {
            aviso.marcarEnviada();
        } else {
            aviso.marcarFallida(resultado.detalle());
        }

        notificacionRepository.save(aviso);
    }

    /**
     * Reintenta los avisos automaticos que fallaron.
     *
     * <p>Se ejecuta periodicamente. Los que agotan los intentos dejan de
     * reintentarse y quedan visibles para atenderlos a mano.
     *
     * @return cuantos se lograron enviar
     */
    @Transactional
    public int reintentarPendientes() {
        List<Notificacion> pendientes =
                notificacionRepository.findPendientesDeEnvio(maxIntentos);

        if (pendientes.isEmpty()) {
            return 0;
        }

        int enviados = 0;
        for (Notificacion aviso : pendientes) {
            despachar(aviso);
            if (aviso.getEstatusEnvio() == EstatusEnvio.ENVIADA) {
                enviados++;
            }
        }

        log.info("Reintento de avisos: {} de {} se lograron enviar",
                enviados, pendientes.size());
        return enviados;
    }

    /**
     * Bandeja de contacto manual: a quien hay que llamar.
     *
     * <p>Son las personas que solo dejaron telefono, mas los correos que no se
     * pudieron entregar.
     */
    @Transactional(readOnly = true)
    public Page<Notificacion> bandejaContacto(Pageable pageable) {
        return notificacionRepository.findByCanalAndEstatusEnvioOrderByFechaCreacionAsc(
                CanalNotificacion.MANUAL, EstatusEnvio.PENDIENTE, pageable);
    }

    /** Cuantos contactos quedan por atender. */
    @Transactional(readOnly = true)
    public long pendientesDeContacto() {
        return notificacionRepository.countByCanalAndEstatusEnvio(
                CanalNotificacion.MANUAL, EstatusEnvio.PENDIENTE);
    }

    /**
     * Registra el resultado de un contacto telefonico.
     *
     * @param notificacionId aviso atendido
     * @param logrado        si se logro hablar con la persona
     * @param observacion    que se le dijo o por que no se pudo
     */
    @Transactional
    public Notificacion registrarContacto(Long notificacionId, boolean logrado,
                                          String observacion) {
        Notificacion aviso = notificacionRepository.findConDetallePorId(notificacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Notificacion", notificacionId));

        if (aviso.getEstatusEnvio() == EstatusEnvio.CONTACTADA
                || aviso.getEstatusEnvio() == EstatusEnvio.NO_CONTACTADA) {
            throw new ReglaNegocioException(MessageConstants.CONTACTO_YA_REGISTRADO);
        }

        // Sin observacion no queda constancia de que se le dijo, y el siguiente
        // que abra el expediente no sabra en que quedo la llamada.
        if (observacion == null || observacion.isBlank()) {
            throw new ReglaNegocioException(
                    "Escribe que le dijiste a la persona o por que no se logro el "
                            + "contacto. Queda en el expediente del trámite.");
        }

        Usuario responsable = SecurityUtil.obtenerUsuarioId()
                .flatMap(usuarioRepository::findById)
                .orElseThrow(() -> new ReglaNegocioException(MessageConstants.NO_AUTORIZADO));

        aviso.registrarContactoManual(logrado, observacion, responsable);
        Notificacion guardado = notificacionRepository.saveAndFlush(aviso);

        auditoria.registrar(AccionAuditoria.MODIFICAR, "notificacion", notificacionId,
                "Contacto %s con %s sobre el tramite %s".formatted(
                        logrado ? "logrado" : "no logrado",
                        aviso.getDestinatario(),
                        aviso.getSolicitud().getFolioSolicitud()));

        log.info("Contacto {} registrado para el tramite {}",
                logrado ? "logrado" : "no logrado",
                aviso.getSolicitud().getFolioSolicitud());

        // Se devuelve con el detalle cargado: el controlador lo convierte a
        // DTO ya fuera de esta transaccion.
        return notificacionRepository.findConDetallePorId(notificacionId).orElse(guardado);
    }

    /** Avisos de un tramite, del mas reciente al mas antiguo. */
    @Transactional(readOnly = true)
    public List<Notificacion> porSolicitud(Long solicitudId) {
        return notificacionRepository.findBySolicitudIdOrderByFechaCreacionDesc(solicitudId);
    }

    /** Indica si el envio de correo esta configurado. */
    public boolean envioHabilitado() {
        return envioCorreo.estaHabilitado();
    }

    /** Telefono de contacto, con el de WhatsApp como alternativa. */
    private String telefonoDe(Persona persona) {
        if (persona.getTelefono() != null && !persona.getTelefono().isBlank()) {
            return persona.getTelefono();
        }
        if (persona.getTelefonoWhatsapp() != null
                && !persona.getTelefonoWhatsapp().isBlank()) {
            return persona.getTelefonoWhatsapp();
        }
        // No deberia ocurrir: el alta exige al menos un medio de contacto.
        return "(sin medio de contacto)";
    }
}
