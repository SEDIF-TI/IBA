package mx.gob.sedif.sea.security.sesion;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.gob.sedif.sea.core.auditoria.AccionAuditoria;
import mx.gob.sedif.sea.core.auditoria.AuditoriaService;
import mx.gob.sedif.sea.exception.MessageConstants;
import mx.gob.sedif.sea.security.Usuario;
import mx.gob.sedif.sea.security.UsuarioRepository;

/**
 * Apertura, rotación y revocación de las sesiones del personal.
 * Área: Núcleo compartido (seguridad). Lógica: ciclo de vida del token de refresco.
 *
 * <p>Colabora con {@code AuthService} (inicio, renovación y cierre de sesión) y con
 * {@code UsuarioAdminService} (reinicio de contraseña y baja de cuentas), que son los que deciden
 * cuándo hay que cerrar las sesiones de una persona.
 */
@Service
public class SesionService {

    private static final Logger log = LoggerFactory.getLogger(SesionService.class);

    /** Motivos de revocación que se guardan en {@code sesion_usuario.s_motivo_revocacion}. */
    public static final String MOTIVO_CIERRE = "CIERRE_SESION";
    public static final String MOTIVO_CAMBIO_PASSWORD = "CAMBIO_PASSWORD";
    public static final String MOTIVO_REINICIO_PASSWORD = "REINICIO_PASSWORD";
    public static final String MOTIVO_BAJA = "BAJA_CUENTA";
    public static final String MOTIVO_REUTILIZACION = "REUTILIZACION_TOKEN";

    /** Días que se conservan las filas vencidas antes de limpiarlas. */
    private static final int DIAS_CONSERVACION = 30;

    private final SesionUsuarioRepository sesionRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoria;
    private final long vigenciaRefrescoMs;

    /**
     * @param vigenciaRefrescoMs vigencia del token de refresco en milisegundos
     *                           ({@code app.jwt.refresh-expiration}); la sesión vence a la par
     */
    public SesionService(SesionUsuarioRepository sesionRepository,
                         UsuarioRepository usuarioRepository,
                         AuditoriaService auditoria,
                         @Value("${app.jwt.refresh-expiration}") long vigenciaRefrescoMs) {
        this.sesionRepository = sesionRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoria = auditoria;
        this.vigenciaRefrescoMs = vigenciaRefrescoMs;
    }

    /**
     * Abre una sesión nueva para un inicio de sesión correcto.
     *
     * @param usuario   cuenta que acaba de autenticarse
     * @param ip        dirección del cliente, para la ficha de la sesión
     * @param userAgent navegador del cliente, recortado a 400 caracteres; puede ser nulo
     * @return identificador de la sesión y el identificador de un solo uso que irá en el token
     */
    @Transactional
    public SesionAbierta abrir(Usuario usuario, String ip, String userAgent) {
        String jti = UUID.randomUUID().toString();
        OffsetDateTime ahora = OffsetDateTime.now();

        SesionUsuario sesion = new SesionUsuario();
        sesion.setIdentificador(UUID.randomUUID());
        sesion.setUsuario(usuario);
        sesion.setHuellaRefresco(huella(jti));
        sesion.setFechaCreacion(ahora);
        sesion.setUltimoUso(ahora);
        sesion.setExpira(ahora.plusNanos(vigenciaRefrescoMs * 1_000_000));
        sesion.setIp(ip);
        sesion.setUserAgent(userAgent);
        sesionRepository.save(sesion);

        return new SesionAbierta(sesion.getIdentificador(), jti);
    }

    /**
     * Valida el token de refresco contra su sesión y emite un identificador de un solo uso nuevo.
     *
     * <p>Regla de rotación: cada token de refresco sirve una sola vez. Si llega uno cuya huella no
     * es la vigente, es una copia (lo usó otra persona antes, o se usa después de haberse renovado)
     * y la sesión completa se revoca. La revocación se conserva aunque se lance la excepción.
     *
     * @param identificador claim {@code sid} del token
     * @param jti           claim {@code jti} del token
     * @return la sesión con su usuario y el identificador nuevo
     * @throws BadCredentialsException si la sesión no existe, está cerrada o vencida, o el token ya se usó
     */
    @Transactional(noRollbackFor = BadCredentialsException.class)
    public Rotacion rotar(UUID identificador, String jti) {
        SesionUsuario sesion = sesionRepository.findByIdentificador(identificador)
                .orElseThrow(() -> new BadCredentialsException(MessageConstants.TOKEN_INVALIDO));

        if (!sesion.estaAbierta()) {
            throw new BadCredentialsException(MessageConstants.TOKEN_INVALIDO);
        }

        if (!MessageDigest.isEqual(
                huella(jti).getBytes(StandardCharsets.US_ASCII),
                sesion.getHuellaRefresco().getBytes(StandardCharsets.US_ASCII))) {
            sesion.revocar(MOTIVO_REUTILIZACION);
            sesionRepository.save(sesion);
            log.warn("Token de refresco reutilizado; se cerró la sesión {} de la cuenta {}",
                    sesion.getId(), sesion.getUsuario().getId());
            auditoria.registrarComo(sesion.getUsuario().getId(), AccionAuditoria.SESION_REVOCADA,
                    "usuario", sesion.getUsuario().getId(),
                    "Se cerró una sesión porque su token de renovación se usó dos veces "
                            + "(posible copia del token)");
            throw new BadCredentialsException(MessageConstants.TOKEN_INVALIDO);
        }

        String nuevo = UUID.randomUUID().toString();
        sesion.setHuellaRefresco(huella(nuevo));
        sesion.setUltimoUso(OffsetDateTime.now());
        sesionRepository.save(sesion);

        return new Rotacion(sesion, nuevo);
    }

    /**
     * Cierra una sesión concreta, por ejemplo al pulsar "Cerrar sesión".
     *
     * @param identificador claim {@code sid} del token de refresco
     * @param motivo        causa del cierre
     * @return la cuenta dueña de la sesión, o {@code null} si la sesión no existía
     */
    @Transactional
    public Usuario revocar(UUID identificador, String motivo) {
        return sesionRepository.findByIdentificador(identificador)
                .map(sesion -> {
                    sesion.revocar(motivo);
                    sesionRepository.save(sesion);
                    return sesion.getUsuario();
                })
                .orElse(null);
    }

    /**
     * Cierra todas las sesiones de una cuenta y sube su versión de tokens, con lo que también los
     * tokens de acceso ya emitidos dejan de servir en la siguiente petición.
     *
     * <p>Se usa al cambiar o reiniciar la contraseña y al dar de baja la cuenta.
     *
     * @param usuario cuenta afectada (entidad administrada por la transacción en curso)
     * @param motivo  causa del cierre
     * @return número de sesiones que estaban abiertas
     */
    @Transactional
    public int revocarTodas(Usuario usuario, String motivo) {
        List<SesionUsuario> abiertas = sesionRepository.abiertasDe(usuario.getId());
        abiertas.forEach(sesion -> sesion.revocar(motivo));
        sesionRepository.saveAll(abiertas);

        usuario.incrementarTokenVersion();
        usuarioRepository.save(usuario);
        return abiertas.size();
    }

    /** Limpia cada madrugada las sesiones vencidas hace más de 30 días. */
    @Scheduled(cron = "0 30 3 * * *", zone = "America/Mexico_City")
    @Transactional
    public void limpiarVencidas() {
        int borradas = sesionRepository.borrarVencidasAntesDe(
                OffsetDateTime.now().minusDays(DIAS_CONSERVACION));
        if (borradas > 0) {
            log.info("Sesiones vencidas eliminadas: {}", borradas);
        }
    }

    /** SHA-256 en hexadecimal; lo único que se guarda del identificador de un solo uso. */
    static String huella(String valor) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no está disponible en la JVM", ex);
        }
    }

    /**
     * Sesión recién abierta.
     *
     * @param identificador claim {@code sid} que llevará el token de refresco
     * @param jti           identificador de un solo uso que llevará el token de refresco
     */
    public record SesionAbierta(UUID identificador, String jti) {
    }

    /**
     * Resultado de una renovación correcta.
     *
     * @param sesion  sesión renovada, con su usuario cargado
     * @param nuevoJti identificador de un solo uso para el token de refresco nuevo
     */
    public record Rotacion(SesionUsuario sesion, String nuevoJti) {
    }
}
