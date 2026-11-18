package mx.gob.sedif.sea.core.auditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Pruebas del registro de actividad en la bitácora.
 *
 * <p>Flujo cubierto: toda petición al API queda registrada con su acción y resultado, y nunca se
 * guardan los valores buscados (una CURP o un apellido), solo los nombres de los filtros.
 */
class RegistroActividadFilterTest {

    private final AuditoriaService auditoria = mock(AuditoriaService.class);
    private final RegistroActividadFilter filtro = new RegistroActividadFilter(auditoria);

    private static MockHttpServletRequest peticion(String metodo, String ruta) {
        MockHttpServletRequest peticion = new MockHttpServletRequest(metodo, "/api" + ruta);
        peticion.setContextPath("/api");
        peticion.setRemoteAddr("203.0.113.9");
        return peticion;
    }

    @ParameterizedTest(name = "{0} con {1} -> {2} / {3}")
    @CsvSource({
            "GET, 200, CONSULTA, EXITO",
            "POST, 200, ALTA, EXITO",
            "PUT, 200, EDICION, EXITO",
            "PATCH, 409, EDICION, RECHAZADO",
            "DELETE, 200, BAJA, EXITO",
            "GET, 401, NO_AUTENTICADO, DENEGADO",
            "POST, 403, ACCESO_DENEGADO, DENEGADO",
            "GET, 500, ERROR, ERROR"})
    @DisplayName("clasifica la acción y el resultado por método y código")
    void clasifica(String metodo, int codigo, String accion, String resultado) {
        assertThat(RegistroActividadFilter.accion(metodo, codigo)).isEqualTo(accion);
        assertThat(RegistroActividadFilter.resultado(codigo)).isEqualTo(resultado);
    }

    @Test
    @DisplayName("guarda los nombres de los filtros, nunca sus valores")
    void noGuardaValoresBuscados() {
        MockHttpServletRequest busqueda = peticion("GET", "/tutores");
        busqueda.setParameter("texto", "MUÑOZ");
        busqueda.setParameter("curp", "GODE561231HDFRRN09");
        busqueda.setParameter("page", "0");

        AuditoriaService.Actividad actividad = filtro.armar(busqueda, 200, 12);

        assertThat(actividad.detalle()).contains("filtros: curp, texto")
                .doesNotContain("MUÑOZ").doesNotContain("GODE561231HDFRRN09")
                .doesNotContain("page");
        assertThat(actividad.ruta()).isEqualTo("/tutores");
        assertThat(actividad.entidad()).isEqualTo("tutores");
    }

    @Test
    @DisplayName("toma el primer identificador numérico de la ruta como registro afectado")
    void identificaElRegistro() {
        AuditoriaService.Actividad actividad =
                filtro.armar(peticion("PATCH", "/solicitudes/45/aprobar"), 200, 3);

        assertThat(actividad.registroId()).isEqualTo(45L);
        assertThat(actividad.entidad()).isEqualTo("solicitudes");
        assertThat(actividad.ip()).isEqualTo("203.0.113.9");
    }

    @Test
    @DisplayName("registra cada petición al terminar, con el código final")
    void registraAlTerminar() throws Exception {
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        respuesta.setStatus(403);

        filtro.doFilter(peticion("GET", "/usuarios"), respuesta, new MockFilterChain());

        ArgumentCaptor<AuditoriaService.Actividad> registro =
                ArgumentCaptor.forClass(AuditoriaService.Actividad.class);
        verify(auditoria).registrarActividad(registro.capture());
        assertThat(registro.getValue().accion()).isEqualTo("ACCESO_DENEGADO");
        assertThat(registro.getValue().codigoHttp()).isEqualTo(403);
    }

    @Test
    @DisplayName("no duplica los eventos de sesión ni registra la sonda de salud")
    void omiteAuthYHealth() throws Exception {
        filtro.doFilter(peticion("POST", "/auth/login"), new MockHttpServletResponse(),
                new MockFilterChain());
        filtro.doFilter(peticion("GET", "/actuator/health"), new MockHttpServletResponse(),
                new MockFilterChain());

        verify(auditoria, never()).registrarActividad(any());
    }

    @Test
    @DisplayName("no registra las respuestas 429 del límite de peticiones")
    void omite429() throws Exception {
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        respuesta.setStatus(429);

        filtro.doFilter(peticion("GET", "/publico/consulta"), respuesta, new MockFilterChain());

        verify(auditoria, never()).registrarActividad(any());
    }
}
