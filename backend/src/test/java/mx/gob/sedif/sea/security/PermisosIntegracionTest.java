package mx.gob.sedif.sea.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.JsonNode;

import mx.gob.sedif.sea.IntegracionBase;

/**
 * Pruebas de integración de permisos, formato de errores, bitácora y bloqueo optimista.
 *
 * <p>Flujos cubiertos: matriz de permisos por rol probada contra la API (no contra la pantalla),
 * respuestas 401, 403, 404 y 400 en formato {@code ApiResponse}, registro de cada petición en la
 * bitácora sin los valores buscados, bitácora de solo inserción y conflicto de edición (409).
 *
 * <p>Alcance de datos: por decisión del proyecto todo el personal ve el padrón estatal completo, así
 * que no hay registros "fuera de alcance"; un identificador inexistente responde 404 sin revelar
 * detalles internos.
 */
class PermisosIntegracionTest extends IntegracionBase {

    private Sesion iba;
    private Sesion informatica;
    private Usuario cuentaIba;

    @BeforeEach
    void sesiones() throws Exception {
        String password = passwordValida();
        cuentaIba = crearUsuario("ADMINISTRADOR", password, false);
        iba = iniciarSesion(cuentaIba.getUsuario(), password);

        String passwordAdmin = passwordValida();
        Usuario cuentaInformatica = crearUsuario("SUPERADMINISTRADOR", passwordAdmin, false);
        informatica = iniciarSesion(cuentaInformatica.getUsuario(), passwordAdmin);
    }

    @Test
    @DisplayName("sin sesión el panel responde 401 en formato ApiResponse y el portal sigue abierto")
    void sinSesion() throws Exception {
        mvc.perform(get("/solicitudes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").isNotEmpty());
        mvc.perform(get("/publico/catalogos")).andExpect(status().isOk());
        mvc.perform(get("/publico/jornadas")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("el IBA no puede administrar cuentas: 403 aunque llame directo a la API, y queda en bitácora")
    void ibaNoAdministraCuentas() throws Exception {
        Map<String, Object> alta = Map.of(
                "usuario", "nueva.cuenta", "nombre", "Nueva", "apellidoPaterno", "Cuenta",
                "correo", "nueva.cuenta@puebla.gob.mx", "rolesIds", List.of(1));

        mvc.perform(post("/usuarios").header("Authorization", iba.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(alta)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        assertThat(bitacora(cuentaIba.getId(), "ACCESO_DENEGADO")).isEqualTo(1);
        mvc.perform(get("/usuarios").header("Authorization", iba.bearer()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("el IBA ya no tiene el permiso de consultar la bitácora; informática sí")
    void bitacoraSoloInformatica() throws Exception {
        JsonNode permisosIba = json.readTree(mvc.perform(
                        post("/auth/refrescar").cookie(iba.refresco()))
                .andReturn().getResponse().getContentAsString()).path("data").path("permisos");
        JsonNode permisosInformatica = json.readTree(mvc.perform(
                        post("/auth/refrescar").cookie(informatica.refresco()))
                .andReturn().getResponse().getContentAsString()).path("data").path("permisos");

        assertThat(permisosIba.toString()).doesNotContain("bitacora:ver").contains("documento:subir");
        assertThat(permisosInformatica.toString()).contains("bitacora:ver");
    }

    @Test
    @DisplayName("un registro inexistente responde 404 sin detalles internos")
    void noEncontrado() throws Exception {
        mvc.perform(get("/solicitudes/999999").header("Authorization", iba.bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("un JSON mal formado es 400 con mensaje para la persona, no un error del sistema")
    void jsonMalFormado() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("la consulta pública lleva folio y CURP en el cuerpo, nunca en la dirección")
    void consultaPublicaPorPost() throws Exception {
        mvc.perform(get("/publico/consulta").param("folio", "SOL-1").param("curp", "GODE561231HDFRRN09"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.success").value(false));
        mvc.perform(post("/publico/consulta").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("folio", "SOL-20260101-00001"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].campo").value("curp"));
        mvc.perform(post("/publico/consulta").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "folio", "SOL-20260101-99999", "curp", "GODE561231HDFRRN09"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("los mensajes de validación sin texto propio salen en español")
    void validacionEnEspanol() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "usuario", "x".repeat(81), "password", "cualquiera"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].mensaje").value("Debe tener entre 0 y 80 caracteres."));
    }

    @Test
    @DisplayName("cada consulta queda en bitácora con los nombres de los filtros, nunca sus valores")
    void bitacoraDeConsultas() throws Exception {
        mvc.perform(get("/tutores").param("texto", "MUÑOZ").param("curp", "GODE561231HDFRRN09")
                        .header("Authorization", iba.bearer()))
                .andExpect(status().isOk());

        String detalle = jdbc.queryForObject(
                "SELECT s_detalle FROM bitacora_auditoria WHERE fn_usuario_id = ? AND s_accion = 'CONSULTA' "
                        + "AND s_ruta = '/tutores' ORDER BY pn_id DESC LIMIT 1",
                String.class, cuentaIba.getId());
        assertThat(detalle).contains("filtros: curp, texto")
                .doesNotContain("MUÑOZ").doesNotContain("GODE561231HDFRRN09");
    }

    @Test
    @DisplayName("la cuenta de la aplicación no puede modificar ni borrar la bitácora")
    void bitacoraSoloInsercion() {
        assertThatThrownBy(() -> jdbc.update("UPDATE bitacora_auditoria SET s_detalle = 'alterado'"))
                .rootCause().hasMessageContaining("solo inserción");
        assertThatThrownBy(() -> jdbc.update("DELETE FROM bitacora_auditoria"))
                .rootCause().hasMessageContaining("solo inserción");
    }

    @Test
    @DisplayName("guardar con una versión vieja responde 409 y no pisa el cambio de la otra persona")
    void edicionConcurrente() throws Exception {
        JsonNode color = json.readTree(mvc.perform(get("/catalogos/colores/2")
                        .header("Authorization", informatica.bearer()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("data");
        long version = color.path("version").asLong();

        Map<String, Object> cambio = new HashMap<>();
        cambio.put("clave", color.path("clave").asText());
        cambio.put("nombre", color.path("nombre").asText() + " (editado)");
        cambio.put("version", version);
        mvc.perform(put("/catalogos/colores/2").header("Authorization", informatica.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(cambio)))
                .andExpect(status().isOk());

        cambio.put("nombre", "Otro nombre");
        mvc.perform(put("/catalogos/colores/2").header("Authorization", iba.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(cambio)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));

        String anterior = jdbc.queryForObject(
                "SELECT j_valor_anterior::text FROM bitacora_auditoria WHERE s_entidad = 'cat_color' "
                        + "AND s_accion = 'MODIFICAR' ORDER BY pn_id DESC LIMIT 1", String.class);
        assertThat(anterior).contains(color.path("nombre").asText());
    }
}
