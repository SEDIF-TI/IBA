package mx.gob.sedif.sea.security;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import mx.gob.sedif.sea.exception.MessageConstants;
import mx.gob.sedif.sea.security.sesion.SesionUsuarioRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Autentica cada petición a partir del token de acceso de la cabecera {@code Authorization}.
 * Área: Núcleo compartido (seguridad). Lógica: validación de la sesión en cada petición.
 *
 * <p>Reglas que aplica, en este orden:
 * <ol>
 *   <li>Sin token, o con un token inválido, deja pasar la petición sin autenticar: la cadena de
 *       seguridad decide si ese destino exige sesión (los públicos funcionan y los protegidos
 *       responden 401).</li>
 *   <li>Un token de refresco no sirve como token de acceso.</li>
 *   <li>La sesión del token debe seguir abierta: cerrar sesión lo invalida al instante.</li>
 *   <li>La cuenta debe seguir activa, sin bloqueo y con la misma versión de tokens que trae el
 *       token. Cambiar o reiniciar la contraseña o dar de baja la cuenta sube la versión, así que
 *       los tokens anteriores dejan de servir al instante.</li>
 *   <li>Si la cuenta tiene pendiente el cambio de contraseña (temporal recién entregada), solo
 *       puede cambiarla o cerrar sesión: cualquier otra ruta responde 403. La pantalla también lo
 *       impide, pero la regla que protege es esta.</li>
 * </ol>
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private static final String CABECERA = "Authorization";
    private static final String PREFIJO = "Bearer ";

    /** Únicas rutas permitidas mientras la contraseña temporal no se haya cambiado. */
    private static final Set<String> RUTAS_CON_PASSWORD_PENDIENTE = Set.of(
            "/auth/cambiar-password", "/auth/logout", "/auth/refrescar");

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsServiceImpl userDetailsService;
    private final SesionUsuarioRepository sesionRepository;

    public JwtAuthFilter(JwtTokenProvider tokenProvider,
                         UserDetailsServiceImpl userDetailsService,
                         SesionUsuarioRepository sesionRepository) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
        this.sesionRepository = sesionRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest peticion,
                                    @NonNull HttpServletResponse respuesta,
                                    @NonNull FilterChain cadena)
            throws ServletException, IOException {

        String token = extraerToken(peticion);
        UsuarioAutenticado usuario = token == null ? null : autenticar(token, peticion);

        if (usuario != null && usuario.isCambioPasswordRequerido()
                && !RUTAS_CON_PASSWORD_PENDIENTE.contains(rutaSinContexto(peticion))) {
            responderCambioPendiente(respuesta);
            return;
        }

        cadena.doFilter(peticion, respuesta);
    }

    /**
     * Coloca al usuario en el contexto de seguridad de esta petición si el token sirve.
     *
     * @param token    token de la cabecera
     * @param peticion petición en curso, para los detalles de autenticación
     * @return el principal autenticado, o {@code null} si el token no permite operar
     */
    private UsuarioAutenticado autenticar(String token, HttpServletRequest peticion) {
        JwtTokenProvider.DatosToken datos = tokenProvider.leer(token).orElse(null);
        if (datos == null || !datos.esAcceso() || datos.usuario() == null
                || datos.sesion() == null) {
            return null;
        }
        if (!sesionRepository.existsByIdentificadorAndRevocadaIsNullAndExpiraAfter(
                datos.sesion(), OffsetDateTime.now())) {
            log.debug("Token de una sesión cerrada para {}", datos.usuario());
            return null;
        }

        try {
            UsuarioAutenticado usuario =
                    (UsuarioAutenticado) userDetailsService.loadUserByUsername(datos.usuario());

            // Cuenta dada de baja o bloqueada después de emitirse el token.
            if (!usuario.isEnabled() || !usuario.isAccountNonLocked()) {
                log.debug("Cuenta inactiva o bloqueada: {}", datos.usuario());
                return null;
            }
            // Token emitido antes de un cambio de contraseña, reinicio o cierre forzado.
            if (!Objects.equals(datos.version(), usuario.getTokenVersion())
                    || !Objects.equals(datos.usuarioId(), usuario.getId())) {
                log.debug("Token de una versión anterior para {}", datos.usuario());
                return null;
            }

            usuario.borrarPassword();
            UsernamePasswordAuthenticationToken autenticacion =
                    new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
            autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(peticion));
            SecurityContextHolder.getContext().setAuthentication(autenticacion);
            return usuario;

        } catch (UsernameNotFoundException ex) {
            // El token es válido pero la cuenta ya no existe.
            log.debug("Token de una cuenta inexistente");
            return null;
        }
    }

    /** Responde 403 en formato {@code ApiResponse} cuando falta cambiar la contraseña temporal. */
    private void responderCambioPendiente(HttpServletResponse respuesta) throws IOException {
        respuesta.setStatus(HttpStatus.FORBIDDEN.value());
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setCharacterEncoding("UTF-8");
        respuesta.getWriter().write(
                "{\"success\":false,\"message\":\"" + MessageConstants.CAMBIO_PASSWORD_REQUERIDO + "\"}");
    }

    /** Ruta sin el prefijo de contexto ({@code /api}). */
    private static String rutaSinContexto(HttpServletRequest peticion) {
        String uri = peticion.getRequestURI();
        String contexto = peticion.getContextPath();
        return contexto != null && !contexto.isEmpty() && uri.startsWith(contexto)
                ? uri.substring(contexto.length())
                : uri;
    }

    /** Lee el token de la cabecera {@code Authorization}; nulo si no viene o está vacío. */
    private String extraerToken(HttpServletRequest peticion) {
        String cabecera = peticion.getHeader(CABECERA);
        if (cabecera != null && cabecera.startsWith(PREFIJO)) {
            String token = cabecera.substring(PREFIJO.length()).trim();
            return token.isEmpty() ? null : token;
        }
        return null;
    }
}
