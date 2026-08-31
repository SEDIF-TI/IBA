package mx.gob.sedif.sea.config;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import mx.gob.sedif.sea.security.Rol;
import mx.gob.sedif.sea.security.RolRepository;
import mx.gob.sedif.sea.security.Usuario;
import mx.gob.sedif.sea.security.UsuarioRepository;

/**
 * Crea la primera cuenta de acceso al arrancar, si no existe ninguna.
 *
 * <p>Deliberadamente NO se hace en una migracion: el archivo quedaria en el
 * repositorio con la contrasena dentro, para siempre. El sistema anterior
 * publicaba "administrador / 12345678" en su propio manual.
 *
 * <p>La contrasena se toma de una variable de entorno y la cuenta nace con la
 * marca que obliga a cambiarla en el primer acceso.
 */
@Configuration
public class UsuarioInicialConfig {

    private static final Logger log = LoggerFactory.getLogger(UsuarioInicialConfig.class);

    private static final String ROL_INICIAL = "SUPERADMINISTRADOR";

    @Bean
    public ApplicationRunner crearUsuarioInicial(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.usuario:}") String nombreUsuario,
            @Value("${app.admin.password:}") String password,
            @Value("${app.admin.correo:}") String correo) {

        return argumentos -> crear(usuarioRepository, rolRepository, passwordEncoder,
                nombreUsuario, password, correo);
    }

    /**
     * Alta efectiva de la cuenta.
     *
     * <p>Cada repositorio abre su propia transaccion, que basta aqui: es una
     * sola escritura en el arranque.
     */
    private void crear(UsuarioRepository usuarioRepository,
                         RolRepository rolRepository,
                         PasswordEncoder passwordEncoder,
                         String nombreUsuario,
                         String password,
                         String correo) {

        // Si ya hay cuentas, no se toca nada: esto solo actua en la primera
        // puesta en marcha.
        if (usuarioRepository.count() > 0) {
            return;
        }

        if (nombreUsuario == null || nombreUsuario.isBlank()
                || password == null || password.isBlank()) {
            log.warn("""

                    ============================================================
                    NO HAY NINGUNA CUENTA DE ACCESO Y NO SE PUDO CREAR LA INICIAL

                    Define estas variables en el archivo .env y reinicia:
                        APP_ADMIN_USUARIO
                        APP_ADMIN_PASSWORD
                        APP_ADMIN_CORREO
                    ============================================================""");
            return;
        }

        if (password.length() < 12) {
            log.error("""

                    ============================================================
                    LA CONTRASENA INICIAL ES DEMASIADO CORTA

                    APP_ADMIN_PASSWORD debe tener al menos 12 caracteres.
                    No se creo ninguna cuenta.
                    ============================================================""");
            return;
        }

        Rol rol = rolRepository.findByClave(ROL_INICIAL).orElse(null);
        if (rol == null) {
            log.error("No existe el rol {}; revisa que las migraciones se hayan aplicado",
                    ROL_INICIAL);
            return;
        }

        Usuario usuario = new Usuario();
        usuario.setUsuario(nombreUsuario.trim());
        usuario.setPasswordHash(passwordEncoder.encode(password));
        usuario.setNombre("Administrador");
        usuario.setApellidoPaterno("del Sistema");
        usuario.setCorreo(correo == null || correo.isBlank()
                ? nombreUsuario.trim() + "@localhost.local"
                : correo.trim());
        usuario.setActivo(true);
        // Obliga a cambiarla en el primer acceso: la contrasena inicial vive en
        // un archivo de configuracion, asi que no debe seguir siendo valida.
        usuario.setCambioPasswordRequerido(true);
        usuario.setRoles(Set.of(rol));

        usuarioRepository.save(usuario);

        log.info("""

                ============================================================
                CUENTA INICIAL CREADA

                Usuario: {}
                Rol:     {}

                El sistema exigira cambiar la contrasena en el primer acceso.
                ============================================================""",
                usuario.getUsuario(), ROL_INICIAL);
    }
}
