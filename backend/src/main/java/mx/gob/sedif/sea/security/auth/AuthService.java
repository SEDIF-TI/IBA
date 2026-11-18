package mx.gob.sedif.sea.security.auth;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.gob.sedif.sea.core.auditoria.AccionAuditoria;
import mx.gob.sedif.sea.core.auditoria.AuditoriaService;
import mx.gob.sedif.sea.exception.MessageConstants;
import mx.gob.sedif.sea.exception.ReglaNegocioException;
import mx.gob.sedif.sea.security.JwtTokenProvider;
import mx.gob.sedif.sea.security.Usuario;
import mx.gob.sedif.sea.security.UsuarioAutenticado;
import mx.gob.sedif.sea.security.UsuarioRepository;
import mx.gob.sedif.sea.security.sesion.SesionService;

/**
 * Inicio de sesión, renovación y cierre de sesión, y cambio de contraseña.
 * Área: Núcleo compartido (seguridad). Lógica: ciclo de vida de la sesión del personal.
 *
 * <p>Las credenciales se comprueban aquí y no delegando en el gestor de autenticación de Spring,
 * porque hace falta llevar la cuenta de intentos fallidos, aplicar el bloqueo temporal por cuenta y
 * dejar cada movimiento en la bitácora. Colabora con {@link SesionService} (sesiones revocables) e
 * {@link IntentosAccesoService} (contador en transacción propia).
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private static final String ENTIDAD = "usuario";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final IntentosAccesoService intentosAccesoService;
    private final SesionService sesionService;
    private final AuditoriaService auditoria;

    /**
     * Hash de una contraseña aleatoria que nadie conoce. Se compara contra él cuando el usuario no
     * existe, para que la respuesta tarde lo mismo que con un usuario real: si no, el tiempo de
     * respuesta revelaría qué cuentas existen.
     */
    private final String hashFicticio;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       IntentosAccesoService intentosAccesoService,
                       SesionService sesionService,
                       AuditoriaService auditoria) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.intentosAccesoService = intentosAccesoService;
        this.sesionService = sesionService;
        this.auditoria = auditoria;
        this.hashFicticio = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    /**
     * Verifica las credenciales, abre una sesión y emite los tokens.
     *
     * <p>Reglas: mismo mensaje para usuario inexistente, contraseña incorrecta y cuenta dada de
     * baja; bloqueo temporal tras el máximo de intentos; cada intento, correcto o no, queda en la
     * bitácora con su número de intento y su motivo (nunca la contraseña ni el texto escrito).
     *
     * @param peticion  usuario y contraseña
     * @param ip        dirección del cliente, para la ficha de la sesión
     * @param userAgent navegador del cliente; puede ser nulo
     * @return token de refresco (para la cookie) y respuesta con el token de acceso
     * @throws BadCredentialsException si las credenciales no son correctas o la cuenta está de baja
     * @throws LockedException         si la cuenta está bloqueada por intentos fallidos
     */
    @Transactional
    public ResultadoAutenticacion autenticar(LoginRequest peticion, String ip, String userAgent) {
        Optional<Usuario> encontrado = usuarioRepository.findByUsuarioIgnoreCase(peticion.usuario().trim());

        if (encontrado.isEmpty()) {
            passwordEncoder.matches(peticion.password(), hashFicticio);
            // No se registra el texto escrito: a veces la gente teclea la contraseña en el usuario.
            auditoria.registrarComo(null, AccionAuditoria.LOGIN_FALLIDO, ENTIDAD, null,
                    "Intento de acceso con un usuario que no existe");
            throw new BadCredentialsException(MessageConstants.CREDENCIALES_INVALIDAS);
        }

        Usuario usuario = encontrado.get();

        if (usuario.estaBloqueado()) {
            auditoria.registrarComo(usuario.getId(), AccionAuditoria.LOGIN_FALLIDO, ENTIDAD,
                    usuario.getId(), "Intento de acceso a una cuenta bloqueada temporalmente");
            throw new LockedException(MessageConstants.CUENTA_BLOQUEADA);
        }

        boolean coincide = passwordEncoder.matches(peticion.password(), usuario.getPasswordHash());

        if (Boolean.FALSE.equals(usuario.getActivo())) {
            auditoria.registrarComo(usuario.getId(), AccionAuditoria.LOGIN_FALLIDO, ENTIDAD,
                    usuario.getId(), "Intento de acceso a una cuenta dada de baja");
            throw new BadCredentialsException(MessageConstants.CREDENCIALES_INVALIDAS);
        }

        if (!coincide) {
            // En transacción aparte: la excepción que sigue revierte esta, y el incremento debe
            // sobrevivir a ese descarte.
            IntentosAccesoService.ResultadoFallo fallo =
                    intentosAccesoService.registrarFallo(usuario.getId());
            auditoria.registrarComo(usuario.getId(), AccionAuditoria.LOGIN_FALLIDO, ENTIDAD,
                    usuario.getId(), "Contraseña incorrecta (intento %d de %d)"
                            .formatted(fallo.intento(), fallo.maximo()));
            if (fallo.bloqueada()) {
                auditoria.registrarComo(usuario.getId(), AccionAuditoria.CUENTA_BLOQUEADA, ENTIDAD,
                        usuario.getId(), "Cuenta bloqueada %d minutos tras %d intentos fallidos"
                                .formatted(fallo.minutosBloqueo(), fallo.intento()));
            }
            throw new BadCredentialsException(MessageConstants.CREDENCIALES_INVALIDAS);
        }

        intentosAccesoService.registrarAccesoCorrecto(usuario.getId());

        ResultadoAutenticacion resultado = abrirSesion(usuario, ip, userAgent);
        auditoria.registrarComo(usuario.getId(), AccionAuditoria.LOGIN, ENTIDAD, usuario.getId(),
                "Inicio de sesión");
        log.info("Acceso correcto de '{}'", usuario.getUsuario());
        return resultado;
    }

    /**
     * Renueva la sesión: valida el token de refresco, lo rota y emite un token de acceso nuevo.
     *
     * <p>Los roles se releen de la base, así que retirar un permiso surte efecto en la siguiente
     * renovación. Si el token ya se había usado, {@link SesionService#rotar} revoca la sesión; esa
     * revocación se conserva aunque aquí se lance la excepción.
     *
     * @param tokenRefresco valor de la cookie; puede ser nulo
     * @return token de refresco nuevo (para la cookie) y respuesta con el token de acceso
     * @throws BadCredentialsException si no hay token, no sirve, su sesión está cerrada o la cuenta
     *                                 ya no puede entrar
     */
    @Transactional(noRollbackFor = BadCredentialsException.class)
    public ResultadoAutenticacion refrescar(String tokenRefresco) {
        JwtTokenProvider.DatosToken datos = Optional.ofNullable(tokenRefresco)
                .flatMap(tokenProvider::leer)
                .filter(JwtTokenProvider.DatosToken::esRefresco)
                .filter(d -> d.sesion() != null && d.jti() != null)
                .orElseThrow(() -> new BadCredentialsException(MessageConstants.SESION_EXPIRADA));

        SesionService.Rotacion rotacion = sesionService.rotar(datos.sesion(), datos.jti());
        Usuario usuario = rotacion.sesion().getUsuario();

        if (Boolean.FALSE.equals(usuario.getActivo()) || usuario.estaBloqueado()) {
            throw new BadCredentialsException(MessageConstants.CUENTA_INACTIVA);
        }
        if (!Objects.equals(datos.version(), usuario.getTokenVersion())) {
            throw new BadCredentialsException(MessageConstants.SESION_EXPIRADA);
        }

        UsuarioAutenticado principal = construirPrincipal(usuario);
        UUID sesion = rotacion.sesion().getIdentificador();
        return new ResultadoAutenticacion(
                tokenProvider.generarTokenRefresco(principal, sesion, rotacion.nuevoJti()),
                armarRespuesta(usuario, principal, sesion));
    }

    /**
     * Cierra la sesión del dispositivo dueño de la cookie y lo deja en la bitácora.
     *
     * <p>Si la cookie no existe o no sirve no hay nada que cerrar y no se considera error: la
     * persona igual espera salir.
     *
     * @param tokenRefresco valor de la cookie; puede ser nulo
     */
    @Transactional
    public void cerrarSesion(String tokenRefresco) {
        Optional.ofNullable(tokenRefresco)
                .flatMap(tokenProvider::leer)
                .filter(JwtTokenProvider.DatosToken::esRefresco)
                .filter(d -> d.sesion() != null)
                .map(d -> sesionService.revocar(d.sesion(), SesionService.MOTIVO_CIERRE))
                .ifPresent(usuario -> auditoria.registrarComo(usuario.getId(),
                        AccionAuditoria.LOGOUT, ENTIDAD, usuario.getId(), "Cierre de sesión"));
    }

    /**
     * Cambia la contraseña de la persona en sesión.
     *
     * <p>Reglas: exige la contraseña actual y que la nueva sea distinta; levanta la marca de cambio
     * obligatorio; cierra todas las sesiones de la cuenta (en cualquier dispositivo) y abre una nueva
     * para el dispositivo desde el que se hizo el cambio, para que la persona siga trabajando.
     *
     * @param usuarioId identificador de la cuenta en sesión
     * @param peticion  contraseña actual y nueva (la política se valida en el {@code Request})
     * @param ip        dirección del cliente
     * @param userAgent navegador del cliente; puede ser nulo
     * @return tokens de la sesión nueva
     * @throws BadCredentialsException si la contraseña actual no es correcta
     * @throws ReglaNegocioException   si la nueva es igual a la actual
     */
    @Transactional
    public ResultadoAutenticacion cambiarPassword(Long usuarioId, CambiarPasswordRequest peticion,
                                                  String ip, String userAgent) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new BadCredentialsException(
                        MessageConstants.CREDENCIALES_INVALIDAS));

        if (!passwordEncoder.matches(peticion.passwordActual(), usuario.getPasswordHash())) {
            throw new BadCredentialsException(MessageConstants.CREDENCIALES_INVALIDAS);
        }

        if (passwordEncoder.matches(peticion.passwordNueva(), usuario.getPasswordHash())) {
            throw new ReglaNegocioException("La contraseña nueva debe ser distinta de la actual");
        }

        usuario.setPasswordHash(passwordEncoder.encode(peticion.passwordNueva()));
        usuario.setCambioPasswordRequerido(false);
        int cerradas = sesionService.revocarTodas(usuario, SesionService.MOTIVO_CAMBIO_PASSWORD);

        auditoria.registrarComo(usuario.getId(), AccionAuditoria.CAMBIO_PASSWORD, ENTIDAD,
                usuario.getId(), "La persona cambió su contraseña; se cerraron %d sesión(es)"
                        .formatted(cerradas));
        log.info("Contraseña cambiada por '{}'", usuario.getUsuario());

        return abrirSesion(usuario, ip, userAgent);
    }

    /** Abre la sesión en base y emite el par de tokens para ella. */
    private ResultadoAutenticacion abrirSesion(Usuario usuario, String ip, String userAgent) {
        SesionService.SesionAbierta sesion = sesionService.abrir(usuario, ip, userAgent);
        UsuarioAutenticado principal = construirPrincipal(usuario);
        return new ResultadoAutenticacion(
                tokenProvider.generarTokenRefresco(principal, sesion.identificador(), sesion.jti()),
                armarRespuesta(usuario, principal, sesion.identificador()));
    }

    /** Arma el principal con los roles y los permisos por separado. */
    private UsuarioAutenticado construirPrincipal(Usuario usuario) {
        Set<String> roles = new HashSet<>();
        Set<String> permisos = new HashSet<>();
        usuario.getRoles().forEach(rol -> {
            roles.add(rol.getClave());
            rol.getPermisos().forEach(permiso -> permisos.add(permiso.getClave()));
        });

        return new UsuarioAutenticado(
                usuario.getId(),
                usuario.getUsuario(),
                null,
                Boolean.TRUE.equals(usuario.getActivo()),
                Boolean.TRUE.equals(usuario.getCambioPasswordRequerido()),
                !usuario.estaBloqueado(),
                usuario.getTokenVersion() == null ? 0 : usuario.getTokenVersion(),
                roles,
                permisos);
    }

    /** Construye la respuesta de sesión, sin incluir el token de refresco. */
    private JwtResponse armarRespuesta(Usuario usuario, UsuarioAutenticado principal, UUID sesion) {
        List<String> roles = usuario.getRoles().stream()
                .map(rol -> rol.getClave())
                .toList();
        List<String> permisos = usuario.getRoles().stream()
                .flatMap(rol -> rol.getPermisos().stream())
                .map(permiso -> permiso.getClave())
                .distinct()
                .toList();

        return JwtResponse.de(
                tokenProvider.generarTokenAcceso(principal, sesion),
                tokenProvider.getExpiracionAcceso(),
                usuario.getUsuario(),
                usuario.getNombreCompleto(),
                roles,
                permisos,
                Boolean.TRUE.equals(usuario.getCambioPasswordRequerido()));
    }

    /**
     * Resultado de abrir o renovar una sesión.
     *
     * @param tokenRefresco token de refresco, que el controlador pone en la cookie HttpOnly
     * @param respuesta     cuerpo de la respuesta, con el token de acceso
     */
    public record ResultadoAutenticacion(String tokenRefresco, JwtResponse respuesta) {
    }
}
