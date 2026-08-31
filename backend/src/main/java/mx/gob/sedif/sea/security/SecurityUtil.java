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
     * Direccion desde la que llega la peticion.
     *
     * <p>Detras del proxy inverso la direccion real viaja en X-Forwarded-For;
     * se toma la primera de la lista, que es la del cliente. Sin esto, todas
     * las peticiones parecerian venir del proxy y tanto la evidencia de
     * consentimiento como el limite por direccion quedarian inservibles.
     */
    public static String obtenerIpCliente(HttpServletRequest peticion) {
        String reenviada = peticion.getHeader("X-Forwarded-For");
        if (reenviada != null && !reenviada.isBlank()) {
            return reenviada.split(",")[0].trim();
        }
        String ipReal = peticion.getHeader("X-Real-IP");
        if (ipReal != null && !ipReal.isBlank()) {
            return ipReal.trim();
        }
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
