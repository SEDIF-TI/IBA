package mx.gob.sedif.sea.security.auth;

import java.time.OffsetDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.gob.sedif.sea.exception.MessageConstants;
import mx.gob.sedif.sea.exception.ReglaNegocioException;
import mx.gob.sedif.sea.security.JwtTokenProvider;
import mx.gob.sedif.sea.security.Usuario;
import mx.gob.sedif.sea.security.UsuarioAutenticado;
import mx.gob.sedif.sea.security.UsuarioRepository;

/**
 * Inicio de sesion, refresco de token y cambio de contrasena.
 *
 * <p>Las credenciales se comprueban aqui y no delegando en el gestor de
 * autenticacion de Spring, porque hace falta llevar la cuenta de intentos
 * fallidos y aplicar el bloqueo temporal por cuenta.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final IntentosAccesoService intentosAccesoService;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       IntentosAccesoService intentosAccesoService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.intentosAccesoService = intentosAccesoService;
    }

    /**
     * Verifica las credenciales y emite los tokens.
     *
     * @param peticion usuario y contrasena
     * @return datos de sesion y token de acceso
     * @throws BadCredentialsException si las credenciales no son correctas
     * @throws LockedException         si la cuenta esta bloqueada
     */
    @Transactional
    public ResultadoAutenticacion autenticar(LoginRequest peticion) {
        Usuario usuario = usuarioRepository.findByUsuarioIgnoreCase(peticion.usuario())
                // Mismo error que para contrasena incorrecta: distinguirlos
                // permitiria averiguar que cuentas existen.
                .orElseThrow(() -> new BadCredentialsException(
                        MessageConstants.CREDENCIALES_INVALIDAS));

        if (usuario.estaBloqueado()) {
            log.warn("Intento de acceso a la cuenta bloqueada '{}'", usuario.getUsuario());
            throw new LockedException(MessageConstants.CUENTA_BLOQUEADA);
        }

        if (Boolean.FALSE.equals(usuario.getActivo())) {
            log.warn("Intento de acceso a la cuenta dada de baja '{}'", usuario.getUsuario());
            throw new BadCredentialsException(MessageConstants.CREDENCIALES_INVALIDAS);
        }

        if (!passwordEncoder.matches(peticion.password(), usuario.getPasswordHash())) {
            // En transaccion aparte: la excepcion que sigue revierte esta, y el
            // incremento debe sobrevivir a ese descarte.
            intentosAccesoService.registrarFallo(usuario.getId());
            throw new BadCredentialsException(MessageConstants.CREDENCIALES_INVALIDAS);
        }

        intentosAccesoService.registrarAccesoCorrecto(usuario.getId());

        UsuarioAutenticado principal = construirPrincipal(usuario);

        log.info("Acceso correcto de '{}'", usuario.getUsuario());

        return new ResultadoAutenticacion(
                tokenProvider.generarTokenAcceso(principal),
                tokenProvider.generarTokenRefresco(principal),
                armarRespuesta(usuario, principal));
    }

    /**
     * Emite un token de acceso nuevo a partir de uno de refresco.
     *
     * <p>Los roles se releen de la base a proposito: asi retirar un permiso
     * surte efecto en el siguiente refresco y no al cabo de siete dias.
     */
    @Transactional(readOnly = true)
    public JwtResponse refrescar(String tokenRefresco) {
        if (tokenRefresco == null || !tokenProvider.esValido(tokenRefresco)) {
            throw new BadCredentialsException(MessageConstants.TOKEN_INVALIDO);
        }
        if (!tokenProvider.esTokenRefresco(tokenRefresco)) {
            // Un token de acceso no sirve para refrescar.
            throw new BadCredentialsException(MessageConstants.TOKEN_INVALIDO);
        }

        Usuario usuario = usuarioRepository
                .findByUsuarioIgnoreCase(tokenProvider.obtenerUsuario(tokenRefresco))
                .orElseThrow(() -> new BadCredentialsException(MessageConstants.TOKEN_INVALIDO));

        if (Boolean.FALSE.equals(usuario.getActivo()) || usuario.estaBloqueado()) {
            throw new BadCredentialsException(MessageConstants.CUENTA_INACTIVA);
        }

        UsuarioAutenticado principal = construirPrincipal(usuario);
        return armarRespuesta(usuario, principal);
    }

    /**
     * Cambia la contrasena de la persona en sesion.
     *
     * <p>Al hacerlo se levanta la marca que obliga al cambio, con lo que el
     * usuario inicial deja de estar en ese estado.
     */
    @Transactional
    public void cambiarPassword(Long usuarioId, CambiarPasswordRequest peticion) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new BadCredentialsException(
                        MessageConstants.CREDENCIALES_INVALIDAS));

        if (!passwordEncoder.matches(peticion.passwordActual(), usuario.getPasswordHash())) {
            throw new BadCredentialsException(MessageConstants.CREDENCIALES_INVALIDAS);
        }

        if (passwordEncoder.matches(peticion.passwordNueva(), usuario.getPasswordHash())) {
            throw new ReglaNegocioException("La contrasena nueva debe ser distinta de la actual");
        }

        usuario.setPasswordHash(passwordEncoder.encode(peticion.passwordNueva()));
        usuario.setCambioPasswordRequerido(false);
        usuarioRepository.save(usuario);

        log.info("Contrasena cambiada por '{}'", usuario.getUsuario());
    }

    /** Arma el principal con roles y permisos como autoridades. */
    private UsuarioAutenticado construirPrincipal(Usuario usuario) {
        java.util.Set<String> autoridades = new java.util.HashSet<>();
        usuario.getRoles().forEach(rol -> {
            autoridades.add(rol.getClave());
            rol.getPermisos().forEach(permiso -> autoridades.add(permiso.getClave()));
        });

        return new UsuarioAutenticado(
                usuario.getId(),
                usuario.getUsuario(),
                usuario.getPasswordHash(),
                Boolean.TRUE.equals(usuario.getActivo()),
                Boolean.TRUE.equals(usuario.getCambioPasswordRequerido()),
                !usuario.estaBloqueado(),
                autoridades);
    }

    /** Construye la respuesta de sesion, sin incluir el token de refresco. */
    private JwtResponse armarRespuesta(Usuario usuario, UsuarioAutenticado principal) {
        List<String> roles = usuario.getRoles().stream()
                .map(rol -> rol.getClave())
                .toList();
        List<String> permisos = usuario.getRoles().stream()
                .flatMap(rol -> rol.getPermisos().stream())
                .map(permiso -> permiso.getClave())
                .distinct()
                .toList();

        return JwtResponse.de(
                tokenProvider.generarTokenAcceso(principal),
                tokenProvider.getExpiracionAcceso(),
                usuario.getUsuario(),
                usuario.getNombreCompleto(),
                roles,
                permisos,
                Boolean.TRUE.equals(usuario.getCambioPasswordRequerido()));
    }

    /**
     * Resultado del inicio de sesion.
     *
     * <p>El token de refresco se devuelve aparte porque el controlador lo pone
     * en una cookie, no en el cuerpo de la respuesta.
     */
    public record ResultadoAutenticacion(
            String tokenAcceso,
            String tokenRefresco,
            JwtResponse respuesta) {
    }
}
