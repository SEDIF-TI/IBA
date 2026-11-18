package mx.gob.sedif.sea.security.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import mx.gob.sedif.sea.IntegracionBase;
import mx.gob.sedif.sea.core.auditoria.AccionAuditoria;
import mx.gob.sedif.sea.security.Usuario;

/**
 * Pruebas de integración del ciclo de vida de la sesión del personal.
 *
 * <p>Flujos cubiertos: inicio de sesión con su registro en bitácora, intentos fallidos y bloqueo,
 * contraseña temporal que no permite operar, rotación del token de refresco con detección de
 * reutilización, cierre de sesión real y cierre forzado al reiniciar la contraseña o dar de baja.
 */
class SesionIntegracionTest extends IntegracionBase {

    @Test
    @DisplayName("un inicio de sesión correcto abre sesión, da cookie HttpOnly acotada y queda en bitácora")
    void inicioDeSesion() throws Exception {
        String password = passwordValida();
        Usuario usuario = crearUsuario("ADMINISTRADOR", password, false);

        Sesion sesion = iniciarSesion(usuario.getUsuario(), password);

        assertThat(sesion.refresco()).isNotNull();
        assertThat(sesion.refresco().isHttpOnly()).isTrue();
        assertThat(sesion.refresco().getPath()).isEqualTo("/api/auth");
        mvc.perform(get("/solicitudes").header("Authorization", sesion.bearer()))
                .andExpect(status().isOk());
        assertThat(bitacora(usuario.getId(), AccionAuditoria.LOGIN)).isEqualTo(1);
    }

    @Test
    @DisplayName("cinco contraseñas incorrectas bloquean la cuenta y cada intento queda numerado")
    void bloqueoPorIntentos() throws Exception {
        Usuario usuario = crearUsuario("ADMINISTRADOR", passwordValida(), false);
        String cuerpo = json.writeValueAsString(
                Map.of("usuario", usuario.getUsuario(), "password", "Incorrecta123"));

        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isLocked());

        assertThat(bitacora(usuario.getId(), AccionAuditoria.LOGIN_FALLIDO)).isEqualTo(6);
        assertThat(bitacora(usuario.getId(), AccionAuditoria.CUENTA_BLOQUEADA)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM bitacora_auditoria WHERE fn_usuario_id = ? AND s_detalle LIKE ?",
                Long.class, usuario.getId(), "%intento 5 de 5%")).isEqualTo(1);
    }

    @Test
    @DisplayName("con la contraseña temporal solo se puede cambiarla; al cambiarla se puede operar")
    void passwordTemporal() throws Exception {
        String temporal = passwordValida();
        Usuario usuario = crearUsuario("ADMINISTRADOR", temporal, true);
        Sesion sesion = iniciarSesion(usuario.getUsuario(), temporal);

        mvc.perform(get("/solicitudes").header("Authorization", sesion.bearer()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        MvcResult cambio = mvc.perform(post("/auth/cambiar-password")
                        .header("Authorization", sesion.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "passwordActual", temporal, "passwordNueva", passwordValida()))))
                .andExpect(status().isOk())
                .andReturn();
        Sesion nueva = sesionDe(cambio);

        mvc.perform(get("/solicitudes").header("Authorization", sesion.bearer()))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/solicitudes").header("Authorization", nueva.bearer()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("la contraseña nueva debe cumplir la política: 12 caracteres con mayúsculas, minúsculas y números")
    void politicaDePassword() throws Exception {
        String password = passwordValida();
        Usuario usuario = crearUsuario("ADMINISTRADOR", password, true);
        Sesion sesion = iniciarSesion(usuario.getUsuario(), password);

        mvc.perform(post("/auth/cambiar-password")
                        .header("Authorization", sesion.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "passwordActual", password, "passwordNueva", "solominusculas"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].campo").value("passwordNueva"));
    }

    @Test
    @DisplayName("el token de refresco rota en cada uso y reutilizar uno viejo cierra la sesión")
    void rotacionYReutilizacion() throws Exception {
        String password = passwordValida();
        Usuario usuario = crearUsuario("ADMINISTRADOR", password, false);
        Sesion inicial = iniciarSesion(usuario.getUsuario(), password);

        MvcResult renovada = mvc.perform(post("/auth/refrescar").cookie(inicial.refresco()))
                .andExpect(status().isOk()).andReturn();
        Sesion segunda = sesionDe(renovada);
        assertThat(segunda.refresco().getValue()).isNotEqualTo(inicial.refresco().getValue());

        mvc.perform(post("/auth/refrescar").cookie(inicial.refresco()))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/refrescar").cookie(segunda.refresco()))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/solicitudes").header("Authorization", segunda.bearer()))
                .andExpect(status().isUnauthorized());
        assertThat(bitacora(usuario.getId(), AccionAuditoria.SESION_REVOCADA)).isEqualTo(1);
    }

    @Test
    @DisplayName("cerrar sesión invalida al instante el token de acceso y el de refresco")
    void cierreDeSesion() throws Exception {
        String password = passwordValida();
        Usuario usuario = crearUsuario("ADMINISTRADOR", password, false);
        Sesion sesion = iniciarSesion(usuario.getUsuario(), password);

        mvc.perform(post("/auth/logout").cookie(sesion.refresco())).andExpect(status().isOk());

        mvc.perform(get("/solicitudes").header("Authorization", sesion.bearer()))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/refrescar").cookie(sesion.refresco()))
                .andExpect(status().isUnauthorized());
        assertThat(bitacora(usuario.getId(), AccionAuditoria.LOGOUT)).isEqualTo(1);
    }

    @Test
    @DisplayName("reiniciar la contraseña o dar de baja la cuenta cierra todas sus sesiones")
    void cierreForzado() throws Exception {
        String passwordAdmin = passwordValida();
        Usuario informatica = crearUsuario("SUPERADMINISTRADOR", passwordAdmin, false);
        Sesion admin = iniciarSesion(informatica.getUsuario(), passwordAdmin);

        String password = passwordValida();
        Usuario usuario = crearUsuario("ADMINISTRADOR", password, false);
        Sesion victima = iniciarSesion(usuario.getUsuario(), password);

        mvc.perform(patch("/usuarios/" + usuario.getId() + "/reiniciar-password")
                        .header("Authorization", admin.bearer()))
                .andExpect(status().isOk());

        mvc.perform(get("/solicitudes").header("Authorization", victima.bearer()))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/refrescar").cookie(victima.refresco()))
                .andExpect(status().isUnauthorized());

        Usuario otra = crearUsuario("ADMINISTRADOR", password, false);
        Sesion otraSesion = iniciarSesion(otra.getUsuario(), password);
        mvc.perform(patch("/usuarios/" + otra.getId() + "/vigencia").param("activa", "false")
                        .header("Authorization", admin.bearer()))
                .andExpect(status().isOk());
        mvc.perform(get("/solicitudes").header("Authorization", otraSesion.bearer()))
                .andExpect(status().isUnauthorized());
    }
}
