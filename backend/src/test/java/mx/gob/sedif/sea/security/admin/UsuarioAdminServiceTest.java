package mx.gob.sedif.sea.security.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import mx.gob.sedif.sea.core.auditoria.AuditoriaService;
import mx.gob.sedif.sea.core.catalogo.SedeRepository;
import mx.gob.sedif.sea.exception.ReglaNegocioException;
import mx.gob.sedif.sea.security.Rol;
import mx.gob.sedif.sea.security.RolRepository;
import mx.gob.sedif.sea.security.Usuario;
import mx.gob.sedif.sea.security.UsuarioRepository;

/**
 * Reglas del alta de cuentas del personal.
 *
 * <p>Lo que se protege aqui no es comodidad: una cuenta decide sobre tramites
 * de terceros, y dejar el sistema sin quien administre lo vuelve irrecuperable
 * sin tocar la base de datos a mano.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UsuarioAdminServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private RolRepository rolRepository;
    @Mock private SedeRepository sedeRepository;
    @Mock private PasswordEncoder codificador;
    @Mock private AuditoriaService auditoria;

    private UsuarioAdminService servicio;

    @BeforeEach
    void preparar() {
        servicio = new UsuarioAdminService(usuarioRepository, rolRepository,
                sedeRepository, codificador, auditoria);
        when(codificador.encode(anyString())).thenReturn("$2a$10$huella");
        when(usuarioRepository.saveAndFlush(any(Usuario.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private Rol rol(Integer id, String clave) {
        Rol rol = new Rol();
        rol.setId(id);
        rol.setClave(clave);
        rol.setNombre(clave);
        return rol;
    }

    private GuardarUsuarioRequest peticion() {
        return new GuardarUsuarioRequest("jperez", "Juan", "Perez", "Gomez",
                "juan@puebla.gob.mx", "2221234567", null, List.of(2));
    }

    @Nested
    @DisplayName("alta de cuentas")
    class Alta {

        @Test
        @DisplayName("genera una contrasena temporal que obliga a cambiarla al entrar")
        void obligaCambioDePassword() {
            when(rolRepository.findById(2)).thenReturn(Optional.of(rol(2, "ADMINISTRADOR")));

            UsuarioAdminService.ResultadoAlta alta = servicio.crear(peticion());

            assertThat(alta.passwordTemporal()).hasSize(12);
            assertThat(alta.usuario().getCambioPasswordRequerido()).isTrue();
            // Nunca se guarda en claro: solo su huella.
            assertThat(alta.usuario().getPasswordHash()).isNotEqualTo(alta.passwordTemporal());
        }

        @Test
        @DisplayName("la contrasena temporal evita caracteres que se confunden al dictarla")
        void evitaCaracteresAmbiguos() {
            when(rolRepository.findById(2)).thenReturn(Optional.of(rol(2, "ADMINISTRADOR")));

            for (int i = 0; i < 50; i++) {
                String temporal = servicio.crear(peticion()).passwordTemporal();
                assertThat(temporal).doesNotContain("0", "O", "1", "l", "I");
            }
        }

        @Test
        @DisplayName("rechaza un usuario que ya existe y dice cual es")
        void rechazaUsuarioDuplicado() {
            when(usuarioRepository.existsByUsuarioIgnoreCase("jperez")).thenReturn(true);

            assertThatThrownBy(() -> servicio.crear(peticion()))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("jperez");
        }

        @Test
        @DisplayName("rechaza un correo repetido y sugiere reactivar en vez de duplicar")
        void rechazaCorreoDuplicado() {
            when(usuarioRepository.existsByCorreoIgnoreCase("juan@puebla.gob.mx"))
                    .thenReturn(true);

            assertThatThrownBy(() -> servicio.crear(peticion()))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("reactivala");
        }

        @Test
        @DisplayName("no crea una cuenta sin rol, porque no podria hacer nada")
        void exigeAlMenosUnRol() {
            GuardarUsuarioRequest sinRol = new GuardarUsuarioRequest("jperez", "Juan",
                    "Perez", null, "juan@puebla.gob.mx", null, null, List.of());

            assertThatThrownBy(() -> servicio.crear(sinRol))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("al menos un rol");
        }
    }

    @Nested
    @DisplayName("bajas")
    class Bajas {

        private Usuario cuentaCon(String clave) {
            Usuario usuario = new Usuario();
            usuario.setId(1L);
            usuario.setUsuario("superadmin");
            Set<Rol> roles = new HashSet<>();
            roles.add(rol(1, clave));
            usuario.setRoles(roles);
            usuario.setActivo(true);
            return usuario;
        }

        @Test
        @DisplayName("no deja dar de baja al ultimo superadministrador")
        void protegeAlUltimoSuperadministrador() {
            Usuario ultimo = cuentaCon("SUPERADMINISTRADOR");
            when(usuarioRepository.findConRolesPorId(1L)).thenReturn(Optional.of(ultimo));
            when(usuarioRepository.contarActivosPorRol("SUPERADMINISTRADOR")).thenReturn(1L);

            assertThatThrownBy(() -> servicio.cambiarVigencia(1L, false))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("unica cuenta de superadministrador");
        }

        @Test
        @DisplayName("si hay otro superadministrador, si permite la baja")
        void permiteLaBajaSiQuedaOtro() {
            Usuario uno = cuentaCon("SUPERADMINISTRADOR");
            when(usuarioRepository.findConRolesPorId(1L)).thenReturn(Optional.of(uno));
            when(usuarioRepository.contarActivosPorRol("SUPERADMINISTRADOR")).thenReturn(2L);

            Usuario resultado = servicio.cambiarVigencia(1L, false);

            assertThat(resultado.getActivo()).isFalse();
        }

        @Test
        @DisplayName("reactivar limpia el bloqueo por intentos fallidos")
        void reactivarLimpiaElBloqueo() {
            Usuario bloqueada = cuentaCon("ADMINISTRADOR");
            bloqueada.setActivo(false);
            bloqueada.setIntentosFallidos(5);
            when(usuarioRepository.findConRolesPorId(1L)).thenReturn(Optional.of(bloqueada));

            Usuario resultado = servicio.cambiarVigencia(1L, true);

            assertThat(resultado.getActivo()).isTrue();
            assertThat(resultado.getIntentosFallidos()).isZero();
            assertThat(resultado.getBloqueoHasta()).isNull();
        }
    }

    @Nested
    @DisplayName("reinicio de contrasena")
    class Reinicio {

        @Test
        @DisplayName("desbloquea la cuenta: la contrasena vieja ya no sirve")
        void reiniciarLiberaElBloqueo() {
            Usuario usuario = new Usuario();
            usuario.setId(3L);
            usuario.setUsuario("mlopez");
            usuario.setIntentosFallidos(5);
            usuario.setRoles(new HashSet<>(Set.of(rol(2, "ADMINISTRADOR"))));
            when(usuarioRepository.findConRolesPorId(3L)).thenReturn(Optional.of(usuario));

            UsuarioAdminService.ResultadoAlta alta = servicio.reiniciarPassword(3L);

            assertThat(alta.passwordTemporal()).hasSize(12);
            assertThat(alta.usuario().getIntentosFallidos()).isZero();
            assertThat(alta.usuario().getBloqueoHasta()).isNull();
            assertThat(alta.usuario().getCambioPasswordRequerido()).isTrue();
        }
    }
}
