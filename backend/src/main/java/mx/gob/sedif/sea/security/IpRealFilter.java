package mx.gob.sedif.sea.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Determina la IP real del cliente detrás de los proxies y la deja en {@code getRemoteAddr()}.
 * Área: Núcleo compartido (seguridad). Lógica: base del límite de peticiones, de la bitácora y de la
 * evidencia del consentimiento del portal.
 *
 * <p>Regla: solo se cree lo que anotaron los proxies de confianza ({@code app.proxies-confiables},
 * lista de subredes en formato CIDR). Se recorre {@code X-Forwarded-For} de derecha a izquierda
 * saltando esos proxies; la primera dirección que no es de confianza es el cliente. Lo que el
 * cliente haya escrito a la izquierda se ignora, y si la conexión no viene de un proxy de confianza
 * la cabecera se ignora por completo.
 *
 * <p>No se usa {@code server.forward-headers-strategy}: con {@code framework} se tomaría el primer
 * valor (lo escribe el cliente) y con {@code native} Tomcat confía en todas las redes privadas, lo
 * que permitiría falsear la IP desde la red interna de la institución.
 *
 * <p>Se ejecuta antes que cualquier otro filtro para que el límite de peticiones y la bitácora vean
 * ya la dirección resuelta.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IpRealFilter extends OncePerRequestFilter {

    private static final String CABECERA = "X-Forwarded-For";

    private final List<IpAddressMatcher> confiables;

    /**
     * @param proxiesConfiables subredes CIDR separadas por comas, por ejemplo
     *                          {@code 127.0.0.1/32,172.31.250.128/25}; las entradas vacías se ignoran
     */
    public IpRealFilter(@Value("${app.proxies-confiables:127.0.0.1/32,::1/128}") String proxiesConfiables) {
        this.confiables = Arrays.stream(proxiesConfiables.split(","))
                .map(String::trim)
                .filter(cidr -> !cidr.isEmpty())
                .map(IpAddressMatcher::new)
                .toList();
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest peticion,
                                    @NonNull HttpServletResponse respuesta,
                                    @NonNull FilterChain cadena)
            throws ServletException, IOException {

        String ip = resolver(peticion.getRemoteAddr(), peticion.getHeader(CABECERA));
        if (ip.equals(peticion.getRemoteAddr())) {
            cadena.doFilter(peticion, respuesta);
            return;
        }

        cadena.doFilter(new HttpServletRequestWrapper(peticion) {
            @Override
            public String getRemoteAddr() {
                return ip;
            }

            @Override
            public String getRemoteHost() {
                return ip;
            }
        }, respuesta);
    }

    /**
     * Calcula la IP del cliente.
     *
     * @param conexion      dirección de quien abrió la conexión TCP con el backend
     * @param reenviadaPara valor de {@code X-Forwarded-For}; puede ser nulo
     * @return la primera dirección, de derecha a izquierda, que no pertenece a un proxy de confianza;
     *         la de la conexión si esta no viene de un proxy de confianza
     */
    String resolver(String conexion, String reenviadaPara) {
        if (!esConfiable(conexion) || reenviadaPara == null || reenviadaPara.isBlank()) {
            return conexion;
        }
        String[] saltos = reenviadaPara.split(",");
        String candidata = conexion;
        for (int i = saltos.length - 1; i >= 0; i--) {
            String salto = saltos[i].trim();
            if (salto.isEmpty() || !pareceIp(salto)) {
                // Un valor que no es IP no puede venir de un proxy: se detiene la búsqueda.
                return candidata;
            }
            candidata = salto;
            if (!esConfiable(salto)) {
                return salto;
            }
        }
        return candidata;
    }

    private boolean esConfiable(String ip) {
        if (!pareceIp(ip)) {
            return false;
        }
        for (IpAddressMatcher red : confiables) {
            try {
                if (red.matches(ip)) {
                    return true;
                }
            } catch (IllegalArgumentException ex) {
                // Dirección con formato inválido para esta red (por ejemplo IPv6 contra IPv4).
            }
        }
        return false;
    }

    /** Acepta solo dígitos, puntos, dos puntos y hexadecimales: evita resolver nombres por DNS. */
    private static boolean pareceIp(String valor) {
        return valor != null && !valor.isEmpty() && valor.length() <= 45
                && valor.chars().allMatch(c -> Character.digit(c, 16) >= 0 || c == '.' || c == ':');
    }
}
