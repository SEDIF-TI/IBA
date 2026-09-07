package mx.gob.sedif.sea.core.solicitud;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.gob.sedif.sea.core.animal.Animal;
import mx.gob.sedif.sea.core.animal.AnimalRepository;
import mx.gob.sedif.sea.core.auditoria.AccionAuditoria;
import mx.gob.sedif.sea.core.auditoria.AuditoriaService;
import mx.gob.sedif.sea.core.catalogo.EstatusSolicitud;
import mx.gob.sedif.sea.core.catalogo.EstatusSolicitudRepository;
import mx.gob.sedif.sea.core.catalogo.MotivoRechazo;
import mx.gob.sedif.sea.core.catalogo.MotivoRechazoRepository;
import mx.gob.sedif.sea.core.catalogo.Municipio;
import mx.gob.sedif.sea.core.catalogo.MunicipioRepository;
import mx.gob.sedif.sea.core.catalogo.ColorRepository;
import mx.gob.sedif.sea.core.catalogo.EspecieRepository;
import mx.gob.sedif.sea.core.catalogo.MetodoIdentificacionRepository;
import mx.gob.sedif.sea.core.catalogo.RazaRepository;
import mx.gob.sedif.sea.core.catalogo.SexoRepository;
import mx.gob.sedif.sea.core.catalogo.TamanoRepository;
import mx.gob.sedif.sea.core.folio.FolioService;
import mx.gob.sedif.sea.core.persona.CurpUtil;
import mx.gob.sedif.sea.core.persona.Persona;
import mx.gob.sedif.sea.core.persona.PersonaRepository;
import mx.gob.sedif.sea.core.titularidad.Titularidad;
import mx.gob.sedif.sea.core.titularidad.TitularidadRepository;
import mx.gob.sedif.sea.exception.MessageConstants;
import mx.gob.sedif.sea.exception.RecursoNoEncontradoException;
import mx.gob.sedif.sea.exception.ReglaNegocioException;
import mx.gob.sedif.sea.exception.TransicionInvalidaException;
import mx.gob.sedif.sea.security.SecurityUtil;
import mx.gob.sedif.sea.security.Usuario;
import mx.gob.sedif.sea.security.UsuarioRepository;
import mx.gob.sedif.sea.util.enums.CanalSolicitud;
import mx.gob.sedif.sea.util.enums.EstatusSolicitudEnum;

/**
 * El tramite de empadronamiento, de principio a fin.
 *
 * <p>Reune las reglas que dan sentido al sistema: reutilizar a la persona
 * cuando ya esta registrada, marcar lo que huele a suplantacion, y no dejar que
 * un tramite salte a un estado al que no puede llegar.
 */
@Service
public class SolicitudService {

    private static final Logger log = LoggerFactory.getLogger(SolicitudService.class);

    private final SolicitudRepository solicitudRepository;
    private final SolicitudHistorialRepository historialRepository;
    private final PersonaRepository personaRepository;
    private final AnimalRepository animalRepository;
    private final TitularidadRepository titularidadRepository;
    private final EstatusSolicitudRepository estatusRepository;
    private final MotivoRechazoRepository motivoRepository;
    private final MunicipioRepository municipioRepository;
    private final EspecieRepository especieRepository;
    private final SexoRepository sexoRepository;
    private final RazaRepository razaRepository;
    private final ColorRepository colorRepository;
    private final TamanoRepository tamanoRepository;
    private final MetodoIdentificacionRepository metodoIdentificacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final FolioService folioService;
    private final AuditoriaService auditoria;
    private final EmisorCertificado emisorCertificado;
    private final NotificadorTramite notificador;
    private final mx.gob.sedif.sea.core.jornada.JornadaRepository jornadaRepository;

    public SolicitudService(SolicitudRepository solicitudRepository,
                            SolicitudHistorialRepository historialRepository,
                            PersonaRepository personaRepository,
                            AnimalRepository animalRepository,
                            TitularidadRepository titularidadRepository,
                            EstatusSolicitudRepository estatusRepository,
                            MotivoRechazoRepository motivoRepository,
                            MunicipioRepository municipioRepository,
                            EspecieRepository especieRepository,
                            SexoRepository sexoRepository,
                            RazaRepository razaRepository,
                            ColorRepository colorRepository,
                            TamanoRepository tamanoRepository,
                            MetodoIdentificacionRepository metodoIdentificacionRepository,
                            UsuarioRepository usuarioRepository,
                            FolioService folioService,
                            AuditoriaService auditoria,
                            EmisorCertificado emisorCertificado,
                            NotificadorTramite notificador,
                            mx.gob.sedif.sea.core.jornada.JornadaRepository jornadaRepository) {
        this.solicitudRepository = solicitudRepository;
        this.historialRepository = historialRepository;
        this.personaRepository = personaRepository;
        this.animalRepository = animalRepository;
        this.titularidadRepository = titularidadRepository;
        this.estatusRepository = estatusRepository;
        this.motivoRepository = motivoRepository;
        this.municipioRepository = municipioRepository;
        this.especieRepository = especieRepository;
        this.sexoRepository = sexoRepository;
        this.razaRepository = razaRepository;
        this.colorRepository = colorRepository;
        this.tamanoRepository = tamanoRepository;
        this.metodoIdentificacionRepository = metodoIdentificacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.folioService = folioService;
        this.auditoria = auditoria;
        this.emisorCertificado = emisorCertificado;
        this.notificador = notificador;
        this.jornadaRepository = jornadaRepository;
    }

    /**
     * Da de alta un tramite desde el portal, sin cuenta de usuario.
     *
     * @param peticion datos del tutor, del animal y del consentimiento
     * @param canal    por donde entro
     * @param ip       direccion de origen, que es la evidencia del consentimiento
     * @return el tramite creado
     */
    @Transactional
    public Solicitud registrar(RegistroPublicoRequest peticion, CanalSolicitud canal, String ip) {
        List<String> avisos = new ArrayList<>();

        Persona persona = resolverPersona(peticion.tutor(), avisos);
        Animal animal = crearAnimal(peticion.animal(), persona);

        // El vinculo se lleva en titularidad, con vigencia, no como llave del
        // animal: asi se conserva la historia cuando se transfiere o fallece.
        Titularidad titularidad = new Titularidad();
        titularidad.setPersona(persona);
        titularidad.setAnimal(animal);
        titularidadRepository.save(titularidad);

        EstatusSolicitud registrada = obtenerEstatus(EstatusSolicitudEnum.REGISTRADA);

        Solicitud solicitud = new Solicitud();
        solicitud.setFolioSolicitud(folioService.generarFolioSolicitud());
        solicitud.setTokenConsulta(UUID.randomUUID());
        solicitud.setPersona(persona);
        solicitud.setAnimal(animal);
        solicitud.setEstatus(registrada);
        solicitud.setCanal(canal);
        solicitud.setFechaRegistro(OffsetDateTime.now());

        // La precarga queda ligada a su jornada, para poder localizarla el dia
        // del evento buscando entre las de esa jornada.
        if (peticion.jornadaId() != null) {
            solicitud.setJornada(jornadaRepository.findById(peticion.jornadaId())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Jornada", peticion.jornadaId())));
        }
        solicitud.setAvisoPrivacidadVersion(peticion.avisoVersion());
        solicitud.setFechaAceptacionAviso(OffsetDateTime.now());
        solicitud.setIpOrigen(ip);

        if (!avisos.isEmpty()) {
            solicitud.marcarRevisionReforzada(String.join("; ", avisos));
        }

        Solicitud guardada = solicitudRepository.save(solicitud);

        // Primer renglon del expediente: el alta no viene de ningun estado previo.
        registrarHistorial(guardada, null, registrada, null, "Alta desde " + canal, null);

        auditoria.registrar(AccionAuditoria.CREAR, "solicitud", guardada.getId(),
                "Alta del trámite %s por %s".formatted(guardada.getFolioSolicitud(), canal));

        log.info("Tramite {} dado de alta ({}){}",
                guardada.getFolioSolicitud(), canal,
                avisos.isEmpty() ? "" : " con revisión reforzada");

        // Acuse de recepcion. Si no dejo correo, queda en la bandeja de contacto.
        notificador.avisar(guardada.getId(), "SOLICITUD_RECIBIDA");

        return solicitudRepository.findConDetallePorId(guardada.getId()).orElse(guardada);
    }

    /**
     * Localiza a la persona por su CURP o la da de alta.
     *
     * <p>Si ya esta registrada se reutiliza: quien registro un gato el mes
     * pasado no deberia volver a capturar sus datos hoy, y ademas asi el
     * personal ve cuantos animales tiene a su cargo.
     *
     * <p>Cuando los datos de contacto que llegan no coinciden con los que ya
     * constaban, se anota para revision reforzada. Como la ciudadania no se
     * autentica, cualquiera que conozca una CURP ajena podria intentar usarla.
     */
    private Persona resolverPersona(RegistroPublicoRequest.TutorRequest tutor,
                                    List<String> avisos) {

        String curp = (tutor.curp() == null || tutor.curp().isBlank())
                ? null
                : CurpUtil.normalizar(tutor.curp());

        if (curp != null) {
            if (!CurpUtil.esValida(curp)) {
                throw new ReglaNegocioException(MessageConstants.CURP_INVALIDA);
            }
            // El cotejo con el nombre no bloquea: hay apellidos compuestos y
            // casos que la regla general no cubre. Solo se deja anotado.
            if (!CurpUtil.coincideConNombre(curp, tutor.nombre(),
                    tutor.apellidoPaterno(), tutor.apellidoMaterno())) {
                avisos.add("La CURP no corresponde con el nombre capturado");
            }

            Optional<Persona> existente = personaRepository.findByCurp(curp);
            if (existente.isPresent()) {
                Persona persona = existente.get();
                verificarContactoConocido(persona, tutor, avisos);
                actualizarDatosFaltantes(persona, tutor);
                return personaRepository.save(persona);
            }
        }

        return personaRepository.save(construirPersona(tutor, curp));
    }

    /** Avisa si los datos de contacto entrantes difieren de los ya registrados. */
    private void verificarContactoConocido(Persona persona,
                                           RegistroPublicoRequest.TutorRequest tutor,
                                           List<String> avisos) {
        if (difiere(persona.getCorreo(), tutor.correo())) {
            avisos.add("El correo no coincide con el registrado anteriormente");
        }
        if (difiere(persona.getTelefono(), tutor.telefono())) {
            avisos.add("El teléfono no coincide con el registrado anteriormente");
        }
    }

    private boolean difiere(String registrado, String entrante) {
        if (registrado == null || registrado.isBlank()
                || entrante == null || entrante.isBlank()) {
            return false;
        }
        return !registrado.trim().equalsIgnoreCase(entrante.trim());
    }

    /** Completa los datos que faltaban, sin pisar los que ya constaban. */
    private void actualizarDatosFaltantes(Persona persona,
                                          RegistroPublicoRequest.TutorRequest tutor) {
        if (vacio(persona.getCorreo()) && !vacio(tutor.correo())) {
            persona.setCorreo(tutor.correo().trim());
        }
        if (vacio(persona.getTelefono()) && !vacio(tutor.telefono())) {
            persona.setTelefono(tutor.telefono().trim());
        }
        if (vacio(persona.getTelefonoWhatsapp()) && !vacio(tutor.telefonoWhatsapp())) {
            persona.setTelefonoWhatsapp(tutor.telefonoWhatsapp().trim());
        }
    }

    private boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }

    /** Arma una persona nueva, derivando de la CURP lo que se pueda. */
    private Persona construirPersona(RegistroPublicoRequest.TutorRequest tutor, String curp) {
        Municipio municipio = municipioRepository.findById(tutor.municipioId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Municipio", tutor.municipioId()));

        Persona persona = new Persona();
        persona.setCurp(curp);
        persona.setNombre(tutor.nombre().trim());
        persona.setApellidoPaterno(tutor.apellidoPaterno().trim());
        persona.setApellidoMaterno(vacio(tutor.apellidoMaterno())
                ? null : tutor.apellidoMaterno().trim());
        persona.setCorreo(vacio(tutor.correo()) ? null : tutor.correo().trim());
        persona.setTelefono(vacio(tutor.telefono()) ? null : tutor.telefono().trim());
        persona.setTelefonoWhatsapp(vacio(tutor.telefonoWhatsapp())
                ? null : tutor.telefonoWhatsapp().trim());
        persona.setMunicipio(municipio);
        persona.setCalle(tutor.calle());
        persona.setNumeroExterior(tutor.numeroExterior());
        persona.setNumeroInterior(tutor.numeroInterior());
        persona.setColonia(tutor.colonia());
        persona.setCodigoPostal(vacio(tutor.codigoPostal()) ? null : tutor.codigoPostal());

        // La CURP trae dentro la fecha de nacimiento, el sexo y la entidad.
        if (curp != null) {
            CurpUtil.extraerFechaNacimiento(curp).ifPresent(persona::setFechaNacimiento);
            CurpUtil.extraerSexo(curp).ifPresent(persona::setSexoCurp);
            CurpUtil.extraerEntidad(curp).ifPresent(persona::setEntidadNacimiento);
        }

        // Queda como identidad pendiente hasta que alguien coteje el documento.
        persona.setIdentidadValidada(false);
        return persona;
    }

    /** Da de alta el animal del tramite. */
    private Animal crearAnimal(RegistroPublicoRequest.AnimalRequest datos, Persona tutor) {
        Animal animal = new Animal();
        animal.setNombre(datos.nombre().trim());
        animal.setEspecie(especieRepository.findById(datos.especieId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Especie", datos.especieId())));
        animal.setSexo(sexoRepository.findById(datos.sexoId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Sexo", datos.sexoId())));
        if (datos.razaId() != null) {
            animal.setRaza(razaRepository.findById(datos.razaId())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Raza", datos.razaId())));
        }
        if (datos.colorId() != null) {
            animal.setColor(colorRepository.findById(datos.colorId())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Color", datos.colorId())));
        }
        if (datos.tamanoId() != null) {
            animal.setTamano(tamanoRepository.findById(datos.tamanoId())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Tamano", datos.tamanoId())));
        }
        if (datos.metodoIdentificacionId() != null) {
            animal.setMetodoIdentificacion(
                    metodoIdentificacionRepository.findById(datos.metodoIdentificacionId())
                            .orElseThrow(() -> new RecursoNoEncontradoException(
                                    "Metodo de identificación",
                                    datos.metodoIdentificacionId())));
        }
        animal.setEdadMeses(datos.edadTotalMeses());
        animal.setVacunado(Boolean.TRUE.equals(datos.vacunado()));
        animal.setEsterilizado(Boolean.TRUE.equals(datos.esterilizado()));
        animal.setCaracteristicas(datos.caracteristicas());
        animal.setIdentificador(vacio(datos.identificador()) ? null : datos.identificador());

        Animal guardado = animalRepository.save(animal);

        guardado.setClaveAnimal(folioService.generarClaveAnimal(
                tutor.getNombre(), tutor.getApellidoPaterno(),
                guardado.getNombre(),
                guardado.getEspecie().getClave(),
                guardado.getSexo().getClave()));

        return animalRepository.save(guardado);
    }

    // ------------------------------------------------------------------
    // Movimientos del tramite
    // ------------------------------------------------------------------

    /**
     * Pasa el tramite a otro estado.
     *
     * <p>Comprueba que el movimiento sea legal y deja renglon en el historial.
     * Sin esa comprobacion se podria entregar un tramite que nunca se aprobo.
     *
     * @param solicitudId tramite a mover
     * @param destino     estado al que debe pasar
     * @param motivoId    motivo del catalogo; obligatorio al observar, rechazar o cancelar
     * @param observacion nota libre del personal
     */
    @Transactional
    public Solicitud cambiarEstatus(Long solicitudId, EstatusSolicitudEnum destino,
                                    Integer motivoId, String observacion) {

        Solicitud solicitud = obtener(solicitudId);
        EstatusSolicitud estatusActual = solicitud.getEstatus();
        EstatusSolicitudEnum origen = EstatusSolicitudEnum.valueOf(estatusActual.getClave());

        if (!origen.puedeTransicionarA(destino)) {
            throw new TransicionInvalidaException(origen, destino);
        }

        // Observar, rechazar y cancelar exigen decir por que: sin motivo, la
        // persona recibe un aviso que no le dice como corregir.
        MotivoRechazo motivo = null;
        if (destino.exigeMotivo()) {
            if (motivoId == null) {
                throw new ReglaNegocioException(MessageConstants.MOTIVO_REQUERIDO);
            }
            motivo = motivoRepository.findById(motivoId)
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Motivo de rechazo", motivoId));
        }

        EstatusSolicitud estatusNuevo = obtenerEstatus(destino);
        Usuario responsable = usuarioEnSesion();

        solicitud.setEstatus(estatusNuevo);
        solicitud.setObservacionResolucion(observacion);
        solicitud.setMotivoRechazo(motivo);

        // Se anota quien resolvio y cuando; sin eso no hay rendicion de cuentas.
        if (destino == EstatusSolicitudEnum.APROBADA
                || destino == EstatusSolicitudEnum.RECHAZADA
                || destino == EstatusSolicitudEnum.CANCELADA) {
            solicitud.setFechaResolucion(OffsetDateTime.now());
            solicitud.setUsuarioResolucion(responsable);
        }

        Solicitud guardada = solicitudRepository.saveAndFlush(solicitud);
        registrarHistorial(guardada, estatusActual, estatusNuevo, motivo, observacion, responsable);

        auditoria.registrar(accionDe(destino), "solicitud", solicitudId,
                "El trámite %s pasa de %s a %s".formatted(
                        guardada.getFolioSolicitud(), origen, destino));

        log.info("Tramite {}: {} -> {}", guardada.getFolioSolicitud(), origen, destino);

        // Se devuelve con el detalle cargado: quien lo reciba lo convertira a
        // DTO ya fuera de esta transaccion.
        return solicitudRepository.findConDetallePorId(solicitudId).orElse(guardada);
    }

    /** Toma el tramite para revisarlo. */
    @Transactional
    public Solicitud tomarParaRevision(Long solicitudId) {
        return cambiarEstatus(solicitudId, EstatusSolicitudEnum.EN_REVISION, null,
                "Trámite en revisión");
    }

    /**
     * Aprueba el tramite y emite su certificado.
     *
     * <p>La emision va aqui y no como un paso aparte porque aprobar sin emitir
     * dejaria a la persona con un tramite resuelto y sin documento, esperando
     * a que alguien se acuerde de generarlo.
     *
     * <p>Tras emitir, el tramite avanza solo a CERTIFICADO_EMITIDO: el
     * documento ya existe y esa es su situacion real.
     */
    @Transactional
    public Solicitud aprobar(Long solicitudId, String observacion) {
        Solicitud aprobada = cambiarEstatus(
                solicitudId, EstatusSolicitudEnum.APROBADA, null, observacion);

        emisorCertificado.emitir(solicitudId);

        Solicitud conCertificado = cambiarEstatus(
                solicitudId, EstatusSolicitudEnum.CERTIFICADO_EMITIDO,
                null, "Certificado emitido");

        notificador.avisar(solicitudId, "SOLICITUD_APROBADA");

        return conCertificado;
    }

    /**
     * Devuelve el tramite para que la persona corrija.
     *
     * <p>El aviso lleva el motivo: sin el, la persona no sabria que corregir.
     */
    @Transactional
    public Solicitud observar(Long solicitudId, Integer motivoId, String observacion) {
        Solicitud observada = cambiarEstatus(
                solicitudId, EstatusSolicitudEnum.OBSERVADA, motivoId, observacion);
        notificador.avisar(solicitudId, "SOLICITUD_OBSERVADA");
        return observada;
    }

    /** Rechaza el tramite de forma definitiva. */
    @Transactional
    public Solicitud rechazar(Long solicitudId, Integer motivoId, String observacion) {
        Solicitud rechazada = cambiarEstatus(
                solicitudId, EstatusSolicitudEnum.RECHAZADA, motivoId, observacion);
        notificador.avisar(solicitudId, "SOLICITUD_RECHAZADA");
        return rechazada;
    }

    /**
     * Registra la entrega de la documentacion.
     *
     * <p>Es el ultimo paso del tramite: la persona vino por su certificado y su
     * credencial, y se los llevo. A partir de aqui el expediente esta cerrado.
     */
    @Transactional
    public Solicitud registrarEntrega(Long solicitudId, String observacion) {
        return cambiarEstatus(solicitudId, EstatusSolicitudEnum.ENTREGADA, null,
                observacion == null || observacion.isBlank()
                        ? "Documentacion entregada a la persona solicitante"
                        : observacion);
    }

    /** Cancela el tramite. */
    @Transactional
    public Solicitud cancelar(Long solicitudId, Integer motivoId, String observacion) {
        return cambiarEstatus(solicitudId, EstatusSolicitudEnum.CANCELADA, motivoId, observacion);
    }

    /**
     * Da de baja un tramite y, si procede, a la persona y el animal.
     *
     * <p>Es para depurar el padron: gente que registro por probar el sistema, o
     * registros con datos evidentemente falsos. La baja es logica, nunca
     * fisica: el registro se conserva con quien lo dio de baja y por que, para
     * poder responder si alguien reclama.
     *
     * <p>No procede sobre un tramite que llego a certificado. Ese documento
     * circula: se fotografia, se presenta en la veterinaria. Desaparecer a su
     * titular dejaria un certificado sin respaldo. Para esos casos se cancela
     * el certificado, que es una operacion distinta y deja constancia.
     *
     * @param solicitudId     tramite a dar de baja
     * @param motivo          por que se depura
     * @param incluirPersona  tambien da de baja al tutor y su animal
     */
    @Transactional
    public void darDeBaja(Long solicitudId, String motivo, boolean incluirPersona) {
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaNegocioException(
                    "Escribe por que se da de baja este registro. Queda en la "
                            + "bitácora por si después hay que explicarlo.");
        }

        Solicitud solicitud = obtener(solicitudId);
        String estatus = solicitud.getEstatus().getClave();

        if (EstatusSolicitudEnum.CERTIFICADO_EMITIDO.name().equals(estatus)
                || EstatusSolicitudEnum.ENTREGADA.name().equals(estatus)) {
            throw new ReglaNegocioException(
                    "No se puede dar de baja un trámite con certificado emitido: ese "
                            + "documento ya circula. Si el certificado no debe surtir "
                            + "efecto, cancélalo desde la ficha del trámite.");
        }

        solicitud.setActivo(false);
        solicitudRepository.save(solicitud);

        auditoria.registrar(AccionAuditoria.ELIMINAR, "solicitud", solicitudId,
                "Baja del trámite %s: %s".formatted(
                        solicitud.getFolioSolicitud(), motivo));

        if (!incluirPersona) {
            log.info("Tramite {} dado de baja; se conservan la persona y el animal",
                    solicitud.getFolioSolicitud());
            return;
        }

        // Solo se da de baja al animal de este tramite.
        var animal = solicitud.getAnimal();
        animal.setActivo(false);
        animalRepository.save(animal);

        auditoria.registrar(AccionAuditoria.ELIMINAR, "animal", animal.getId(),
                "Baja de %s junto con el trámite %s: %s".formatted(
                        animal.getNombre(), solicitud.getFolioSolicitud(), motivo));

        // La titularidad se cierra: ya no hay quien responda por el animal.
        titularidadRepository.findByAnimalIdAndVigenteTrue(animal.getId())
                .ifPresent(titularidad -> {
                    titularidad.cerrar("Baja del registro: " + motivo);
                    titularidadRepository.save(titularidad);
                });

        // La persona solo se da de baja si este era su unico tramite vigente.
        // Alguien puede haber registrado un animal por probar y otro en serio.
        var persona = solicitud.getPersona();
        long otrosTramites = solicitudRepository.countByPersonaIdAndActivoTrue(persona.getId());

        if (otrosTramites == 0) {
            persona.setActivo(false);
            personaRepository.save(persona);

            auditoria.registrar(AccionAuditoria.ELIMINAR, "persona", persona.getId(),
                    "Baja de %s al no quedarle trámites vigentes: %s".formatted(
                            persona.getNombreCompleto(), motivo));

            log.info("Tramite {}, su animal y la persona dados de baja",
                    solicitud.getFolioSolicitud());
        } else {
            log.info("Tramite {} y su animal dados de baja; la persona conserva "
                            + "{} trámite(s) vigente(s)",
                    solicitud.getFolioSolicitud(), otrosTramites);
        }
    }

    /**
     * Reactiva un registro dado de baja por error.
     *
     * <p>La baja es logica precisamente para que se pueda deshacer.
     */
    @Transactional
    public Solicitud reactivar(Long solicitudId, String motivo) {
        Solicitud solicitud = solicitudRepository.findConDetallePorId(solicitudId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud", solicitudId));

        if (Boolean.TRUE.equals(solicitud.getActivo())) {
            throw new ReglaNegocioException("Este trámite no estaba dado de baja");
        }

        solicitud.setActivo(true);
        solicitudRepository.save(solicitud);

        var animal = solicitud.getAnimal();
        animal.setActivo(true);
        animalRepository.save(animal);

        var persona = solicitud.getPersona();
        persona.setActivo(true);
        personaRepository.save(persona);

        auditoria.registrar(AccionAuditoria.MODIFICAR, "solicitud", solicitudId,
                "Reactivacion del trámite %s: %s".formatted(
                        solicitud.getFolioSolicitud(),
                        motivo == null || motivo.isBlank() ? "sin motivo indicado" : motivo));

        log.info("Tramite {} reactivado", solicitud.getFolioSolicitud());
        return solicitudRepository.findConDetallePorId(solicitudId).orElse(solicitud);
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    /**
     * Tramite con el tutor y el animal ya cargados.
     *
     * <p>Se traen las relaciones a proposito: quien reciba esta entidad la va a
     * convertir a DTO fuera de la transaccion.
     */
    @Transactional(readOnly = true)
    public Solicitud obtener(Long id) {
        return solicitudRepository.findConDetallePorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud", id));
    }

    /**
     * Consulta publica de seguimiento.
     *
     * <p>Exige folio Y CURP. Nunca solo la CURP: quien conociera la de un
     * tercero podria ver sus datos, y no hay autenticacion que lo impida.
     */
    @Transactional(readOnly = true)
    public Solicitud consultarPublica(String folio, String curp) {
        String curpNormalizada = CurpUtil.normalizar(curp);

        Solicitud solicitud = solicitudRepository
                .consultarPublica(folio.trim(), curpNormalizada)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        MessageConstants.SOLICITUD_NO_ENCONTRADA));

        auditoria.registrarConsultaCurp(curpNormalizada,
                solicitud.getPersona().getId(), true);

        return solicitudRepository.findConDetallePorId(solicitud.getId()).orElse(solicitud);
    }

    /** Acceso por la liga de correccion que se envia al observar un tramite. */
    @Transactional(readOnly = true)
    public Solicitud consultarPorToken(UUID token) {
        Solicitud solicitud = solicitudRepository.findByTokenConsulta(token)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        MessageConstants.SOLICITUD_NO_ENCONTRADA));
        return solicitudRepository.findConDetallePorId(solicitud.getId()).orElse(solicitud);
    }

    /** Expediente completo del tramite. */
    @Transactional(readOnly = true)
    public List<SolicitudHistorial> obtenerHistorial(Long solicitudId) {
        return historialRepository.findBySolicitudIdOrderByFechaDesc(solicitudId);
    }

    // ------------------------------------------------------------------
    // Apoyo
    // ------------------------------------------------------------------

    /** Escribe un renglon del expediente. Toda transicion pasa por aqui. */
    private void registrarHistorial(Solicitud solicitud, EstatusSolicitud anterior,
                                    EstatusSolicitud nuevo, MotivoRechazo motivo,
                                    String observacion, Usuario usuario) {
        SolicitudHistorial renglon = new SolicitudHistorial();
        renglon.setSolicitud(solicitud);
        renglon.setEstatusAnterior(anterior);
        renglon.setEstatusNuevo(nuevo);
        renglon.setMotivoRechazo(motivo);
        renglon.setObservacion(observacion);
        renglon.setUsuario(usuario);
        renglon.setFecha(OffsetDateTime.now());
        historialRepository.save(renglon);
    }

    private EstatusSolicitud obtenerEstatus(EstatusSolicitudEnum estatus) {
        return estatusRepository.findByClave(estatus.name())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Estatus de solicitud", estatus.name()));
    }

    /** Usuario en sesion; nulo cuando la accion viene del portal publico. */
    private Usuario usuarioEnSesion() {
        return SecurityUtil.obtenerUsuarioId()
                .flatMap(usuarioRepository::findById)
                .orElse(null);
    }

    private String accionDe(EstatusSolicitudEnum destino) {
        return switch (destino) {
            case APROBADA -> AccionAuditoria.APROBAR;
            case OBSERVADA -> AccionAuditoria.OBSERVAR;
            case RECHAZADA -> AccionAuditoria.RECHAZAR;
            case CANCELADA -> AccionAuditoria.CANCELAR;
            default -> AccionAuditoria.MODIFICAR;
        };
    }
}
