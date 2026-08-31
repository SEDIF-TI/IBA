package mx.gob.sedif.sea.security;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Limita cuantas peticiones admite cada direccion en los destinos sensibles.
 *
 * <p>Cubre dos riesgos distintos:
 * <ul>
 *   <li><b>Fuerza bruta</b> contra el inicio de sesion.</li>
 *   <li><b>Basura</b> en el formulario publico. Un formulario abierto sin
 *       autenticacion se llena de registros falsos, y el personal acabaria
 *       depurando basura en vez de resolviendo tramites.</li>
 * </ul>
 *
 * <p>Los contadores viven en memoria del proceso. Es suficiente mientras haya
 * una sola instancia; si algun dia se replica el backend habra que moverlos a
 * un almacen compartido, porque cada instancia contaria por separado.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    /** Rutas de autenticacion: el limite mas estricto. */
    private static final String RUTA_LOGIN = "/auth/login";
    private static final String RUTA_REFRESCO = "/auth/refrescar";

    /** Alta de tramites desde el portal. */
    private static final String RUTA_PUBLICA = "/publico/solicitud";

    /** Consultas de seguimiento y verificacion del certificado. */
    private static final String RUTA_CONSULTA = "/publico/consulta";
    private static final String RUTA_VERIFICACION = "/publico/verificar";

    private final Map<String, Bucket> cubetas = new ConcurrentHashMap<>();

    private final int limiteLogin;
    private final int limitePublico;
    private final int limiteConsulta;

    public RateLimitingFilter(
            @Value("${app.rate-limit.login:5}") int limiteLogin,
            @Value("${app.rate-limit.publico:20}") int limitePublico,
            @Value("${app.rate-limit.consulta:30}") int limiteConsulta) {
        this.limiteLogin = limiteLogin;
        this.limitePublico = limitePublico;
        this.limiteConsulta = limiteConsulta;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest peticion,
                                    @NonNull HttpServletResponse respuesta,
                                    @NonNull FilterChain cadena)
            throws ServletException, IOException {

        Integer limite = limitePara(peticion.getRequestURI());

        if (limite == null) {
            cadena.doFilter(peticion, respuesta);
            return;
        }

        String ip = SecurityUtil.obtenerIpCliente(peticion);
        // La cubeta es por direccion Y por ruta: agotar el limite del formulario
        // publico no debe impedir consultar un folio.
        String llave = ip + "|" + rutaNormalizada(peticion.getRequestURI());

        Bucket cubeta = cubetas.computeIfAbsent(llave, clave -> crearCubeta(limite));

        if (cubeta.tryConsume(1)) {
            cadena.doFilter(peticion, respuesta);
        } else {
            log.warn("Limite de peticiones alcanzado por {} en {}", ip, peticion.getRequestURI());
            responderDemasiadasPeticiones(respuesta);
        }
    }

    /** Limite por minuto que corresponde a la ruta, o nulo si no se limita. */
    private Integer limitePara(String ruta) {
        if (ruta.contains(RUTA_LOGIN) || ruta.contains(RUTA_REFRESCO)) {
            return limiteLogin;
        }
        if (ruta.contains(RUTA_PUBLICA)) {
            return limitePublico;
        }
        if (ruta.contains(RUTA_CONSULTA) || ruta.contains(RUTA_VERIFICACION)) {
            return limiteConsulta;
        }
        return null;
    }

    /** Agrupa las rutas por familia para que compartan cubeta. */
    private String rutaNormalizada(String ruta) {
        if (ruta.contains(RUTA_LOGIN) || ruta.contains(RUTA_REFRESCO)) {
            return "auth";
        }
        if (ruta.contains(RUTA_PUBLICA)) {
            return "publico";
        }
        return "consulta";
    }

    /** Cubeta que se rellena por completo cada minuto. */
    private Bucket crearCubeta(int peticionesPorMinuto) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(peticionesPorMinuto)
                        .refillIntervally(peticionesPorMinuto, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    /** Responde 429 con el mismo formato uniforme del resto del API. */
    private void responderDemasiadasPeticiones(HttpServletResponse respuesta) throws IOException {
        respuesta.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setCharacterEncoding("UTF-8");
        respuesta.getWriter().write("""
                {"success":false,"message":"Demasiadas peticiones. Espera un momento antes de volver a intentar."}""");
    }

    /**
     * Libera las cubetas acumuladas.
     *
     * <p>Sin esto el mapa crece indefinidamente: cada direccion distinta deja
     * una entrada. Lo invoca una tarea programada.
     */
    public void limpiarCubetas() {
        int antes = cubetas.size();
        cubetas.clear();
        log.debug("Cubetas de limite liberadas: {}", antes);
    }
}
