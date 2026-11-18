package mx.gob.sedif.sea.security;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utilidades para consultar quien esta operando.
 *
 * <p>Todos los metodos contemplan que no haya sesion: el registro publico es
 * anonimo, asi que la ausencia de usuario es normal y no un fallo.
 */
public final class SecurityUtil {

    private SecurityUtil() {
    }

    /** Principal autenticado, si lo hay. */
    public static Optional<UsuarioAutenticado> obtenerUsuarioActual() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !autenticacion.isAuthenticated()) {
            return Optional.empty();
        }
        if (autenticacion.getPrincipal() instanceof UsuarioAutenticado usuario) {
            return Optional.of(usuario);
        }
        return Optional.empty();
    }

    /** Identificador del usuario en sesion, para auditoria y bitacora. */
    public static Optional<Long> obtenerUsuarioId() {
        return obtenerUsuarioActual().map(UsuarioAutenticado::getId);
    }

    /** Nombre de usuario en sesion. */
    public static Optional<String> obtenerNombreUsuario() {
        return obtenerUsuarioActual().map(UsuarioAutenticado::getUsername);
    }

    /** Verdadero si quien opera tiene el permiso indicado. */
    public static boolean tienePermiso(String permiso) {
        return obtenerUsuarioActual()
                .map(usuario -> usuario.getAuthorities().stream()
                        .anyMatch(autoridad -> autoridad.getAuthority().equals(permiso)))
                .orElse(false);
    }

    /**
     * Dirección IP real del cliente.
     *
     * <p>Nunca se leen a mano las cabeceras {@code X-Forwarded-For} o {@code X-Real-IP}: su primer
     * valor lo escribe el propio cliente y bastaría con inventarlo para saltarse el límite de
     * peticiones y falsear la IP de la bitácora y del consentimiento.
     *
     * <p>La traducción la hace {@link IpRealFilter} antes que cualquier otro filtro: recorre
     * {@code X-Forwarded-For} de derecha a izquierda, salta solo los proxies de confianza
     * ({@code app.proxies-confiables}) y deja en {@code getRemoteAddr()} la primera dirección que no
     * es de confianza, que es la que anotó el primer proxy al recibir la conexión.
     *
     * @param peticion petición en curso
     * @return la IP del cliente; nunca nula dentro de una petición HTTP
     */
    public static String obtenerIpCliente(HttpServletRequest peticion) {
        return peticion.getRemoteAddr();
    }

    /** Navegador o cliente que originó la peticion, recortado para la bitacora. */
    public static String obtenerUserAgent(HttpServletRequest peticion) {
        String agente = peticion.getHeader("User-Agent");
        if (agente == null) {
            return null;
        }
        return agente.length() > 400 ? agente.substring(0, 400) : agente;
    }
}
