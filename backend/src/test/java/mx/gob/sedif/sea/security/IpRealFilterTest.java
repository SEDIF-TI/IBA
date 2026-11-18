package mx.gob.sedif.sea.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Pruebas de la resolución de la IP real del cliente detrás de los proxies.
 *
 * <p>Flujo cubierto: base del límite de peticiones y de la bitácora; en particular, que inventar
 * {@code X-Forwarded-For} no cambie la IP, ni desde internet ni desde la red interna.
 */
class IpRealFilterTest {

    /** Igual que la configuración local: los contenedores viven en la mitad alta de la red. */
    private final IpRealFilter filtro =
            new IpRealFilter("127.0.0.1/32,172.31.250.128/25,172.18.0.0/16");

    @Test
    @DisplayName("sin proxy de confianza la cabecera se ignora por completo")
    void conexionDirectaIgnoraCabecera() {
        assertThat(filtro.resolver("203.0.113.5", "1.2.3.4")).isEqualTo("203.0.113.5");
    }

    @Test
    @DisplayName("detrás del Nginx del proyecto se toma lo que anotó él, no lo que escribió el cliente")
    void ignoraLoQueEscribeElCliente() {
        // El cliente mandó 1.2.3.4; Nginx (172.31.250.130) anotó la conexión real 203.0.113.5.
        assertThat(filtro.resolver("172.31.250.130", "1.2.3.4, 203.0.113.5"))
                .isEqualTo("203.0.113.5");
    }

    @Test
    @DisplayName("con Nginx Proxy Manager delante del Nginx del proyecto se saltan ambos")
    void saltaVariosProxies() {
        assertThat(filtro.resolver("172.31.250.130", "1.2.3.4, 203.0.113.5, 172.18.0.7"))
                .isEqualTo("203.0.113.5");
    }

    @Test
    @DisplayName("la puerta de enlace de Docker no es de confianza: el equipo anfitrión no puede falsear")
    void puertaDeEnlaceNoEsConfiable() {
        assertThat(filtro.resolver("172.31.250.130", "198.51.100.1, 172.31.250.1"))
                .isEqualTo("172.31.250.1");
    }

    @Test
    @DisplayName("una dirección de la red interna de la institución tampoco puede falsear")
    void redInternaNoEsConfiable() {
        assertThat(filtro.resolver("172.31.250.130", "198.51.100.1, 10.0.5.20"))
                .isEqualTo("10.0.5.20");
    }

    @Test
    @DisplayName("un valor que no es IP detiene la búsqueda en el último salto confiable")
    void valorBasura() {
        assertThat(filtro.resolver("172.31.250.130", "<script>, 203.0.113.5"))
                .isEqualTo("203.0.113.5");
        assertThat(filtro.resolver("172.31.250.130", "no-es-ip")).isEqualTo("172.31.250.130");
    }

    @Test
    @DisplayName("el filtro deja la IP resuelta en getRemoteAddr para el resto del sistema")
    void envuelveLaPeticion() throws Exception {
        MockHttpServletRequest peticion = new MockHttpServletRequest("GET", "/api/publico/consulta");
        peticion.setRemoteAddr("172.31.250.130");
        peticion.addHeader("X-Forwarded-For", "1.2.3.4, 203.0.113.5");
        MockFilterChain cadena = new MockFilterChain();

        filtro.doFilter(peticion, new MockHttpServletResponse(), cadena);

        assertThat(cadena.getRequest().getRemoteAddr()).isEqualTo("203.0.113.5");
    }
}
