package mx.gob.sedif.sea.core.jornada;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.gob.sedif.sea.core.auditoria.AccionAuditoria;
import mx.gob.sedif.sea.core.auditoria.AuditoriaService;
import mx.gob.sedif.sea.core.catalogo.Municipio;
import mx.gob.sedif.sea.core.catalogo.MunicipioRepository;
import mx.gob.sedif.sea.core.solicitud.Solicitud;
import mx.gob.sedif.sea.core.solicitud.SolicitudRepository;
import mx.gob.sedif.sea.exception.MessageConstants;
import mx.gob.sedif.sea.exception.RecursoNoEncontradoException;
import mx.gob.sedif.sea.exception.ReglaNegocioException;
import mx.gob.sedif.sea.security.SecurityUtil;
import mx.gob.sedif.sea.security.Usuario;
import mx.gob.sedif.sea.security.UsuarioRepository;

/**
 * Jornadas municipales de empadronamiento.
 *
 * <p>El personal viaja al municipio. La jornada se publica por anticipado, la
 * ciudadania precarga sus registros desde el portal, y el dia del evento se
 * busca a cada persona por CURP o apellidos, se valida en sitio y se imprime
 * todo de una vez.
 *
 * <p>La busqueda del dia del evento es la operacion critica: se hace con la
 * gente formada enfrente, asi que tiene que responder rapido y tolerar que el
 * apellido se teclee sin acentos o con una errata.
 */
@Service
public class JornadaService {

    private static final Logger log = LoggerFactory.getLogger(JornadaService.class);

    private final JornadaRepository jornadaRepository;
    private final SolicitudRepository solicitudRepository;
    private final MunicipioRepository municipioRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoria;

    public JornadaService(JornadaRepository jornadaRepository,
                          SolicitudRepository solicitudRepository,
                          MunicipioRepository municipioRepository,
                          UsuarioRepository usuarioRepository,
                          AuditoriaService auditoria) {
        this.jornadaRepository = jornadaRepository;
        this.solicitudRepository = solicitudRepository;
        this.municipioRepository = municipioRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoria = auditoria;
    }

    /**
     * Da de alta una jornada.
     *
     * <p>Nace sin publicar: se prepara con calma y se publica cuando el area
     * confirma sede y fechas. Publicar por accidente una jornada a medio armar
     * mandaria a la gente a un lugar equivocado.
     */
    @Transactional
    public Jornada crear(JornadaRequest peticion) {
        validarFechas(peticion.fechaInicio(), peticion.fechaFin());

        Municipio municipio = municipioRepository.findById(peticion.municipioId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Municipio", peticion.municipioId()));

        Jornada jornada = new Jornada();
        aplicar(jornada, peticion, municipio);
        jornada.setPublicada(false);

        Jornada guardada = jornadaRepository.save(jornada);

        auditoria.registrar(AccionAuditoria.CREAR, "jornada", guardada.getId(),
                "Alta de la jornada '%s' en %s".formatted(
                        guardada.getNombre(), municipio.getNombre()));

        log.info("Jornada '{}' dada de alta en {}", guardada.getNombre(), municipio.getNombre());
        return guardada;
    }

    /** Modifica una jornada. */
    @Transactional
    public Jornada actualizar(Long id, JornadaRequest peticion) {
        validarFechas(peticion.fechaInicio(), peticion.fechaFin());

        Jornada jornada = obtener(id);
        Municipio municipio = municipioRepository.findById(peticion.municipioId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Municipio", peticion.municipioId()));

        aplicar(jornada, peticion, municipio);
        Jornada guardada = jornadaRepository.save(jornada);

        auditoria.registrar(AccionAuditoria.MODIFICAR, "jornada", id,
                "Cambios en la jornada '%s'".formatted(guardada.getNombre()));

        return guardada;
    }

    /**
     * Publica la jornada en el portal.
     *
     * <p>A partir de aqui la ciudadania la ve y puede precargar sus registros.
     */
    @Transactional
    public Jornada publicar(Long id) {
        Jornada jornada = obtener(id);

        if (Boolean.TRUE.equals(jornada.getPublicada())) {
            throw new ReglaNegocioException(
                    "La jornada '%s' ya esta publicada en el portal"
                            .formatted(jornada.getNombre()));
        }

        if (jornada.getFechaFin().isBefore(LocalDate.now())) {
            throw new ReglaNegocioException(
                    "No se puede publicar una jornada que ya termino. "
                            + "Ajusta las fechas antes de publicarla.");
        }

        jornada.publicar();
        Jornada guardada = jornadaRepository.save(jornada);

        auditoria.registrar(AccionAuditoria.MODIFICAR, "jornada", id,
                "Jornada '%s' publicada en el portal".formatted(jornada.getNombre()));

        log.info("Jornada '{}' publicada", jornada.getNombre());
        return guardada;
    }

    /**
     * Retira la jornada del portal.
     *
     * <p>Las precargas que ya se hicieron se conservan: la gente registro sus
     * datos de buena fe y esos tramites siguen siendo validos.
     */
    @Transactional
    public Jornada despublicar(Long id, String motivo) {
        Jornada jornada = obtener(id);

        if (Boolean.FALSE.equals(jornada.getPublicada())) {
            throw new ReglaNegocioException("La jornada no estaba publicada");
        }

        long precargas = solicitudRepository.countByJornadaId(id);

        jornada.setPublicada(false);
        Jornada guardada = jornadaRepository.save(jornada);

        auditoria.registrar(AccionAuditoria.MODIFICAR, "jornada", id,
                "Jornada '%s' retirada del portal: %s".formatted(
                        jornada.getNombre(), motivo));

        log.info("Jornada '{}' retirada del portal; conserva {} precargas",
                jornada.getNombre(), precargas);

        return guardada;
    }

    /**
     * Busca a una persona el dia del evento.
     *
     * <p>Es la operacion que se usa con la gente formada enfrente: se teclea la
     * CURP o el apellido y aparecen sus precargas. Tolera que se escriba sin
     * acentos, porque nadie va a poner la enie de "Munoz" con prisa.
     *
     * @param jornadaId jornada en curso
     * @param texto     CURP, apellidos o nombre
     */
    @Transactional(readOnly = true)
    public Page<Solicitud> buscarEnJornada(Long jornadaId, String texto, Pageable pageable) {
        // Se comprueba que exista para poder decirlo con claridad si no.
        obtener(jornadaId);

        String filtro = (texto == null || texto.isBlank()) ? null : texto.trim().toUpperCase();

        // Buscar por CURP es una consulta de datos personales y queda en bitacora.
        if (filtro != null && filtro.length() == 18) {
            auditoria.registrarConsultaCurp(filtro, null, true);
        }

        return solicitudRepository.buscarEnJornada(jornadaId, filtro, pageable);
    }

    /** Jornadas publicadas y vigentes, para el calendario del portal. */
    @Transactional(readOnly = true)
    public List<Jornada> publicadasVigentes() {
        return jornadaRepository.findPublicadasVigentes(LocalDate.now());
    }

    /** Jornadas que se estan celebrando hoy. */
    @Transactional(readOnly = true)
    public List<Jornada> enCurso() {
        return jornadaRepository.findEnCurso(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<Jornada> todas() {
        return jornadaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Jornada obtener(Long id) {
        return jornadaRepository.findConDetallePorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Jornada", id));
    }

    /**
     * Resumen de avance de una jornada.
     *
     * <p>Es lo que el responsable consulta durante el evento para saber como va.
     */
    @Transactional(readOnly = true)
    public ResumenJornada resumen(Long jornadaId) {
        Jornada jornada = obtener(jornadaId);

        long precargadas = solicitudRepository.countByJornadaId(jornadaId);
        long atendidas = solicitudRepository.countByJornadaIdAndEstatusClaveIn(
                jornadaId, List.of("APROBADA", "CERTIFICADO_EMITIDO", "ENTREGADA"));
        long pendientes = precargadas - atendidas;

        Integer cupo = jornada.getCupo();
        Integer lugaresLibres = cupo == null ? null : Math.max(0, cupo - (int) precargadas);

        return new ResumenJornada(
                jornada.getId(), jornada.getNombre(),
                jornada.getMunicipio().getNombre(),
                precargadas, atendidas, pendientes, cupo, lugaresLibres);
    }

    /** Copia los datos editables a la jornada. */
    private void aplicar(Jornada jornada, JornadaRequest peticion, Municipio municipio) {
        jornada.setNombre(peticion.nombre().trim());
        jornada.setDescripcion(peticion.descripcion());
        jornada.setMunicipio(municipio);
        jornada.setDomicilioSede(peticion.domicilioSede().trim());
        jornada.setFechaInicio(peticion.fechaInicio());
        jornada.setFechaFin(peticion.fechaFin());
        jornada.setHorario(peticion.horario());
        jornada.setCupo(peticion.cupo());

        if (peticion.usuarioResponsableId() != null) {
            jornada.setUsuarioResponsable(
                    usuarioRepository.findById(peticion.usuarioResponsableId())
                            .orElseThrow(() -> new RecursoNoEncontradoException(
                                    "Usuario responsable", peticion.usuarioResponsableId())));
        }
    }

    /** Comprueba que el periodo de la jornada tenga sentido. */
    private void validarFechas(LocalDate inicio, LocalDate fin) {
        if (fin.isBefore(inicio)) {
            throw new ReglaNegocioException(
                    "La fecha de termino no puede ser anterior a la de inicio. "
                            + "Revisa las dos fechas de la jornada.");
        }
    }

    /**
     * Avance de una jornada.
     *
     * @param jornadaId     identificador
     * @param nombre        nombre de la jornada
     * @param municipio     donde se celebra
     * @param precargadas   registros que la ciudadania adelanto
     * @param atendidas     tramites ya resueltos
     * @param pendientes    tramites por atender
     * @param cupo          limite de atencion; nulo si no lo tiene
     * @param lugaresLibres cuantos quedan; nulo si no hay cupo
     */
    public record ResumenJornada(
            Long jornadaId,
            String nombre,
            String municipio,
            long precargadas,
            long atendidas,
            long pendientes,
            Integer cupo,
            Integer lugaresLibres) {
    }
}
