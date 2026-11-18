package mx.gob.sedif.sea;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import mx.gob.sedif.sea.security.RolRepository;
import mx.gob.sedif.sea.security.Usuario;
import mx.gob.sedif.sea.security.UsuarioRepository;

import jakarta.servlet.http.Cookie;

/**
 * Base de las pruebas de integración: levanta la aplicación completa contra un PostgreSQL real.
 * Área: Núcleo compartido (pruebas).
 *
 * <p>El contenedor de la base ejecuta el mismo {@code deploy/db/init/01-roles.sh} que producción,
 * así que Flyway migra con la cuenta dueña y la aplicación opera con la cuenta sin superusuario:
 * las pruebas comprueban también que los permisos de la base alcanzan para operar.
 *
 * <p>Ningún secreto está escrito en el código: las contraseñas de la base, el secreto JWT y las
 * contraseñas de las cuentas de prueba se generan al arrancar. El contenedor se comparte entre
 * todas las clases de prueba (patrón singleton de Testcontainers) para no levantarlo varias veces.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegracionBase {

    private static final SecureRandom AZAR = new SecureRandom();

    protected static final String DB_OWNER = "sea_owner";
    protected static final String DB_APP = "sea_app";
    private static final String DB_OWNER_PASSWORD = aleatorio(24);
    private static final String DB_APP_PASSWORD = aleatorio(24);

    /** PostgreSQL de la misma versión que producción, inicializado con el script de cuentas. */
    @SuppressWarnings("resource")
    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("sea")
                    .withUsername("sea_super")
                    .withPassword(aleatorio(24))
                    .withEnv("DB_OWNER_USER", DB_OWNER)
                    .withEnv("DB_OWNER_PASSWORD", DB_OWNER_PASSWORD)
                    .withEnv("DB_APP_USER", DB_APP)
                    .withEnv("DB_APP_PASSWORD", DB_APP_PASSWORD)
                    .withCopyFileToContainer(
                            MountableFile.forHostPath(Path.of("..", "deploy", "db", "init", "01-roles.sh"), 0755),
                            "/docker-entrypoint-initdb.d/01-roles.sh");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) throws Exception {
        registro.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registro.add("spring.datasource.username", () -> DB_APP);
        registro.add("spring.datasource.password", () -> DB_APP_PASSWORD);
        registro.add("spring.flyway.user", () -> DB_OWNER);
        registro.add("spring.flyway.password", () -> DB_OWNER_PASSWORD);
        registro.add("app.jwt.secret", () -> aleatorio(64));
        registro.add("app.admin.usuario", () -> "");
        String almacen = Files.createTempDirectory("sea-docs").toString();
        registro.add("app.storage.path", () -> almacen);
        // Límites altos: el límite de peticiones tiene sus propias pruebas; aquí estorbaría.
        registro.add("app.rate-limit.login", () -> "10000");
        registro.add("app.rate-limit.refresco", () -> "10000");
        registro.add("app.rate-limit.consulta", () -> "10000");
        registro.add("app.rate-limit.publico", () -> "10000");
    }

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @Autowired protected UsuarioRepository usuarioRepository;
    @Autowired protected RolRepository rolRepository;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected JdbcTemplate jdbc;

    /** Texto aleatorio de la longitud indicada, para contraseñas y secretos de prueba. */
    protected static String aleatorio(int bytes) {
        byte[] valor = new byte[bytes];
        AZAR.nextBytes(valor);
        return Base64.getEncoder().encodeToString(valor);
    }

    /** Contraseña aleatoria que cumple la política (mayúsculas, minúsculas y números, 12+). */
    protected static String passwordValida() {
        return "Pr" + UUID.randomUUID().toString().replace("-", "").substring(0, 14) + "9x";
    }

    /**
     * Crea una cuenta lista para operar.
     *
     * @param rol                     clave del rol (ADMINISTRADOR o SUPERADMINISTRADOR)
     * @param password                contraseña en claro
     * @param cambioPasswordRequerido si debe cambiarla antes de operar
     * @return la cuenta guardada
     */
    protected Usuario crearUsuario(String rol, String password, boolean cambioPasswordRequerido) {
        String nombre = "prueba" + UUID.randomUUID().toString().substring(0, 8);
        Usuario usuario = new Usuario();
        usuario.setUsuario(nombre);
        usuario.setPasswordHash(passwordEncoder.encode(password));
        usuario.setNombre("Persona");
        usuario.setApellidoPaterno("De Prueba");
        usuario.setCorreo(nombre + "@prueba.gob.mx");
        usuario.setActivo(true);
        usuario.setCambioPasswordRequerido(cambioPasswordRequerido);
        usuario.setRoles(Set.of(rolRepository.findByClave(rol).orElseThrow()));
        return usuarioRepository.save(usuario);
    }

    /** Respuesta de un inicio de sesión: token de acceso en el cuerpo y cookie de refresco. */
    protected record Sesion(String tokenAcceso, Cookie refresco) {

        public String bearer() {
            return "Bearer " + tokenAcceso;
        }
    }

    /** Inicia sesión y devuelve los tokens; falla la prueba si no responde 200. */
    protected Sesion iniciarSesion(String usuario, String password) throws Exception {
        MvcResult resultado = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("usuario", usuario, "password", password))))
                .andReturn();
        if (resultado.getResponse().getStatus() != 200) {
            throw new AssertionError("El inicio de sesión respondió "
                    + resultado.getResponse().getStatus());
        }
        return sesionDe(resultado);
    }

    /** Extrae token y cookie de una respuesta de login, refresco o cambio de contraseña. */
    protected Sesion sesionDe(MvcResult resultado) throws Exception {
        JsonNode cuerpo = json.readTree(resultado.getResponse().getContentAsString());
        return new Sesion(cuerpo.path("data").path("tokenAcceso").asText(),
                resultado.getResponse().getCookie("sea_refresh"));
    }

    /** Número de registros de bitácora con la acción indicada para una cuenta. */
    protected long bitacora(Long usuarioId, String accion) {
        Long total = jdbc.queryForObject(
                "SELECT count(*) FROM bitacora_auditoria WHERE fn_usuario_id = ? AND s_accion = ?",
                Long.class, usuarioId, accion);
        return total == null ? 0 : total;
    }
}
