package mx.gob.sedif.sea.config;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import mx.gob.sedif.sea.security.UsuarioAutenticado;

/**
 * Provee el usuario responsable de cada alta y cada cambio, para las columnas
 * de auditoria {@code fn_usuario_creacion_id} y {@code fn_usuario_modificacion_id}
 * que lleva toda tabla transaccional.
 *
 * <p>Devuelve vacio cuando no hay sesion. Es el caso normal, no un error: el
 * registro publico es anonimo (la ciudadania no tiene cuenta), asi que las
 * solicitudes que entran por el portal se crean sin usuario responsable.
 */
@Configuration
public class AuditoriaConfig {

    @Bean
    public AuditorAware<Long> auditorAware() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth == null || !auth.isAuthenticated()) {
                return Optional.empty();
            }
            if (auth.getPrincipal() instanceof UsuarioAutenticado usuario) {
                return Optional.ofNullable(usuario.getId());
            }
            return Optional.empty();
        };
    }
}
