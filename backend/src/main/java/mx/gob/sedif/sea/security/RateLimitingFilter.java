package mx.gob.sedif.sea.security;

import java.io.IOException;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Limita cuántas peticiones admite cada dirección IP en los destinos sensibles.
 * Área: Núcleo compartido (seguridad). Lógica: protección del inicio de sesión y del portal público.
 *
 * <p>Cubre dos riesgos distintos:
 * <ul>
 *   <li><b>Fuerza bruta</b> contra el inicio de sesión y el refresco del token.</li>
 *   <li><b>Basura y enumeración</b> en el portal público: registros falsos, cargas de archivos
 *       y pruebas de combinaciones folio + CURP en la consulta y en el acuse.</li>
 * </ul>
 *
 * <p>La cubeta es por (IP + familia de ruta): agotar el límite del formulario público no impide
 * consultar un folio. La IP se obtiene con {@link SecurityUtil#obtenerIpCliente}, que no confía en
 * cabeceras escritas por el cliente.
 *
 * <p>Los contadores viven en memoria del proceso, en una caché que olvida las direcciones inactivas
 * a los diez minutos. Basta mientras haya una sola instancia del backend; si se replica, habrá que
 * moverlos a un almacén compartido.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    /** Familias de ruta que se limitan. Cada una tiene su propia cubeta por IP. */
    enum Familia {
        LOGIN, REFRESCO, DOCUMENTO, REGISTRO, CONSULTA
    }

    /** Tope de direcciones distintas en memoria: acota el consumo aunque llegue un barrido de IPs. */
    private static final long MAXIMO_CUBETAS = 100_000;

    private final Cache<String, Bucket> cubetas = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .maximumSize(MAXIMO_CUBETAS)
            .build();

    private final int limiteLogin;
    private final int limiteRefresco;
    private final int limitePublico;
    private final int limiteConsulta;
    private final int limiteDocumento;

    /**
     * @param limiteLogin     intentos de inicio de sesión por minuto e IP
     * @param limiteRefresco  renovaciones de token por minuto e IP (cada recarga del panel hace una)
     * @param limitePublico   registros de trámite por minuto e IP
     * @param limiteConsulta  consultas de folio, descargas de acuse y verificaciones por minuto e IP
     * @param limiteDocumento cargas de documento desde el portal por minuto e IP
     */
    public RateLimitingFilter(
            @Value("${app.rate-limit.login:5}") int limiteLogin,
            @Value("${app.rate-limit.refresco:120}") int limiteRefresco,
            @Value("${app.rate-limit.publico:20}") int limitePublico,
            @Value("${app.rate-limit.consulta:30}") int limiteConsulta,
            @Value("${app.rate-limit.documento:10}") int limiteDocumento) {
        this.limiteLogin = limiteLogin;
        this.limiteRefresco = limiteRefresco;
        this.limitePublico = limitePublico;
        this.limiteConsulta = limiteConsulta;
        this.limiteDocumento = limiteDocumento;
    }

    /**
     * Deja pasar la petición si su cubeta tiene saldo; si no, responde 429 con el formato
     * {@code ApiResponse}. Las rutas que no pertenecen a ninguna familia no se limitan.
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest peticion,
                                    @NonNull HttpServletResponse respuesta,
                                    @NonNull FilterChain cadena)
            throws ServletException, IOException {

        Familia familia = familiaDe(rutaSinContexto(peticion));
        if (familia == null) {
            cadena.doFilter(peticion, respuesta);
            return;
        }

        String ip = SecurityUtil.obtenerIpCliente(peticion);
        Bucket cubeta = cubetas.get(ip + "|" + familia, llave -> crearCubeta(limiteDe(familia)));

        if (cubeta.tryConsume(1)) {
            cadena.doFilter(peticion, respuesta);
        } else {
            log.warn("Límite de peticiones alcanzado por {} en {}", ip, familia);
            responderDemasiadasPeticiones(respuesta);
        }
    }

    /**
     * Familia a la que pertenece una ruta del API.
     *
     * @param ruta ruta sin el prefijo de contexto ({@code /api}), por ejemplo {@code /auth/login}
     * @return la familia, o {@code null} si la ruta no se limita
     */
    static Familia familiaDe(String ruta) {
        if (ruta.startsWith("/auth/login")) {
            return Familia.LOGIN;
        }
        if (ruta.startsWith("/auth/refrescar")) {
            return Familia.REFRESCO;
        }
        if (ruta.startsWith("/publico/documento")) {
            return Familia.DOCUMENTO;
        }
        if (ruta.startsWith("/publico/solicitud")) {
            return Familia.REGISTRO;
        }
        // El acuse exige folio + CURP igual que la consulta y además genera un PDF: si quedara
        // fuera, serviría para probar combinaciones sin freno y para saturar el servidor.
        if (ruta.startsWith("/publico/consulta")
                || ruta.startsWith("/publico/acuse")
                || ruta.startsWith("/publico/verificar")) {
            return Familia.CONSULTA;
        }
        return null;
    }

    private int limiteDe(Familia familia) {
        return switch (familia) {
            case LOGIN -> limiteLogin;
            case REFRESCO -> limiteRefresco;
            case DOCUMENTO -> limiteDocumento;
            case REGISTRO -> limitePublico;
            case CONSULTA -> limiteConsulta;
        };
    }

    /** Ruta de la petición sin el prefijo de contexto, para comparar contra rutas del API. */
    private static String rutaSinContexto(HttpServletRequest peticion) {
        String uri = peticion.getRequestURI();
        String contexto = peticion.getContextPath();
        return contexto != null && !contexto.isEmpty() && uri.startsWith(contexto)
                ? uri.substring(contexto.length())
                : uri;
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
                {"success":false,"message":"Demasiadas peticiones. Espera un minuto antes de volver a intentar."}""");
    }
}
