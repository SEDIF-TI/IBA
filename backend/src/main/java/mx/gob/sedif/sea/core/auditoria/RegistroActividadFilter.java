package mx.gob.sedif.sea.core.auditoria;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import mx.gob.sedif.sea.security.SecurityUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Registra en la bitácora cada petición al API: quién, qué, cuándo, desde dónde y con qué resultado.
 * Área: Núcleo compartido (auditoría). Lógica: bitácora de todos los movimientos.
 *
 * <p>Va dentro de la cadena de Spring Security, después del límite de peticiones y antes de la
 * autenticación por token, para ver el usuario autenticado al terminar la petición y registrar
 * también las respuestas 401 y 403 (accesos denegados) y 500 (errores).
 *
 * <p>Reglas de privacidad: nunca guarda el cuerpo de la petición ni de la respuesta, ni los valores
 * de los parámetros de consulta (una búsqueda por CURP o apellido queda como "filtros: curp" o
 * "filtros: texto"). La ruta se guarda tal cual porque solo lleva identificadores internos.
 *
 * <p>No registra lo que ya tiene un registro explícito y más preciso: los eventos de
 * {@code /auth/**} (inicio de sesión, intentos fallidos, bloqueos, cierres y cambios de contraseña
 * los registra {@code AuthService}). Tampoco la sonda de salud ni las peticiones {@code OPTIONS}
 * del navegador, que no son acciones de una persona, ni las rechazadas con 429 por el límite de
 * peticiones (registrarlas convertiría un ataque en miles de escrituras a la base).
 */
@Component
public class RegistroActividadFilter extends OncePerRequestFilter {

    private static final Pattern NUMERO = Pattern.compile("\\d{1,18}");

    private final AuditoriaService auditoria;

    public RegistroActividadFilter(AuditoriaService auditoria) {
        this.auditoria = auditoria;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest peticion) {
        String ruta = rutaSinContexto(peticion);
        return "OPTIONS".equalsIgnoreCase(peticion.getMethod())
                || ruta.startsWith("/actuator")
                || ruta.startsWith("/auth/");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest peticion,
                                    @NonNull HttpServletResponse respuesta,
                                    @NonNull FilterChain cadena)
            throws ServletException, IOException {

        long inicio = System.currentTimeMillis();
        boolean fallo = false;
        try {
            cadena.doFilter(peticion, respuesta);
        } catch (IOException | ServletException | RuntimeException ex) {
            fallo = true;
            throw ex;
        } finally {
            int codigo = fallo ? 500 : respuesta.getStatus();
            if (codigo != 429) {
                auditoria.registrarActividad(armar(peticion, codigo,
                        System.currentTimeMillis() - inicio));
            }
        }
    }

    /**
     * Convierte la petición terminada en un registro de bitácora.
     *
     * @param peticion   petición atendida
     * @param codigo     código HTTP de la respuesta
     * @param duracionMs milisegundos que tardó
     * @return los datos depurados para {@link AuditoriaService#registrarActividad}
     */
    AuditoriaService.Actividad armar(HttpServletRequest peticion, int codigo, long duracionMs) {
        String ruta = rutaSinContexto(peticion);
        String[] segmentos = ruta.split("/");
        String entidad = segmentos.length > 1 && !segmentos[1].isBlank() ? segmentos[1] : "api";
        Long registroId = null;
        for (String segmento : segmentos) {
            if (NUMERO.matcher(segmento).matches()) {
                registroId = Long.valueOf(segmento);
                break;
            }
        }

        String metodo = peticion.getMethod();
        List<String> filtros = Collections.list(peticion.getParameterNames()).stream()
                .filter(nombre -> !nombre.equals("page") && !nombre.equals("size")
                        && !nombre.equals("sort"))
                .sorted()
                .toList();
        String detalle = metodo + " " + ruta + " -> " + codigo
                + (filtros.isEmpty() ? "" : " · filtros: " + String.join(", ", filtros));

        return new AuditoriaService.Actividad(
                SecurityUtil.obtenerUsuarioId().orElse(null),
                accion(metodo, codigo),
                entidad,
                registroId,
                metodo,
                ruta.length() > 300 ? ruta.substring(0, 300) : ruta,
                codigo,
                resultado(codigo),
                duracionMs,
                SecurityUtil.obtenerIpCliente(peticion),
                SecurityUtil.obtenerUserAgent(peticion),
                detalle.length() > 1000 ? detalle.substring(0, 1000) : detalle);
    }

    /** Acción según el método y el resultado: lo denegado y lo fallido se distinguen aparte. */
    static String accion(String metodo, int codigo) {
        if (codigo == 401) {
            return "NO_AUTENTICADO";
        }
        if (codigo == 403) {
            return "ACCESO_DENEGADO";
        }
        if (codigo >= 500) {
            return "ERROR";
        }
        return switch (metodo.toUpperCase()) {
            case "GET", "HEAD" -> "CONSULTA";
            case "POST" -> "ALTA";
            case "PUT", "PATCH" -> "EDICION";
            case "DELETE" -> "BAJA";
            default -> metodo.toUpperCase();
        };
    }

    /** EXITO (2xx y 3xx), DENEGADO (401 y 403), ERROR (5xx) o RECHAZADO (otros 4xx). */
    static String resultado(int codigo) {
        if (codigo < 400) {
            return "EXITO";
        }
        if (codigo == 401 || codigo == 403) {
            return "DENEGADO";
        }
        return codigo >= 500 ? "ERROR" : "RECHAZADO";
    }

    private static String rutaSinContexto(HttpServletRequest peticion) {
        String uri = peticion.getRequestURI();
        String contexto = peticion.getContextPath();
        return contexto != null && !contexto.isEmpty() && uri.startsWith(contexto)
                ? uri.substring(contexto.length())
                : uri;
    }
}
