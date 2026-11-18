package mx.gob.sedif.sea.security.sesion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.BadCredentialsException;

import mx.gob.sedif.sea.core.auditoria.AccionAuditoria;
import mx.gob.sedif.sea.core.auditoria.AuditoriaService;
import mx.gob.sedif.sea.security.Usuario;
import mx.gob.sedif.sea.security.UsuarioRepository;

/**
 * Pruebas del ciclo de vida de las sesiones: apertura, rotación del token de refresco, detección
 * de reutilización y cierre masivo al cambiar o reiniciar la contraseña.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SesionServiceTest {

    @Mock private SesionUsuarioRepository sesionRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AuditoriaService auditoria;

    private SesionService servicio;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        servicio = new SesionService(sesionRepository, usuarioRepository, auditoria, 604_800_000L);
        usuario = new Usuario();
        usuario.setId(7L);
        when(sesionRepository.save(any(SesionUsuario.class))).thenAnswer(i -> i.getArgument(0));
    }

    /** Sesión abierta cuyo identificador de un solo uso vigente es {@code jti}. */
    private SesionUsuario sesionCon(String jti) {
        SesionUsuario sesion = new SesionUsuario();
        sesion.setIdentificador(UUID.randomUUID());
        sesion.setUsuario(usuario);
        sesion.setHuellaRefresco(SesionService.huella(jti));
        sesion.setFechaCreacion(OffsetDateTime.now());
        sesion.setExpira(OffsetDateTime.now().plusDays(7));
        when(sesionRepository.findByIdentificador(sesion.getIdentificador()))
                .thenReturn(Optional.of(sesion));
        return sesion;
    }

    @Test
    @DisplayName("al abrir sesión solo se guarda la huella del identificador, nunca el valor")
    void abrirGuardaSoloHuella() {
        SesionService.SesionAbierta abierta = servicio.abrir(usuario, "203.0.113.1", "prueba");

        ArgumentCaptor<SesionUsuario> guardada = ArgumentCaptor.forClass(SesionUsuario.class);
        verify(sesionRepository).save(guardada.capture());
        assertThat(guardada.getValue().getHuellaRefresco())
                .isEqualTo(SesionService.huella(abierta.jti()))
                .isNotEqualTo(abierta.jti())
                .matches("[0-9a-f]{64}");
        assertThat(guardada.getValue().getExpira()).isAfter(OffsetDateTime.now().plusDays(6));
    }

    @Test
    @DisplayName("cada renovación entrega un identificador nuevo y el anterior deja de servir")
    void rotaElIdentificador() {
        SesionUsuario sesion = sesionCon("primero");

        SesionService.Rotacion rotacion = servicio.rotar(sesion.getIdentificador(), "primero");

        assertThat(rotacion.nuevoJti()).isNotEqualTo("primero");
        assertThat(sesion.getHuellaRefresco()).isEqualTo(SesionService.huella(rotacion.nuevoJti()));
        assertThat(sesion.estaAbierta()).isTrue();
    }

    @Test
    @DisplayName("usar dos veces el mismo token cierra la sesión y lo deja en la bitácora")
    void reutilizacionCierraLaSesion() {
        SesionUsuario sesion = sesionCon("primero");
        servicio.rotar(sesion.getIdentificador(), "primero");

        assertThatThrownBy(() -> servicio.rotar(sesion.getIdentificador(), "primero"))
                .isInstanceOf(BadCredentialsException.class);

        assertThat(sesion.estaAbierta()).isFalse();
        assertThat(sesion.getMotivoRevocacion()).isEqualTo(SesionService.MOTIVO_REUTILIZACION);
        verify(auditoria).registrarComo(eq(7L), eq(AccionAuditoria.SESION_REVOCADA),
                anyString(), anyLong(), anyString());
    }

    @Test
    @DisplayName("una sesión cerrada no se puede renovar")
    void sesionCerradaNoRenueva() {
        SesionUsuario sesion = sesionCon("vigente");
        sesion.revocar(SesionService.MOTIVO_CIERRE);

        assertThatThrownBy(() -> servicio.rotar(sesion.getIdentificador(), "vigente"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("una sesión inexistente se rechaza")
    void sesionInexistente() {
        when(sesionRepository.findByIdentificador(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.rotar(UUID.randomUUID(), "x"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("cerrar todas las sesiones revoca cada una y sube la versión de tokens")
    void revocarTodasSubeVersion() {
        SesionUsuario una = sesionCon("a");
        SesionUsuario otra = sesionCon("b");
        when(sesionRepository.abiertasDe(7L)).thenReturn(List.of(una, otra));

        int cerradas = servicio.revocarTodas(usuario, SesionService.MOTIVO_REINICIO_PASSWORD);

        assertThat(cerradas).isEqualTo(2);
        assertThat(una.estaAbierta()).isFalse();
        assertThat(otra.estaAbierta()).isFalse();
        assertThat(usuario.getTokenVersion()).isEqualTo(1);
        verify(usuarioRepository).save(usuario);
    }
}
