package mx.gob.sedif.sea.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

/**
 * Verifica como se exponen roles y permisos al motor de autorizacion.
 *
 * <p>La distincion no es un detalle: si los permisos llevaran el prefijo
 * ROLE_, ninguna comprobacion de tipo hasAuthority('documento:descargar')
 * llegaria a cumplirse y el personal quedaria sin acceso a su propio trabajo.
 */
class UsuarioAutenticadoTest {

    @Test
    @DisplayName("los roles llevan el prefijo ROLE_ y los permisos no")
    void separaRolesDePermisos() {
        UsuarioAutenticado usuario = new UsuarioAutenticado(
                1L, "jperez", "hash", true, false, true,
                Set.of("ADMINISTRADOR"),
                Set.of("documento:descargar", "solicitud:aprobar"));

        Set<String> autoridades = usuario.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(java.util.stream.Collectors.toSet());

        assertThat(autoridades).contains("ROLE_ADMINISTRADOR");
        assertThat(autoridades)
                .as("el permiso debe quedar tal cual, sin prefijo")
                .contains("documento:descargar", "solicitud:aprobar");
        assertThat(autoridades)
                .as("ningun permiso debe llevar el prefijo de rol")
                .doesNotContain("ROLE_documento:descargar", "ROLE_solicitud:aprobar");
    }

    @Test
    @DisplayName("refleja el estado de la cuenta")
    void reflejaEstadoDeLaCuenta() {
        UsuarioAutenticado bloqueado = new UsuarioAutenticado(
                1L, "jperez", "hash", true, true, false,
                Set.of("ADMINISTRADOR"), Set.of());

        assertThat(bloqueado.isEnabled()).isTrue();
        assertThat(bloqueado.isAccountNonLocked()).isFalse();
        assertThat(bloqueado.isCambioPasswordRequerido()).isTrue();
    }

    @Test
    @DisplayName("permite descartar el hash tras autenticar")
    void descartaElHash() {
        UsuarioAutenticado usuario = new UsuarioAutenticado(
                1L, "jperez", "$2a$12$hash", true, false, true,
                Set.of("ADMINISTRADOR"), Set.of());

        assertThat(usuario.getPassword()).isNotNull();
        usuario.borrarPassword();
        assertThat(usuario.getPassword()).isNull();
    }
}
