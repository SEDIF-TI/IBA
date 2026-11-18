package mx.gob.sedif.sea.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Pruebas del límite de peticiones por IP y familia de ruta.
 *
 * <p>Flujo cubierto: protección del inicio de sesión y del portal público contra fuerza bruta,
 * basura y enumeración de folio + CURP, incluida la evasión con cabeceras falsas.
 */
class RateLimitingFilterTest {

    /** Límites bajos para llegar al tope con pocas peticiones. */
    private final RateLimitingFilter filtro = new RateLimitingFilter(2, 3, 2, 2, 2);

    /** Petición al API con el prefijo de contexto real ({@code /api}). */
    private static MockHttpServletRequest peticion(String ruta, String ip) {
        MockHttpServletRequest peticion = new MockHttpServletRequest("POST", "/api" + ruta);
        peticion.setContextPath("/api");
        peticion.setRemoteAddr(ip);
        return peticion;
    }

    private int enviar(MockHttpServletRequest peticion) throws Exception {
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        filtro.doFilter(peticion, respuesta, new MockFilterChain());
        return respuesta.getStatus();
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "/auth/login, LOGIN",
            "/auth/refrescar, REFRESCO",
            "/publico/documento, DOCUMENTO",
            "/publico/solicitud, REGISTRO",
            "/publico/consulta, CONSULTA",
            "/publico/acuse, CONSULTA",
            "/publico/verificar/abc, CONSULTA"})
    @DisplayName("cada ruta sensible pertenece a su familia, incluido el acuse")
    void clasificaRutas(String ruta, RateLimitingFilter.Familia esperada) {
        assertThat(RateLimitingFilter.familiaDe(ruta)).isEqualTo(esperada);
    }

    @Test
    @DisplayName("las rutas del panel y los catálogos públicos no se limitan")
    void noLimitaRutasComunes() {
        assertThat(RateLimitingFilter.familiaDe("/solicitudes")).isNull();
        assertThat(RateLimitingFilter.familiaDe("/publico/catalogos")).isNull();
    }

    @Test
    @DisplayName("al agotar el límite responde 429 en formato ApiResponse")
    void respondeDemasiadasPeticiones() throws Exception {
        assertThat(enviar(peticion("/auth/login", "203.0.113.10"))).isEqualTo(200);
        assertThat(enviar(peticion("/auth/login", "203.0.113.10"))).isEqualTo(200);

        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        filtro.doFilter(peticion("/auth/login", "203.0.113.10"), respuesta, new MockFilterChain());

        assertThat(respuesta.getStatus()).isEqualTo(429);
        assertThat(respuesta.getContentAsString()).contains("\"success\":false");
    }

    @Test
    @DisplayName("inventar X-Forwarded-For en cada petición no evita el límite")
    void ignoraCabecerasFalsas() throws Exception {
        for (int i = 0; i < 2; i++) {
            MockHttpServletRequest falsa = peticion("/auth/login", "203.0.113.20");
            falsa.addHeader("X-Forwarded-For", "198.51.100." + i);
            falsa.addHeader("X-Real-IP", "198.51.100." + i);
            assertThat(enviar(falsa)).isEqualTo(200);
        }
        MockHttpServletRequest tercera = peticion("/auth/login", "203.0.113.20");
        tercera.addHeader("X-Forwarded-For", "198.51.100.99");
        assertThat(enviar(tercera)).isEqualTo(429);
    }

    @Test
    @DisplayName("el login y el refresco tienen cubetas separadas")
    void familiasIndependientes() throws Exception {
        enviar(peticion("/auth/login", "203.0.113.30"));
        enviar(peticion("/auth/login", "203.0.113.30"));
        assertThat(enviar(peticion("/auth/login", "203.0.113.30"))).isEqualTo(429);
        assertThat(enviar(peticion("/auth/refrescar", "203.0.113.30"))).isEqualTo(200);
    }

    @Test
    @DisplayName("la IP del cliente es la que resolvió Tomcat, no la de las cabeceras")
    void ipDelClienteNoLeeCabeceras() {
        MockHttpServletRequest falsa = peticion("/publico/consulta", "203.0.113.40");
        falsa.addHeader("X-Forwarded-For", "1.2.3.4");
        assertThat(SecurityUtil.obtenerIpCliente(falsa)).isEqualTo("203.0.113.40");
    }
}
